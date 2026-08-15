package com.prontuario.glasses.device

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.meta.wearable.dat.camera.Camera
import com.meta.wearable.dat.camera.addCamera
import com.meta.wearable.dat.camera.types.PhotoData
import com.meta.wearable.dat.camera.types.StreamConfiguration
import com.meta.wearable.dat.camera.types.VideoQuality
import com.meta.wearable.dat.core.Wearables
import com.meta.wearable.dat.core.selectors.AutoDeviceSelector
import com.meta.wearable.dat.core.session.DeviceSession
import com.meta.wearable.dat.core.session.DeviceSessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Gateway DAT 0.9.0 real. ⚠️ AND-07: AINDA NÃO COMPILADO contra os artefatos Maven (sem token
 * na máquina em 15/08). Escrito a partir do sample oficial CameraAccess; assinaturas de
 * DatResult/PhotoData devem ser validadas na primeira compilação real.
 *
 * Ordem obrigatória (MEMORY.md §4): a rota HFP/SCO é responsabilidade do AudioRouteManager e
 * deve estar estável ANTES de startVideo()/capturePhoto().
 */
class DatDeviceGateway(private val context: Context) : DeviceGateway {

    companion object {
        private const val TAG = "DatDeviceGateway"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val deviceSelector = AutoDeviceSelector()

    private var session: DeviceSession? = null
    private var camera: Camera? = null

    private val _sessionState = MutableStateFlow(GatewaySessionState.IDLE)
    override val sessionState: StateFlow<GatewaySessionState> = _sessionState.asStateFlow()

    private val _isVideoActive = MutableStateFlow(false)
    override val isVideoActive: StateFlow<Boolean> = _isVideoActive.asStateFlow()

    private val _events = MutableSharedFlow<GatewayEvent>(
        extraBufferCapacity = 32,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val events: Flow<GatewayEvent> = _events.asSharedFlow()

    private val _videoFrames = MutableSharedFlow<CompressedFrame>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val videoFrames: Flow<CompressedFrame> = _videoFrames.asSharedFlow()

    override suspend fun startSession(): Result<Unit> =
        suspendCancellableCoroutine { continuation ->
            _sessionState.value = GatewaySessionState.STARTING
            Wearables.createSession(deviceSelector)
                .onSuccess { created ->
                    session = created
                    observeSession(created)
                    created.start()
                    continuation.resume(Result.success(Unit))
                }
                .onFailure { error, _ ->
                    Log.e(TAG, "createSession falhou: ${error.description}")
                    _sessionState.value = GatewaySessionState.IDLE
                    continuation.resume(Result.failure(IllegalStateException(error.description)))
                }
        }

    private fun observeSession(session: DeviceSession) {
        scope.launch {
            session.state.collect { state ->
                _sessionState.value = when (state) {
                    DeviceSessionState.IDLE -> GatewaySessionState.IDLE
                    DeviceSessionState.STARTING -> GatewaySessionState.STARTING
                    DeviceSessionState.STARTED -> GatewaySessionState.STARTED
                    DeviceSessionState.PAUSED -> GatewaySessionState.PAUSED
                    DeviceSessionState.STOPPING -> GatewaySessionState.STOPPING
                    DeviceSessionState.STOPPED -> GatewaySessionState.STOPPED
                }
            }
        }
        scope.launch {
            session.errors.collect { error ->
                // Mapeamento HW-03: erros tipados de bateria/térmico alimentam a escada L0–L4
                val name = error.toString()
                val event = when {
                    name.contains("BATTERY_CRITICAL") -> GatewayEvent.BatteryCritical
                    name.contains("PEAK_POWER") -> GatewayEvent.PeakPowerShutdown
                    name.contains("THERMAL_CRITICAL") || name.contains("THERMAL_EMERGENCY") ->
                        GatewayEvent.ThermalWarning(level = 2)
                    else -> GatewayEvent.StreamFailure(name, critical = false)
                }
                _events.tryEmit(event)
            }
        }
    }

    override suspend fun stopSession() {
        camera?.let { runCatching { it.stop() } }
        camera = null
        session?.stop()
        session = null
    }

    override suspend fun startVideo(config: VideoConfig): Result<Unit> {
        val activeSession = session
            ?: return Result.failure(IllegalStateException("Sessão DAT não iniciada"))
        val streamConfiguration = StreamConfiguration(
            videoQuality = config.quality.toDatQuality(),
            frameRate = config.frameRate,
            compressVideo = true,
        )
        return suspendCancellableCoroutine { continuation ->
            activeSession.addCamera(streamConfiguration)
                .onSuccess { createdCamera ->
                    camera = createdCamera
                    val stream = createdCamera.stream
                    scope.launch {
                        stream.videoStream.collect { frame ->
                            val bytes = ByteArray(frame.buffer.remaining())
                            frame.buffer.get(bytes)
                            _videoFrames.tryEmit(
                                CompressedFrame(
                                    data = bytes,
                                    presentationTimeUs = frame.presentationTimeUs,
                                    width = frame.width,
                                    height = frame.height,
                                    isCodecConfig = frame.isCodecConfig,
                                ),
                            )
                        }
                    }
                    scope.launch {
                        stream.errorStream.collect { error ->
                            _events.tryEmit(
                                GatewayEvent.StreamFailure(
                                    error.toString(),
                                    critical = error.toString().contains("CRITICAL"),
                                ),
                            )
                        }
                    }
                    stream.start()
                    _isVideoActive.value = true
                    continuation.resume(Result.success(Unit))
                }
                .onFailure { error, _ ->
                    continuation.resume(Result.failure(IllegalStateException(error.description)))
                }
        }
    }

    override suspend fun stopVideo() {
        camera?.let { runCatching { it.stop() } }
        camera = null
        _isVideoActive.value = false
    }

    override suspend fun capturePhoto(): Result<Bitmap> {
        val stream = camera?.stream
            ?: return Result.failure(IllegalStateException("Câmera não anexada — foto pontual exige addCamera antes"))
        return suspendCancellableCoroutine { continuation ->
            scope.launch {
                stream.capturePhoto()
                    .onSuccess { photoData ->
                        when (photoData) {
                            is PhotoData.Bitmap ->
                                continuation.resume(Result.success(photoData.bitmap))
                            else ->
                                continuation.resume(
                                    Result.failure(IllegalStateException("Formato de foto não suportado: $photoData")),
                                )
                        }
                    }
                    .onFailure { error, _ ->
                        continuation.resume(Result.failure(IllegalStateException(error.description)))
                    }
            }
        }
    }

    private fun VideoQualityProfile.toDatQuality(): VideoQuality = when (this) {
        VideoQualityProfile.LOW -> VideoQuality.LOW
        VideoQualityProfile.MEDIUM -> VideoQuality.MEDIUM
        VideoQualityProfile.HIGH -> VideoQuality.HIGH
    }

    fun dispose() {
        scope.cancel()
    }
}

package com.prontuario.glasses.device

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow

/**
 * Gateway simulado (flavor sim): valida pipeline de chunks/cofre/escada sem óculos nem token
 * (PRG-01, AND-07). NÃO valida coexistência BT — isso só no hardware real (DAT-04).
 * [thermalAfterMs]: injeta THERMAL_CRITICAL após N ms de vídeo (teste da escada ao vivo).
 */
class SimDeviceGateway(private val thermalAfterMs: Long? = null) : DeviceGateway {

    private val _sessionState = MutableStateFlow(GatewaySessionState.IDLE)
    override val sessionState: StateFlow<GatewaySessionState> = _sessionState.asStateFlow()

    private val _isVideoActive = MutableStateFlow(false)
    override val isVideoActive: StateFlow<Boolean> = _isVideoActive.asStateFlow()

    private val _events = MutableSharedFlow<GatewayEvent>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val events: Flow<GatewayEvent> = _events.asSharedFlow()

    private var videoConfig: VideoConfig = VideoConfig()

    override val videoFrames: Flow<CompressedFrame> = flow {
        var pts = 0L
        var counter = 0
        var thermalFired = false
        while (_isVideoActive.value) {
            val intervalUs = 1_000_000L / videoConfig.frameRate
            // Payload sintético (~2 KB) só para exercitar chunking/cifra; formato real depende de DAT-10
            val payload = ByteArray(2048) { ((counter + it) % 251).toByte() }
            emit(
                CompressedFrame(
                    data = payload,
                    presentationTimeUs = pts,
                    width = videoConfig.quality.width,
                    height = videoConfig.quality.height,
                    isCodecConfig = counter == 0,
                ),
            )
            counter++
            pts += intervalUs
            if (!thermalFired && thermalAfterMs != null && pts / 1000 >= thermalAfterMs) {
                thermalFired = true
                _events.tryEmit(GatewayEvent.ThermalWarning(level = 2))
            }
            delay(intervalUs / 1000)
        }
    }

    override suspend fun startSession(): Result<Unit> {
        _sessionState.value = GatewaySessionState.STARTING
        delay(300)
        _sessionState.value = GatewaySessionState.STARTED
        _events.tryEmit(GatewayEvent.Info("Sessão simulada iniciada"))
        return Result.success(Unit)
    }

    override suspend fun stopSession() {
        _isVideoActive.value = false
        _sessionState.value = GatewaySessionState.STOPPING
        delay(100)
        _sessionState.value = GatewaySessionState.STOPPED
    }

    override suspend fun startVideo(config: VideoConfig): Result<Unit> {
        if (_sessionState.value != GatewaySessionState.STARTED) {
            return Result.failure(IllegalStateException("Sessão não iniciada"))
        }
        videoConfig = config
        _isVideoActive.value = true
        return Result.success(Unit)
    }

    override suspend fun stopVideo() {
        _isVideoActive.value = false
    }

    override suspend fun capturePhoto(): Result<Bitmap> {
        if (_sessionState.value != GatewaySessionState.STARTED) {
            return Result.failure(IllegalStateException("Sessão não iniciada"))
        }
        val bitmap = Bitmap.createBitmap(720, 1280, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            drawColor(Color.DKGRAY)
            drawText(
                "SIM ${System.currentTimeMillis()}",
                40f,
                640f,
                Paint().apply {
                    color = Color.WHITE
                    textSize = 48f
                },
            )
        }
        return Result.success(bitmap)
    }

    /** Injeção de eventos para testar a escada de degradação (spec §3). */
    fun inject(event: GatewayEvent) {
        _events.tryEmit(event)
    }
}

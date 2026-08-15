package com.prontuario.glasses.device

import android.graphics.Bitmap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

enum class GatewaySessionState { IDLE, STARTING, STARTED, PAUSED, STOPPING, STOPPED }

/** Resoluções reais entregues pelo DAT (docs/LIMITACOES.md DAT-02). */
enum class VideoQualityProfile(val width: Int, val height: Int) {
    LOW(360, 640),
    MEDIUM(504, 896),
    HIGH(720, 1280),
}

/** Baseline da spec de gravação de segurança: LOW @ 7 fps (analogia CCTV). */
data class VideoConfig(
    val quality: VideoQualityProfile = VideoQualityProfile.LOW,
    val frameRate: Int = 7,
)

sealed class GatewayEvent {
    data class ThermalWarning(val level: Int) : GatewayEvent()
    data object BatteryCritical : GatewayEvent()
    data object PeakPowerShutdown : GatewayEvent()
    data class StreamFailure(val message: String, val critical: Boolean) : GatewayEvent()
    data class Info(val message: String) : GatewayEvent()
}

class CompressedFrame(
    val data: ByteArray,
    val presentationTimeUs: Long,
    val width: Int,
    val height: Int,
    val isCodecConfig: Boolean,
)

/**
 * Isola o SDK DAT do resto do app (DAT-06): flavor `sim` usa [com.prontuario.glasses.device]
 * simulado; flavor `dat` implementa contra o SDK real 0.9.0.
 */
interface DeviceGateway {
    val sessionState: StateFlow<GatewaySessionState>
    val isVideoActive: StateFlow<Boolean>
    val events: Flow<GatewayEvent>
    val videoFrames: Flow<CompressedFrame>

    suspend fun startSession(): Result<Unit>

    suspend fun stopSession()

    suspend fun startVideo(config: VideoConfig): Result<Unit>

    suspend fun stopVideo()

    /** Foto pontual por comando — comportamento clínico padrão ("câmera como evento"). */
    suspend fun capturePhoto(): Result<Bitmap>
}

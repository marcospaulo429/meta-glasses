package com.prontuario.glasses.capture

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Estado observável do serviço para a UI (evita binder; Activity só coleta o flow). */
object ServiceBus {

    data class CaptureStatus(
        val running: Boolean = false,
        val encounterId: String? = null,
        val route: String = "-",
        val ladder: LadderLevel = LadderLevel.L0,
        val audioChunks: Int = 0,
        val videoChunks: Int = 0,
        val videoActive: Boolean = false,
        val lastEvent: String = "-",
        val asrAvailable: Boolean = false,
        val partialText: String = "",
        val segments: Int = 0,
        val photos: Int = 0,
        val phoneBatteryStartPct: Int = -1,
        val draftReady: Boolean = false,
    )

    private val _status = MutableStateFlow(CaptureStatus())
    val status: StateFlow<CaptureStatus> = _status.asStateFlow()

    fun update(transform: (CaptureStatus) -> CaptureStatus) {
        _status.value = transform(_status.value)
    }

    fun reset() {
        _status.value = CaptureStatus()
    }
}

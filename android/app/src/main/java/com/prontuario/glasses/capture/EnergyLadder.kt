package com.prontuario.glasses.capture

import com.prontuario.glasses.device.GatewayEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Escada de degradação L0–L4 (spec arquiteto-android §3).
 * Invariante: áudio clínico é a última coisa a cair. Descida automática; subida só manual.
 */
enum class LadderLevel(val rank: Int, val description: String) {
    L0(0, "normal: áudio + vídeo mínimo + foto"),
    L1(1, "economia: fps reduzido"),
    L2(2, "vídeo encerrado; câmera só por evento"),
    L3(3, "câmera removida; só áudio HFP"),
    L4(4, "fallback: mic do telefone"),
}

class EnergyLadder {

    private val _level = MutableStateFlow(LadderLevel.L0)
    val level: StateFlow<LadderLevel> = _level.asStateFlow()

    fun onGatewayEvent(event: GatewayEvent) {
        when (event) {
            is GatewayEvent.ThermalWarning -> if (event.level >= 2) demoteTo(LadderLevel.L2) else demoteTo(LadderLevel.L1)
            is GatewayEvent.BatteryCritical -> demoteTo(LadderLevel.L3)
            is GatewayEvent.PeakPowerShutdown -> demoteTo(LadderLevel.L4)
            is GatewayEvent.StreamFailure ->
                // 1º sinal de instabilidade SCO/stream → sacrificar vídeo (DAT-04/DAT-11)
                if (event.critical) demoteTo(LadderLevel.L3) else demoteTo(LadderLevel.L2)
            is GatewayEvent.Info -> Unit
        }
    }

    fun onGlassesBatteryPct(pct: Int) {
        when {
            pct < 25 -> demoteTo(LadderLevel.L2)
            pct < 40 -> demoteTo(LadderLevel.L1)
        }
    }

    fun onDeviceLost() = demoteTo(LadderLevel.L4)

    /** Subida somente manual/entre consultas (spec §3). */
    fun resetManually() {
        _level.value = LadderLevel.L0
    }

    private fun demoteTo(target: LadderLevel) {
        if (target.rank > _level.value.rank) _level.value = target
    }
}

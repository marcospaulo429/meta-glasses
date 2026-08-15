package com.prontuario.glasses.capture

import com.prontuario.glasses.device.GatewayEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class EnergyLadderTest {

    @Test
    fun `comeca em L0`() {
        assertEquals(LadderLevel.L0, EnergyLadder().level.value)
    }

    @Test
    fun `thermal leve desce para L1`() {
        val ladder = EnergyLadder()
        ladder.onGatewayEvent(GatewayEvent.ThermalWarning(level = 1))
        assertEquals(LadderLevel.L1, ladder.level.value)
    }

    @Test
    fun `instabilidade de stream nao critica encerra video (L2)`() {
        val ladder = EnergyLadder()
        ladder.onGatewayEvent(GatewayEvent.StreamFailure("GATT timeout", critical = false))
        assertEquals(LadderLevel.L2, ladder.level.value)
    }

    @Test
    fun `bateria critica preserva apenas audio (L3)`() {
        val ladder = EnergyLadder()
        ladder.onGatewayEvent(GatewayEvent.BatteryCritical)
        assertEquals(LadderLevel.L3, ladder.level.value)
    }

    @Test
    fun `peak power cai para mic do telefone (L4)`() {
        val ladder = EnergyLadder()
        ladder.onGatewayEvent(GatewayEvent.PeakPowerShutdown)
        assertEquals(LadderLevel.L4, ladder.level.value)
    }

    @Test
    fun `nunca sobe automaticamente`() {
        val ladder = EnergyLadder()
        ladder.onGatewayEvent(GatewayEvent.BatteryCritical)
        ladder.onGatewayEvent(GatewayEvent.ThermalWarning(level = 1))
        assertEquals(LadderLevel.L3, ladder.level.value)
    }

    @Test
    fun `reset manual volta a L0`() {
        val ladder = EnergyLadder()
        ladder.onGatewayEvent(GatewayEvent.PeakPowerShutdown)
        ladder.resetManually()
        assertEquals(LadderLevel.L0, ladder.level.value)
    }

    @Test
    fun `bateria baixa dos oculos degrada gradualmente`() {
        val ladder = EnergyLadder()
        ladder.onGlassesBatteryPct(35)
        assertEquals(LadderLevel.L1, ladder.level.value)
        ladder.onGlassesBatteryPct(20)
        assertEquals(LadderLevel.L2, ladder.level.value)
    }
}

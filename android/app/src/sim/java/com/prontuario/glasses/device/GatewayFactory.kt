package com.prontuario.glasses.device

import android.content.Context

object GatewayFactory {
    fun create(context: Context): DeviceGateway {
        // Teste da escada ao vivo: prefs sim_thermal_after_s > 0 injeta THERMAL após N s de vídeo
        val thermalAfterS = context.getSharedPreferences("feature_flags", Context.MODE_PRIVATE)
            .getInt("sim_thermal_after_s", 0)
        return SimDeviceGateway(thermalAfterMs = if (thermalAfterS > 0) thermalAfterS * 1000L else null)
    }
}

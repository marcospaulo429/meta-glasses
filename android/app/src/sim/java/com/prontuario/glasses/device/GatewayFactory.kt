package com.prontuario.glasses.device

import android.content.Context

object GatewayFactory {
    fun create(@Suppress("UNUSED_PARAMETER") context: Context): DeviceGateway = SimDeviceGateway()
}

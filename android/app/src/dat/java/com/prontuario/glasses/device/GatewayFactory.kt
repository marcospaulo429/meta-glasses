package com.prontuario.glasses.device

import android.content.Context

object GatewayFactory {
    fun create(context: Context): DeviceGateway = DatDeviceGateway(context)
}

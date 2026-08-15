package com.prontuario.glasses.audio

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager

/**
 * Rota de áudio HFP/SCO conforme decisão consolidada (MEMORY.md §4):
 * MODE_IN_COMMUNICATION + setCommunicationDevice; nunca startBluetoothSco() (AND-04).
 */
class AudioRouteManager(context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    /** @return true se a rota SCO foi selecionada; false = fallback mic do telefone (nível L4). */
    fun selectBluetoothScoRoute(): Boolean {
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        val scoDevice = audioManager.availableCommunicationDevices
            .firstOrNull { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO }
            ?: return false
        return audioManager.setCommunicationDevice(scoDevice)
    }

    fun currentRouteLabel(): String {
        val device = audioManager.communicationDevice ?: return "mic do telefone"
        return when (device.type) {
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "óculos (Bluetooth SCO)"
            AudioDeviceInfo.TYPE_BUILTIN_MIC -> "mic do telefone"
            else -> device.productName?.toString() ?: "desconhecida"
        }
    }

    fun isScoActive(): Boolean =
        audioManager.communicationDevice?.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO

    /** Encerramento ordenado (MEMORY.md §4): limpar communication device e voltar a MODE_NORMAL. */
    fun release() {
        audioManager.clearCommunicationDevice()
        audioManager.mode = AudioManager.MODE_NORMAL
    }
}

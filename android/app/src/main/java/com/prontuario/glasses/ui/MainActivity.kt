package com.prontuario.glasses.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.switchmaterial.SwitchMaterial
import com.prontuario.glasses.capture.ConsultationCaptureService
import com.prontuario.glasses.capture.ServiceBus
import com.prontuario.glasses.config.FeatureFlags
import com.prontuario.glasses.encounter.ConsentRecord
import kotlinx.coroutines.launch

/**
 * Companion mínimo: consentimento → iniciar (com Activity visível, AND-01) → status.
 * Revisão SOAP entra na Fase B (docs/PLANO.md).
 */
class MainActivity : AppCompatActivity() {

    private lateinit var statusView: TextView
    private lateinit var patientConsent: SwitchMaterial
    private lateinit var companionPresent: SwitchMaterial
    private lateinit var companionConsent: SwitchMaterial
    private lateinit var videoConsent: SwitchMaterial
    private lateinit var startButton: Button
    private lateinit var stopButton: Button

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            startButton.isEnabled = grants[Manifest.permission.RECORD_AUDIO] == true
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildLayout())

        startButton.setOnClickListener { startCapture() }
        stopButton.setOnClickListener { ConsultationCaptureService.stop(this) }

        lifecycleScope.launch {
            ServiceBus.status.collect { status ->
                statusView.text = buildString {
                    appendLine("Em captura: ${if (status.running) "SIM" else "não"}")
                    appendLine("Consulta: ${status.encounterId ?: "-"}")
                    appendLine("Rota de áudio: ${status.route}")
                    appendLine("Nível de energia: ${status.ladder.name} — ${status.ladder.description}")
                    appendLine("Chunks de áudio: ${status.audioChunks}")
                    appendLine("Vídeo de segurança: ${if (status.videoActive) "ativo (${status.videoChunks} chunks)" else "desligado"}")
                    appendLine("Último evento: ${status.lastEvent}")
                }
                startButton.isEnabled = !status.running && hasAudioPermission()
                stopButton.isEnabled = status.running
            }
        }

        requestPermissionsIfNeeded()
    }

    private fun startCapture() {
        if (!patientConsent.isChecked) {
            statusView.text = "⚠️ Sem consentimento do paciente a captura não inicia (docs/LGPD.md §3)."
            return
        }
        val consent = ConsentRecord(
            patientConsented = patientConsent.isChecked,
            companionPresent = companionPresent.isChecked,
            companionConsented = companionConsent.isChecked,
            securityVideoConsented = videoConsent.isChecked,
        )
        ConsultationCaptureService.start(this, consent)
    }

    private fun hasAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    private fun requestPermissionsIfNeeded() {
        val needed = listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.POST_NOTIFICATIONS,
        ).filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) permissionLauncher.launch(needed.toTypedArray())
    }

    private fun buildLayout(): View {
        val density = resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()

        patientConsent = SwitchMaterial(this).apply { text = "Paciente informado e consentiu" }
        companionPresent = SwitchMaterial(this).apply { text = "Acompanhante presente" }
        companionConsent = SwitchMaterial(this).apply { text = "Acompanhante consentiu" }
        videoConsent = SwitchMaterial(this).apply {
            text = "Vídeo de segurança (consentimento específico)"
            visibility = if (FeatureFlags.securityVideoEnabled(context)) View.VISIBLE else View.GONE
        }
        startButton = Button(this).apply { text = "Iniciar consulta" }
        stopButton = Button(this).apply {
            text = "Encerrar consulta"
            isEnabled = false
        }
        statusView = TextView(this).apply {
            text = "Pronto."
            setPadding(0, dp(16), 0, 0)
        }

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(24), dp(24), dp(24))
            addView(TextView(context).apply {
                text = "Assistente de Prontuário — captura"
                textSize = 20f
            })
            addView(patientConsent)
            addView(companionPresent)
            addView(companionConsent)
            addView(videoConsent)
            addView(startButton)
            addView(stopButton)
            addView(statusView)
        }
        return ScrollView(this).apply { addView(column) }
    }
}

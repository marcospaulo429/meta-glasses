package com.prontuario.glasses.ui

import android.Manifest
import android.content.Intent
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
import com.prontuario.glasses.BuildConfig
import com.prontuario.glasses.capture.ConsultationCaptureService
import com.prontuario.glasses.capture.ServiceBus
import com.prontuario.glasses.config.FeatureFlags
import com.prontuario.glasses.encounter.ConsentRecord
import com.prontuario.glasses.vault.RecoveryKeyStore
import java.io.File
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
                    appendLine("Consulta: ${status.encounterId?.take(8) ?: "-"}")
                    appendLine("Rota de áudio: ${status.route}")
                    appendLine("ASR: ${if (status.asrAvailable) "ativo" else "modelo não instalado (scripts/install-vosk-model.sh)"}")
                    appendLine("Nível de energia: ${status.ladder.name} — ${status.ladder.description}")
                    appendLine("Bateria do telefone no início: ${if (status.phoneBatteryStartPct >= 0) "${status.phoneBatteryStartPct}%" else "-"}")
                    appendLine("Chunks de áudio: ${status.audioChunks} · Segmentos: ${status.segments} · Fotos: ${status.photos}")
                    appendLine("Vídeo de segurança: ${if (status.videoActive) "ativo (${status.videoChunks} chunks, blindado)" else "desligado"}")
                    appendLine("Rascunho: ${if (status.draftReady) "pronto para revisão ✅" else "-"}")
                    if (status.partialText.isNotBlank()) appendLine("Ouvindo: “${status.partialText}”")
                    appendLine("Último evento: ${status.lastEvent}")
                }
                startButton.isEnabled = !status.running && hasAudioPermission()
                stopButton.isEnabled = status.running
            }
        }

        requestPermissionsIfNeeded()

        // HARNESS (debug): adb shell am start ... --ez auto_start true
        if (BuildConfig.DEBUG && intent.getBooleanExtra("auto_start", false)) {
            patientConsent.isChecked = true
            startCapture()
        }
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
        val custodianButton = Button(this).apply {
            text = "Gerar chave do custodiante (demo)"
            visibility = videoConsent.visibility
            setOnClickListener {
                // DEMO: em produção o par nasce fora do aparelho (SEC-01)
                val pem = RecoveryKeyStore.generateDemoPair(context)
                val out = File(getExternalFilesDir(null), "custodian_private_key.pem")
                out.writeText(pem)
                statusView.text =
                    "Chave privada do custodiante exportada UMA vez para:\n${out.absolutePath}\n" +
                    "Entregue ao custodiante e apague do aparelho. Vídeo agora grava em modo blindado."
            }
        }
        startButton = Button(this).apply { text = "Iniciar consulta" }
        stopButton = Button(this).apply {
            text = "Encerrar consulta"
            isEnabled = false
        }
        val reviewButton = Button(this).apply {
            text = "Revisar última consulta"
            setOnClickListener { startActivity(Intent(context, ReviewActivity::class.java)) }
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
            addView(custodianButton)
            addView(startButton)
            addView(stopButton)
            addView(reviewButton)
            addView(statusView)
        }
        return ScrollView(this).apply { addView(column) }
    }
}

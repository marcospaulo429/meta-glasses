package com.prontuario.glasses.capture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.os.BatteryManager
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
import com.prontuario.glasses.R
import com.prontuario.glasses.asr.StreamingTranscriber
import com.prontuario.glasses.asr.VoiceCommandDetector
import com.prontuario.glasses.asr.VoskSpeechPort
import com.prontuario.glasses.audio.AudioRouteManager
import com.prontuario.glasses.audio.ConsultationAudioRecorder
import com.prontuario.glasses.config.FeatureFlags
import com.prontuario.glasses.device.DeviceGateway
import com.prontuario.glasses.device.GatewayFactory
import com.prontuario.glasses.device.VideoConfig
import com.prontuario.glasses.encounter.ChunkKind
import com.prontuario.glasses.encounter.ConsentRecord
import com.prontuario.glasses.encounter.Encounter
import com.prontuario.glasses.encounter.EncounterRepository
import com.prontuario.glasses.soap.HeuristicSoapClassifier
import com.prontuario.glasses.soap.PassthroughFactExtractor
import com.prontuario.glasses.soap.ProvenanceValidator
import com.prontuario.glasses.soap.SoapJson
import com.prontuario.glasses.soap.TranscriptSegment
import com.prontuario.glasses.ui.MainActivity
import com.prontuario.glasses.vault.RecoveryKeyStore
import com.prontuario.glasses.vault.SecurityVault
import com.prontuario.glasses.vault.WrapPolicy
import java.io.DataOutputStream
import java.io.File
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

/**
 * Foreground Service de captura (microphone|connectedDevice — spec §6).
 * Fluxo obrigatório (AND-01): UI visível → consentimento → startForegroundService() → captura.
 */
class ConsultationCaptureService : Service() {

    companion object {
        private const val TAG = "CaptureService"
        private const val CHANNEL_ID = "capture_channel"
        private const val NOTIFICATION_ID = 2001
        private const val ACTION_START = "com.prontuario.glasses.capture.START"
        private const val ACTION_STOP = "com.prontuario.glasses.capture.STOP"
        private const val CHUNK_MS = 60_000L

        const val EXTRA_PATIENT_CONSENT = "patient_consent"
        const val EXTRA_COMPANION_PRESENT = "companion_present"
        const val EXTRA_COMPANION_CONSENT = "companion_consent"
        const val EXTRA_VIDEO_CONSENT = "video_consent"

        fun start(context: Context, consent: ConsentRecord) {
            val intent = Intent(context, ConsultationCaptureService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_PATIENT_CONSENT, consent.patientConsented)
                putExtra(EXTRA_COMPANION_PRESENT, consent.companionPresent)
                putExtra(EXTRA_COMPANION_CONSENT, consent.companionConsented)
                putExtra(EXTRA_VIDEO_CONSENT, consent.securityVideoConsented)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            // STOP via onStartCommand para nunca quebrar o contrato do startForegroundService
            val intent = Intent(context, ConsultationCaptureService::class.java).apply {
                action = ACTION_STOP
            }
            context.startForegroundService(intent)
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var repository: EncounterRepository
    private lateinit var routeManager: AudioRouteManager
    private var gateway: DeviceGateway? = null
    private var recorder: ConsultationAudioRecorder? = null
    private var encounter: Encounter? = null
    private var tts: TextToSpeech? = null

    private val ladder = EnergyLadder()
    private val pcmBuffer = PcmChunkBuffer()
    private var audioSeq = 0
    private var videoSeq = 0
    private var photoSeq = 0
    private var chunkJob: Job? = null
    private var videoJob: Job? = null
    private var videoChunkFile: File? = null
    private var videoChunkStream: DataOutputStream? = null

    private var transcriber: StreamingTranscriber? = null
    private val segments = mutableListOf<TranscriptSegment>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        repository = EncounterRepository(this)
        routeManager = AudioRouteManager(this)
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) tts?.language = Locale("pt", "BR")
        }
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Foreground primeiro, sempre — mesmo para STOP (contrato Android 14+)
        try {
            startForeground(
                NOTIFICATION_ID,
                buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao entrar em foreground", e)
            stopSelf()
            return START_NOT_STICKY
        }

        when (intent?.action) {
            ACTION_START -> startCapture(intent)
            ACTION_STOP -> {
                stopCapture()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startCapture(intent: Intent) {
        if (ServiceBus.status.value.running) return

        val consent = ConsentRecord(
            patientConsented = intent.getBooleanExtra(EXTRA_PATIENT_CONSENT, false),
            companionPresent = intent.getBooleanExtra(EXTRA_COMPANION_PRESENT, false),
            companionConsented = intent.getBooleanExtra(EXTRA_COMPANION_CONSENT, false),
            securityVideoConsented = intent.getBooleanExtra(EXTRA_VIDEO_CONSENT, false),
        )
        val created = try {
            repository.create(consent)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Consentimento inválido: ${e.message}")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        encounter = created
        ladder.resetManually()
        segments.clear()
        audioSeq = 0
        videoSeq = 0
        photoSeq = 0

        // ASR opcional: sem modelo Vosk instalado o app segue capturando (IA-03)
        val commandDetector = VoiceCommandDetector(
            onPhotoCommand = { capturePhotoByVoice() },
            onStopCommand = { stop(applicationContext) },
        )
        val speechPort = VoskSpeechPort.tryCreate(File(getExternalFilesDir(null), "models/vosk-pt"))
        transcriber = speechPort?.let { port ->
            StreamingTranscriber(
                port = port,
                onSegment = { segment ->
                    synchronized(segments) { segments.add(segment) }
                    ServiceBus.update { it.copy(segments = segments.size, partialText = "") }
                },
                onPartial = { partial ->
                    commandDetector.onPartial(partial)
                    ServiceBus.update { it.copy(partialText = partial) }
                },
                onUtteranceEnd = commandDetector::onUtteranceEnd,
            )
        }

        // Rota HFP primeiro, câmera depois (MEMORY.md §4: ordem de inicialização)
        val scoOk = routeManager.selectBluetoothScoRoute()
        if (!scoOk) {
            ladder.onDeviceLost()
            speak("Óculos não encontrados. Usando microfone do telefone.")
        }

        recorder = ConsultationAudioRecorder { buffer, read ->
            pcmBuffer.append(buffer, read)
            transcriber?.feed(buffer, read)
        }
        if (recorder?.start() != true) {
            Log.e(TAG, "AudioRecord falhou; abortando consulta")
            repository.discard(created)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        ServiceBus.update {
            it.copy(
                running = true,
                encounterId = created.id,
                route = routeManager.currentRouteLabel(),
                ladder = ladder.level.value,
                asrAvailable = transcriber != null,
                phoneBatteryStartPct = phoneBatteryPct(),
                draftReady = false,
            )
        }

        chunkJob = scope.launch {
            while (true) {
                delay(CHUNK_MS)
                rotateAudioChunk()
            }
        }

        scope.launch {
            ladder.level.collect { level ->
                ServiceBus.update { it.copy(ladder = level) }
                if (level != LadderLevel.L0) speak("Modo de economia nível ${level.name}.")
                if (level.rank >= LadderLevel.L2.rank) stopSecurityVideo()
            }
        }

        gateway = GatewayFactory.create(applicationContext).also { gw ->
            scope.launch {
                gw.events.collect { event ->
                    ladder.onGatewayEvent(event)
                    ServiceBus.update { it.copy(lastEvent = event.toString()) }
                }
            }
            scope.launch {
                gw.startSession()
                    .onSuccess {
                        val videoAllowed = FeatureFlags.securityVideoEnabled(applicationContext) &&
                            consent.securityVideoConsented &&
                            ladder.level.value == LadderLevel.L0
                        if (videoAllowed) startSecurityVideo(gw)
                    }
                    .onFailure { Log.w(TAG, "Sessão do gateway indisponível: ${it.message}") }
            }
        }
        repository.auditLog.append(
            "capture_started",
            JSONObject()
                .put("encounterId", created.id)
                .put("scoRoute", scoOk)
                .put("asrAvailable", transcriber != null)
                .put("phoneBatteryPct", phoneBatteryPct()),
        )
    }

    /** Checkpoint "Eficiência de bateria": telemetria do telefone por consulta. */
    private fun phoneBatteryPct(): Int =
        getSystemService(BatteryManager::class.java)
            ?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1

    /** "Registrar imagem" → foto pontual cifrada vinculada ao encontro (câmera como evento). */
    private fun capturePhotoByVoice() {
        val enc = encounter ?: return
        val gw = gateway ?: return
        scope.launch {
            gw.capturePhoto()
                .onSuccess { bitmap ->
                    val seq = photoSeq++
                    val plain = File(enc.dir, "photo_$seq.tmp")
                    plain.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                    val encrypted = File(enc.dir, "photo_$seq.enc")
                    val crypto = SecurityVault.encryptFile(
                        plain, encrypted, repository.aadFor(enc.id, seq),
                    )
                    plain.delete()
                    repository.addChunk(enc, ChunkKind.PHOTO, seq, encrypted, crypto)
                    ServiceBus.update { it.copy(photos = seq + 1) }
                    speak("Imagem registrada.")
                }
                .onFailure {
                    Log.w(TAG, "capturePhoto falhou: ${it.message}")
                    speak("Não foi possível registrar a imagem.")
                }
        }
    }

    /** EM DISPUTA (MEMORY.md §4.1): só executa com flag ligada + consentimento específico. */
    private fun startSecurityVideo(gw: DeviceGateway) {
        val enc = encounter ?: return
        // Modo blindado: sem chave pública do custodiante, vídeo NÃO grava (invariante SEC-01)
        if (RecoveryKeyStore.publicKey(applicationContext) == null) {
            Log.w(TAG, "Vídeo bloqueado: chave do custodiante não configurada")
            ServiceBus.update { it.copy(lastEvent = "vídeo bloqueado: sem chave do custodiante") }
            return
        }
        videoJob = scope.launch {
            gw.startVideo(VideoConfig()).onFailure {
                Log.w(TAG, "Vídeo de segurança indisponível: ${it.message}")
                return@launch
            }
            var chunkStartedAt = System.currentTimeMillis()
            openVideoChunk(enc)
            gw.videoFrames.collect { frame ->
                videoChunkStream?.let { stream ->
                    stream.writeLong(frame.presentationTimeUs)
                    stream.writeInt(frame.width)
                    stream.writeInt(frame.height)
                    stream.writeBoolean(frame.isCodecConfig)
                    stream.writeInt(frame.data.size)
                    stream.write(frame.data)
                }
                if (System.currentTimeMillis() - chunkStartedAt >= CHUNK_MS) {
                    closeVideoChunk(enc)
                    openVideoChunk(enc)
                    chunkStartedAt = System.currentTimeMillis()
                }
            }
        }
        ServiceBus.update { it.copy(videoActive = true) }
    }

    private fun stopSecurityVideo() {
        videoJob?.cancel()
        videoJob = null
        encounter?.let { closeVideoChunk(it) }
        scope.launch { gateway?.stopVideo() }
        ServiceBus.update { it.copy(videoActive = false) }
    }

    private fun openVideoChunk(enc: Encounter) {
        val file = File(enc.dir, "video_${videoSeq}.tmp")
        videoChunkFile = file
        videoChunkStream = DataOutputStream(file.outputStream().buffered())
    }

    private fun closeVideoChunk(enc: Encounter) {
        val stream = videoChunkStream ?: return
        val file = videoChunkFile ?: return
        videoChunkStream = null
        videoChunkFile = null
        runCatching { stream.close() }
        if (file.length() == 0L) {
            file.delete()
            return
        }
        val seq = videoSeq++
        val encrypted = File(enc.dir, "video_$seq.enc")
        val custodianKey = RecoveryKeyStore.publicKey(applicationContext)
        if (custodianKey == null) {
            // Nunca gravar vídeo decifrável localmente: sem custodiante, descarta o chunk
            Log.w(TAG, "Chunk de vídeo descartado: sem chave do custodiante")
            file.delete()
            return
        }
        val crypto = SecurityVault.encryptFile(
            file,
            encrypted,
            repository.aadFor(enc.id, seq),
            WrapPolicy.RecoveryOnly(custodianKey),
        )
        file.delete()
        repository.addChunk(enc, ChunkKind.VIDEO, seq, encrypted, crypto)
        ServiceBus.update { it.copy(videoChunks = seq + 1) }
    }

    private fun rotateAudioChunk() {
        val enc = encounter ?: return
        if (pcmBuffer.isEmpty()) return
        val pcm = pcmBuffer.drain()
        val seq = audioSeq++
        val wav = File(enc.dir, "audio_$seq.tmp")
        WavWriter.write(pcm, wav)
        val encrypted = File(enc.dir, "audio_$seq.enc")
        val crypto = SecurityVault.encryptFile(wav, encrypted, repository.aadFor(enc.id, seq))
        wav.delete()
        repository.addChunk(enc, ChunkKind.AUDIO, seq, encrypted, crypto)
        ServiceBus.update { it.copy(audioChunks = seq + 1) }
    }

    private fun stopCapture() {
        chunkJob?.cancel()
        stopSecurityVideo()
        recorder?.stop()
        recorder = null
        rotateAudioChunk()
        transcriber?.let {
            it.finish()
            it.close()
        }
        transcriber = null
        runBlocking { gateway?.stopSession() }
        gateway = null
        routeManager.release()
        encounter?.let { enc ->
            generateDraft(enc)
            repository.auditLog.append(
                "capture_stopped",
                JSONObject().put("encounterId", enc.id).put("phoneBatteryPct", phoneBatteryPct()),
            )
        }
        encounter = null
        ServiceBus.update { it.copy(running = false) }
    }

    /** Pipeline anti-alucinação: transcrição → fatos → SOAP → validação de proveniência. */
    private fun generateDraft(enc: Encounter) {
        val snapshot = synchronized(segments) { segments.toList() }
        if (snapshot.isEmpty()) {
            speak("Consulta encerrada. Sem transcrição disponível.")
            return
        }
        val facts = runBlocking { PassthroughFactExtractor().extract(enc.id, snapshot) }
        val note = HeuristicSoapClassifier.classify(enc.id, facts)
        val validation = ProvenanceValidator.validate(note, snapshot)

        repository.saveDocument(
            enc, "transcript", SoapJson.transcriptToJson(snapshot).toString(2).toByteArray(),
        )
        repository.saveDocument(
            enc, "soap_draft", SoapJson.noteToJson(note, validation).toString(2).toByteArray(),
        )
        ServiceBus.update { it.copy(draftReady = true) }

        val uncertain = facts.count { it.status.name == "UNCERTAIN" }
        speak(
            "Consulta encerrada. Rascunho pronto com ${facts.size} fatos" +
                if (uncertain > 0) ", $uncertain para revisar." else ".",
        )
    }

    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "capture")
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notification_channel_description)
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** Notificação sem nome de paciente nem conteúdo clínico (docs/LGPD.md §5). */
    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onDestroy() {
        if (ServiceBus.status.value.running) stopCapture()
        tts?.shutdown()
        scope.cancel()
        super.onDestroy()
    }
}

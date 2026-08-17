# Guia de Testes — Prontuário Glasses

Três ambientes, do mais acessível ao mais real. Tudo que passa num nível não precisa ser retestado no seguinte — cada nível existe para validar o que o anterior **não consegue**.

## 1. Emulador (sem celular, sem óculos) — validado em 15/08

O que valida: pipeline completo (ASR→SOAP→cofre), modo blindado, break-glass, escada de energia, background, crash/órfãos.
O que NÃO valida: Bluetooth/HFP real, bateria, OEM battery savers, DAT.

### Setup (uma vez)

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools:$PATH"

# Emulador + AVD
sdkmanager "emulator" "system-images;android-35;google_apis;arm64-v8a"
echo no | avdmanager create avd -n prontuario_test -k "system-images;android-35;google_apis;arm64-v8a" -d pixel_7

# Subir (NUNCA pipe a saída — SIGPIPE mata o emulador)
nohup emulator -avd prontuario_test -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect > /tmp/emulator.log 2>&1 &
adb wait-for-device

# Instalar app
cd android && ./gradlew :app:assembleSimDebug
adb install -r app/build/outputs/apk/sim/debug/app-sim-debug.apk
for p in RECORD_AUDIO POST_NOTIFICATIONS BLUETOOTH_CONNECT; do adb shell pm grant com.prontuario.glasses.sim android.permission.$p; done

# Modelo Vosk — via run-as no filesDir INTERNO (adb push em /sdcard/Android/data fica invisível pro app — FUSE)
MODEL=~/.cache/prontuario-glasses/vosk-model-small-pt-0.3   # baixado por scripts/install-vosk-model.sh
adb push "$MODEL" /data/local/tmp/vosk-pt
adb shell "run-as com.prontuario.glasses.sim mkdir -p files/models && run-as com.prontuario.glasses.sim cp -r /data/local/tmp/vosk-pt files/models/"

# Áudio de consulta simulada (voz PT-BR do macOS)
say -v Luciana -o /tmp/consulta.aiff "Bom dia... [roteiro com queixa, exame, 'registrar imagem', prescrição, 'encerrar consulta']"
afconvert -f WAVE -d LEI16@16000 -c 1 /tmp/consulta.aiff /tmp/test_input.wav
adb push /tmp/test_input.wav /sdcard/Android/data/com.prontuario.glasses.sim/files/test_input.wav
```

### Rodar uma consulta simulada

O harness (só em build debug) troca o microfone pelo WAV quando `files/test_input.wav` existe:

```bash
adb shell am start -n com.prontuario.glasses.sim/com.prontuario.glasses.ui.MainActivity --ez auto_start true              # só áudio
adb shell am start ... --ez auto_start true --ez video true    # + vídeo blindado (gera chave do custodiante demo)
adb shell am start ... --ez review_confirm_atestado true       # confirma atestado pendente e gera o PDF
adb logcat -s CaptureService   # acompanhar; rascunhos saem como "HARNESS draft" / "HARNESS atestado"
```

Para testar o atestado, inclua no roteiro do `say`: "Vou emitir atestado de três dias por motivo de doença". O PDF sai em `files/encounters/<id>/atestado.pdf` (extrair com `run-as cat`).

Flags de teste (via prefs): `sim_thermal_after_s` injeta THERMAL_CRITICAL após N s de vídeo (testa a escada L0→L2).

### Verificações úteis

```bash
PKG=com.prontuario.glasses.sim
ENC=$(adb shell run-as $PKG ls -t files/encounters | head -1)
adb shell run-as $PKG ls files/encounters/$ENC/                 # audio_N.enc, video_N.enc, photo_N.enc, *.enc
adb shell run-as $PKG cat files/encounters/$ENC/manifest.json   # wrapMode: VIDEO=RECOVERY_ONLY, resto=LOCAL
adb shell run-as $PKG cat files/audit/audit.jsonl               # cadeia hash-encadeada
adb shell input keyevent 26                                     # bloquear tela no meio → captura deve continuar
adb shell am force-stop $PKG                                    # crash → próximo boot limpa órfãos (audit: orphans_cleaned)
```

Break-glass (perito com a chave privada): extrair `video_N.enc` + `manifest.json` + `files/custodian_demo.pem` via `run-as cat`, desembrulhar a DEK com RSA-OAEP(SHA-256) e decifrar AES-GCM com AAD `encounterId:seq` — script de referência no histórico do commit `feat(harness)`.

### Resultados de 15/08 (baseline)
- Pipeline completo ✅ · foto por voz ✅ · background/tela bloqueada ✅ · crash/órfãos ✅
- Break-glass externo ✅ (integridade confere; transplante de chunk rejeitado)
- vosk-small-pt: erra fármacos ("cefaleia intencional", dipirona irreconhecível) — IA-03
- Áudio 8 kHz (HFP simulado): degradação severa ("doutor"→"dor", "náusea" some) — AND-03

## 2. Celular físico (antes do hackathon)

O que valida (além do emulador): rota Bluetooth real, microfone real, bateria, battery savers de OEM, TTS no canal BT.

1. **Instalar**: `./gradlew :app:installSimDebug` + `scripts/install-vosk-model.sh` (modelo via adb, sem run-as: o script usa o externalFilesDir, que funciona em aparelho físico com o app já executado uma vez).
2. **Rota HFP com fone Bluetooth comum** (proxy dos óculos): parear fone BT com mic → iniciar consulta SEM `test_input.wav` no aparelho (harness desliga sozinho) → status deve mostrar "óculos (Bluetooth SCO)" → falar a consulta pelo fone.
   - Valida: `setCommunicationDevice`, AudioRecord via SCO, qualidade real do ASR em banda estreita (comparar com o resultado 8 kHz do emulador).
3. **Battery saver OEM**: ativar economia agressiva (Xiaomi/Samsung) → consulta de 10 min com tela bloqueada → captura não pode morrer (AND-02). Se morrer: whitelist de bateria + documentar.
4. **Consulta real de 20 min** (critério de aceitação): sem route loss, chunks íntegros, %/h de bateria anotado (telemetria já sai no audit: `phoneBatteryPct` no start/stop).
5. **Modelo maior**: repetir com `vosk-model-pt-fb` (grande) e comparar erros de fármacos; decidir modelo do hackathon.
6. **TTS + captura simultâneos** no canal BT (AND-05): resposta falada não pode derrubar o AudioRecord.

## 3. Óculos reais (dia do hackathon, 18/09 — 1ª hora)

Pré-requisitos (fazer ANTES do dia): GitHub token `read:packages` em `local.properties` (`github_token=...`), `APPLICATION_ID`/`CLIENT_TOKEN` do Wearables Developer Center (`-PmwdatAppId=... -PmwdatClientToken=...`), compilar flavor `dat` (`./gradlew :app:assembleDatDebug`) e corrigir assinaturas do `DatDeviceGateway` contra o SDK real.

Ordem dos testes (go/no-go por camada — ver contingências em docs/PLANO.md §3):
1. **Onboarding**: parear óculos, registro no app Meta AI, permissão de câmera (redirect), firmware.
2. **Áudio HFP dos óculos**: rota SCO estável + AudioRecord + TTS no speaker dos óculos. Falhou → mic do celular (L4).
3. **capturePhoto()** pontual: latência e enquadramento a 0,5/1/1,5 m (FOV ~53°, DAT-03).
4. **Coexistência HFP + stream** (DAT-04, o teste decisivo): vídeo LOW@7fps + áudio por 15–20 min; observar `CRITICAL_STREAM_ERROR`/GATT/heartbeat no logcat. Falhou → desligar vídeo (L2), demo segue com áudio+foto.
5. **Formato do videoStream** (DAT-10): logar `isCompressed`/`isCodecConfig` do primeiro frame — define se o gravador precisa de re-encode.
6. **Bateria/térmico**: anotar % dos óculos e `ThermalLevel` a cada 10 min (HW-02/HW-05); erros tipados devem descer a escada automaticamente.
7. **Ensaio da demo** com o roteiro da consulta simulada + gravação de vídeo de backup do funcionamento.

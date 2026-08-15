#!/usr/bin/env bash
# Baixa o modelo Vosk PT-BR (small, ~31 MB) e instala no aparelho via adb.
# Uso: ./scripts/install-vosk-model.sh [applicationId]
set -euo pipefail

APP_ID="${1:-com.prontuario.glasses.sim}"
MODEL="vosk-model-small-pt-0.3"
URL="https://alphacephei.com/vosk/models/${MODEL}.zip"
CACHE_DIR="${HOME}/.cache/prontuario-glasses"

mkdir -p "$CACHE_DIR"
if [ ! -d "$CACHE_DIR/$MODEL" ]; then
  echo ">> Baixando $MODEL..."
  curl -L -o "$CACHE_DIR/$MODEL.zip" "$URL"
  unzip -q "$CACHE_DIR/$MODEL.zip" -d "$CACHE_DIR"
  rm "$CACHE_DIR/$MODEL.zip"
fi

echo ">> Enviando para o aparelho ($APP_ID)..."
adb shell mkdir -p "/sdcard/Android/data/$APP_ID/files/models"
adb push "$CACHE_DIR/$MODEL" "/sdcard/Android/data/$APP_ID/files/models/vosk-pt"
echo ">> Pronto. O app detecta o modelo em files/models/vosk-pt na próxima consulta."

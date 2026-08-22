# Plano Mestre — Medware

> Planejamento completo até o hackathon (18/09). Sem implementação da ideia até o fim do planejamento.
> Documentos irmãos: [MEMORY.md](../MEMORY.md) · [LIMITACOES.md](LIMITACOES.md) · [LGPD.md](LGPD.md) · [PESQUISA.md](PESQUISA.md)
> Última atualização: 2026-08-15.

## 1. Visão em uma frase

O médico conversa normalmente com o paciente usando os AI Glasses; o telefone transforma a consulta em um rascunho de prontuário SOAP com rastreabilidade total, que o médico revisa e confirma — mãos livres, 100% on-device, zero nuvem.

## 2. Arquitetura (referência: doc v2 em docs/)

```
RAY-BAN META WAYFARER GEN 2
  CÂMERA    ──► DAT 0.9.0 ──► DeviceSession.addCamera() → Camera.stream / capturePhoto()
  MICROFONE ──► Bluetooth HFP/SCO ──► AudioManager(MODE_IN_COMMUNICATION) + AudioRecord
  SPEAKER   ◄── Bluetooth HFP/SCO ◄── TTS

ANDROID (companion + processamento)
  MainActivity ──► consentimento, start/stop, revisão
  ConsultationCaptureService (FGS microphone) ──► captura, estado, buffers
  Pipeline: VAD → ASR PT-BR → extração factual → classificação SOAP → validação de provenance → rascunho
  Review UI ──► médico edita/confirma → registro oficial (fora do escopo do MVP)
```

Regras de ordem: rota HFP estável **antes** de iniciar câmera; AudioRecord persistente a consulta inteira; encerramento ordenado (áudio → communication device → câmera → session).

## 3. Fases do plano

### Fase A — Planejamento e validação documental (15–22/08) ← estamos aqui
- [x] Extrair e consolidar edital + arquitetura v2.
- [x] Pesquisa dos links (AUP, Muse Glimmer, repo DAT) → docs/PESQUISA.md.
- [x] Documento de limitações → docs/LIMITACOES.md.
- [x] LGPD → docs/LGPD.md.
- [ ] Clonar repo DAT em `vendor/` e estudar: CHANGELOG, skill camera-streaming, MockDeviceKit, discussão #136.
- [ ] **Entrega Final da Ideia (22/08)** no template da organização — responsável: redator-entrega.
- [ ] Perguntas para mentores no Ideathon (hoje): interpretação da AUP p/ contexto clínico (POL-01); modelo do smartphone do hackathon (PRG-03); acesso antecipado a token/Developer Center (DAT-08).

### Fase B — Prototipagem sem hardware (23/08–17/09, se selecionados)
Tudo contra **MockDeviceKit** (`mwdat-mockdevice`) + fone Bluetooth comum simulando rota HFP:
1. Projeto Android base: permissões, FGS microphone, lifecycle DAT (mock).
2. Rota de áudio HFP com fone BT: `setCommunicationDevice` + AudioRecord persistente + TTS.
3. Benchmark ASR PT-BR (candidatos: Whisper small/medium on-device via whisper.cpp, Vosk PT-BR, Android SpeechRecognizer offline) com áudio degradado a 8/16 kHz simulando HFP.
4. Benchmark LLM local (Gemma 3n E2B/E4B via MediaPipe/AI Edge) no melhor Android que tivermos: latência, RAM, qualidade da extração factual.
5. Pipeline factual → SOAP com saída JSON estrita + provenance + métrica de unsupported-statement-rate.
6. Review UI mínima do companion.
7. Ensaios da demo: consulta simulada roteirizada de 5 min mapeando os 5 checkpoints do edital.

### Fase C — Hackathon (18/09, 1 dia)
| Hora (relativa) | Atividade |
|---|---|
| H0–H1 | Onboarding: pareamento, registro Meta AI, permissões, firmware. Teste de compatibilidade HFP+câmera do aparelho real (DAT-04) — decisão go/no-go por camada |
| H1–H3 | Trocar mock por DAT real; validar áudio end-to-end e capturePhoto |
| H3–H5 | Integração pipeline completo + instrumentação de bateria/thermal |
| H5–H6 | Ensaio da demo + gravação de vídeo de backup |
| H6+ | Pitch |

Contingências por camada (de LIMITACOES.md): câmera instável → demo só áudio + foto avulsa; ASR ruim com HFP → mic do celular; LLM lento → estruturação pós-consulta em lote.

## 4. Mapeamento checkpoints do edital → nossa solução

| Checkpoint | Como cumprimos | Evidência na demo |
|---|---|---|
| IA funcional | ASR + extração factual + LLM local | Rascunho SOAP gerado ao vivo |
| Câmera/microfone | Mic (HFP) como canal principal; foto por comando | Consulta falada + "registrar imagem" |
| Output por áudio | TTS confirmações e resumo falado | "Consulta registrada; 2 pendências: dose de X não informada..." |
| Privacidade | docs/LGPD.md + opt-outs + modo avião | Demo com rede desligada |
| Bateria | Perfis de energia + budget + telemetria própria | Painel de consumo no companion |

## 5. Matriz de riscos (top 5, do doc v2 + pesquisa)

| Risco | Sev. | Plano |
|---|---|---|
| HFP + câmera DAT instável no aparelho do hackathon | Alta | Teste na 1ª hora; contingência por camada |
| ASR PT-BR médico ruim com HFP 8 kHz | Alta | Benchmark antecipado com áudio degradado; fallback mic celular |
| FGS morto por OEM battery saver | Alta | FGS microphone + testes doze/lock; aparelho de referência |
| LLM alucina fatos clínicos | Alta | Extração factual, provenance, JSON estrito, revisão humana |
| Interpretação da AUP sobre "locais sensíveis" | Alta | Validar com mentores Meta hoje; argumento de gravação profissional consentida |

## 6. Critérios de aceitação (congela antes do hackathon)

- Áudio: 20 min contínuos sem route loss/crash.
- Background: tela bloqueada não interrompe captura; retorno preserva estado.
- Câmera: capturePhoto ≥95% de sucesso, vinculada ao encounter correto.
- IA: 100% dos fatos com provenance; unsupported-statement-rate < meta (definir após 1º benchmark).
- Privacidade: checklist de docs/LGPD.md §5 completo; demo funciona em modo avião.
- Bateria: %/h medido por perfil (30/60 min).

## 7. Divisão de trabalho (equipe de 3 + subagentes)

| Frente | Humano | Subagente de apoio |
|---|---|---|
| Android/DAT/áudio | dev Android (requisito do edital) | `arquiteto-android` |
| IA (ASR/LLM/pipeline) | membro 2 | `pesquisador` |
| Produto/pitch/compliance | membro 3 | `lgpd-guardian`, `redator-entrega` |

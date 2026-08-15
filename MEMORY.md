# Memória do Projeto — AI Glasses Brasil 2026

> Fonte de verdade compartilhada entre agentes e sessões. Atualizar sempre que uma decisão for tomada ou um fato for verificado/invalidado.
> Última atualização: 2026-08-15 (dia do Ideathon online).

## 1. O que é este projeto

**Assistente de Prontuário Automático** para AI Glasses (Ray-Ban Meta Wayfarer Gen 2) + Android on-device, participante do **Programa AI Glasses Brasil 2026** (CEIA/UFG/FUNAPE/Meta). Trilha: **Bem-Estar** (assistência a profissionais de saúde).

O médico usa os óculos durante a consulta; o áudio é capturado e processado 100% no Android (VAD → ASR PT-BR → extração factual → SOAP), com fotos pontuais por comando de voz. O prontuário oficial só nasce após revisão e confirmação humana.

## 2. Datas críticas (edital)

| Marco | Data |
|---|---|
| Ideathon online | 15/08/2026 (hoje) |
| **Entrega Final da Ideia** | **22/08/2026** |
| Segundo Filtro | 23–29/08/2026 |
| Resultado (5 equipes) | 31/08/2026 |
| Hackathon presencial (Meta SP, 1 dia) | 18/09/2026 |

## 3. Checkpoints obrigatórios do hackathon (edital, Seção 8.1)

1. **IA funcional e comprovável** (API, nuvem ou local) — nosso caso: local.
2. **Câmera ou microfone** como canal principal de entrada.
3. **Output por áudio** (único canal de saída; sem display).
4. **Privacidade e dados** — justificativa explícita de tratamento.
5. **Eficiência de bateria** — estratégia clara no celular e/ou óculos.

Critérios do Segundo Filtro: Viabilidade técnica (30), Impacto (30), Aderência ao toolkit/hardware (20), Considerações éticas (20).

## 4. Decisões consolidadas (não reabrir sem novo fato)

- **Microfone NÃO é API pública do DAT** → áudio via Bluetooth HFP/SCO do Android (`AudioManager.setCommunicationDevice`, `MODE_IN_COMMUNICATION`). Não usar `startBluetoothSco()` em API 31+.
- **Baseline: DAT Android 0.9.0** (03/08/2026). Câmera via `DeviceSession.addCamera(...)` → `Camera.stream`. Não usar APIs antigas `addStream/removeStream`.
- **DAM é sempre habilitado no 0.9** — a chave `DAM_ENABLED` é ignorada (orientação antiga do 0.8 está obsoleta).
- **Câmera como evento, não stream**: `capturePhoto()` por comando de voz; sem vídeo contínuo.
- **IA 100% on-device no Android** (candidato LLM: Gemma 3n E2B/E4B — decidir só após benchmark). Sem dado clínico na nuvem.
- **Captura em Foreground Service** tipo `microphone` (Android 14+ exige `FOREGROUND_SERVICE_MICROPHONE`; iniciar com Activity visível).
- **SOAP** como estrutura operacional; FHIR como camada futura.
- **Extração factual antes de estruturação** — LLM não completa lacunas; campos "não informado"/"incerto" são válidos; todo fato tem provenance.
- **Opt-out de telemetria do DAT**: `ANALYTICS_OPT_OUT=true` e `CRASH_REPORTING_OPT_OUT=true`.
- **15/08 — time autorizou implementação.** Fase de código iniciada no branch `dev/marcos`, app em `android/` com flavors `sim` (sem DAT, compila sem token) e `dat` (DAT real, requer token — DAT-08).

### 4.1 Gravação de segurança em vídeo — resolvida como "MODO BLINDADO" (15/08, tarde)

Proposta original do time: vídeo contínuo em baixa qualidade como proteção médico-legal. Parecer `lgpd-guardian` foi ⛔ (necessidade, art. 6º III) e `arquiteto-android` deu viável condicional. **Marcos aprovou o meio-termo refinado**:
- Vídeo fica no celular do médico, mas em **modo blindado**: DEK de cada chunk embrulhada SOMENTE com a chave pública do custodiante — **o aparelho é tecnicamente incapaz de decifrar** (nem o médico assiste).
- Chave privada com custodiante institucional; abertura só mediante **ordem judicial** (gatilho judicial, custódia institucional — "oficial de justiça" não guarda chave).
- Invariante no código: **sem chave pública do custodiante configurada, vídeo não grava**.
- Eliminação pelo titular: crypto-erasure (apagar DEKs do manifesto), sem precisar decifrar.
- Feature flag continua OFF por padrão; áudio+fotos+hash-chain seguem sendo a narrativa principal do pitch; vídeo é camada opcional.
- ⚠️ Pendente: validar AUP ("locais sensíveis", POL-01/POL-04) com mentores Meta antes do pitch.
- Implementado e testado em 15/08: `WrapPolicy.RecoveryOnly` + `RecoveryKeyStore` + testes JVM (transplante de chunk, decifra local recusada, break-glass com justificativa).

## 5. Fatos de hardware verificados (ficha técnica + repo DAT)

- Câmera nativa 12 MP ultra-wide (3024×4032; vídeo até 3K), **mas o DAT entrega stream máx. 720×1280 @ 30 fps** (também 504×896 / 360×640; 2/7/15/24/30 fps).
- **FOV do stream ~53° horizontal vs ~88° nativo** (issue pública do repo).
- Array de 5 microfones; 32 GB flash; BT 5.3; Wi-Fi 6.
- Bateria: até 8 h de uso (ficha) — **tratar como hipótese, não garantia**; benchmark obrigatório sob nossa carga.
- DAT expõe erros tipados: `BATTERY_CRITICAL`, `PEAK_POWER_SHUTDOWN`, `THERMAL_CRITICAL`, `THERMAL_EMERGENCY` + thermal level.
- Coexistência HFP/SCO + câmera é instável em alguns telefones (discussão #136 do repo: `CRITICAL_STREAM_ERROR`, GATT timeout); houve teste OK em Galaxy A25 + DAT 0.8 (~5,5 min, MEDIUM/24fps + mic). **Depende de aparelho/firmware/versão — testar no dispositivo real do hackathon.**
- DAT depende do app Meta AI para registro/permissões.

## 6. Links de referência recebidos (15/08, Lucas Isaac)

- Acceptable Use Policy: https://wearables.developer.meta.com/acceptable-use-policy/ — restrições em `docs/PESQUISA.md` e `docs/LIMITACOES.md`.
- Muse Glimmer (modelo agentic open da Meta): https://research.meta.ai/blog/introducing-muse-glimmer-open-agentic-model — avaliar como LLM local vs Gemma 3n.
- Repo DAT Android: https://github.com/facebook/meta-wearables-dat-android — clone local em `vendor/meta-wearables-dat-android` (somente referência; fora do build).

## 7. Pendências / próximas ações

- [x] Clonar repo DAT (`vendor/meta-wearables-dat-android`) e verificar CHANGELOG 0.9.0 — confirmado; achado novo: `StreamError.THERMAL_EMERGENCY` removido no 0.9, usar `DeviceSessionError` + `ThermalLevel` (ver docs/PESQUISA.md §4).
- [x] Avaliar Muse Glimmer — 30B, não roda em smartphone; uso possível: LLM-as-a-judge no notebook (docs/PESQUISA.md §2).
- [x] Ler AUP — risco "locais sensíveis" registrado como POL-01 em docs/LIMITACOES.md.
- [ ] Ler skill `camera-streaming` e discussão #136 em detalhe.
- [ ] Preparar documento de Entrega Final (prazo 22/08) no template da organização.
- [ ] Definir metas numéricas: WER alvo do ASR, unsupported-statement-rate alvo do LLM.
- [x] Roteiro de demo: o app já sustenta consulta simulada fim-a-fim no flavor `sim` (falta ensaiar).
- [ ] GitHub token + conta Wearables Developer Center (DAT-08/AND-07) — **único bloqueio do flavor `dat`**.
- [ ] Conectar MCP de docs da Meta no VS Code: https://mcp.developer.meta.com/wearables
- [x] Disputa da gravação de segurança resolvida como MODO BLINDADO (§4.1).
- [ ] Verificar formato do `videoStream` (DAT-10): frames decodificados vs bitstream HEVC.
- [ ] Instalar modelo Vosk num aparelho físico e medir WER PT-BR (scripts/install-vosk-model.sh).
- [ ] Benchmark LLM local (Gemma 3n) para substituir o classificador heurístico (IA-04).
- [ ] Edição campo a campo na tela de revisão (hoje: confirmar/descartar/erase).

### 7.1 Implementado e testado (15/08, flavor `sim`, 32+ testes verdes)

Captura: FGS mic+connectedDevice · rota HFP · AudioRecord 16 kHz · chunks 60s AES-GCM · escada L0–L4.
ASR: Vosk PT-BR streaming com timestamps por palavra · comandos de voz ("registrar imagem", "encerrar consulta") · app funciona sem modelo (degradação limpa).
SOAP: extração factual → classificador heurístico (zero alucinação por construção) → validador de proveniência (unsupported-statement-rate) → rascunho cifrado.
Segurança: modo blindado do vídeo (RecoveryOnly) · crypto-erasure por tipo · gaps documentados · auditoria hash-encadeada · telemetria de bateria do telefone.
UI: consentimento bloqueante · status ao vivo · revisão com confirmar/descartar/erase/verificar auditoria.

## 8. Convenções do repositório

- Documentação de planejamento em `docs/` (Markdown).
- Limitações e mitigações: `docs/LIMITACOES.md` (documento vivo — toda limitação nova entra lá, com status e mitigação).
- LGPD/privacidade: `docs/LGPD.md`.
- Subagentes em `.github/agents/` (`orquestrador`, `pesquisador`, `arquiteto-android`, `lgpd-guardian`, `redator-entrega`).
- **Nenhuma implementação de código da ideia até o fim do planejamento** (decisão do time em 15/08).

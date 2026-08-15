# Pesquisa — Links de Referência (15/08/2026)

> Resumo verificado dos três links enviados por Lucas Isaac + fontes derivadas. Conclusões acionáveis no final de cada seção.

## 1. Meta Wearables Developer Acceptable Use Policy
Fonte: https://wearables.developer.meta.com/acceptable-use-policy/ (última atualização: 22/09/2025; consultada em 15/08/2026)

Pontos relevantes para o nosso caso:
- Aplica-se a **qualquer uso da plataforma**, publicado ou não (inclui protótipo de hackathon).
- Proibições gerais: ilegalidade, violação de direitos de terceiros, assédio, conteúdo danoso.
- **Crítico para nós**: lista de integrações não permitidas inclui **"encorajar o uso de sensores do device ou gravação em locais sensíveis"** (*sensitive locations*). O texto não define a lista de locais; ambiente clínico pode se enquadrar.
- Também vedado: integrações que degradem o device, conteúdo que viole direitos de terceiros, deturpação do propósito da integração em metadados.

**Conclusão acionável:** registrado como POL-01 em [LIMITACOES.md](LIMITACOES.md). Nosso argumento de conformidade: gravação iniciada pelo profissional responsável pelo ambiente, com consentimento explícito do paciente, transparência ativa (notificação + aviso verbal), finalidade legítima de documentação clínica — o oposto de gravação encoberta. **Validar interpretação com mentores Meta no Ideathon/hackathon antes do pitch.**

## 2. Muse Glimmer (Meta Superintelligence Labs)
Fonte: https://research.meta.ai/blog/introducing-muse-glimmer-open-agentic-model (10/08/2026)

- Modelo **agentic open-weights, 30B parâmetros, licença Apache 2.0** (Hugging Face: `meta-models/Muse-Glimmer-30B`).
- Otimizado para agentes locais: tool calling, multi-step reasoning, failure recovery, multimodal (texto+imagem), 100+ idiomas, reasoning effort controlável.
- Quantizado a ~4-bit: **~17–20 GB só de pesos; envelope recomendado 24–32 GB** (Mac M4/M5 Max, RTX 5090). Speculative decoding via drafter DFlash (1.5–3.1× mais rápido).
- Suporte a llama.cpp, MLX, ExecuTorch, Ollama, LM Studio, vLLM.

**Conclusão acionável (IA-01 em LIMITACOES.md):** **não roda em smartphone** — não substitui o candidato on-device (Gemma 3n E2B/E4B). Usos legítimos para nós:
1. **LLM-as-a-judge no notebook** durante desenvolvimento: avaliar qualidade/alucinação dos rascunhos SOAP gerados pelo modelo pequeno (o blog cita esse caso de uso).
2. Ponto de conexão com o ecossistema Meta no pitch ("avaliamos a família Muse; escolhemos X para on-device por Y").
3. ExecuTorch citado como framework edge — mesma stack que podemos usar para o modelo pequeno no Android.

## 3. Repo `facebook/meta-wearables-dat-android`
Fonte: https://github.com/facebook/meta-wearables-dat-android (versão 0.9.0, release ~03/08/2026)

- **Developer preview**. Artefatos Maven (GitHub Packages, exige token `read:packages`): `mwdat-core`, `mwdat-camera`, `mwdat-display`, **`mwdat-mockdevice`**.
- `mwdat-display` existe, mas é para **Meta Ray-Ban Display** — nosso Wayfarer não tem display (não usar).
- **`mwdat-mockdevice` / MockDeviceKit: permite testar sem óculos físico** — peça central da nossa Fase B (não teremos hardware antes do hackathon).
- Requer `APPLICATION_ID` do Wearables Developer Center no manifest; registro de organização/release channel no Developer Center.
- Opt-outs documentados: `ANALYTICS_OPT_OUT` e `CRASH_REPORTING_OPT_OUT` (padrão: **habilitados** — precisamos desligar).
- Developer Terms: "Meta may collect information about how users' Meta devices communicate with your app".
- Suporte a AI-assisted development: plugin Claude Code/Codex, `.github/copilot-instructions.md`, `AGENTS.md`, skills (getting started, camera streaming, session lifecycle, permissions, debugging, MockDevice, sample app).
- **MCP server público de docs: `https://mcp.developer.meta.com/wearables`** (sem auth) + `llms.txt`: https://wearables.developer.meta.com/llms.txt?full=true — conectar no VS Code para consulta viva da documentação.
- Docs de referência 0.9: https://wearables.developer.meta.com/docs/reference/android/dat/0.9

**Conclusões acionáveis:**
1. Clonar em `vendor/meta-wearables-dat-android` (referência local, fora do build).
2. Providenciar GitHub token + conta no Developer Center **antes** do hackathon (DAT-08).
3. Adicionar o MCP server de docs ao VS Code do time.
4. Fase B inteira sobre MockDeviceKit.

## 4. Verificação no clone local (15/08)

CHANGELOG 0.9.0 lido no clone `vendor/meta-wearables-dat-android` — confirma o doc v2:
- Câmera consolidada: `DeviceSession.addCamera()` → `Camera.stream`; `addStream/removeStream` **removidos**.
- DAM sempre habilitado; chave `DAM_ENABLED` ignorada.
- `CRASH_REPORTING_OPT_OUT` novo no 0.9.0.
- Sample de câmera grava vídeo com som opcional continuando em background.
- **Novo achado:** 0.9.0 **removeu** `StreamError.THERMAL_EMERGENCY`, `DeviceSessionError.DEVICE_POWERED_OFF` e `DeviceSessionError.NOT_INITIALIZED` — o tratamento térmico/energia deve se apoiar em `DeviceSessionError` (`BATTERY_CRITICAL`, `PEAK_POWER_SHUTDOWN`, `THERMAL_CRITICAL`, `THERMAL_EMERGENCY`, desde 0.7.0) + `ThermalLevel` via `Wearables.getDeviceState()`.
- MockDeviceKit: `pairGlasses(GlassesModel.RAYBAN_META)` (0.8.0+), simulação de captouch, don/fold — suficiente para a Fase B sem hardware.
- Timeout adicionado ao capturePhoto (0.7.0) — evita travamento permanente de captura.

## 5. Lacunas de pesquisa (próximas)

- [x] ~~Ler CHANGELOG 0.9.0 completo no clone~~ (feito 15/08, ver §4).
- [ ] Discussão #136 (HFP+câmera) — estado atual e workarounds novos.
- [ ] Benchmarks públicos de Whisper/Vosk PT-BR em áudio 8 kHz (para IA-03).
- [ ] Gemma 3n no Android via MediaPipe/AI Edge: requisitos reais de RAM e latência (para IA-04).
- [ ] Termos do Wearables Developer Center sobre uso em demos/hackathons.

# Limitações e Mitigações — Documento Vivo

> Toda limitação descoberta entra aqui com ID, impacto e mitigação. Nunca apagar linha: se resolvida, mudar o status.
> Legenda de status: 🔴 aberta · 🟡 mitigação definida (não testada) · 🟢 mitigada/verificada.
> Última atualização: 2026-08-15.

## 1. Hardware / Óculos (Ray-Ban Meta Wayfarer Gen 2)

| ID | Limitação | Impacto no projeto | Mitigação | Status |
|---|---|---|---|---|
| HW-01 | **Sem display** — saída só por áudio | Toda confirmação/feedback ao médico é falado | Design de interação por voz curto e não intrusivo; revisão visual fica no companion Android após a consulta | 🟡 |
| HW-02 | **Bateria "até 8 h" é ficha técnica, não garantia** sob HFP contínuo + câmera + speaker | Consulta longa pode esgotar os óculos | Perfis de energia (idle/áudio/áudio+foto); câmera só por evento; budget de energia no Session Manager; benchmark real por perfil antes do hackathon | 🟡 |
| HW-03 | **Thermal/peak power pode encerrar operação** (`THERMAL_CRITICAL`, `PEAK_POWER_SHUTDOWN` etc.) — nota: 0.9.0 removeu `StreamError.THERMAL_EMERGENCY`; sinal térmico vem de `DeviceSessionError` + `ThermalLevel` (`Wearables.getDeviceState()`) | Perda de captura no meio da consulta | Observar `ThermalLevel` + erros de `DeviceSessionError`; downgrade de câmera e encerramento seguro com aviso por áudio | 🟡 |
| HW-04 | Armazenamento e processamento nos óculos são limitados | Não dá para rodar IA nos óculos | Toda IA (VAD/ASR/LLM) roda no Android; óculos = interface de captura/áudio | 🟢 (decisão de arquitetura) |
| HW-05 | Bateria dos óculos sob **HFP + streaming de vídeo contínuo simultâneos** é desconhecida e certamente << 8h; "uso por turno" provavelmente inviável | Expectativa do time (gravação por turno) pode não se sustentar | Reposicionar como gravação **por consulta**; carga no case entre consultas; benchmark de curva de bateria por config no hackathon; escada de degradação L0–L4 | 🔴 |

## 2. DAT (Meta Wearables Device Access Toolkit 0.9.0)

| ID | Limitação | Impacto no projeto | Mitigação | Status |
|---|---|---|---|---|
| DAT-01 | **Microfone NÃO é API pública do DAT** — docs da Meta (citadas em #136): sessões DAT "share microphone and speaker access with the system Bluetooth stack"; há guia oficial de ordering (HFP configurado e estabilizado ANTES do stream) | Não existe "mic dos óculos" via SDK; o caminho é o BT do sistema | Áudio via HFP/SCO: `MODE_IN_COMMUNICATION` + `setCommunicationDevice(TYPE_BLUETOOTH_SCO)`; rota estabilizada antes do stream (implementado nessa ordem) | 🟢 (caminho oficial confirmado 19/08) |
| DAT-02 | **Stream de câmera máx. 720×1280 @ 30 fps** (nativo é 12 MP/3K) | Qualidade de imagem clínica limitada | Preferir `capturePhoto()`; testar enquadramento a 0,5/1/1,5 m; aceitar limite para lesões focalizadas | 🟡 |
| DAT-03 | **FOV do stream ~53° horizontal vs ~88° nativo** (issue pública) | Enquadramento mais difícil do que o esperado | Treinar posicionamento do médico; feedback por áudio ("aproxime-se"); testes de enquadramento no protótipo | 🟡 |
| DAT-04 | **Coexistência HFP/SCO + câmera instável em alguns telefones** (discussão #136: `CRITICAL_STREAM_ERROR`, GATT/heartbeat timeout) | Risco de queda de stream/áudio durante consulta | Ordem de inicialização: HFP estável primeiro, câmera depois; reconexão controlada; testar 15–20 min no aparelho real do hackathon; ter ≥2 aparelhos se possível | 🔴 |
| DAT-05 | **DAM sempre habilitado no 0.9** (`DAM_ENABLED` ignorada) | Orientações antigas do 0.8 são inválidas | Baseline 0.9.0; testar comportamento real do DAM no dispositivo | 🟡 |
| DAT-06 | **Developer preview** — API pode quebrar entre versões | Retrabalho de integração | Fixar versão 0.9.0 no build; acompanhar CHANGELOG; encapsular DAT atrás de interface própria (Session Manager) | 🟡 |
| DAT-07 | **Depende do app Meta AI** para registro/permissões | Setup extra no dia do hackathon; ponto de falha | Checklist de onboarding: pareamento, registro, permissões e firmware verificados antes de codar | 🟡 |
| DAT-08 | **Maven requer GitHub token** (`read:packages`) e `APPLICATION_ID` do Wearables Developer Center | Sem token/registro não compila | Criar conta/organização no Developer Center e token antes do hackathon; documentar no setup | 🔴 |
| DAT-09 | Telemetria/crash reporting do SDK habilitados por padrão | Risco de vazamento de metadados em contexto clínico | `ANALYTICS_OPT_OUT=true` + `CRASH_REPORTING_OPT_OUT=true` no manifest | 🟡 |
| DAT-10 | **Formato do `videoStream` não confirmado** (frames decodificados vs bitstream HEVC com `compressVideo=true`) — define se há re-encode no telefone | Custo de CPU/bateria no telefone e arquitetura do gravador de chunks | Sample usa passthrough HEVC (`isCompressed=true`); verificar no MockDeviceKit; re-encode MediaCodec HW como caso base | 🔴 |
| DAT-11 | **Estabilidade de streaming multi-hora nunca demonstrada** (#136 validou ~5,5 min); reconexão em sessão longa não documentada | Gravação de segurança pode ter buracos ou derrubar SCO | Chunks de 60s com fechamento atômico + manifesto que documenta gaps; 1º sinal de instabilidade → encerrar vídeo preservando áudio; teste 30+ min no hackathon | 🔴 |
| DAT-12 | **`capturePhoto()` só funciona durante um stream ativo** (#136: "capture a single frame during a stream"; skill oficial confirma o fluxo addCamera→stream.start→capturePhoto) | "Foto sem stream" não existe; foto pontual = burst breve de stream | Gateway implementa burst: addCamera → start → capturePhoto → stop; latência do burst a medir no hackathon | 🟡 (verificado em fonte 19/08) |

## 3. Android

| ID | Limitação | Impacto no projeto | Mitigação | Status |
|---|---|---|---|---|
| AND-01 | **FGS de microfone não pode iniciar de background** (Android 14+; permissão while-in-use) | Captura precisa começar com a Activity visível | Fluxo: UI visível → consentimento → `startForegroundService()` → usuário pode sair da tela | 🟡 |
| AND-02 | **OEM battery savers matam serviços** (Xiaomi/Samsung etc.) | Captura pode morrer em background | Foreground Service tipo `microphone` + notificação persistente; testar doze/tela bloqueada/otimização de bateria no aparelho do hackathon | 🔴 |
| AND-03 | **Áudio HFP/SCO é banda estreita** — #136 corrige: Ray-Ban Meta negocia **mSBC wideband 16 kHz** (não 8 kHz CVSD) | Qualidade do ASR degradada vs mic do celular, mas menos que o pior caso | **Evidência medida (15/08, emulador, 8 kHz = pior caso): degradação severa.** Real é 16 kHz mSBC — melhor que o simulado; validar no hardware; fallback mic do celular (L4) | 🟡 (pior caso medido; real é melhor) |
| AND-04 | `startBluetoothSco()` deprecado (API 31+) | Rota de áudio frágil se usar API antiga | Usar `setCommunicationDevice()`; aguardar rota estabilizar antes de gravar | 🟡 |
| AND-05 | Perfil BT pode alternar (HFP×A2DP) ao entrar em comunicação | TTS pode sair no canal errado / cortar | Manter `MODE_IN_COMMUNICATION` durante a consulta inteira; não abrir/fechar HFP por comando | 🟡 |
| AND-06 | **Encode HEVC + cifra contínuos no telefone** somam-se a ASR/LLM on-device: bateria/térmico do telefone desconhecido (PRG-03) | Telefone pode virar o gargalo antes dos óculos | Encoder de hardware; 360p/7fps baseline; medir consumo do telefone separado no benchmark | 🔴 |
| AND-07 | **Sem GitHub token na máquina de dev (15/08)** — artefatos Maven do DAT inacessíveis | Flavor `dat` do app não compila; só o flavor `sim` | Criar token `read:packages` + conta no Wearables Developer Center (ver DAT-08); arquitetura com `DeviceGateway` isola o SDK | 🔴 |

## 4. IA on-device

| ID | Limitação | Impacto no projeto | Mitigação | Status |
|---|---|---|---|---|
| IA-01 | **Muse Glimmer (30B) NÃO roda em smartphone** — quantizado ainda exige ~17–20 GB + KV cache (24–32 GB de envelope) | Não é opção para o pipeline clínico on-device | Manter Gemma 3n E2B/E4B como candidato; Muse Glimmer pode servir no notebook como LLM-as-a-judge para avaliar rascunhos SOAP em desenvolvimento | 🟢 (avaliado 15/08) |
| IA-02 | LLM pode alucinar fatos clínicos | Risco inaceitável em prontuário | Pipeline extração factual → estruturação; saída JSON estrita; provenance por fato; métrica unsupported-statement-rate; revisão humana obrigatória | 🟡 |
| IA-03 | ASR PT-BR com fala médica (fármacos, doses) é difícil, pior com HFP | Transcrição errada = prontuário errado | **Evidência medida (15/08, emulador, vosk-small-pt-0.3, áudio TTS limpo 16 kHz): "cefaleia tensional"→"intencional", "dipirona quinhentos miligramas"→"de quinhentos mil e grande", "imagem"→"image".** Mitigações: modelo maior (vosk-model-pt-fb ou whisper small), boost de vocabulário médico, campos "incerto", comandos de voz por prefixo tolerante | 🟡 (medido; modelo small insuficiente p/ fármacos) |
| IA-04 | Latência e RAM do LLM no smartphone do hackathon (desconhecido) | Pode não caber ou ser lento demais | Benchmark no aparelho real; plano B: modelo menor ou estruturação em lote pós-consulta (não precisa ser tempo real) | 🔴 |
| IA-05 | **Vosk segmenta sentenças mal em fala contínua sem pausas** (teste 15/08: consulta de 44s virou 3 segmentos longos) — fatos ficam grandes demais para classificação SOAP fina | Rascunho com blocos multi-assunto na seção errada | Segmentação secundária por marcadores linguísticos antes da classificação; fala real tem mais pausas que TTS; LLM local resolverá na Fase B | 🟡 |

## 5. Políticas Meta (Acceptable Use Policy + Developer Terms)

| ID | Limitação | Impacto no projeto | Mitigação | Status |
|---|---|---|---|---|
| POL-01 | **AUP proíbe "encorajar uso de sensores/gravação em locais sensíveis"** | Consultório é potencialmente local sensível — risco central do nosso caso de uso | Argumentação de compliance: gravação é iniciada pelo profissional responsável, com consentimento explícito e informado do paciente, finalidade legítima de documentação clínica, sem gravação encoberta (notificação persistente + aviso verbal). Validar interpretação com mentores Meta no Ideathon/hackathon | 🔴 |
| POL-02 | Developer Terms: Meta pode coletar dados de como o device se comunica com o app | Metadados em contexto clínico | Opt-outs do SDK (DAT-09); nenhum conteúdo clínico transita pelo SDK além do necessário (fotos via DAT; áudio via BT do Android) | 🟡 |
| POL-03 | AUP proíbe violar direitos de terceiros | Voz do paciente é dado pessoal sensível | Consentimento registrado antes da captura (ver docs/LGPD.md) | 🟡 |
| POL-04 | **Vídeo contínuo como gravação de segurança** — parecer LGPD original foi ⛔ (necessidade, art. 6º III; agrava POL-01) | Risco de compliance central do caso de uso | **Resolvido como MODO BLINDADO (15/08)**: chunk de vídeo indecifrável no aparelho (DEK só na chave pública do custodiante), abertura só por ordem judicial, consentimento específico + acompanhante, crypto-erasure para eliminação, flag OFF por padrão (ver docs/LGPD.md §8). Pendente: validação da AUP com mentores Meta | 🟡 |

## 6. Segurança de dados (cofre de gravação)

| ID | Limitação | Impacto no projeto | Mitigação | Status |
|---|---|---|---|---|
| SEC-01 | **Break-glass sem servidor**: custódia da chave de recuperação é processo humano; perda da chave = vídeo irrecuperável; roubo = acesso indevido | Garantia médico-legal pode falhar nos dois sentidos | Envelope duplo (KEK no Keystore + chave pública de recuperação); chave privada com custodiante institucional; log de auditoria hash-encadeado; documentar procedimento de custódia | 🟡 |

## 7. Programa / Logística

| ID | Limitação | Impacto no projeto | Mitigação | Status |
|---|---|---|---|---|
| PRG-01 | **Óculos e smartphone só no dia do hackathon** (fornecidos pela organização, devolvidos ao final) | Zero acesso a hardware real antes de 18/09 | Desenvolver contra `mwdat-mockdevice` (MockDeviceKit — testar sem óculos físico); emular rota HFP com fone BT comum; reservar 1ª hora do hackathon para testes de compatibilidade | 🟡 |
| PRG-02 | Hackathon é 1 dia único | Sem tempo para descobrir problemas lá | Tudo que puder ser validado com mock/emulação deve estar 🟢 antes; roteiro de demo ensaiado; checkpoints mapeados | 🟡 |
| PRG-03 | Smartphone do hackathon é desconhecido (modelo/OEM) | DAT-04, AND-02 e IA-04 dependem do aparelho | Plano de contingência por camada: sem câmera → só áudio; ASR ruim → mic do celular; LLM lento → estruturação pós-consulta | 🟡 |

## 8. Como adicionar uma limitação

1. Categoria certa (ou nova seção).
2. ID sequencial, descrição factual **com fonte** (issue, doc, teste próprio).
3. Impacto no *nosso* caso de uso, mitigação proposta e status.
4. Refletir decisões novas em `MEMORY.md`.

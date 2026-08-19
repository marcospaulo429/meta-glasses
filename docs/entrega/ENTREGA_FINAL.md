# Entrega Final da Ideia — Assistente de Prontuário Automático

**Programa AI Glasses Brasil 2026 · Trilha: Bem-Estar**
Documento estruturado (Seção A) — 19/08/2026
Diagrama de arquitetura (Seção B): `arquitetura.png` / `arquitetura.mmd`

---

## A.1 Problema

Médicos brasileiros gastam parcela significativa da consulta **documentando em vez de atendendo**: estudos internacionais apontam até 2 horas de trabalho burocrático para cada hora de contato com paciente, e a digitação durante o atendimento divide a atenção do profissional, degrada a relação médico-paciente e produz prontuários incompletos — com consequências clínicas (informação perdida), legais (registro frágil em disputas) e humanas (burnout documental).

O problema tem três camadas que atacamos juntas:
1. **Tempo**: a documentação compete com o atendimento.
2. **Qualidade do registro**: o que não é anotado na hora se perde ou é reconstruído de memória.
3. **Proteção médico-legal**: o profissional raramente tem evidência íntegra e auditável do que foi dito e feito na consulta.

## A.2 Usuário-alvo

**Primário**: médico(a) de consultório/ambulatório em consultas eletivas (clínica geral, dermatologia, ortopedia — especialidades com exame visual se beneficiam da foto contextual). Ele veste os óculos, conduz a consulta normalmente e não toca em tela durante o atendimento.

**Secundários**: paciente (beneficiado pela atenção integral e pelo consentimento transparente) e instituição de saúde (controladora dos dados, beneficiada por registros completos, auditáveis e conformes à LGPD).

## A.3 Walkthrough de uso

### Fluxo principal

1. **Preparo (30s, única interação com o celular)**: médico abre o companion, confere seu perfil (nome/CRM), digita o nome do paciente e registra os consentimentos — paciente, acompanhante (se presente) e, separadamente, autorização de CID em atestado. **Sem consentimento do paciente, o app se recusa a iniciar** (bloqueio em código).
2. Médico toca "Iniciar consulta", **guarda o celular** (tela pode ser bloqueada — a captura roda em Foreground Service com notificação persistente e neutra).
3. A consulta acontece **naturalmente**: o áudio flui dos óculos por Bluetooth HFP e é transcrito em tempo real no telefone (Vosk PT-BR, on-device), com timestamp por palavra.
4. Ao examinar uma lesão, o médico diz **"registrar imagem"** → a câmera dos óculos é ativada por poucos segundos e captura uma foto pontual (`capturePhoto()` do DAT durante um burst breve de stream), cifrada e vinculada à consulta → resposta por áudio: *"Imagem registrada"*.
5. Ao definir a conduta, diz **"emitir atestado de três dias"** → *"Atestado anotado"*.
6. Diz **"encerrar consulta"** → o telefone estrutura a transcrição em fatos com proveniência → rascunho SOAP → validação anti-alucinação → resposta por áudio: *"Consulta encerrada. Rascunho pronto com N fatos, X para revisar. Atestado de 3 dias aguardando revisão."*
7. **Entre consultas**, o médico abre a tela de revisão: vê S/O/A/P com cada fato apontando para o trecho do áudio (`[20s–40s]`), fatos incertos marcados, e o rascunho do atestado. **Edita, confirma ou descarta.** Só após a confirmação o rascunho vira base do registro oficial e o atestado vira PDF (com hash auditado; assinatura física do médico — ICP-Brasil na fase 2).

### Fluxos de exceção

| Exceção | Comportamento |
|---|---|
| Paciente recusa | Captura não inicia; consulta segue normal sem o app |
| Óculos com bateria baixa / aquecimento | **Escada de degradação automática L0→L4**: reduz fps → encerra vídeo → remove câmera → por último cai para o mic do celular. Áudio clínico é o último a morrer; cada transição é anunciada por áudio |
| Óculos desconectam no meio | Fallback imediato para o microfone do celular (L4) — a consulta não é perdida |
| App morre (crash/OEM) | Perda máxima de 60s (chunks selados a cada minuto); a lacuna fica **documentada** no manifesto (gap de sequência) — juridicamente melhor que um registro "editado"; temporários órfãos são descartados |
| ASR incerto / termo não reconhecido | Fato marcado **"incerto"** ou campo **"não informado"** — o sistema nunca completa lacunas (testado: quando o ASR destruiu o CID falado, o atestado saiu corretamente **sem** CID) |
| Paciente pede exclusão (LGPD art. 18) | "Descartar consulta" apaga tudo; vídeo de segurança tem **crypto-erasure** (destruição das chaves torna o conteúdo irrecuperável sem precisar decifrá-lo) |

## A.4 Decisões técnicas (justificativa + alternativas descartadas)

| # | Decisão | Justificativa | Alternativas descartadas |
|---|---|---|---|
| 1 | **Áudio via Bluetooth HFP/SCO do Android** (`setCommunicationDevice`), não pelo SDK | O DAT 0.9 **não expõe microfone como API pública**; a própria documentação da Meta define o caminho suportado: sessões DAT "compartilham microfone e alto-falante com o stack Bluetooth do sistema", com guia oficial de ordenação (rota HFP estabilizada **antes** de iniciar o stream) — exatamente a ordem que implementamos | Esperar API de áudio do DAT (não existe); usar só o mic do celular (perde o mãos-livres — mantido apenas como fallback L4) |
| 2 | **IA 100% on-device** (ASR Vosk + pipeline no telefone) | Dado de saúde é sensível (LGPD art. 5º II, art. 11): zero conteúdo clínico em rede elimina a maior superfície de risco; funciona sem internet; latência previsível; demo em modo avião | ASR/LLM em nuvem (Whisper API, GPT): transferência de dado sensível, dependência de rede, custo por consulta, impossível demonstrar minimização equivalente |
| 3 | **Câmera como evento**: burst breve de stream + `capturePhoto()` por voz, sem stream contínuo como padrão | A captura de foto do DAT ocorre durante um stream ativo; ativamos o stream por poucos segundos apenas no comando de voz. Stream contínuo é evitado porque a coexistência HFP+stream varia por aparelho (issue pública #136: há handset validado com 5,5 min estáveis e handset com timeouts de GATT) e drena bateria | Vídeo contínuo como padrão (rejeitado pela análise LGPD de necessidade e pela AUP; sobrevive apenas como **modo blindado opcional**, desligado por padrão) |
| 4 | **Pipeline em 2 estágios: extração factual → classificação SOAP**, com proveniência obrigatória por fato e validador de alucinação (unsupported-statement-rate) | Reduz a liberdade generativa a zero no MVP: fato sem origem no áudio **não compila** (invariante no modelo de dados); a métrica torna a alucinação mensurável | "LLM escreve o prontuário" em um passo (alucinação não mensurável, inaceitável em documento clínico) |
| 5 | **Classificador heurístico agora; LLM local (Gemma 3n) na fase 2** atrás da mesma interface | Heurística tem zero alucinação por construção e roda em qualquer aparelho; a interface `FactExtractor` permite trocar pelo LLM após benchmark de RAM/latência no hardware real | Muse Glimmer on-device (30B ≈ 17–20 GB — não cabe em smartphone; avaliado e descartado); LLM em nuvem (decisão #2) |
| 6 | **Cofre por envelope**: DEK AES-GCM por chunk de 60s, KEK no Android Keystore; AAD amarra chunk à consulta | Perda máxima de 60s em falha; chunk não pode ser transplantado entre consultas (verificado por teste criptográfico); chave-mestra não exportável | Cifrar o arquivo inteiro no fim (perde tudo em crash); armazenar em servidor próprio (reintroduz nuvem) |
| 7 | **Modo blindado do vídeo (opcional)**: DEK embrulhada **somente** na chave pública do custodiante institucional; abertura só com a chave privada, mediante ordem judicial | Proteção médico-legal com **garantia técnica**: nem o médico consegue assistir (validado ponta a ponta: decifra externa com a chave do custodiante funciona; local é impossível). Sem chave configurada, vídeo não grava | Vídeo decifrável no aparelho (risco de uso indevido); gravação sem consentimento específico (vetado pela análise LGPD) |
| 8 | **Foreground Service `microphone\|connectedDevice`**, iniciado com Activity visível | Exigência do Android 14+ para FGS de microfone; sobrevive a tela bloqueada/background (testado) | WorkManager/serviço comum (morto pelo sistema); manter Activity aberta (péssima UX mãos-livres) |
| 9 | **Desenvolvimento contra gateway próprio (`DeviceGateway`) + MockDeviceKit** | Hardware real só existe no dia do hackathon; a abstração isola o SDK em preview (que muda a cada versão) e permitiu validar todo o pipeline em emulador antes de tocar nos óculos | Desenvolver direto contra o DAT só no dia (risco integral concentrado em 1 dia) |
| 10 | **SOAP como estrutura operacional; FHIR como camada futura** | SOAP é o padrão cognitivo do médico brasileiro; FHIR entra na integração com sistemas (fase 2) sem retrabalho do modelo | FHIR nativo desde o MVP (complexidade sem valor demonstrável no hackathon) |
| 11 | **Atestado por voz com guardrails CFM** (Res. 1.658/2002): dias/CID nunca inferidos; CID exige consentimento específico; documento só após confirmação | Mesmo princípio anti-alucinação aplicado a documento legal; testado com ASR degradado (extraiu dias corretos e recusou-se a inventar CID) | Receita médica no MVP (regulação de prescrição eletrônica exige assinatura qualificada — fica como roadmap) |

## A.5 Concorrentes e diferenciação

| Concorrente | O que faz | Por que somos diferentes |
|---|---|---|
| **Nuance DAX Copilot** (Microsoft) | Ambient scribe líder global; grava a consulta pelo celular/sala e gera nota clínica na nuvem Azure | Nuvem (dado sai do consultório), inglês-primeiro, custo de assinatura alto, sem mãos-livres real nem captura visual |
| **Abridge** / **Suki** | Scribes ambient por app de celular, nuvem | Mesmas limitações: nuvem + telefone sobre a mesa; nenhum usa o ponto de vista do médico |
| **Amazon HealthScribe** | API de transcrição/sumarização clínica (AWS) | É infraestrutura de nuvem para terceiros — o oposto do nosso desenho de minimização |
| **Voa Health** (Brasil) | Scribe ambient PT-BR via celular, processamento em nuvem | PT-BR nativo como nós, mas nuvem e sem hardware vestível |
| **Prontuários eletrônicos BR** (iClinic, Feegow etc.) | Registro manual/dictation | Não resolvem a captura ambient; são o **destino** futuro da nossa exportação (FHIR) |

**Nossa combinação é inédita**: (1) óculos = mãos 100% livres + câmera no ponto de vista do médico; (2) **zero nuvem** — único da categoria demonstrável em modo avião; (3) rastreabilidade fato-a-fato com métrica de alucinação; (4) modo blindado com garantia criptográfica para proteção médico-legal; (5) atestado por comando de voz com guardrails do CFM.

## A.6 Os cinco pilares técnicos obrigatórios

### 1. Uso de IA (funcional e comprovável)
ASR streaming PT-BR on-device (Vosk, timestamps por palavra) + pipeline de estruturação clínica: extração factual → classificação SOAP → **validador de proveniência** que mede o percentual de afirmações sem suporte na transcrição (no protótipo: **0%**). Detecção de comandos de voz sobre os parciais do ASR. Fase 2: LLM local (Gemma 3n via AI Edge) atrás da mesma interface, com a mesma métrica de aceitação. *Comprovável: o protótipo já executa tudo isso em emulador Android, com consulta simulada de ponta a ponta.*

### 2. Câmera ou microfone como entrada principal
**Microfone dos óculos** é o canal primário (consulta inteira via Bluetooth HFP/SCO → `AudioRecord` persistente 16 kHz — caminho definido pela documentação da Meta: mic/speaker compartilhados com o stack BT do sistema; HFP dos óculos negocia wideband mSBC 16 kHz). **Câmera** via DAT 0.9 (`DeviceSession.addCamera` → burst breve de stream + `capturePhoto()`) acionada por comando de voz para evidência visual pontual — desenho deliberado de "câmera como evento" pelas limitações verificadas do stream (resolução 720p máx., FOV ~53°, coexistência com HFP dependente do aparelho).

### 3. Saída por áudio
Toda a interação durante a consulta é falada (TTS PT-BR): confirmações curtas (*"Imagem registrada"*, *"Atestado anotado"*), avisos da escada de energia (*"Modo de economia nível L2"*), fallbacks (*"Óculos não encontrados, usando microfone do telefone"*) e o resumo final com pendências. Desenho não-intrusivo: frases de 1 linha, nunca áudio contínuo.

### 4. Privacidade e dados
Dado de saúde = sensível (LGPD art. 5º, II; art. 11 — base legal: tutela da saúde + consentimento destacado como salvaguarda). Medidas implementadas: consentimento **bloqueante** (paciente, acompanhante e CID separados); processamento 100% local — **o app sequer declara a permissão INTERNET no manifest**, sendo incapaz de rede por construção (demonstrável em modo avião); cifra AES-GCM por chunk com chave no Keystore; telemetria e crash-reporting do SDK **desligados** (`ANALYTICS_OPT_OUT`, `CRASH_REPORTING_OPT_OUT`); notificação persistente sem conteúdo clínico; auditoria append-only hash-encadeada (adulteração detectável); revisão humana obrigatória antes de qualquer registro; direito de eliminação por descarte total e **crypto-erasure**; vídeo de segurança (opcional, OFF por padrão) indecifrável no aparelho — só custodiante institucional com ordem judicial.

### 5. Eficiência de bateria
Estratégia em três frentes, instrumentada: (a) **os óculos só trabalham quando necessário** — mic contínuo é inevitável (é o produto), mas câmera é por evento e vídeo opcional roda a 360p@7fps; (b) **escada de degradação L0→L4** dirigida pelos erros tipados do DAT (`BATTERY_CRITICAL`, `PEAK_POWER_SHUTDOWN`, `THERMAL_*`) e nível térmico — degrada vídeo → câmera → rota BT, preservando o áudio clínico; (c) **IA pesada roda no telefone**, nunca nos óculos, e a estruturação ocorre uma única vez ao encerrar (não em tempo real). Telemetria de bateria do telefone é auditada por consulta; a curva de consumo dos óculos por perfil será medida no hardware real (protocolo pronto).

---

## Status de validação (transparência técnica)

| Validado em protótipo funcional (emulador + 52 testes automatizados) | Pendente de hardware real (protocolo pronto) |
|---|---|
| Pipeline completo fala→SOAP→cofre→revisão; foto e atestado por voz; modo blindado com decifra externa; escada de energia; background/tela bloqueada; recuperação de crash; consentimento bloqueante | **Captura do mic dos óculos via HFP** — caminho definido pela documentação oficial da Meta (mic/speaker via stack BT do sistema) e demonstrado publicamente em hardware real (5,5 min estáveis com stream simultâneo), mas que só validaremos nós mesmos no hackathon; coexistência HFP+câmera no aparelho fornecido (go/no-go na 1ª hora, com contingência por camada até o mic do celular); curva de bateria dos óculos; qualidade do ASR com HFP real (piso já estimado em simulação 8 kHz — o real usa mSBC 16 kHz, melhor que o simulado); FOV de enquadramento |

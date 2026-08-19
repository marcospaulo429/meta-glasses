# Entrega Final da Ideia — Assistente de Prontuário Automático

**Programa AI Glasses Brasil 2026 · Trilha: Bem-Estar**
Documento estruturado (Seção A) — 19/08/2026
Diagrama de arquitetura (Seção B): `arquitetura.png` / `arquitetura.mmd`

> **Resumo executivo.** O médico veste os Ray-Ban Meta, atende normalmente e dita por voz fotos clínicas e atestados; o celular transforma a consulta em rascunho de prontuário SOAP com rastreabilidade fato-a-fato — **100% on-device, sem nuvem** (o app nem declara permissão de INTERNET). O médico revisa e confirma; nada é registrado sem ele. **Já roda hoje**: pipeline completo validado em emulador Android com 52 testes automatizados (fala→SOAP→cofre cifrado→revisão→PDF de atestado). **Falta validar em hardware real** (protocolo pronto para a 1ª hora do hackathon): mic dos óculos via HFP, coexistência com câmera e bateria. Diferencial contra DAX/Abridge/HealthScribe: único demonstrável em modo avião, com ponto de vista do médico e proteção médico-legal criptográfica.

---

## A.1 Problema e impacto

Médicos gastam parcela significativa da consulta **documentando em vez de atendendo**: o estudo de referência (Sinsky et al., *Annals of Internal Medicine*, 2016) mediu ~2 horas de trabalho em prontuário/burocracia para cada 1 hora de contato clínico. A digitação durante o atendimento divide a atenção, degrada a relação médico-paciente e produz prontuários incompletos — com consequências clínicas (informação perdida), legais (registro frágil em disputas) e humanas (burnout documental).

O problema tem três camadas que atacamos juntas:
1. **Tempo**: a documentação compete com o atendimento.
2. **Qualidade do registro**: o que não é anotado na hora se perde ou é reconstruído de memória.
3. **Proteção médico-legal**: o profissional raramente tem evidência íntegra e auditável do que foi dito e feito.

**Impacto quantificado (estimativa com premissas explícitas, a validar em piloto):**

| Premissa | Valor | Fonte/base |
|---|---|---|
| Médicos ativos no Brasil | ~575 mil | Demografia Médica CFM/USP 2024 |
| Documentação embutida numa consulta de 20 min | ~5–7 min | Proporção do achado de Sinsky et al. |
| Tempo devolvido por consulta (rascunho pronto + revisão rápida) | ~4–5 min | Revisão alvo ≤ 2 min (≤10% da consulta) |
| Ganho por médico (16 consultas/turno) | **~65–80 min/dia ≈ 25h/mês** | Cálculo direto |

Métricas de sucesso definidas: tempo de documentação por consulta (antes/depois), tempo de revisão ≤ 10% da duração da consulta, completude do prontuário (campos preenchidos com proveniência) e adoção (consultas capturadas/dia).

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
7. **Entre consultas**, o médico abre a tela de revisão: vê S/O/A/P com cada fato apontando para o trecho do áudio (`[20s–40s]`), fatos incertos marcados, e o rascunho do atestado. **Edita, confirma ou descarta.** Na consulta simulada do protótipo, revisar o rascunho leva <1 min; a meta de produto é **revisão ≤ 10% da duração da consulta** (≤2 min para 20 min) — se a revisão custasse 10 min, o produto não devolveria tempo algum, por isso ela é métrica de aceitação. Só após a confirmação o rascunho vira base do registro oficial e o atestado vira PDF (hash auditado; assinatura física do médico — ICP-Brasil na fase 2).

### Fluxos de exceção

| Exceção | Comportamento |
|---|---|
| Paciente recusa | Captura não inicia; consulta segue normal sem o app |
| Óculos com bateria baixa / aquecimento / desconexão | Degradação automática em escada até o mic do celular, com avisos por áudio — detalhada em A.7.1; **a consulta nunca é perdida** |
| App morre (crash/OEM) | Perda máxima de 60s (chunks selados a cada minuto); a lacuna fica **documentada** no manifesto (gap de sequência) — juridicamente melhor que um registro "editado" |
| ASR incerto / termo não reconhecido | Fato marcado **"incerto"** ou campo **"não informado"** — o sistema nunca completa lacunas (testado: quando o ASR destruiu o CID falado, o atestado saiu corretamente **sem** CID) |
| Paciente pede exclusão (LGPD art. 18) | "Descartar consulta" apaga tudo; vídeo de segurança tem **crypto-erasure** (destruir as chaves torna o conteúdo irrecuperável) |

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
ASR streaming PT-BR on-device (Vosk, timestamps por palavra) + pipeline de estruturação clínica: extração factual → classificação SOAP → **validador de proveniência** que mede o percentual de afirmações sem suporte na transcrição. Importância da métrica bem enquadrada: no MVP heurístico ela é **0% por construção** (o classificador só reorganiza fatos, não gera texto) — seu papel real é **gatear a adoção do LLM local (Gemma 3n) na fase 2**, que só substitui a heurística se mantiver a taxa ≤ 2% no mesmo corpus.
**Qualidade do ASR, com honestidade**: WER formal ainda não medido (exige celular físico — protocolo pronto: corpus PT-BR médico, WER/CER + acurácia de entidades críticas). Resultado qualitativo já observado: com o modelo small, linguagem geral é fiel, mas **fármacos e doses degradam** ("dipirona quinhentos miligramas" saiu irreconhecível) — por isso o desenho marca entidades incertas para revisão em vez de confiar no ASR, e o plano prevê modelo maior + boost de vocabulário médico, com meta de **acurácia ≥ 95% em entidades críticas** (fármaco, dose, pressão) antes de qualquer piloto real. Detecção de comandos de voz roda sobre os parciais do ASR com prefixos tolerantes a erro (testado: "emitir"→"admitir" não quebrou o comando).
*Comprovável: o protótipo executa tudo isso em emulador Android, com consulta simulada de ponta a ponta.*

### 2. Câmera ou microfone como entrada principal
**Microfone dos óculos** é o canal primário — pelo caminho definido na documentação oficial da Meta ("Use device microphones and speakers"): HFP pelo stack Bluetooth do sistema, com o nosso código de rota idêntico ao exemplo oficial Android (`MODE_IN_COMMUNICATION` + `setCommunicationDevice(TYPE_BLUETOOTH_SCO)`), áudio 8 kHz mono conforme a spec, e a ordenação oficial respeitada (rota estabilizada antes do stream). Ressalva honesta que vamos testar no hackathon: em HFP o beamforming dos óculos prioriza a voz de quem os veste — se a voz do paciente chegar fraca, o desenho prevê o mic do telefone como captura de sala (nível L4 da escada, já implementado), mantendo óculos para TTS e câmera. **Câmera** via DAT 0.9 (`DeviceSession.addCamera` → burst breve de stream + `capturePhoto()`) acionada por comando de voz para evidência visual pontual — desenho deliberado de "câmera como evento" pelas limitações verificadas do stream (resolução 720p máx., FOV ~53°, coexistência com HFP dependente do aparelho).

### 3. Saída por áudio
Toda a interação durante a consulta é falada (TTS PT-BR): confirmações curtas (*"Imagem registrada"*, *"Atestado anotado"*), avisos da escada de energia (*"Modo de economia nível L2"*), fallbacks (*"Óculos não encontrados, usando microfone do telefone"*) e o resumo final com pendências. Desenho não-intrusivo: frases de 1 linha, nunca áudio contínuo.

### 4. Privacidade e dados
Dado de saúde = sensível (LGPD art. 5º, II; art. 11 — base legal: tutela da saúde + consentimento destacado como salvaguarda). Medidas implementadas: consentimento **bloqueante** (paciente, acompanhante e CID separados); processamento 100% local — **o app sequer declara a permissão INTERNET no manifest**, sendo incapaz de rede por construção (demonstrável em modo avião); cifra AES-GCM por chunk com chave no Keystore; telemetria e crash-reporting do SDK **desligados** (`ANALYTICS_OPT_OUT`, `CRASH_REPORTING_OPT_OUT`); notificação persistente sem conteúdo clínico; auditoria append-only hash-encadeada (adulteração detectável); revisão humana obrigatória antes de qualquer registro; direito de eliminação por descarte total e **crypto-erasure**; vídeo de segurança (opcional, OFF por padrão) indecifrável no aparelho — só custodiante institucional com ordem judicial.

### 5. Eficiência de bateria
Estratégia em três frentes, instrumentada: (a) **os óculos só trabalham quando necessário** — mic contínuo é inevitável (é o produto), mas câmera é por evento e vídeo opcional roda a 360p@7fps; (b) **degradação automática em escada** dirigida pelos sinais tipados do DAT (detalhada em A.7.1); (c) **IA pesada roda no telefone**, nunca nos óculos, e a estruturação ocorre uma única vez ao encerrar. Telemetria de bateria auditada por consulta; modelo operacional completo de energia (turnos, case, checklist) em A.7.1.

---

## A.7 Precauções e plano de contingência

### A.7.1 Energia — modelo operacional "por turnos de consulta"

O erro clássico de wearable clínico é planejar como se a bateria fosse infinita. Nós invertemos: **o dia de trabalho é dividido em ciclos consulta ⇄ recarga**, apoiados nos números oficiais do hardware:

| Fato (fonte: Meta, ficha oficial Gen 2) | Valor |
|---|---|
| Uso misto | até 8 h |
| Áudio contínuo (nosso perfil dominante) | ~5 h |
| Case carregador | **+48 h de cargas** |
| Fast charge no case | **50% em ~20 min** |
| Carregamento | somente via case (sem porta nos óculos) |

**Matemática do turno**: uma consulta típica de 20–30 min em nosso perfil (HFP contínuo + fotos pontuais + TTS eventual) consome, por estimativa conservadora sobre os ~5 h de áudio contínuo, **~7–10% da bateria**. Entre consultas há tipicamente 5–15 min de intervalo administrativo — janela em que os óculos **voltam ao case** (fast charge repõe ~2,5% por minuto). Resultado: em regime estacionário, o conjunto óculos+case sustenta um turno de 8–12 h de atendimentos **sem tomada**, porque o case funciona como a "bateria externa" de 48 h que acompanha o médico.

Precauções em camadas:

1. **Pré-voo (checklist no companion antes de cada consulta)**: % dos óculos, % do case, % do telefone, rota BT ativa, modelo ASR carregado. Abaixo de 20% nos óculos, o app recomenda iniciar já em modo áudio-somente (L2).
2. **Durante**: escada de degradação L0→L4 automática guiada pelos erros tipados do DAT (`BATTERY_CRITICAL`, `PEAK_POWER_SHUTDOWN`, `THERMAL_*`) e `ThermalLevel` — corta primeiro vídeo, depois câmera, por último a rota BT; **o áudio clínico é o último a cair e a consulta nunca é perdida** (fallback final: mic do telefone).
3. **Entre consultas**: óculos no case (turno de recarga); o companion exibe a projeção "quantas consultas cabem na carga atual".
4. **Telefone**: ao contrário dos óculos, opera carregando — em consultório fica em powerbank/tomada; o custo pesado (ASR/LLM) roda nele exatamente por isso.
5. **Telemetria auditada**: % de bateria no início/fim de cada consulta entra no log de auditoria — em campo, o modelo estimado é substituído por dados reais do próprio uso.
6. **Instituições com volume alto**: recomendação de 2º par de óculos em rodízio pelo case (custo marginal baixo frente ao ganho de disponibilidade).

### A.7.2 Privacidade e segurança — modelo de ameaças

Além do desenho preventivo (pilar 4), mapeamos ameaças concretas e o controle que responde a cada uma:

| Ameaça | Controle implementado |
|---|---|
| Perda/roubo do celular do médico | Dados em `filesDir` privado, cifrados AES-GCM com chaves no Android Keystore (não-exportáveis, hardware-backed); vídeo blindado nem existe em forma decifrável |
| App malicioso no mesmo aparelho | Sandbox Android + arquivos no diretório privado + **app sem permissão INTERNET** (exfiltração pelo nosso processo é impossível por construção) |
| Coação/curiosidade sobre o vídeo de segurança | Modo blindado: nem o médico, nem o app, nem perícia no aparelho decifram — só a chave privada do custodiante, mediante ordem judicial |
| Adulteração de prontuário/atestado a posteriori | Log de auditoria hash-encadeado + cadeia de custódia sha256 por chunk: qualquer alteração quebra a cadeia de forma detectável |
| Vazamento por tela/notificação | Notificação do serviço é neutra (sem nome de paciente); revisão exige abrir o app |
| Telemetria de SDK | `ANALYTICS_OPT_OUT` + `CRASH_REPORTING_OPT_OUT` ativos |
| Captura de terceiros incidentais | Consentimento de acompanhante obrigatório; aviso de ambiente; direito de eliminação com crypto-erasure |
| Gravação encoberta (AUP da Meta) | Impossível no desenho: consentimento bloqueante em código + notificação persistente + LED nativo dos óculos + anúncios por TTS |
| **AUP — "locais sensíveis"**: a Acceptable Use Policy veda encorajar gravação em locais sensíveis, e consultório médico é candidato óbvio | **Risco declarado, não escondido.** Nossa interpretação: a vedação mira captura encoberta/indiscriminada; aqui a gravação é iniciada pelo profissional responsável pelo ambiente, com consentimento explícito e registrado de todos os presentes, transparência ativa (LED + notificação + TTS) e finalidade legítima de documentação clínica. **Buscaremos confirmação formal dessa interpretação com os mentores Meta antes do hackathon**; se negativa, o produto opera sem armazenar mídia bruta (só transcrição), preservando o valor central |
| Perda da chave do custodiante | Procedimento de custódia com cópias redundantes lacradas; perda degrada apenas o vídeo opcional — áudio clínico e prontuário não dependem dela |

### A.7.3 Continuidade da consulta — o que acontece quando algo falha

| Falha | Comportamento (implementado e testado em emulador) |
|---|---|
| Óculos desconectam / bateria acaba | Fallback automático para mic do telefone com aviso por TTS; consulta continua |
| App morre (crash, OEM battery saver) | Perda máxima de 60 s (chunks selados por minuto); lacuna documentada no manifesto; temporários órfãos descartados no retorno |
| ASR indisponível (modelo não instalado) | Captura continua (áudio cifrado íntegro); transcrição pode ser reprocessada depois |
| Coexistência mic+câmera instável no aparelho | Escada L2: câmera sai, áudio fica — decidido automaticamente no primeiro sinal de erro de stream |
| Termos médicos não reconhecidos | Fato marcado "incerto" para revisão — nunca preenchido por inferência |

Princípio unificador: **nenhuma falha de tecnologia pode custar a consulta nem inventar conteúdo clínico** — degradar é aceitável, perder ou alucinar não.

---

## Status de validação (transparência técnica)

| Validado em protótipo funcional (emulador + 52 testes automatizados) | Pendente de hardware real (protocolo pronto) |
|---|---|
| Pipeline completo fala→SOAP→cofre→revisão; foto e atestado por voz; modo blindado com decifra externa; escada de energia; background/tela bloqueada; recuperação de crash; consentimento bloqueante | **Captura do mic dos óculos via HFP** — nosso código é idêntico ao exemplo oficial da documentação da Meta, mas só validaremos nós mesmos no hardware; **beamforming em HFP prioriza a voz do médico** (docs oficiais) — medir a captação do paciente e, se preciso, usar o mic do telefone como captura de sala (fallback já implementado); coexistência HFP+câmera no aparelho fornecido (go/no-go na 1ª hora); curva de bateria dos óculos; ASR com HFP real (piso 8 kHz já medido em simulação = spec oficial); FOV de enquadramento |

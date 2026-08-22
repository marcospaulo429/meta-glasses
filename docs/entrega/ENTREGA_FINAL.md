# Entrega Final da Ideia — Medware

**Programa AI Glasses Brasil 2026 · Trilha Bem-Estar · 22/08/2026**

**Repositório oficial:** <https://github.com/marcospaulo429/meta-glasses>

> A **Medware** é um assistente de prontuário automático para consultas mãos livres. O médico atende usando Ray-Ban Meta e sem tocar em telas. O áudio é processado localmente no Android para gerar um rascunho estruturado em Subjetivo, Objetivo, Avaliação e Plano (SOAP); fotos clínicas e atestados podem ser solicitados por voz. O médico revisa e confirma tudo. O pipeline não envia intencionalmente conteúdo clínico a serviços de Inteligência Artificial (IA) em nuvem.

---

# Seção A — Documento estruturado

## A1. Problema

Médicos dividem a atenção entre o paciente e a documentação. Sinsky et al. (*Annals of Internal Medicine*, 2016) observaram aproximadamente duas horas de trabalho em prontuário e tarefas administrativas para cada hora de contato clínico. Isso reduz o contato visual, favorece registros reconstruídos de memória e amplia o trabalho após o expediente.

Nossa hipótese de impacto, a validar em piloto, é devolver **4–5 minutos por consulta**. Em um turno com 16 consultas, isso representa **65–80 minutos por dia**, ou cerca de **25 horas por mês por médico**. A revisão deve consumir no máximo 10% da duração da consulta; se custar mais, o produto não cumpriu sua função.

Métricas de sucesso:

- tempo de documentação antes/depois;
- tempo de revisão do rascunho;
- acurácia de entidades clínicas críticas (fármaco, dose, pressão);
- proporção de fatos com origem verificável na transcrição;
- consultas concluídas sem perda de áudio.

## A2. Usuário-alvo

**Usuário principal:** médico de consultório ou ambulatório em consultas eletivas, especialmente clínica geral e especialidades que se beneficiam de fotografia contextual, como dermatologia e ortopedia.

**Beneficiários:** paciente, que recebe mais atenção durante o atendimento, e instituição de saúde, que recebe um registro revisado, íntegro e auditável.

**Contexto de uso:** consultas de 20–30 minutos, com smartphone Android pareado e óculos Ray-Ban Meta de segunda geração. O telefone permanece guardado durante a consulta e reaparece apenas para o preparo e a revisão.

## A3. Walkthrough principal

1. **Preparo:** antes de qualquer captura, o médico informa finalidade, dados tratados, retenção, descarte e possibilidade de recusa/revogação. O companion registra separadamente consentimento para áudio, presença e consentimento de acompanhante, autorização contextual de foto e autorização de inclusão da Classificação Internacional de Doenças (CID). Sem consentimento para áudio, a captura não inicia; terceiro que não consinta deve sair da área de captação ou a funcionalidade permanece suspensa.
2. **Início:** o médico toca “Iniciar consulta” e guarda o telefone. Um Foreground Service mantém a captura com tela bloqueada e notificação persistente neutra.
3. **Áudio:** o Android solicita o microfone dos óculos pelo perfil Bluetooth para chamadas em modo mãos livres (*Hands-Free Profile* — HFP), conforme a documentação oficial da Meta. O áudio chega ao `AudioRecord` em 8 kHz mono e é processado no telefone.
4. **IA local:** Vosk para português brasileiro transcreve com timestamps. O pipeline extrai fatos, exige proveniência e os organiza em SOAP. Lacunas permanecem “não informado” ou “incerto”.
5. **Foto clínica:** após explicar a finalidade e confirmar autorização específica do paciente e de terceiros enquadrados, o médico diz “registrar imagem”. O app pede confirmação sonora, executa um único burst pelo Kit de Acesso aos Dispositivos (*Device Access Toolkit* — DAT) 0.9 (`addCamera → stream.start → capturePhoto → stop`), cifra a imagem e responde: “Imagem registrada”. Recusar a foto não interrompe o restante da consulta.
6. **Atestado:** o médico solicita um rascunho por voz. Dias e CID só entram se forem enunciados e permanecem sujeitos à revisão explícita; nunca são inferidos. O CID exige autorização específica e revogável. Sem ela, o atestado segue sem CID. O Formato Portátil de Documento (PDF) gerado não é válido como documento médico até receber a assinatura exigida.
7. **Encerramento:** “encerrar consulta” fecha os chunks, gera transcrição e rascunho cifrados e responde por áudio com o número de fatos e pendências.
8. **Revisão humana:** o médico abre a revisão, vê cada fato com o trecho de origem, edita quando necessário e confirma ou descarta. O atestado só vira PDF após confirmação e ainda exige assinatura médica.

**Latência percebida:** comandos simples devem receber confirmação sonora em menos de 1 segundo; tarefas finais podem ocorrer após o encerramento, sem silêncio durante a consulta.

## A4. Walkthrough de exceção

| Falha ou exceção | Resposta do sistema |
|---|---|
| Paciente não consente | A captura não inicia; a consulta segue sem o app. |
| Óculos/HFP indisponíveis | O app anuncia o fallback e usa o microfone do telefone como captura de sala. |
| Voz do paciente chega fraca pelo beamforming dos óculos | O médico pode usar o mic do telefone sobre a mesa; óculos permanecem para câmera e áudio de saída. Esse é o teste prioritário no hardware real. |
| Câmera, bateria ou temperatura entram em condição crítica | A escada remove primeiro vídeo/câmera e preserva áudio; o DAT fornece `ThermalLevel` e erros tipados como `BATTERY_CRITICAL`. |
| App é encerrado no meio da consulta | Chunks selados a cada 60 s limitam a perda; lacunas ficam documentadas e arquivos temporários são descartados no retorno. |
| Modelo de Reconhecimento Automático de Fala (ASR) está ausente | A captura assistida não inicia; o médico é avisado e segue sem o app ou documenta manualmente. Não mantemos áudio indefinidamente para reprocessamento futuro. |
| Termo clínico não é reconhecido | Campo fica incerto/não informado. No teste, um CID mal transcrito foi corretamente omitido em vez de inventado. |
| Paciente pede eliminação | O encontro é descartado e as chaves associadas podem ser destruídas por crypto-erasure. |
| Acompanhante/terceiro não consente | A captura permanece suspensa; só retoma após a pessoa sair da área ou consentir de forma registrada. |
| Terceiro entra durante a consulta | O médico pausa áudio/câmera, informa a pessoa e registra a decisão antes de retomar. |
| Paciente não autoriza foto | Nenhuma imagem é capturada; áudio e documentação podem continuar conforme os demais consentimentos. |
| Paciente não autoriza ou revoga CID | O CID é removido do rascunho/PDF; o atestado pode continuar sem ele. |

**Retenção mínima (requisito para piloto real):** áudio bruto existe somente durante captura e revisão, cifrado localmente, e é eliminado automaticamente após confirmação ou descarte. Fotos só permanecem se o médico confirmar sua necessidade; imagens rejeitadas são eliminadas na revisão. Transcrição e rascunhos locais são eliminados após exportação confirmada ao prontuário oficial ou descarte. Artefatos temporários de consultas abandonadas são limpos no próximo início seguro. Até essa política estar integralmente implementada e validada, o app será demonstrado apenas com atores e dados simulados.

## A5. Decisões técnicas e trade-offs

| Decisão | Por quê | Alternativa descartada |
|---|---|---|
| **IA 100% on-device no Android** | Dado de saúde é sensível; o pipeline clínico não envia intencionalmente conteúdo a serviços remotos. O flavor `sim` não declara permissão `INTERNET`; independência integral de rede ainda será validada no flavor DAT. | Interfaces de Programação de Aplicações (APIs) de ASR e Modelo Grande de Linguagem (LLM) em nuvem: exigem rede e ampliam a superfície da Lei Geral de Proteção de Dados Pessoais (LGPD). |
| **Áudio via HFP do Android; câmera via DAT 0.9** | A Meta documenta HFP como caminho de microfone e `setCommunicationDevice(TYPE_BLUETOOTH_SCO)` no Android. DAT cuida da câmera, não da Modulação por Código de Pulso (PCM) do microfone. | Esperar uma API de microfone no DAT; usar vídeo gravado para extrair áudio depois. Ambas inviabilizam o uso ao vivo. |
| **Foto como evento, não vídeo contínuo** | `capturePhoto()` exige stream ativo, então usamos burst curto. Reduz bateria, exposição de terceiros e risco de coexistência HFP+câmera. | Stream contínuo como padrão: alto consumo e necessidade/adequação questionáveis em ambiente clínico. |
| **Extração factual antes do SOAP, com proveniência obrigatória** | No Produto Mínimo Viável (MVP) heurístico, o sistema reorganiza fatos e não gera afirmações novas. A métrica `unsupported-statement-rate` será o gate para adotar LLM local depois (meta ≤2%). | LLM escrever o prontuário em uma etapa: alucinação difícil de medir e auditar. |
| **Cofre local por envelopes criptográficos** | Padrão Avançado de Criptografia no modo Galois/Counter (AES-GCM) por chunk, chave de criptografia de dados por arquivo, chave de criptografia de chaves no Android Keystore e dados autenticados adicionais vinculando o chunk à consulta. Limita a perda e permite detectar reutilização indevida de chunks. | Cifrar tudo apenas ao final ou guardar em servidor: maior perda em crash ou retorno da nuvem. |

**Estado técnico honesto:** o flavor `sim` e o pipeline clínico estão implementados e testados em emulador. O flavor `dat` foi escrito a partir do sample oficial, mas ainda depende do token `read:packages`, credenciais do Wearables Developer Center e validação no hardware real. Antes do stream simultâneo, a ordem oficial a validar é `addCamera → estabilizar HFP → stream.start`. O simulador atual é nosso `SimDeviceGateway`; o MockDeviceKit oficial será usado assim que o flavor DAT puder resolver as dependências.

## A6. Concorrentes e âncora de originalidade

| Concorrente | O que já resolve | Nossa diferença |
|---|---|---|
| **Nuance DAX Copilot / Abridge** | Ambient scribe maduro, com transcrição e nota clínica. | Processamento em nuvem e telefone/sala como interface. Nossa proposta é local, mãos livres, com câmera egocêntrica e proveniência fato a fato. |
| **Voa Health (Brasil)** | Scribe clínico em PT-BR. | Valida a demanda local, mas usa fluxo por celular/nuvem; nosso pipeline foi desenhado para não depender de internet e explora os óculos como entrada e saída. A operação integral offline ainda será validada no flavor DAT. |

A originalidade não é “mais um resumidor”: é a combinação de **interface vestível sem tela**, **processamento clínico local**, **foto contextual por voz**, **proveniência obrigatória** e **revisão humana antes de qualquer documento oficial**.

## A7. Mapa dos cinco pilares obrigatórios

### 1. Uso de IA

- **Implementado:** Vosk para português brasileiro on-device; timestamps; comandos de voz; extração factual; classificação SOAP; campos incertos; atestado por fala.
- **Evidência:** consulta no Formato de Arquivo de Áudio Waveform (WAV) de 44 s executada de ponta a ponta em emulador; 52 testes automatizados no repositório.
- **Limite conhecido:** Taxa de Erro de Palavras (WER) formal ainda não medida e o Vosk small errou fármacos/doses.
- **Meta antes de piloto:** ≥95% de acurácia em entidades críticas e WER, além da Taxa de Erro de Caracteres (CER), medidas em corpus médico em português brasileiro. Modelo maior ou LLM local só entra se cumprir os gates.

### 2. Câmera/microfone

- **Microfone:** HFP pelo stack Bluetooth, caminho oficial da Meta; captura PCM via `AudioRecord`.
- **Câmera:** DAT 0.9, burst sob demanda e `capturePhoto()` durante stream ativo.
- **Risco:** beamforming HFP favorece a voz de quem veste os óculos e pode atenuar o paciente. Contingência: mic do telefone como captura de sala.

### 3. Saída por áudio

O sistema de conversão de texto em fala (*Text-to-Speech* — TTS) do Android responde pelos alto-falantes dos óculos via HFP com mensagens curtas: confirmação de imagem/atestado, fallback de rota e resumo final. Durante HFP, a saída é 8 kHz mono; isso é suficiente para confirmações, não para conteúdo longo.

### 4. Privacidade e segurança

- dados de saúde tratados localmente, sem envio intencional de conteúdo clínico a serviços remotos;
- consentimento bloqueante, específico por finalidade e revogável;
- AES-GCM, chaves no Android Keystore, arquivos privados, `allowBackup=false` e auditoria local hash-encadeada destinada a detectar alterações;
- analytics e crash reporting do DAT desativados e verificados antes da demo;
- notificação persistente neutra, diodo emissor de luz (LED) nativo quando a câmera estiver ativa e confirmação sonora de cada foto;
- revisão humana obrigatória antes de qualquer registro/documento;
- retenção mínima e eliminação automática de mídia bruta;
- demonstrações exclusivamente com atores e dados simulados.

Na submissão e na demo, os atores serão previamente informados sobre a captura; não serão usados pacientes reais, dados clínicos reais nem ambientes de atendimento em funcionamento. A desativação de analytics/crash reporting e a ausência de conteúdo clínico em logs, notificações e telemetria serão registradas em checklist pré-demo.

**Uso responsável em ambiente clínico:** o MVP adota uma única política conservadora: consentimento específico e revogável, nenhuma captura encoberta, câmera somente por evento, nenhum vídeo contínuo e retenção temporária do áudio cifrado apenas até a revisão. Demonstrações usam exclusivamente atores e dados simulados. Uso com pacientes reais depende de validação institucional, jurídica e das regras vigentes da plataforma antes do piloto.

### 5. Eficiência de bateria

Dados oficiais do Gen 2: até 8 h em uso misto, aproximadamente 5 h de áudio contínuo, case com até 48 h adicionais e 50% de carga em cerca de 20 min. São máximos de fabricante, não garantia sob nossa carga.

**Plano operacional:** consulta de 20–30 min → óculos retornam ao case entre atendimentos. O case é a bateria externa suportada; os óculos não operam conectados a powerbank. Para alto volume, dois pares podem operar em rodízio.

**Plano técnico:** câmera apenas por evento; IA pesada no telefone; escada desliga câmera antes do áudio; telefone pode permanecer em tomada/powerbank. Hoje auditamos a bateria do telefone. O DAT 0.9 oferece nível térmico e erros críticos, mas não identificamos API pública de porcentagem da bateria dos óculos; essa leitura será manual no Meta AI durante o benchmark.

**Teste do hackathon:** anotar bateria inicial/final em consultas de 20 e 30 min nos perfis áudio, áudio+fotos e áudio+stream; medir `%/h`, temperatura, quedas e tempo de recarga. Critério go/no-go: áudio íntegro por 30 min e reserva mínima de 20%; se não cumprir, reduzir câmera e operar por turnos menores.

---

# Seção B — Diagrama de arquitetura

Arquivos para upload:

- imagem: `arquitetura.png` (também disponível em Gráficos Vetoriais Escaláveis — SVG);
- código-fonte: `arquitetura.mmd`.

O diagrama separa óculos, Android e armazenamento local; nomeia HFP, DAT, Vosk, pipeline SOAP, TTS e controles de privacidade. Não há nuvem clínica nem exportação no MVP; Recursos Rápidos de Interoperabilidade em Saúde (*Fast Healthcare Interoperability Resources* — FHIR) aparecem somente como roadmap fora da fronteira funcional entregue.

---

# Seção D — Confirmações finais

## D1. Manutenção de escopo

**O objetivo central foi mantido:** reduzir a carga de documentação clínica com AI Glasses, gerando um rascunho de prontuário para revisão do médico.

O escopo foi refinado após pesquisa do DAT, da Política de Uso Aceitável (AUP) e testes:

- **removemos vídeo contínuo do MVP** e adotamos foto pontual por comando de voz, porque vídeo permanente aumenta bateria, exposição de terceiros e risco de coexistência Bluetooth;
- **mantivemos áudio via HFP**, caminho oficial documentado pela Meta, com fallback para o mic do telefone;
- **adicionamos atestado como rascunho opcional**, reutilizando o mesmo padrão seguro: somente dados enunciados, consentimento de CID e confirmação médica;
- **adiamos LLM local generativo**, mantendo classificação heurística até existir benchmark que cumpra os gates de proveniência e memória.

Essas mudanças preservam o problema e tornam o MVP mais viável, testável e aderente aos princípios de necessidade e minimização.

## D2. Coerência entre artefatos

Confirmamos que documento, diagrama e roteiro do vídeo-pitch descrevem o mesmo MVP: processamento clínico local, áudio temporário cifrado eliminado após revisão, fotos pontuais autorizadas e eliminação imediata das rejeitadas, sem vídeo contínuo, com revisão humana obrigatória e demonstração somente com atores.

## D3. Autoria e uso de IA

Confirmamos que a proposta, as decisões e os artefatos são de autoria da equipe. Ferramentas de IA foram usadas como apoio para pesquisa, revisão crítica, redação, geração assistida de código e testes. A equipe selecionou as decisões, verificou afirmações em fontes oficiais, executou os testes e assume responsabilidade integral pelo conteúdo entregue. Não apresentamos texto ou código gerado sem revisão como evidência de funcionamento.

---

# Fontes principais

1. Meta Wearables DAT 0.9 — documentação, áudio HFP, câmera e ciclo de sessão: <https://wearables.developer.meta.com/llms.txt?full=true>
2. Meta Wearables Acceptable Use Policy: <https://wearables.developer.meta.com/acceptable-use-policy/>
3. Meta — bateria dos AI Glasses: <https://www.meta.com/help/ai-glasses/303057485648146/>
4. Repositório oficial DAT Android e samples: <https://github.com/facebook/meta-wearables-dat-android>
5. Sinsky C. et al. Allocation of Physician Time in Ambulatory Practice. *Annals of Internal Medicine*, 2016.
6. Scheffer M. et al. *Demografia Médica no Brasil 2024*. Faculdade de Medicina da Universidade de São Paulo (FMUSP) / Associação Médica Brasileira (AMB).

# Respostas para o formulário — Medware

Contagens incluem espaços e pontuação.

## Resumo em uma frase — 136/160 caracteres

A Medware prepara rascunhos de prontuário e atestado para médicos durante consultas, usando voz, óculos inteligentes e processamento local.

## A1 — O problema — 280/400 caracteres

Médicos dividem a atenção entre o paciente e a documentação durante e após consultas. Isso reduz o contato visual, favorece registros feitos de memória e prolonga o trabalho. A Medware prepara rascunhos revisáveis de prontuário e atestado, sem retirar do médico a decisão clínica.

## A2 — Usuário-alvo — 262/300 caracteres

Médicos de 25 a 65 anos que fazem consultas eletivas de 20 a 30 minutos em consultórios ou ambulatórios. Têm a atenção dividida pela documentação e usam a Medware em cada consulta consentida, com óculos Ray-Ban Meta e smartphone Android, revisando tudo ao final.

## A3 — Walkthrough de interação

1. **Consentir:** o médico explica a captura e registra a autorização no telefone. Sem consentimento, nada é capturado; com autorização, o sistema confirma por áudio que está pronto.
2. **Iniciar:** o médico toca em “Iniciar consulta” e guarda o telefone. O microfone dos óculos envia áudio ao Android pelo perfil Bluetooth para chamadas em modo mãos livres (*Hands-Free Profile* — HFP); os óculos respondem: “Captura iniciada”.
3. **Conversar:** médico e paciente conversam normalmente. O Vosk para português brasileiro transcreve localmente; a inteligência artificial (IA) extrai fatos, preserva a origem e os organiza em Subjetivo, Objetivo, Avaliação e Plano (SOAP), sem interromper a conversa.
4. **Registrar foto:** após autorização específica, o médico diz “registrar imagem”. O microfone reconhece o comando; a câmera faz uma captura pontual pelo Kit de Acesso aos Dispositivos (*Device Access Toolkit* — DAT) 0.9; os óculos respondem: “Imagem registrada”.
5. **Preparar atestado:** o médico diz “emitir atestado de três dias”. A IA inclui somente dias e códigos clínicos que foram enunciados; os óculos respondem: “Rascunho de atestado preparado para revisão”.
6. **Encerrar:** o médico diz “encerrar consulta”. A IA finaliza a transcrição e o rascunho SOAP com origem verificável; os óculos informam por áudio a quantidade de fatos e pendências.
7. **Revisar:** no telefone, o médico confere a origem de cada fato, corrige, confirma ou descarta. Nenhum prontuário ou atestado se torna oficial automaticamente.

## A4 — Walkthrough de exceção — 335/400 caracteres

Se a transcrição de um código clínico tiver baixa confiança ou não possuir trecho de origem, o validador percebe a incerteza. A Medware omite o código, marca o campo como “não informado” e mantém o restante do rascunho. Pelos óculos, avisa: “Código clínico não confirmado; revise no celular.” Assim, nunca responde com falsa confiança.

## A5 — Decisões técnicas e trade-offs

### Decisão 1

**A5[1].a — A decisão — 70/80 caracteres**  
Fizemos processamento local em vez de inteligência artificial em nuvem

**A5[1].b — Por que esse lado — 79/160 caracteres**  
Evitamos envio intencional de conteúdo clínico e operamos sem depender da rede.

**A5[1].c — O que isso custou — 115/160 caracteres**  
Exige mais memória e energia do telefone. Mitigamos com um pipeline estreito e processamento final após a consulta.

### Decisão 2

**A5[2].a — A decisão — 70/80 caracteres**  
Fizemos áudio via Bluetooth em vez de esperar áudio no toolkit da Meta

**A5[2].b — Por que esse lado — 83/160 caracteres**  
O perfil de chamadas do Android é o canal disponível para áudio ao vivo dos óculos.

**A5[2].c — O que isso custou — 106/160 caracteres**  
A voz do paciente pode chegar fraca. Mitigamos com teste de rota e microfone do telefone como alternativa.

### Decisão 3

**A5[3].a — A decisão — 45/80 caracteres**  
Fizemos foto pontual em vez de vídeo contínuo

**A5[3].b — Por que esse lado — 85/160 caracteres**  
Reduzimos consumo, exposição de terceiros e conflitos entre câmera e áudio Bluetooth.

**A5[3].c — O que isso custou — 110/160 caracteres**  
Perdemos contexto em movimento. Mitigamos com confirmação por voz e nova captura quando a imagem estiver ruim.

### Decisão 4 — opcional

**A5[4].a — A decisão — 54/80 caracteres**  
Fizemos extração factual em vez de texto clínico livre

**A5[4].b — Por que esse lado — 99/160 caracteres**  
Cada fato mantém sua origem na transcrição, facilitando a revisão e reduzindo conteúdo sem suporte.

**A5[4].c — O que isso custou — 90/160 caracteres**  
O rascunho pode ficar incompleto. Mitigamos marcando incertezas e exigindo revisão médica.

### Decisão 5 — opcional

**A5[5].a — A decisão — 52/80 caracteres**  
Fizemos revisão médica em vez de registro automático

**A5[5].b — Por que esse lado — 81/160 caracteres**  
Prontuário e atestado permanecem como rascunhos até o médico revisar e confirmar.

**A5[5].c — O que isso custou — 104/160 caracteres**  
Acrescenta uma etapa no telefone. Mediremos o tempo de revisão e só avançaremos se houver ganho líquido.

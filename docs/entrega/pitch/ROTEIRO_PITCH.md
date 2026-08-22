# Roteiro de apresentação — Medware

Roteiro para um vídeo-pitch de aproximadamente **3 minutos**. O texto em “Fala sugerida” é o que deve ser dito. As demais seções servem para compreender o slide e responder perguntas da banca.

## Visão geral

| Slide | Função na narrativa | Tempo-alvo |
|---|---|---:|
| 1 | Marca e problema | 25 s |
| 2 | Validação local | 20 s |
| 3 | Solução | 25 s |
| 4 | Experiência do usuário | 35 s |
| 5 | Arquitetura e contingências | 40 s |
| 6 | Evidência, equipe e próximos passos | 35 s |

Tempo total: **3 minutos**.

---

## Slide 1 — Medware

### Objetivo

Fazer a banca lembrar do nome, entender o problema e ouvir a promessa central da solução.

### Fala sugerida

> “Nós somos a Medware. Nossa proposta é simples: a consulta fica com o médico; o registro, com a inteligência. Um estudo publicado por Sinsky e colaboradores observou cerca de duas horas de prontuário e tarefas administrativas para cada hora de atendimento clínico. Queremos enfrentar essa disputa por atenção com um assistente clínico mãos livres.”

### O que cada frase significa

- **“Nós somos a Medware.”** Apresenta a marca sem rodeios. Faça uma pausa curta para o nome permanecer na memória.
- **“A consulta fica com o médico.”** O médico continua responsável pelo atendimento, pelo raciocínio e pela decisão clínica.
- **“O registro, com a inteligência.”** A tecnologia ajuda a capturar, organizar e preparar o rascunho. Ela não substitui o médico nem finaliza documentos sozinha.
- **“Duas horas para cada hora.”** É um resultado observado por Sinsky et al. em 2016. Não diga que essa proporção vale para todo médico brasileiro.
- **“Assistente clínico mãos livres.”** O médico usa voz e óculos durante a consulta, reduzindo a alternância contínua para o telefone.

### Como apresentar

Comece olhando para a câmera, não para o slide. Diga “Medware” devagar. Dê ênfase a **médico**, **inteligência** e **duas horas**. A estatística sustenta o problema; ela não deve ocupar toda a abertura.

### Transição

> “Antes de escolher a tecnologia, buscamos entender se essa dor também aparecia na prática.”

### Não diga

- que todo médico gasta exatamente duas horas;
- que a Medware já eliminou esse tempo;
- que a inteligência artificial faz o prontuário sozinha.

---

## Slide 2 — Validação local

### Objetivo

Mostrar contato com um profissional real e, ao mesmo tempo, transparência sobre o estágio da validação.

### Fala sugerida

> “Na nossa escuta inicial, o Dr. Ranieri resumiu bem o problema: ‘O prontuário deveria registrar a consulta, não interrompê-la.’ Essa fala orientou nossas perguntas sobre tempo, contato visual e confiança. Agora, a validação deve medir quatro eixos: precisão, privacidade, revisão médica e bateria.”

### O que cada frase significa

- **“Escuta inicial.”** Houve uma conversa relevante, mas não um estudo clínico ou uma pesquisa quantitativa.
- **Citação do Dr. Ranieri.** Mostra que a formulação do problema faz sentido para um médico. Não significa que ele validou todo o produto.
- **Tempo.** Quanto da rotina é consumido pela documentação antes e depois da consulta.
- **Contato visual.** Se o registro atual interrompe a relação entre médico e paciente.
- **Confiança.** O que o médico precisa enxergar para confiar em um rascunho automático.
- **Precisão, privacidade, revisão e bateria.** São eixos a medir, não resultados já comprovados.

### Como apresentar

Leia a citação com uma pausa antes de **“não interrompê-la”**. Ao falar dos quatro eixos, marque cada palavra com um ritmo regular. O tom deve ser seguro, não defensivo.

### Transição

> “Com esse problema em foco, desenhamos uma experiência para atender primeiro e revisar depois.”

### Não diga

- que a Medware foi validada ou aprovada por médicos;
- que houve teste com pacientes;
- que já foi comprovado ganho de tempo ou de confiança.

---

## Slide 3 — A solução

### Objetivo

Explicar a Medware em linguagem simples e mostrar onde permanece o controle humano.

### Fala sugerida

> “A Medware permite que o médico conduza a consulta sem alternar continuamente para uma tela. O áudio é processado localmente no Android, e comandos de voz permitem registrar uma foto ou preparar um atestado. Ao final, o médico recebe um rascunho clínico para editar, confirmar ou descartar. A inteligência prepara; o médico decide.”

### O que cada frase significa

- **“Atender primeiro. Revisar depois.”** A revisão não desaparece; ela é deslocada para um momento explícito após a consulta.
- **“Sem alternar continuamente para uma tela.”** O telefone ainda é usado no preparo e na revisão. Não prometa uma experiência permanentemente sem telas.
- **“Processado localmente.”** O conteúdo clínico é tratado no Android, sem envio intencional para serviços de inteligência artificial em nuvem.
- **“Registrar uma foto.”** A captura é pontual, autorizada e acionada por voz. Não há vídeo contínuo no produto mínimo viável.
- **“Preparar um atestado.”** O sistema gera apenas um rascunho. O documento exige revisão, confirmação e assinatura médica.
- **“O médico decide.”** É a principal barreira contra automação indevida.

### Como apresentar

Dê ênfase a **localmente**, **rascunho** e **médico decide**. Não leia os três comandos exibidos; cite apenas um como exemplo se houver tempo.

### Transição

> “Na prática, essa experiência acontece em cinco momentos.”

### Não diga

- que o médico nunca usa tela;
- que a Medware emite atestados autonomamente;
- que o rascunho vira prontuário sem confirmação.

---

## Slide 4 — A experiência

### Objetivo

Percorrer o caso de uso de ponta a ponta e demonstrar o comportamento seguro quando existe incerteza.

### Fala sugerida

> “O fluxo começa pelo consentimento. Depois, médico e paciente conversam normalmente, e somente o conteúdo autorizado entra no sistema. Por voz, o médico pode solicitar uma foto ou um rascunho de atestado. No Android, a Medware organiza fatos clínicos e preserva a origem de cada informação. Por fim, o médico revisa, corrige e confirma. Se a transcrição falhar em um código clínico, o sistema omite o dado em vez de inventar.”

### O que cada etapa significa

- **Consentir.** Antes de capturar, o paciente é informado sobre finalidade, dados, retenção e possibilidade de recusa.
- **Conversar.** A consulta segue de forma natural; o objetivo é reduzir interrupções, não vigiar continuamente.
- **Registrar.** Fotos e atestados dependem de comando e autorização. A foto usa uma captura pontual.
- **Estruturar.** O sistema extrai fatos presentes na transcrição e os organiza no modelo clínico.
- **Prontuário com origem.** Cada fato deve apontar para o trecho que lhe deu origem, permitindo conferência.
- **Revisar.** O médico pode editar, confirmar, descartar ou apagar.
- **“Omite, não adivinha.”** Quando há incerteza, a Medware reduz a automação. Essa é uma decisão de segurança, não uma falha escondida.

### Como apresentar

Acompanhe visualmente os cinco blocos da esquerda para a direita. Diminua o ritmo na frase final e enfatize **omite** e **não inventa**.

### Transição

> “Por trás desses cinco momentos, cada tecnologia tem uma função bem delimitada.”

### Não diga

- que o consentimento sozinho resolve toda a conformidade legal;
- que a transcrição é perfeita;
- que o sistema diagnostica ou completa informações ausentes.

---

## Slide 5 — Como funciona

### Objetivo

Demonstrar viabilidade técnica, privacidade e capacidade de continuar de forma segura quando uma camada falha.

### Fala sugerida

> “Os óculos fornecem áudio, câmera e interação mãos livres. No Android, o Vosk transcreve localmente, e o pipeline organiza fatos com origem verificável. O médico revisa o resultado, e as confirmações retornam por voz. O áudio usa o perfil Bluetooth de chamadas; a câmera usa o toolkit da Meta. O conteúdo clínico não é enviado intencionalmente para inteligência em nuvem. E, se uma camada falhar, reduzimos a automação: preservamos o áudio, usamos o microfone do telefone ou marcamos a informação como não informada.”

### O que cada bloco significa

- **Óculos.** Funcionam como interface de captura e saída, não como computador que executa toda a inteligência.
- **Android local.** É onde rodam captura, transcrição, organização clínica, criptografia e revisão.
- **Vosk.** É o mecanismo atual de reconhecimento de fala em português brasileiro.
- **Pipeline clínico.** Extrai fatos, organiza o rascunho e mantém a origem das informações.
- **Revisão.** Nenhum resultado vira documento oficial sem decisão humana.
- **Voz.** As respostas devem ser curtas porque os óculos não possuem tela.
- **Perfil Bluetooth de chamadas.** É o caminho documentado para acessar o microfone dos óculos.
- **Toolkit da Meta.** É o caminho usado para controlar a câmera e capturar uma foto pontual.
- **“Sem inteligência em nuvem.”** Refere-se ao conteúdo clínico. O toolkit ainda pode exigir serviços da plataforma, com telemetria desativada conforme a configuração prevista.
- **Degradação segura.** Uma falha remove funções antes de comprometer o registro: câmera é desativada antes do áudio, o telefone pode assumir o microfone e dados incertos não são inferidos.

### Como apresentar

Percorra as caixas da esquerda para a direita. Não transforme a fala em lista de tecnologias. A ideia principal é: **cada camada tem responsabilidade limitada e o sistema falha com segurança**.

### Transição

> “Essa arquitetura já deixou de ser apenas desenho; ela possui uma base executável e testada.”

### Não diga

- que todo o processamento ocorre dentro dos óculos;
- que o toolkit da Meta fornece áudio do microfone;
- que a integração já foi validada no hardware real;
- que processamento local, sozinho, garante conformidade com a Lei Geral de Proteção de Dados Pessoais.

---

## Slide 6 — Evidência, equipe e próximos passos

### Objetivo

Provar capacidade de execução, separar o que já funciona do que falta validar, apresentar a equipe e fechar com impacto humano.

### Fala sugerida

> “Hoje, temos 52 testes automatizados cobrindo o pipeline executado em emulador: prontuário, foto, atestado, cofre criptográfico e revisão. O próximo passo é validar no hardware real a voz pelos óculos, a foto pontual e a autonomia por consulta. Nosso diferencial combina processamento clínico local, origem verificável e decisão médica. Lucas Pacheco lidera Android e front-end; Marcos Paulo, inteligência artificial; e Lucas Isaac, inteligência artificial, privacidade e requisitos. A Medware não substitui o médico. Ela devolve tempo e atenção ao paciente.”

### O que cada frase significa

- **“52 testes automatizados.”** É evidência objetiva da lógica de software coberta pelos testes.
- **“Executado em emulador.”** Delimita honestamente o ambiente validado. Não prova integração com os óculos reais.
- **Prontuário, foto, atestado, cofre e revisão.** Resume as áreas funcionais cobertas pelo pipeline.
- **Hardware real.** Ainda precisam ser medidos áudio, câmera, bateria, latência e estabilidade no dispositivo do hackathon.
- **Processamento clínico local.** Reduz exposição de dados sensíveis e dependência de rede.
- **Origem verificável.** Permite ao médico conferir de onde veio cada fato.
- **Decisão médica.** A revisão e a confirmação continuam obrigatórias.
- **Equipe.** Os papéis cobrem execução Android, inteligência artificial, interface, privacidade e requisitos.
- **“Não substitui o médico.”** Delimita responsabilidade e evita uma promessa inadequada.
- **“Devolve tempo e atenção.”** É a visão de impacto; ainda precisa ser medida em piloto.

### Como apresentar

Dê ênfase a **52 testes**, mas faça uma pausa antes de **em emulador**. Apresente os nomes sem pressa. Na última frase, olhe diretamente para a câmera, reduza o ritmo e encerre. Não acrescente explicações depois dela.

### Não diga

- que os 52 testes validam os óculos ou o produto completo;
- que câmera, áudio e bateria já foram testados no hardware real;
- que a redução de tempo já foi comprovada;
- que a Medware está pronta para uso com pacientes.

---

## Pronúncia e termos técnicos

- **Medware:** “méd-uér”.
- **Vosk:** “vósk”.
- **Android:** use a pronúncia comum em português.
- Se precisar citar **HFP**, diga “agá-efe-pê” e explique: perfil Bluetooth de chamadas.
- Se precisar citar **DAT**, diga “dê-á-tê” e explique: toolkit de acesso aos dispositivos da Meta.
- Evite falar nomes de classes de código, versões e detalhes criptográficos no pitch; deixe-os para perguntas da banca.

## Como ensaiar

1. Grave uma leitura sem slides e marque o tempo de cada bloco.
2. Corte palavras, nunca acelere para caber.
3. Treine as transições até não precisar anunciá-las como títulos.
4. Faça uma pausa curta após a marca, a citação, a limitação do emulador e a frase final.
5. Mantenha entre 2:50 e 3:00 para absorver respirações e troca de slides.
6. Se houver três apresentadores, uma divisão natural é:
   - Lucas Pacheco: slides 1 e 3;
   - Marcos Paulo: slides 4 e 5;
   - Lucas Isaac: slides 2 e 6.

## Respostas curtas para perguntas prováveis

**Já funciona nos óculos reais?**  
“A lógica do pipeline está testada em emulador. A integração de áudio, câmera e bateria será validada no hardware real no hackathon.”

**A inteligência pode inventar informação clínica?**  
“O produto mínimo viável extrai e organiza fatos presentes na transcrição. Informação incerta é marcada como não informada, e o médico revisa tudo.”

**Os dados vão para a nuvem?**  
“O conteúdo clínico é processado localmente no Android e não é enviado intencionalmente para serviços de inteligência artificial em nuvem.”

**O paciente pode recusar?**  
“Sim. Sem consentimento para áudio, a captura não inicia. Autorizações de foto e de código clínico são separadas.”

**Por que usar óculos em vez de apenas um celular?**  
“Os óculos mantêm câmera, voz e retorno sonoro na perspectiva do médico, reduzindo a alternância de atenção durante a consulta; o telefone permanece como unidade local de processamento e revisão.”

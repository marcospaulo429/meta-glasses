# LGPD e Privacidade — Assistente de Prontuário Automático

> Análise de conformidade com a Lei nº 13.709/2018 (LGPD) e desenho privacy-by-design.
> Este documento alimenta diretamente o checkpoint obrigatório "Privacidade e dados" (edital, Seção 8.1) e o critério "Considerações éticas" (20 pts no Segundo Filtro).
> Última atualização: 2026-08-15.

## 1. Enquadramento legal

| Questão | Resposta |
|---|---|
| Que dados tratamos? | Voz do paciente e do médico (áudio), transcrição, fotos clínicas pontuais, rascunho SOAP, metadados de consulta (encounter_id, timestamps) |
| Natureza dos dados | **Dados pessoais sensíveis** — dado referente à saúde (art. 5º, II) + biometria vocal potencial |
| Base legal principal | Art. 11, II, "f": **tutela da saúde**, em procedimento realizado por profissionais de saúde — complementada por **consentimento específico e destacado** (art. 11, I) como salvaguarda adicional |
| Controlador | A instituição de saúde / o profissional (na demo: a equipe, com dados simulados) |
| Operador | O app (processamento local no dispositivo do controlador — sem terceiro na cadeia) |
| Transferência internacional | **Nenhuma** — processamento 100% on-device; único ponto de atenção é a telemetria do DAT, desativada via opt-out |

## 2. Princípios do art. 6º aplicados ao desenho

| Princípio | Como o projeto atende |
|---|---|
| Finalidade | Única e explícita: apoio à documentação clínica da consulta em andamento. Nada de uso secundário |
| Adequação/Necessidade (minimização) | Áudio só durante consulta ativa; câmera só por comando pontual; sem wake-word em idle; sem vídeo contínuo |
| Livre acesso / Transparência | Paciente informado antes da captura; companion mostra tudo que foi capturado; notificação persistente durante gravação |
| Qualidade dos dados | Extração factual com provenance; campos "não informado"/"incerto"; revisão humana obrigatória antes do registro oficial |
| Segurança | Criptografia em repouso (Android Keystore + SQLCipher ou equivalente); app não exportado; sem backup automático de dados clínicos (`allowBackup=false`) |
| Prevenção | Threat model antes do hackathon; opt-out de telemetria/crash do SDK |
| Não discriminação | IA não infere diagnóstico nem completa lacunas; apenas organiza o que foi dito |
| Responsabilização | Log de auditoria local: quem iniciou, quando, consentimento registrado, o que foi editado na revisão |

## 3. Fluxo de consentimento (desenho)

1. **Antes da consulta**: médico abre o companion, informa o paciente verbalmente e registra o consentimento (toque do próprio médico + registro de data/hora; em produto real: assinatura/termo institucional).
2. **Recusa**: se o paciente recusa, a captura automática **não inicia** — a consulta segue normal sem o app.
3. **Durante**: notificação persistente no Android indica captura ativa (sem nome do paciente ou conteúdo clínico); médico pode encerrar por voz ou pelo app a qualquer momento.
4. **Revogação**: paciente pode pedir exclusão; companion tem ação "descartar consulta" que apaga áudio, transcrição, fotos e rascunho.

## 4. Ciclo de vida dos dados

| Etapa | Regra |
|---|---|
| Captura | Buffer local criptografado; nenhum dado sai do dispositivo |
| Processamento | VAD/ASR/LLM on-device; sem chamada de rede com conteúdo clínico |
| Retenção do áudio bruto | Mínima: descartado após confirmação do prontuário pelo médico (política final a validar com instituição/DPO) |
| Retenção de transcrição/SOAP | Até exportação para o sistema oficial; depois, descarte ou retenção conforme política institucional |
| Fotos | Vinculadas ao encounter_id, mesmas regras do áudio |
| Treinamento de modelos | **Nunca** com dados clínicos, por padrão e por design |
| Descarte | Exclusão segura; ação "descartar consulta" disponível a qualquer momento |

## 5. Medidas técnicas obrigatórias (checklist)

- [ ] `ANALYTICS_OPT_OUT=true` e `CRASH_REPORTING_OPT_OUT=true` no manifest (telemetria DAT).
- [ ] `android:allowBackup="false"` e exclusão de dados clínicos de qualquer auto-backup.
- [ ] Criptografia em repouso (Keystore-backed).
- [ ] Notificação de FGS sem dados pessoais.
- [ ] Nenhuma permissão de rede usada para conteúdo clínico (idealmente: demonstrar em modo avião).
- [ ] Log de auditoria local (consentimento, início/fim, edições da revisão).
- [ ] Tela "descartar consulta" com exclusão completa.
- [ ] Dados de demo do hackathon: **somente consulta simulada com atores/roteiro** — nunca paciente real.

## 6. Pontos de atenção / riscos residuais

| Risco | Tratamento |
|---|---|
| AUP da Meta proíbe encorajar gravação em "locais sensíveis" (ver POL-01 em docs/LIMITACOES.md) | Gravação profissional, consentida, transparente e iniciada pelo responsável; validar interpretação com mentores Meta |
| Voz de terceiros (acompanhante) captada incidentalmente | Informar no consentimento; minimização de retenção; não identificar falantes além de médico/paciente |
| Prontuário é documento regulado (CFM) | Posicionar o produto como **rascunho de apoio**; registro oficial é ato do médico após revisão — nunca automático |
| Metadados da notificação/BT | Notificação genérica; nome BT do device sem identificação de paciente |

## 7. Argumento para a banca (resumo de 30 segundos)

"Tratamos dado sensível de saúde, então fizemos o caminho mais conservador: tudo roda no telefone, nada vai para a nuvem, telemetria do SDK desligada, o paciente consente antes, pode recusar e pode apagar. A IA não inventa: só organiza o que foi dito, com rastreabilidade de cada fato até o trecho do áudio, e o médico revisa e confirma antes de existir qualquer registro oficial. LGPD: base legal de tutela da saúde (art. 11, II, f) com consentimento destacado como salvaguarda."

## 8. Gravação de segurança em vídeo contínuo (proposta 15/08) — EM DISPUTA

**Status: feature flag desligada por padrão (`SECURITY_VIDEO_ENABLED=false`). Decisão do time pendente antes de 22/08.**

- **Parecer do guardião LGPD: ⛔** — vídeo contínuo do paciente falha o teste de **necessidade** (art. 6º, III): a finalidade de proteção médico-legal é atingível com meios menos invasivos já existentes (áudio integral consentido + fotos pontuais + log de auditoria hash-encadeado com hash de integridade dos artefatos). Também agrava frontalmente POL-01 (AUP da Meta: gravação em locais sensíveis).
- **Se o time decidir prosseguir mesmo assim**, requisitos mínimos inegociáveis:
  1. Base legal: exercício regular de direitos (art. 11, II, "d") **+ consentimento específico e destacado para o vídeo**, separado do consentimento do áudio clínico.
  2. Consentimento também do **acompanhante**; aviso visível na porta do consultório (analogia CCTV) para terceiros incidentais; direito de eliminação garantido.
  3. Vídeo nunca alimenta a IA, nunca sai do dispositivo, cifrado por chunk (AES-GCM, DEK por chunk, KEK no Keystore, envelope duplo com chave de recuperação institucional).
  4. Break-glass: justificativa estruturada registrada **antes** da decifra + custodiante institucional (nunca o médico usuário) + biometria + log de auditoria append-only hash-encadeado.
  5. Retenção máxima definida por prescrição de responsabilidade civil (referência: 3 anos CC art. 206 §3º V / 5 anos CDC art. 27 — validar com jurídico); destruição criptográfica no vencimento.
  6. Validar com mentores Meta se a AUP comporta esse uso **antes** de apresentar no pitch.
- **Alternativa recomendada pelo guardião** (proteção equivalente sem vídeo): manter áudio integral + fotos pontuais com **cadeia de custódia**: hash SHA-256 de cada artefato no log de auditoria assinado, provando integridade e não-adulteração — atende à finalidade médico-legal com minimização.

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

## 8. Gravação de segurança em vídeo — MODO BLINDADO (adotado 15/08)

**Status: implementado atrás de feature flag (`SECURITY_VIDEO_ENABLED=false` por padrão). Desenho refinado após o parecer ⛔ do guardião: a versão adotada responde às objeções com garantia técnica, não só política.**

Desenho: o vídeo fica no celular do médico, mas cada chunk é cifrado com DEK embrulhada **somente** na chave pública do custodiante institucional (`WrapPolicy.RecoveryOnly`). O aparelho é **tecnicamente incapaz** de decifrar — nem o médico assiste. A chave privada fica com o custodiante (direção clínica/DPO/escrow), que só a usa mediante **ordem judicial**. Sem chave pública configurada, o vídeo simplesmente não grava (invariante no código).

Salvaguardas obrigatórias:
1. Base legal: exercício regular de direitos (art. 11, II, "d") + **consentimento específico e destacado para o vídeo**, separado do áudio clínico.
2. Consentimento também do **acompanhante**; aviso visível na porta (analogia CCTV) para terceiros incidentais.
3. Vídeo nunca alimenta a IA, nunca sai do dispositivo; AAD por chunk impede transplante entre consultas.
4. Break-glass: justificativa estruturada registrada **antes** da decifra + log de auditoria hash-encadeado; decifra sem justificativa é recusada pelo código.
5. Eliminação (art. 18): **crypto-erasure** — apagar as DEKs embrulhadas do manifesto torna o vídeo irrecuperável sem precisar decifrá-lo.
6. Retenção máxima por prescrição de responsabilidade civil (3 anos CC art. 206 §3º V / 5 anos CDC art. 27 — validar com jurídico); destruição criptográfica no vencimento.
7. Risco residual SEC-01: perda da chave privada = vídeo irrecuperável — custódia com backup redundante (processo institucional, não código).
8. ⚠️ AUP da Meta (POL-01): criptografar não muda a *captura* em local sensível — validar com mentores Meta **antes** do pitch.

Narrativa de pitch: liderar com áudio+fotos+cadeia de custódia (hash SHA-256 no log assinado); apresentar o vídeo blindado como camada **opcional** de proteção médico-legal — "nem o médico consegue assistir; só ordem judicial abre".

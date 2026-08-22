# Medware

**Assistente de prontuário automático** da trilha Bem-Estar do Programa AI Glasses Brasil 2026 (CEIA/UFG/FUNAPE/Meta).

O médico conduz a consulta com Ray-Ban Meta, sem tocar em telas. O Android processa o áudio localmente, extrai fatos com proveniência e prepara um rascunho SOAP. Fotos clínicas e atestados podem ser solicitados por voz. Nada se torna registro oficial sem revisão humana.

## Entrega da competição

| Artefato | Formato editável | Formato para envio |
|---|---|---|
| Documento oficial | [ENTREGA_FINAL.md](docs/entrega/ENTREGA_FINAL.md) | [ENTREGA_FINAL.pdf](docs/entrega/ENTREGA_FINAL.pdf) |
| Arquitetura | [arquitetura.mmd](docs/entrega/arquitetura.mmd) | [arquitetura.png](docs/entrega/arquitetura.png) |
| Apresentação | [PITCH_AI_GLASSES_BRASIL.pptx](docs/entrega/pitch/PITCH_AI_GLASSES_BRASIL.pptx) | [PITCH_AI_GLASSES_BRASIL.pdf](docs/entrega/pitch/PITCH_AI_GLASSES_BRASIL.pdf) |

## Diferenciais

- processamento clínico local, sem envio intencional de conteúdo clínico para IA em nuvem;
- captura de áudio pelo perfil Bluetooth HFP e foto pontual pela câmera via DAT 0.9;
- extração factual antes da classificação SOAP, com origem rastreável;
- consentimento específico, retenção mínima e criptografia local;
- revisão e confirmação obrigatórias pelo médico;
- 52 testes automatizados e pipeline validado de ponta a ponta no emulador.

## Mapa do repositório

| Caminho | Conteúdo |
|---|---|
| [docs/entrega/](docs/entrega/) | Entrega oficial: documento, arquitetura e apresentação |
| [docs/PLANO.md](docs/PLANO.md) | Plano mestre até o hackathon (18/09) |
| [docs/LIMITACOES.md](docs/LIMITACOES.md) | Limitações encontradas × mitigações, com fonte e status (documento vivo) |
| [docs/LGPD.md](docs/LGPD.md) | Conformidade LGPD / privacidade / checkpoint do edital |
| [docs/PESQUISA.md](docs/PESQUISA.md) | Resumo verificado das fontes (AUP, Muse Glimmer, repo DAT) |
| [android/](android/) | App companion (Kotlin). Flavors: `sim` (roda sem óculos/token) e `dat` (DAT 0.9.0 real) |
| [android/TESTING.md](android/TESTING.md) | Guia de testes em 3 níveis: emulador · celular físico · óculos no hackathon |
| [scripts/](scripts/) | Utilitários (instalação do modelo ASR no aparelho) |
| `vendor/meta-wearables-dat-android/` | Clone do repo oficial DAT (referência local; não versionado) |
| `docs/*.pdf` | Edital e materiais históricos de referência |

## Executar os testes

```bash
cd android
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
./gradlew :app:assembleSimDebug        # flavor sim — sem token, sem óculos
./gradlew :app:testSimDebugUnitTest    # testes (cofre, auditoria, escada de energia)
```

Flavor `dat` (integração real) exige: `github_token` (escopo `read:packages`) em `android/local.properties` + `mwdatAppId`/`mwdatClientToken` do [Wearables Developer Center](https://wearables.developer.meta.com/) via `-P` ou `gradle.properties` — ver AND-07/DAT-08 em [docs/LIMITACOES.md](docs/LIMITACOES.md).

**Guia de testes** (emulador · celular físico · óculos no hackathon): [android/TESTING.md](android/TESTING.md).

## Estado do MVP

**MVP `sim` funcional e testado fim-a-fim em emulador** (52 testes automatizados): consulta por voz → transcrição PT-BR on-device → rascunho SOAP com validação anti-alucinação → foto e atestado (PDF) por comando de voz → revisão do médico — tudo cifrado, auditado e sem permissão de INTERNET no manifest. Vídeo blindado opcional (indecifrável no aparelho; só custodiante com ordem judicial). Flavor `dat` aguarda credenciais (AND-07). Próximos marcos: Entrega Final (**22/08**), Hackathon (**18/09**, Meta SP).

Convenções: commits em inglês (conventional commits); branch de trabalho: `main`.
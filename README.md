# meta-glasses

Planejamento do **Assistente de Prontuário Automático** — Programa AI Glasses Brasil 2026 (CEIA/UFG/FUNAPE/Meta).

Consulta médica capturada pelos Ray-Ban Meta Wayfarer Gen 2, processada 100% on-device no Android (ASR → extração factual → SOAP), com revisão humana obrigatória. Sem display: interação por voz; saída por áudio.

## Mapa do repositório

| Arquivo | Conteúdo |
|---|---|
| [MEMORY.md](MEMORY.md) | Fonte de verdade: decisões, fatos verificados, datas, pendências |
| [docs/PLANO.md](docs/PLANO.md) | Plano mestre até o hackathon (18/09) |
| [docs/LIMITACOES.md](docs/LIMITACOES.md) | Limitações encontradas × mitigações (documento vivo) |
| [docs/LGPD.md](docs/LGPD.md) | Conformidade LGPD / privacidade / checkpoint do edital |
| [docs/PESQUISA.md](docs/PESQUISA.md) | Resumo verificado das fontes (AUP, Muse Glimmer, repo DAT) |
| [.github/agents/](.github/agents/) | Subagentes: orquestrador, pesquisador, arquiteto-android, lgpd-guardian, redator-entrega |
| `android/` | App companion (Kotlin). Flavors: `sim` (roda sem óculos/token) e `dat` (DAT 0.9.0 real) |
| `vendor/meta-wearables-dat-android/` | Clone do repo oficial DAT (referência local; não versionado) |
| `docs/*.pdf` | Edital e arquitetura v2 |

## Build do app

```bash
cd android
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
./gradlew :app:assembleSimDebug        # flavor sim — sem token, sem óculos
./gradlew :app:testSimDebugUnitTest    # testes (cofre, auditoria, escada de energia)
```

Flavor `dat` (integração real) exige: `github_token` (escopo `read:packages`) em `android/local.properties` + `mwdatAppId`/`mwdatClientToken` do [Wearables Developer Center](https://wearables.developer.meta.com/) via `-P` ou `gradle.properties` — ver AND-07/DAT-08 em [docs/LIMITACOES.md](docs/LIMITACOES.md).

**Guia de testes** (emulador · celular físico · óculos no hackathon): [android/TESTING.md](android/TESTING.md).

## Estado atual

**MVP `sim` funcional (15/08)**: consulta fim-a-fim sem óculos — áudio cifrado por chunks, ASR PT-BR (Vosk) com comandos de voz, rascunho SOAP com validação anti-alucinação, vídeo blindado opcional (indecifrável no aparelho; só custodiante com ordem judicial), crypto-erasure, auditoria hash-encadeada e tela de revisão. Modelo ASR: `scripts/install-vosk-model.sh`. Flavor `dat` aguarda token/credenciais (AND-07). Próximos marcos: Entrega Final da Ideia (**22/08**), Hackathon (**18/09**, Meta SP).
# Apresentação

Apresentação de seis slides para o vídeo-pitch do **Assistente de Prontuário Automático**.

Repositório: <https://github.com/marcospaulo429/meta-glasses>

## Arquivos finais

- [PITCH_AI_GLASSES_BRASIL.pptx](PITCH_AI_GLASSES_BRASIL.pptx) — apresentação editável com notas do apresentador;
- [PITCH_AI_GLASSES_BRASIL.pdf](PITCH_AI_GLASSES_BRASIL.pdf) — versão exportada para conferência e envio;
- [preview/](preview/) — prévias dos seis slides.

## Regenerar o PPTX

Requisitos: Node.js 18 ou superior.

```bash
npm install
npm run gerar
```

O arquivo [generate_slides.js](generate_slides.js) contém o conteúdo, o layout e as notas do apresentador. A dependência está fixada em [package-lock.json](package-lock.json) para que o resultado seja reproduzível.

## Exportar e conferir

Com LibreOffice e Poppler instalados:

```bash
soffice --headless --convert-to pdf --outdir . PITCH_AI_GLASSES_BRASIL.pptx
find preview -type f -delete
pdftoppm -png -r 90 PITCH_AI_GLASSES_BRASIL.pdf preview/slide
```

## Validação local

O slide 2 inclui uma frase aprovada para atribuição ao Dr. Ranieri e apresenta as perguntas que orientarão a próxima rodada de entrevistas. Os eixos de precisão, privacidade, revisão e bateria são temas de investigação, não resultados quantitativos.

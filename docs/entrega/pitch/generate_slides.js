const pptxgen = require('pptxgenjs');
const path = require('path');

const pptx = new pptxgen();
pptx.layout = 'LAYOUT_WIDE';
pptx.author = 'Equipe Assistente de Prontuário Automático';
pptx.subject = 'Vídeo-pitch — AI Glasses Brasil 2026';
pptx.title = 'Assistente de Prontuário Automático';
pptx.company = 'AI Glasses Brasil 2026';
pptx.lang = 'pt-BR';
pptx.theme = {
  headFontFace: 'Aptos Display',
  bodyFontFace: 'Aptos',
  lang: 'pt-BR',
};
pptx.defineSlideMaster({
  title: 'MASTER',
  background: { color: 'F7F8F5' },
  objects: [
    { rect: { x: 0, y: 7.38, w: 13.333, h: 0.12, fill: { color: '0E604B' }, line: { color: '0E604B' } } },
  ],
  slideNumber: { x: 12.66, y: 7.05, w: 0.28, h: 0.18, color: '64706C', fontFace: 'Aptos', fontSize: 8, align: 'right' },
});

const C = {
  ink: '192321',
  muted: '60706B',
  green: '0E604B',
  mint: 'D9EEE6',
  lime: 'C8E662',
  coral: 'F06B4F',
  cream: 'F7F8F5',
  white: 'FFFFFF',
  line: 'CCD5D1',
  pale: 'EAF0ED',
  amber: 'E9B949',
};

const outDir = __dirname;
const hero = path.join(outDir, 'assets', 'doctor-hero.jpg');
const repositoryUrl = 'https://github.com/marcospaulo429/meta-glasses';
const repositoryLabel = 'github.com/marcospaulo429/meta-glasses';

function addHeader(slide, kicker, title, subtitle) {
  slide.addText(kicker.toUpperCase(), {
    x: 0.65, y: 0.35, w: 4.4, h: 0.25,
    fontFace: 'Aptos', fontSize: 10, bold: true, color: C.green,
    charSpacing: 1.4, margin: 0,
  });
  slide.addText(title, {
    x: 0.65, y: 0.72, w: 12.0, h: 0.72,
    fontFace: 'Aptos Display', fontSize: 30, bold: true, color: C.ink,
    margin: 0, breakLine: false, fit: 'shrink',
  });
  if (subtitle) {
    slide.addText(subtitle, {
      x: 0.67, y: 1.48, w: 11.8, h: 0.35,
      fontFace: 'Aptos', fontSize: 13, color: C.muted, margin: 0,
    });
  }
}

function addFooter(slide, text = 'Assistente de Prontuário Automático · AI Glasses Brasil 2026') {
  slide.addText(text, {
    x: 0.65, y: 7.03, w: 5.9, h: 0.18,
    fontFace: 'Aptos', fontSize: 7.5, color: C.muted, margin: 0,
  });
  slide.addText(repositoryLabel, {
    x: 6.65, y: 7.03, w: 5.45, h: 0.18,
    fontFace: 'Aptos', fontSize: 7.5, color: C.green, margin: 0,
    align: 'right', hyperlink: { url: repositoryUrl },
  });
}

function addBullet(slide, text, x, y, w, color = C.ink, accent = C.green) {
  slide.addShape(pptx.ShapeType.ellipse, {
    x, y: y + 0.12, w: 0.12, h: 0.12,
    fill: { color: accent }, line: { color: accent },
  });
  slide.addText(text, {
    x: x + 0.24, y, w: w - 0.24, h: 0.38,
    fontFace: 'Aptos', fontSize: 17, color, margin: 0,
    breakLine: false, fit: 'shrink',
  });
}

function addPill(slide, text, x, y, w, fill, color = C.ink) {
  slide.addShape(pptx.ShapeType.roundRect, {
    x, y, w, h: 0.42, rectRadius: 0.08,
    fill: { color: fill }, line: { color: fill },
  });
  slide.addText(text, {
    x: x + 0.12, y: y + 0.07, w: w - 0.24, h: 0.22,
    fontFace: 'Aptos', fontSize: 10.5, bold: true, color,
    align: 'center', margin: 0, fit: 'shrink',
  });
}

function addNotes(slide, lines) {
  if (typeof slide.addNotes === 'function') slide.addNotes(lines.join('\n'));
}

// 1 — Problema
{
  const slide = pptx.addSlide();
  slide.background = { color: '0B211B' };
  slide.addImage({ path: hero, x: 6.0, y: 0, w: 7.333, h: 7.5, transparency: 8 });
  slide.addShape(pptx.ShapeType.rect, {
    x: 5.55, y: 0, w: 2.2, h: 7.5,
    fill: { color: '0B211B', transparency: 20 },
    line: { color: '0B211B', transparency: 100 },
  });
  slide.addText('O PRONTUÁRIO DISPUTA\nATENÇÃO COM O PACIENTE', {
    x: 0.72, y: 0.72, w: 5.6, h: 1.45,
    fontFace: 'Aptos Display', fontSize: 29, bold: true, color: C.white,
    margin: 0, breakLine: false, fit: 'shrink',
  });
  slide.addText('Escutar, raciocinar e documentar — ao mesmo tempo.', {
    x: 0.75, y: 2.35, w: 4.85, h: 0.65,
    fontFace: 'Aptos', fontSize: 17, color: 'D5E1DC', margin: 0,
  });
  slide.addShape(pptx.ShapeType.line, {
    x: 0.75, y: 3.25, w: 4.7, h: 0,
    line: { color: C.lime, width: 2 },
  });
  slide.addText('ATÉ', {
    x: 0.75, y: 3.62, w: 0.75, h: 0.3,
    fontFace: 'Aptos', fontSize: 12, bold: true, color: C.lime, margin: 0,
  });
  slide.addText('2h', {
    x: 0.75, y: 3.95, w: 1.25, h: 0.92,
    fontFace: 'Aptos Display', fontSize: 49, bold: true, color: C.white, margin: 0,
  });
  slide.addText('de prontuário e tarefas administrativas\npara cada 1h de atendimento clínico', {
    x: 2.05, y: 4.05, w: 3.3, h: 0.72,
    fontFace: 'Aptos', fontSize: 14, color: C.white, margin: 0,
  });
  slide.addText('Sinsky et al. · Annals of Internal Medicine · 2016', {
    x: 0.76, y: 5.02, w: 4.8, h: 0.28,
    fontFace: 'Aptos', fontSize: 8.5, color: 'A9B8B2', margin: 0,
  });
  slide.addText('Assistente de Prontuário Automático', {
    x: 0.75, y: 6.76, w: 4.6, h: 0.25,
    fontFace: 'Aptos', fontSize: 9, color: C.white, bold: true, margin: 0,
  });
  slide.addText(repositoryLabel, {
    x: 0.75, y: 7.02, w: 4.9, h: 0.18,
    fontFace: 'Aptos', fontSize: 7.5, color: C.lime, margin: 0,
    hyperlink: { url: repositoryUrl },
  });
  addNotes(slide, [
    'Uma consulta deveria ser uma conversa entre médico e paciente.',
    'Mas o médico precisa escutar, raciocinar e documentar ao mesmo tempo.',
    'Essa disputa reduz o contato visual e frequentemente estende o trabalho para depois do atendimento.',
  ]);
}

// 2 — Validação com médicos
{
  const slide = pptx.addSlide('MASTER');
  addHeader(slide, 'Validação local', 'Essa dor também aparece na prática', 'Uma escuta inicial e quatro perguntas para orientar a validação com médicos.');

  slide.addShape(pptx.ShapeType.roundRect, {
    x: 0.65, y: 2.05, w: 3.55, h: 3.85,
    fill: { color: C.green }, line: { color: C.green }, radius: 0.08,
  });
  slide.addText('“', {
    x: 0.98, y: 2.25, w: 0.5, h: 0.65,
    fontFace: 'Georgia', fontSize: 44, color: C.lime, margin: 0,
  });
  slide.addText('“O prontuário deveria registrar a consulta, não interrompê-la.”', {
    x: 1.0, y: 2.92, w: 2.85, h: 1.25,
    fontFace: 'Aptos Display', fontSize: 22, bold: true, color: C.white,
    margin: 0, valign: 'mid', fit: 'shrink',
  });
  slide.addText('— Dr. Ranieri · médico entrevistado', {
    x: 1.0, y: 4.95, w: 2.8, h: 0.35,
    fontFace: 'Aptos', fontSize: 10, color: 'D5E1DC', italic: true, margin: 0,
  });

  addPill(slide, 'PERGUNTA 1', 4.72, 2.05, 1.25, C.mint, C.green);
  slide.addText('Quanto tempo vai para o prontuário?', {
    x: 4.75, y: 2.62, w: 3.3, h: 0.52,
    fontFace: 'Aptos Display', fontSize: 21, bold: true, color: C.ink, margin: 0,
  });
  addPill(slide, 'PERGUNTA 2', 8.62, 2.05, 1.25, C.mint, C.green);
  slide.addText('O registro reduz o contato visual?', {
    x: 8.65, y: 2.62, w: 3.55, h: 0.52,
    fontFace: 'Aptos Display', fontSize: 21, bold: true, color: C.ink, margin: 0,
  });
  addPill(slide, 'PERGUNTA 3', 4.72, 4.08, 1.25, C.mint, C.green);
  slide.addText('O que gera confiança em um rascunho automático?', {
    x: 4.75, y: 4.65, w: 3.45, h: 0.74,
    fontFace: 'Aptos Display', fontSize: 20, bold: true, color: C.ink, margin: 0,
  });
  addPill(slide, 'EIXOS DE VALIDAÇÃO', 8.62, 4.08, 1.8, 'FDE1DA', C.coral);
  slide.addText('Precisão · privacidade · revisão · bateria', {
    x: 8.65, y: 4.65, w: 3.55, h: 0.74,
    fontFace: 'Aptos Display', fontSize: 19, bold: true, color: C.ink, margin: 0,
  });
  addFooter(slide);
  addNotes(slide, [
    'Não queríamos assumir que um estudo internacional representava a rotina brasileira.',
    'O Dr. Ranieri resumiu a tensão central: o prontuário deveria registrar a consulta, não interrompê-la.',
    'A próxima rodada de entrevistas mede tempo, contato visual, confiança e os principais receios.',
  ]);
}

// 3 — Solução
{
  const slide = pptx.addSlide('MASTER');
  addHeader(slide, 'A solução', 'Atender primeiro. Revisar depois.', 'Uma experiência clínica mãos livres — com o médico sempre no controle.');

  slide.addShape(pptx.ShapeType.ellipse, {
    x: 0.85, y: 2.05, w: 3.5, h: 3.5,
    fill: { color: C.green }, line: { color: C.green },
  });
  slide.addText('CONSULTA\nMÃOS LIVRES', {
    x: 1.33, y: 3.0, w: 2.55, h: 0.92,
    fontFace: 'Aptos Display', fontSize: 28, bold: true, color: C.white,
    align: 'center', margin: 0, fit: 'shrink',
  });
  slide.addText('Ray-Ban Meta + Android', {
    x: 1.35, y: 4.15, w: 2.5, h: 0.3,
    fontFace: 'Aptos', fontSize: 11, color: C.lime, align: 'center', margin: 0,
  });

  addBullet(slide, 'Consulta sem tocar em telas', 5.05, 2.05, 6.8);
  addBullet(slide, 'Prontuário clínico processado localmente', 5.05, 2.92, 6.8);
  addBullet(slide, 'Foto e atestado por comando de voz', 5.05, 3.79, 6.8);
  addBullet(slide, 'Médico revisa e confirma tudo', 5.05, 4.66, 6.8, C.ink, C.coral);

  addPill(slide, '“REGISTRAR IMAGEM”', 5.05, 5.66, 2.25, C.mint, C.green);
  addPill(slide, '“EMITIR ATESTADO”', 7.48, 5.66, 2.15, C.pale, C.ink);
  addPill(slide, '“ENCERRAR CONSULTA”', 9.82, 5.66, 2.4, 'FDE1DA', C.coral);
  addFooter(slide);
  addNotes(slide, [
    'Nossa proposta é um assistente de prontuário automático para Ray-Ban Meta.',
    'O médico conduz a consulta sem tocar em telas.',
    'O áudio é processado localmente no Android, fotos e atestados podem ser solicitados por voz e, ao final, o médico recebe um rascunho para revisar.',
    'A inteligência artificial prepara; o médico decide.',
  ]);
}

// 4 — Walkthrough
{
  const slide = pptx.addSlide('MASTER');
  addHeader(slide, 'A experiência', 'Uma consulta, cinco momentos', 'Do consentimento ao prontuário revisado.');

  const steps = [
    ['1', 'CONSENTIR', 'Finalidade e autorizações'],
    ['2', 'CONVERSAR', 'Áudio pelos óculos'],
    ['3', 'REGISTRAR', 'Foto e atestado por voz'],
    ['4', 'ESTRUTURAR', 'Prontuário com origem'],
    ['5', 'REVISAR', 'Editar, confirmar ou descartar'],
  ];
  const startX = 0.72;
  const stepW = 2.26;
  const gap = 0.28;
  const y = 2.35;

  steps.forEach((step, i) => {
    const x = startX + i * (stepW + gap);
    if (i < steps.length - 1) {
      slide.addShape(pptx.ShapeType.chevron, {
        x: x + stepW - 0.06, y: y + 0.87, w: 0.42, h: 0.48,
        fill: { color: C.line }, line: { color: C.line },
      });
    }
    slide.addShape(pptx.ShapeType.roundRect, {
      x, y, w: stepW, h: 2.2,
      fill: { color: i === 4 ? C.green : C.white },
      line: { color: i === 4 ? C.green : C.line, width: 1.2 },
      radius: 0.06,
    });
    slide.addShape(pptx.ShapeType.ellipse, {
      x: x + 0.18, y: y + 0.18, w: 0.45, h: 0.45,
      fill: { color: i === 4 ? C.lime : C.mint },
      line: { color: i === 4 ? C.lime : C.mint },
    });
    slide.addText(step[0], {
      x: x + 0.18, y: y + 0.24, w: 0.45, h: 0.18,
      fontFace: 'Aptos', fontSize: 11, bold: true,
      color: i === 4 ? C.ink : C.green, align: 'center', margin: 0,
    });
    slide.addText(step[1], {
      x: x + 0.18, y: y + 0.85, w: stepW - 0.36, h: 0.36,
      fontFace: 'Aptos Display', fontSize: 17, bold: true,
      color: i === 4 ? C.white : C.ink, align: 'center', margin: 0, fit: 'shrink',
    });
    slide.addText(step[2], {
      x: x + 0.2, y: y + 1.38, w: stepW - 0.4, h: 0.52,
      fontFace: 'Aptos', fontSize: 11.5,
      color: i === 4 ? 'DDEAE5' : C.muted, align: 'center', valign: 'mid', margin: 0,
    });
  });

  slide.addShape(pptx.ShapeType.roundRect, {
    x: 1.55, y: 5.35, w: 10.15, h: 0.72,
    fill: { color: 'FFF3D1' }, line: { color: C.amber, width: 1 }, radius: 0.05,
  });
  slide.addText('A transcrição falhou em um código clínico? O sistema omite — não tenta adivinhar.', {
    x: 1.85, y: 5.57, w: 9.55, h: 0.26,
    fontFace: 'Aptos', fontSize: 14, bold: true, color: '6B5110', align: 'center', margin: 0,
  });
  addFooter(slide);
  addNotes(slide, [
    'Antes de começar, o paciente é informado e registra o consentimento.',
    'Durante a consulta, o microfone dos óculos envia o áudio ao Android por HFP.',
    'Ao dizer registrar imagem, o DAT captura uma foto pontual e responde imagem registrada.',
    'Ao encerrar, a IA organiza apenas fatos presentes na transcrição.',
    'Em nosso teste, o ASR errou um CID e o sistema corretamente o omitiu.',
  ]);
}

// 5 — Arquitetura + exceções
{
  const slide = pptx.addSlide('MASTER');
  addHeader(slide, 'Como funciona', 'Inteligência local. Médico no controle.', 'Uma arquitetura estreita, auditável e desenhada para degradar com segurança.');

  const boxes = [
    { x: 0.72, w: 2.35, title: 'ÓCULOS', detail: 'Áudio + câmera Meta', fill: C.green, fg: C.white },
    { x: 3.42, w: 2.35, title: 'ANDROID LOCAL', detail: 'AudioRecord + Vosk', fill: C.mint, fg: C.ink },
    { x: 6.12, w: 2.35, title: 'PIPELINE CLÍNICO', detail: 'Fatos + prontuário + origem', fill: C.pale, fg: C.ink },
    { x: 8.82, w: 2.35, title: 'REVISÃO', detail: 'Editar · confirmar · excluir', fill: 'FDE1DA', fg: C.ink },
  ];
  boxes.forEach((b, i) => {
    slide.addShape(pptx.ShapeType.roundRect, {
      x: b.x, y: 2.15, w: b.w, h: 1.38,
      fill: { color: b.fill }, line: { color: b.fill }, radius: 0.06,
    });
    slide.addText(b.title, {
      x: b.x + 0.18, y: 2.42, w: b.w - 0.36, h: 0.3,
      fontFace: 'Aptos Display', fontSize: 17, bold: true, color: b.fg,
      align: 'center', margin: 0,
    });
    slide.addText(b.detail, {
      x: b.x + 0.18, y: 2.86, w: b.w - 0.36, h: 0.25,
      fontFace: 'Aptos', fontSize: 10.5, color: b.fg,
      align: 'center', margin: 0,
    });
    if (i < boxes.length - 1) {
      slide.addShape(pptx.ShapeType.chevron, {
        x: b.x + b.w + 0.12, y: 2.58, w: 0.36, h: 0.5,
        fill: { color: C.line }, line: { color: C.line },
      });
    }
  });
  slide.addShape(pptx.ShapeType.chevron, {
    x: 11.42, y: 2.58, w: 0.36, h: 0.5,
    fill: { color: C.line }, line: { color: C.line },
  });
  slide.addShape(pptx.ShapeType.roundRect, {
    x: 11.82, y: 2.15, w: 0.84, h: 1.38,
    fill: { color: C.ink }, line: { color: C.ink }, radius: 0.05,
  });
  slide.addText('VOZ', {
    x: 11.95, y: 2.65, w: 0.58, h: 0.25,
    fontFace: 'Aptos Display', fontSize: 16, bold: true, color: C.white, align: 'center', margin: 0,
  });

  slide.addText('QUANDO ALGO FALHA', {
    x: 0.75, y: 4.24, w: 3.0, h: 0.28,
    fontFace: 'Aptos', fontSize: 11, bold: true, color: C.coral, charSpacing: 1.1, margin: 0,
  });
  addBullet(slide, 'Câmera ou bateria crítica → preserva o áudio', 0.78, 4.75, 3.65, C.ink, C.coral);
  addBullet(slide, 'Áudio dos óculos indisponível → mic do telefone', 4.6, 4.75, 3.45, C.ink, C.coral);
  addBullet(slide, 'Transcrição incerta → não informado, nunca inferido', 8.25, 4.75, 4.25, C.ink, C.coral);

  slide.addShape(pptx.ShapeType.roundRect, {
    x: 3.75, y: 5.85, w: 5.85, h: 0.55,
    fill: { color: C.green }, line: { color: C.green }, radius: 0.05,
  });
  slide.addText('Sem inteligência em nuvem · retenção mínima · revisão humana', {
    x: 4.05, y: 6.03, w: 5.25, h: 0.2,
    fontFace: 'Aptos', fontSize: 12, bold: true, color: C.white, align: 'center', margin: 0,
  });
  addFooter(slide, 'Fluxo completo entregue em arquitetura.png + arquitetura.mmd');
  addNotes(slide, [
    'O DAT 0.9 controla a câmera, enquanto o áudio usa o caminho HFP documentado pela Meta.',
    'No telefone, o Vosk transcreve e o pipeline organiza os fatos sem gerar informação nova.',
    'As respostas voltam por TTS.',
    'Se alguma camada falha, o sistema degrada função antes de perder a consulta.',
  ]);
}

// 6 — Evidência e fechamento
{
  const slide = pptx.addSlide('MASTER');
  addHeader(slide, 'Evidência, equipe e próximos passos', 'Não é apenas uma ideia', 'Execução comprovada, competências complementares e um próximo teste objetivo.');

  slide.addShape(pptx.ShapeType.roundRect, {
    x: 0.72, y: 2.02, w: 3.5, h: 3.15,
    fill: { color: C.green }, line: { color: C.green }, radius: 0.07,
  });
  slide.addText('52', {
    x: 1.2, y: 2.25, w: 2.55, h: 0.95,
    fontFace: 'Aptos Display', fontSize: 58, bold: true, color: C.white,
    align: 'center', margin: 0,
  });
  slide.addText('testes automatizados', {
    x: 1.15, y: 3.25, w: 2.65, h: 0.38,
    fontFace: 'Aptos', fontSize: 15, bold: true, color: C.lime,
    align: 'center', margin: 0,
  });
  slide.addText('Pipeline Android de ponta a ponta\nprontuário · foto · atestado · cofre · revisão', {
    x: 1.05, y: 3.92, w: 2.85, h: 0.72,
    fontFace: 'Aptos', fontSize: 13, color: C.white,
    align: 'center', valign: 'mid', margin: 0,
  });

  slide.addText('NO HARDWARE REAL', {
    x: 4.82, y: 2.06, w: 2.75, h: 0.28,
    fontFace: 'Aptos', fontSize: 11, bold: true, color: C.coral, charSpacing: 1.1, margin: 0,
  });
  addBullet(slide, 'Captura da voz pelos óculos', 4.85, 2.62, 3.1, C.ink, C.coral);
  addBullet(slide, 'Foto pontual com o toolkit Meta', 4.85, 3.45, 3.1, C.ink, C.coral);
  addBullet(slide, 'Autonomia por consulta', 4.85, 4.28, 3.1, C.ink, C.coral);

  slide.addText('NOSSO DIFERENCIAL', {
    x: 8.6, y: 2.06, w: 2.75, h: 0.28,
    fontFace: 'Aptos', fontSize: 11, bold: true, color: C.green, charSpacing: 1.1, margin: 0,
  });
  addBullet(slide, 'Processamento clínico local', 8.62, 2.62, 3.7);
  addBullet(slide, 'Proveniência fato a fato', 8.62, 3.45, 3.7);
  addBullet(slide, 'Médico sempre no controle', 8.62, 4.28, 3.7);

  slide.addText('EQUIPE', {
    x: 0.75, y: 5.43, w: 1.0, h: 0.25,
    fontFace: 'Aptos', fontSize: 10.5, bold: true, color: C.green,
    charSpacing: 1.1, margin: 0,
  });
  const team = [
    ['LUCAS PACHECO', 'Android e front-end'],
    ['MARCOS PAULO', 'Inteligência artificial'],
    ['LUCAS ISAAC', 'Inteligência artificial, privacidade e requisitos'],
  ];
  team.forEach((member, index) => {
    const x = 0.75 + index * 4.13;
    slide.addShape(pptx.ShapeType.line, {
      x, y: 5.82, w: 3.65, h: 0,
      line: { color: index === 2 ? C.coral : C.green, width: 2 },
    });
    slide.addText(member[0], {
      x, y: 5.98, w: 3.65, h: 0.24,
      fontFace: 'Aptos Display', fontSize: 13, bold: true, color: C.ink,
      margin: 0, fit: 'shrink',
    });
    slide.addText(member[1], {
      x, y: 6.28, w: 3.65, h: 0.3,
      fontFace: 'Aptos', fontSize: 10.5, color: C.muted,
      margin: 0, fit: 'shrink',
    });
  });
  slide.addText('Devolver ao médico tempo e atenção para o paciente.', {
    x: 3.25, y: 6.67, w: 6.85, h: 0.25,
    fontFace: 'Aptos Display', fontSize: 15, bold: true, color: C.green,
    align: 'center', margin: 0,
  });
  addFooter(slide);
  addNotes(slide, [
    'Já validamos o pipeline completo no emulador Android: áudio real, transcrição, foto por voz, SOAP, atestado, criptografia e revisão.',
    'São 52 testes automatizados.',
    'Nossa equipe reúne Lucas Pacheco em Android e front-end, Marcos Paulo em inteligência artificial e Lucas Isaac em inteligência artificial, privacidade e requisitos.',
    'No hackathon, nossa primeira hora será dedicada a validar o HFP, a câmera DAT e a autonomia no hardware real.',
    'Não queremos substituir o julgamento médico. Queremos devolver ao médico tempo e atenção para o paciente.',
  ]);
}

pptx.writeFile({ fileName: path.join(outDir, 'PITCH_AI_GLASSES_BRASIL.pptx') });

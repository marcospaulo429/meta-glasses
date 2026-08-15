package com.prontuario.glasses.asr

import com.prontuario.glasses.soap.TranscriptSegment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakePort(
    private val script: List<Pair<Boolean, String>>,
) : SpeechPort {
    private var index = -1
    private var lastFinalJson = "{}"

    override fun acceptWaveform(pcm: ByteArray, length: Int): Boolean {
        index++
        val (isFinal, json) = script[index]
        if (isFinal) lastFinalJson = json
        return isFinal
    }

    override fun result(): String = lastFinalJson
    override fun partial(): String = script[index].second
    override fun finalResult(): String = """{"text":""}"""
    override fun close() {}
}

class StreamingTranscriberTest {

    // 32 KB = 1 s de PCM 16 kHz/16-bit mono
    private val oneSecond = ByteArray(32_000)

    @Test
    fun `segmento final carrega texto e timestamps por palavra`() {
        val segments = mutableListOf<TranscriptSegment>()
        val port = FakePort(
            listOf(
                false to """{"partial":"paciente relata"}""",
                true to """{"text":"paciente relata dor","result":[
                    {"word":"paciente","start":0.5,"end":1.0},
                    {"word":"relata","start":1.1,"end":1.5},
                    {"word":"dor","start":1.6,"end":1.9}]}""",
            ),
        )
        val transcriber = StreamingTranscriber(port, onSegment = segments::add)
        transcriber.feed(oneSecond, oneSecond.size)
        transcriber.feed(oneSecond, oneSecond.size)

        assertEquals(1, segments.size)
        assertEquals("paciente relata dor", segments[0].text)
        assertEquals(500, segments[0].startMs)
        assertEquals(1900, segments[0].endMs)
        assertEquals(0, segments[0].audioChunkSeq)
    }

    @Test
    fun `partials sao propagados e utterance end reseta detector`() {
        val partials = mutableListOf<String>()
        var utteranceEnds = 0
        val port = FakePort(
            listOf(
                false to """{"partial":"registrar ima"}""",
                false to """{"partial":"registrar imagem"}""",
                true to """{"text":"registrar imagem"}""",
            ),
        )
        val transcriber = StreamingTranscriber(
            port,
            onSegment = {},
            onPartial = partials::add,
            onUtteranceEnd = { utteranceEnds++ },
        )
        repeat(3) { transcriber.feed(oneSecond, oneSecond.size) }

        assertEquals(listOf("registrar ima", "registrar imagem"), partials)
        assertEquals(1, utteranceEnds)
    }

    @Test
    fun `texto vazio nao gera segmento`() {
        val segments = mutableListOf<TranscriptSegment>()
        val port = FakePort(listOf(true to """{"text":""}"""))
        StreamingTranscriber(port, onSegment = segments::add).feed(oneSecond, oneSecond.size)
        assertTrue(segments.isEmpty())
    }

    @Test
    fun `segmento apos 60s cai no chunk seguinte`() {
        val segments = mutableListOf<TranscriptSegment>()
        val port = FakePort(
            List(61) { false to """{"partial":""}""" } +
                (true to """{"text":"pressao doze por oito","result":[
                    {"word":"pressao","start":61.0,"end":61.5},
                    {"word":"doze","start":61.6,"end":62.0},
                    {"word":"por","start":62.1,"end":62.2},
                    {"word":"oito","start":62.3,"end":62.6}]}"""),
        )
        val transcriber = StreamingTranscriber(port, onSegment = segments::add)
        repeat(62) { transcriber.feed(oneSecond, oneSecond.size) }

        assertEquals(1, segments[0].audioChunkSeq)
    }
}

class VoiceCommandDetectorTest {

    @Test
    fun `detecta comando de foto com acento e caixa diferentes`() {
        var fired = 0
        val detector = VoiceCommandDetector(onPhotoCommand = { fired++ })
        detector.onPartial("por favor REGISTRAR IMAGEM da lesão")
        assertEquals(1, fired)
    }

    @Test
    fun `dispara no maximo uma vez por sentenca`() {
        var fired = 0
        val detector = VoiceCommandDetector(onPhotoCommand = { fired++ })
        detector.onPartial("registrar imagem")
        detector.onPartial("registrar imagem agora")
        assertEquals(1, fired)
        detector.onUtteranceEnd()
        detector.onPartial("tirar foto")
        assertEquals(2, fired)
    }

    @Test
    fun `nao dispara sem palavra-chave`() {
        var fired = 0
        val detector = VoiceCommandDetector(onPhotoCommand = { fired++ })
        detector.onPartial("paciente com imagem corporal preservada")
        assertEquals(0, fired)
    }

    @Test
    fun `detecta comando de encerrar`() {
        var stops = 0
        val detector = VoiceCommandDetector(onPhotoCommand = {}, onStopCommand = { stops++ })
        detector.onPartial("podemos encerrar consulta")
        assertEquals(1, stops)
    }
}

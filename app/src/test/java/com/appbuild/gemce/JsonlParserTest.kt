package com.appbuild.gemce

import com.appbuild.gemce.domain.model.CardJsonItem
import com.appbuild.gemce.domain.model.GradingResultJson
import com.appbuild.gemce.domain.model.KeyPointJsonItem
import com.appbuild.gemce.domain.model.McqJsonItem
import com.appbuild.gemce.domain.model.ShortAnswerJsonItem
import com.appbuild.gemce.domain.parser.JsonlParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonlParserTest {

    @Test
    fun testParseCardLine() {
        val line = """{"type":"card","front":"Ano ang function ng mitochondria?","back":"Gumagawa ng ATP (energy) para sa cell.","evidence":"Mitochondria produce ATP through cellular respiration."}"""
        val result = JsonlParser.parseLine(line)

        assertTrue(result is CardJsonItem)
        val card = result as CardJsonItem
        assertEquals("Ano ang function ng mitochondria?", card.front)
        assertEquals("Gumagawa ng ATP (energy) para sa cell.", card.back)
        assertEquals("Mitochondria produce ATP through cellular respiration.", card.evidence)
    }

    @Test
    fun testParseMcqLine() {
        val line = """{"type":"mcq","stem":"Which organelle produces most of the cell's ATP?","choices":["Ribosome","Mitochondrion","Golgi body","Lysosome"],"answer":1,"explanation":"Mitochondria carry out cellular respiration.","evidence":"Mitochondria produce ATP."}"""
        val result = JsonlParser.parseLine(line)

        assertTrue(result is McqJsonItem)
        val mcq = result as McqJsonItem
        assertEquals("Which organelle produces most of the cell's ATP?", mcq.stem)
        assertEquals(4, mcq.choices.size)
        assertEquals(1, mcq.answer)
    }

    @Test
    fun testParseKeyPointLine() {
        val line = """{"type":"point","text":"Mitochondria produce ATP through cellular respiration.","evidence":"Mitochondria produce ATP through cellular respiration."}"""
        val result = JsonlParser.parseLine(line)

        assertTrue(result is KeyPointJsonItem)
        val point = result as KeyPointJsonItem
        assertEquals("Mitochondria produce ATP through cellular respiration.", point.text)
    }

    @Test
    fun testParseShortAnswerLine() {
        val line = """{"type":"short","stem":"Explain how mitochondria produce ATP.","reference_answer":"Through cellular respiration, converting nutrients and oxygen into ATP.","rubric":["mentions cellular respiration","mentions ATP as the product"],"evidence":"Cellular respiration."}"""
        val result = JsonlParser.parseLine(line)

        assertTrue(result is ShortAnswerJsonItem)
        val item = result as ShortAnswerJsonItem
        assertEquals("Explain how mitochondria produce ATP.", item.stem)
        assertEquals(2, item.rubric.size)
    }

    @Test
    fun testParseGradingResult() {
        val line = """{"verdict":"partial","score":0.5,"feedback":"You named ATP, but you did not mention cellular respiration.","missing":["cellular respiration"]}"""
        val result = JsonlParser.parseGradingResult(line)

        assertNotNull(result)
        val grading = result as GradingResultJson
        assertEquals("partial", grading.verdict)
        assertEquals(0.5f, grading.score, 0.01f)
        assertEquals(1, grading.missing.size)
    }

    @Test
    fun testMalformedLinesAreGracefullyIgnored() {
        assertNull(JsonlParser.parseLine(""))
        assertNull(JsonlParser.parseLine("   "))
        assertNull(JsonlParser.parseLine("```json"))
        assertNull(JsonlParser.parseLine("""{"broken": json here"""))
        assertNull(JsonlParser.parseLine("""{"unrecognized_type": true}"""))
    }
}

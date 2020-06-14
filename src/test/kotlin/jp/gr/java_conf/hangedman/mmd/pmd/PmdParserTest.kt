package jp.gr.java_conf.hangedman.mmd.pmd

import jp.gr.java_conf.hangedman.mmd.PmdLoader
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat

class PmdParserTest {

    @Test
    fun testParse() {
        val stream = PmdLoader.getResourceAsStream("HatsuneMiku.pmd")
        val pmdStruct = PmdParser.parse(stream)

        assertArrayEquals("Pmd".toByteArray(), pmdStruct.magic)
        assertEquals("1.00".toFloat(), pmdStruct.version)
        assertThat(pmdStruct.modelName!!.toString(charset = charset("Shift_JIS")),
                allOf(notNullValue(), startsWith("初音ミク"))
        )
        assertThat(pmdStruct.comment!!.toString(charset = charset("Shift_JIS")),
                allOf(notNullValue(), startsWith("PolyMo用モデルデータ：初音ミク ver.1.3"))
        )
    }
}
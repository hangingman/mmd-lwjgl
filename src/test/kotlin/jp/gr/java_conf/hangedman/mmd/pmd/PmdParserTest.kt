package jp.gr.java_conf.hangedman.mmd.pmd

import jp.gr.java_conf.hangedman.mmd.PmdLoader
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PmdParserTest {

    @Test
    fun testParse() {
        val stream = PmdLoader.getResourceAsStream("HatsuneMiku.pmd")
        val pmdStruct = PmdParser.parse(stream)

        assertArrayEquals("Pmd".toByteArray(), pmdStruct.magic)
    }
}
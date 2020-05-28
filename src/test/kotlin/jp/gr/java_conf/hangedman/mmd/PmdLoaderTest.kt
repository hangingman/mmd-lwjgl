package jp.gr.java_conf.hangedman.mmd

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class PmdLoaderTest {

    @Test
    fun testLoadPmdFile() {
        PmdLoader.loadPmdFile("HatsuneMiku.pmd")
    }

    @Test
    fun testGetResourceAsStream() {
        assertNotNull(PmdLoader.getResourceAsStream("HatsuneMiku.pmd"))
    }

    @Test
    fun testPmdHeader() {
        PmdLoader.pmdHeader()
    }
}
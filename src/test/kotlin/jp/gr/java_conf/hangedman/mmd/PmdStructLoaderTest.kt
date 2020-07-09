package jp.gr.java_conf.hangedman.mmd

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class PmdStructLoaderTest {

    @Test
    fun testLoadPmdFile() {
        MeshLoader.loadMeshFile("HatsuneMiku.pmd")
    }

    @Test
    fun testGetResourceAsStream() {
        assertNotNull(MeshLoader.getResourceAsStream("HatsuneMiku.pmd"))
    }
}
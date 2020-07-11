package jp.gr.java_conf.hangedman.mmd.mesh.pmd

import jp.gr.java_conf.hangedman.mmd.mesh.pmd.Material.Companion.NUL
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class PmdStructTest {

    var pmdStruct: PmdStruct = PmdStruct("")

    @BeforeEach
    fun beforeEach() {
        pmdStruct = PmdStruct("dummy${File.separator}hoge")
    }

    @Test
    fun getTexturePathsTest() {
        pmdStruct.material = arrayOf<Material>(
                Material().apply {
                    textureFileName = ByteArray(20)
                },
                Material().apply {
                    textureFileName = "sample.bmp$NUL".toByteArray().copyInto(ByteArray(20){-3})
                }
        )
        val results = pmdStruct.getTexturePaths()
        assertThat(results, hasSize(1))
        assertThat(results, contains("dummy${File.separator}sample.bmp"))
    }
}
package jp.gr.java_conf.hangedman.mmd.mesh.pmx

import com.igormaznitsa.jbbp.JBBPParser
import com.igormaznitsa.jbbp.mapper.JBBPMapper.FLAG_IGNORE_MISSING_VALUES
import jp.gr.java_conf.hangedman.mmd.MeshLoader.getResourceAsStream
import jp.gr.java_conf.hangedman.mmd.mesh_if.Mesh
import jp.gr.java_conf.hangedman.mmd.mesh_if.MeshParser

object PmxParser : MeshParser {

    private val pmxParser: JBBPParser = JBBPParser.prepare("""
    """.trimIndent())

    override fun <T : Mesh> parse(meshPath: String): T {
        val stream = getResourceAsStream(meshPath)

        @Suppress("UNCHECKED_CAST")
        return pmxParser
                .parse(stream) // Struct内部にデータ型以外のものも設定したいのでフラグをセット
                .mapTo(PmxStruct(meshPath), FLAG_IGNORE_MISSING_VALUES) as T
    }
}
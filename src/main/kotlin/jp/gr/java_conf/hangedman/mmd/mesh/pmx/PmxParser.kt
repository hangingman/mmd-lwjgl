package jp.gr.java_conf.hangedman.mmd.mesh.pmx

import com.igormaznitsa.jbbp.JBBPParser
import com.igormaznitsa.jbbp.mapper.JBBPMapper.FLAG_IGNORE_MISSING_VALUES
import java.io.InputStream

object PmxParser {

    private val pmxParser: JBBPParser = JBBPParser.prepare("""
    """.trimIndent())

    fun parse(stream: InputStream): PmxStruct {
        return pmxParser
                .parse(stream) // Struct内部にデータ型以外のものも設定したいのでフラグをセット
                .mapTo(PmxStruct(), FLAG_IGNORE_MISSING_VALUES)
    }
}
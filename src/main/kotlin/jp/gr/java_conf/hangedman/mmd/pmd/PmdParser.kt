package jp.gr.java_conf.hangedman.mmd.pmd

import com.igormaznitsa.jbbp.JBBPParser
import java.io.InputStream

object PmdParser {

    private val pmdParser: JBBPParser = JBBPParser.prepare("""
        byte[3] magic;
        <floatj version;
        byte[20] modelName;
        byte[256] comment;
    """.trimIndent())

    fun parse(stream: InputStream): PmdStruct {
        return pmdParser.parse(stream).mapTo(PmdStruct())
    }
}
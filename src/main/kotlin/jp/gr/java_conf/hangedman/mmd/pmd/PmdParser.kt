package jp.gr.java_conf.hangedman.mmd.pmd

import com.igormaznitsa.jbbp.JBBPParser
import java.io.InputStream

object PmdParser {

    private val pmdParser: JBBPParser = JBBPParser.prepare("""
        // ヘッダ
        byte[3] magic;
        <floatj version;
        byte[20] modelName;
        byte[256] comment;
        
        // 頂点リスト
        <int vertCount; 
        vertex [vertCount]{
            <floatj[3] pos;
            <floatj[3] normalVec;
            <floatj[2] uv;
            <short[2] boneNum;
            byte boneWeight;
            byte edgeFlag;
        }
        
        // 面頂点リスト
        <int faceVertCount;
        <short[faceVertCount] faceVertIndex;
    """.trimIndent())

    fun parse(stream: InputStream): PmdStruct {
        return pmdParser.parse(stream).mapTo(PmdStruct())
    }
}
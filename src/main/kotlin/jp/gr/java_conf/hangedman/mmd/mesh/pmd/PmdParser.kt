package jp.gr.java_conf.hangedman.mmd.mesh.pmd

import com.igormaznitsa.jbbp.JBBPParser
import com.igormaznitsa.jbbp.mapper.JBBPMapper.FLAG_IGNORE_MISSING_VALUES
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
        
        // 材質リスト
        <int materialCount;
        material [materialCount]{
            <floatj[3] diffuseColor;
            <floatj alpha;
            <floatj specularity;
            <floatj[3] specularColor;
            <floatj[3] ambientColor;
            byte toonIndex;
            byte edgeFlag;
            <int faceVertCount;
            byte[20] textureFileName;
        }
    """.trimIndent())

    fun parse(stream: InputStream): PmdStruct {
        return pmdParser
                .parse(stream) // Struct内部にデータ型以外のものも設定したいのでフラグをセット
                .mapTo(PmdStruct(), FLAG_IGNORE_MISSING_VALUES)
    }
}
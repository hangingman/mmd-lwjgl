package jp.gr.java_conf.hangedman.mmd.pmd

import com.igormaznitsa.jbbp.mapper.Bin

@Bin
class PmdStruct {

    // ヘッダ
    var magic: ByteArray? = null
    var version: Float = 0F
    var modelName: ByteArray? = null
    var comment: ByteArray? = null

    // 頂点リスト
    var vertCount: Int = 0
    set(vertCount) {
        field = vertCount
        this.vertex = arrayOfNulls<Vertex?>(vertCount)
    }
    var vertex: Array<Vertex?> = arrayOfNulls<Vertex?>(0)
}

class Vertex {
    @Bin var pos: FloatArray? = null
    @Bin var normalVec: FloatArray? = null
    @Bin var uv: FloatArray? = null
    @Bin var boneNum: ShortArray? = null
    @Bin var boneWeight: Byte = 0
    @Bin var edgeFlag: Byte = 0
}
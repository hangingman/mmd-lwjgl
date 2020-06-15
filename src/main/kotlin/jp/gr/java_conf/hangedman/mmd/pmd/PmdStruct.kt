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
    @Bin var pos: FloatArray = floatArrayOf(0F, 0F, 0F)
    @Bin var normalVec: FloatArray = floatArrayOf(0F, 0F, 0F)
    @Bin var uv: FloatArray = floatArrayOf(0F, 0F)
    @Bin var boneNum: ShortArray = shortArrayOf(0, 0)
    @Bin var boneWeight: Byte = 0
    @Bin var edgeFlag: Byte = 0
}
package jp.gr.java_conf.hangedman.mmd.pmd

import com.igormaznitsa.jbbp.mapper.Bin

@Bin
class PmdStruct {

    // ヘッダ
    @Bin(order = 1)
    var magic: ByteArray? = null
    @Bin(order = 2)
    var version: Float = 0F
    @Bin(order = 3)
    var modelName: ByteArray? = null
    @Bin(order = 4)
    var comment: ByteArray? = null

    // 頂点リスト
    @Bin(order = 5)
    var vertCount: Int = 0
    @Bin(order = 6, arraySizeExpr = "vertCount")
    var vertex: Array<Vertex>? = null

    // 面頂点リスト
    @Bin(order = 7)
    var faceVertCount: Int = 0
    @Bin(order = 8, arraySizeExpr = "faceVertCount")
    var faceVertIndex: ShortArray? = null

    // 材質リスト
    @Bin(order = 9)
    var materialCount: Int = 0
    @Bin(order = 10, arraySizeExpr = "materialCount")
    var material: Array<Material>? = null
}

class Vertex {
    @Bin var pos: FloatArray = floatArrayOf(0F, 0F, 0F)
    @Bin var normalVec: FloatArray = floatArrayOf(0F, 0F, 0F)
    @Bin var uv: FloatArray = floatArrayOf(0F, 0F)
    @Bin var boneNum: ShortArray = shortArrayOf(0, 0)
    @Bin var boneWeight: Byte = 0
    @Bin var edgeFlag: Byte = 0
}

class Material {
    @Bin var diffuseColor: FloatArray = floatArrayOf(0F, 0F, 0F)
    @Bin var alpha: Float = 0F
    @Bin var specularity: Float = 0F
    @Bin var specularColor: FloatArray = floatArrayOf(0F, 0F, 0F)
    @Bin var mirrorColor: FloatArray = floatArrayOf(0F, 0F, 0F)
    @Bin var toonIndex: Byte = 0
    @Bin var edgeFlag: Byte = 0
    @Bin var faceVertCount: Int = 0
    @Bin var textureFileName: ByteArray = ByteArray(0)
}
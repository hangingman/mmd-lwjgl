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

    // 面頂点リストの適用合計値に対してどの材質リストを使うかを保持する連想配列
    val materialRanged: List<Pair<Int, Material>> by lazy {
        this.material!!
                .mapIndexed { i, m ->
                    val materialRanged = this.material!!.filterIndexed { index, material ->
                        index <= i
                    }.map { it.faceVertCount }.sum()
                    materialRanged to m
                }
    }

    // 頂点に対してどの材質リストを使うかを保持する連想配列
    val vertexMaterialMap: List<Pair<Int, Material>> by lazy {
        this.vertex!!
                .mapIndexed { index, _ ->
                    val faceVertIndex = this.faceVertIndex!!.indexOfFirst { faceVert -> faceVert == index.toShort() }
                    val material = materialRanged.find { m -> m.first >= faceVertIndex }
                    index to material!!.second
                }
    }
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
    @Bin var diffuseColor: FloatArray = floatArrayOf(0F, 0F, 0F)  // 物体色
    @Bin var alpha: Float = 0F                                    // 物体色透過率
    @Bin var specularity: Float = 0F                              // 光沢度
    @Bin var specularColor: FloatArray = floatArrayOf(0F, 0F, 0F) // 光沢色
    @Bin var ambientColor: FloatArray = floatArrayOf(0F, 0F, 0F)  // 環境色
    @Bin var toonIndex: Byte = 0                                  // toon番号
    @Bin var edgeFlag: Byte = 0                                   // エッジ
    @Bin var faceVertCount: Int = 0                               // 面頂点数
    @Bin var textureFileName: ByteArray = ByteArray(0)       // テクスチャーファイル名
}
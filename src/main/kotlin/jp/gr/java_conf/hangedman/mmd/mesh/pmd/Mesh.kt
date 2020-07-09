package jp.gr.java_conf.hangedman.mmd.mesh.pmd

import com.igormaznitsa.jbbp.mapper.Bin
import jp.gr.java_conf.hangedman.lwjgl.BufferBuilder.buildFloatBuffer
import jp.gr.java_conf.hangedman.lwjgl.BufferBuilder.buildShortBuffer
import jp.gr.java_conf.hangedman.mmd.mesh_if.Mesh
import java.nio.FloatBuffer
import java.nio.ShortBuffer

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

@Bin
class Mesh : Mesh {

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

    override fun verticesBuffer(): FloatBuffer {
        // 頂点リスト
        val vertices = this.vertex!!
                .map { v -> v.pos }
                .flatMap { fArray ->
                    mutableListOf<Float>().also {
                        it.addAll(fArray.asList())
                    }
                }.toFloatArray()
        return buildFloatBuffer(vertices)
    }

    override fun alphaBuffer(): FloatBuffer {

        val vertexMaterialMap = this.vertexMaterialMap

        return this.vertex!!
                .mapIndexed { i, _ ->
                    val floatList = mutableListOf<Float>()
                    val m = vertexMaterialMap.find { (range, _) -> i <= range }!!.second
                    floatList.add(m.alpha)
                    floatList
                }.flatten().toFloatArray().run {
                    buildFloatBuffer(this)
                }
    }

    override fun diffuseColorsBuffer(): FloatBuffer {

        val vertexMaterialMap = this.vertexMaterialMap

        return this.vertex!!
                .mapIndexed { i, _ ->
                    val floatList = mutableListOf<Float>()
                    val m = vertexMaterialMap.find { (range, _) -> i <= range }!!.second
                    floatList.addAll(m.diffuseColor.toList())
                    floatList
                }.flatten().toFloatArray().run {
                    buildFloatBuffer(this)
                }
    }

    override fun ambientColorsBuffer(): FloatBuffer {

        val vertexMaterialMap = this.vertexMaterialMap

        return this.vertex!!
                .mapIndexed { i, _ ->
                    val floatList = mutableListOf<Float>()
                    val m = vertexMaterialMap.find { (range, _) -> i <= range }!!.second
                    floatList.addAll(m.ambientColor.toList())
                    floatList
                }.flatten().toFloatArray().run {
                    buildFloatBuffer(this)
                }
    }

    override fun specularColorsBuffer(): FloatBuffer {

        val vertexMaterialMap = this.vertexMaterialMap

        return this.vertex!!
                .mapIndexed { i, _ ->
                    val floatList = mutableListOf<Float>()
                    val m = vertexMaterialMap.find { (range, _) -> i <= range }!!.second
                    floatList.addAll(m.specularColor.toList())
                    floatList
                }.flatten().toFloatArray().run {
                    buildFloatBuffer(this)
                }
    }

    override fun shininessBuffer(): FloatBuffer {

        val vertexMaterialMap = this.vertexMaterialMap

        return this.vertex!!
                .mapIndexed { i, _ ->
                    val floatList = mutableListOf<Float>()
                    val m = vertexMaterialMap.find { (range, _) -> i <= range }!!.second
                    floatList.add(m.specularity)
                    floatList
                }.flatten().toFloatArray().run {
                    buildFloatBuffer(this)
                }
    }

    override fun edgeFlagBuffer(): FloatBuffer {

        val vertexMaterialMap = this.vertexMaterialMap

        return this.vertex!!
                .mapIndexed { i, _ ->
                    val fList = mutableListOf<Float>()
                    val m = vertexMaterialMap.find { (range, _) -> i <= range }!!.second
                    fList.add(m.edgeFlag.toFloat())
                    fList
                }.flatten().toFloatArray().run {
                    buildFloatBuffer(this)
                }
    }

    override fun normalsBuffer(): FloatBuffer {
        val normals = this.vertex!!
                .map { v -> v.normalVec }
                .flatMap { fArray ->
                    mutableListOf<Float>().also {
                        it.addAll(fArray.asList())
                    }
                }.toFloatArray()

        return buildFloatBuffer(normals)
    }

    override fun faceVertPair(): Pair<Int, ShortBuffer> {
        return this.faceVertCount to buildShortBuffer(this.faceVertIndex!!)
    }

    override fun getModelYMax(): Float {
        return vertex!!.map{ v -> v.pos[1] }.max()!!
    }

    override fun getModelYMin(): Float {
        return vertex!!.map{ v -> v.pos[1] }.min()!!
    }
}

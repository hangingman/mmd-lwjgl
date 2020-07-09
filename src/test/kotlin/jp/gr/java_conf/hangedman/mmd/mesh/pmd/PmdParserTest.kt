package jp.gr.java_conf.hangedman.mmd.mesh.pmd

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import jp.gr.java_conf.hangedman.mmd.MeshLoader
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PmdParserTest {

    private lateinit var mesh: PmdStruct

    @BeforeEach
    fun beforeEach() {
        val stream = MeshLoader.getResourceAsStream("HatsuneMiku.pmd")
        mesh = PmdParser.parse(stream)
    }

    @Test
    fun testParseHeader() {
        // ヘッダー
        assertArrayEquals("Pmd".toByteArray(), mesh.magic)
        assertEquals("1.00".toFloat(), mesh.version)
        assertThat(mesh.modelName!!.toString(charset = charset("Shift_JIS")),
                allOf(notNullValue(), startsWith("初音ミク"))
        )
        assertThat(mesh.comment!!.toString(charset = charset("Shift_JIS")),
                allOf(notNullValue(), startsWith("PolyMo用モデルデータ：初音ミク ver.1.3"))
        )
    }

    @Test
    fun testParseVertex() {

        // 頂点リスト
        assertEquals(9036, mesh.vertCount)

        val stream = this::class.java.classLoader.getResourceAsStream("vertex.csv")
        requireNotNull(stream)

        val rows: List<Map<String, String>> = csvReader().readAllWithHeader(stream)
        rows.forEach { e ->
            val index = e["index"]?.toInt() ?: throw IllegalStateException()
            val x = e["x"]?.toFloat() ?: throw IllegalStateException()
            val y = e["y"]?.toFloat() ?: throw IllegalStateException()
            val z = e["z"]?.toFloat() ?: throw IllegalStateException()
            val nx = e["nx"]?.toFloat() ?: throw IllegalStateException()
            val ny = e["ny"]?.toFloat() ?: throw IllegalStateException()
            val nz = e["nz"]?.toFloat() ?: throw IllegalStateException()
            val u = e["u"]?.toFloat() ?: throw IllegalStateException()
            val v = e["v"]?.toFloat() ?: throw IllegalStateException()
            val bone1 = e["bone1"]?.toShort() ?: throw IllegalStateException()
            val bone2 = e["bone2"]?.toShort() ?: throw IllegalStateException()
            val weight = e["boneWeight"]?.toByte() ?: throw IllegalStateException()
            val edge = e["edgeFlag"]?.toByte() ?: throw IllegalStateException()

            mesh.vertex.run {
                this!![index].run {
                    requireNotNull(this)
                    assertArrayEquals(floatArrayOf(x, y, z), this.pos)
                    assertArrayEquals(floatArrayOf(nx, ny, nz), this.normalVec)
                    assertArrayEquals(floatArrayOf(u, v), this.uv)
                    assertArrayEquals(shortArrayOf(bone1, bone2), this.boneNum)
                    assertEquals(weight, this.boneWeight)
                    assertEquals(edge, this.edgeFlag)
                }
            }
        }
    }

    @Test
    fun testParseFaceVert() {
        // 面頂点リスト
        assertEquals(44991, mesh.faceVertCount)

        val stream = this::class.java.classLoader.getResourceAsStream("face_vertex.csv")
        requireNotNull(stream)

        val rows: List<Map<String, String>> = csvReader().readAllWithHeader(stream)
        rows.forEach { e ->
            val index = e["index"]?.toInt() ?: throw IllegalStateException()
            val faceVert1 = e["faceVert1"]?.toShort() ?: throw IllegalStateException()
            val faceVert2 = e["faceVert2"]?.toShort() ?: throw IllegalStateException()
            val faceVert3 = e["faceVert3"]?.toShort() ?: throw IllegalStateException()

            assertEquals(faceVert1, this.mesh.faceVertIndex?.get(3 * index))
            assertEquals(faceVert2, this.mesh.faceVertIndex?.get(3 * index + 1))
            assertEquals(faceVert3, this.mesh.faceVertIndex?.get(3 * index + 2))
        }
    }

    @Test
    fun testMaterial() {
        // 材質リスト
        assertEquals(17, mesh.materialCount)

        val stream = this::class.java.classLoader.getResourceAsStream("material.csv")
        requireNotNull(stream)

        val rows: List<Map<String, String>> = csvReader().readAllWithHeader(stream)
        rows.forEach { e ->
            val index = e["index"]?.toInt() ?: throw IllegalStateException()
            val dr = e["dr"]?.toFloat() ?: throw IllegalStateException()
            val dg = e["dg"]?.toFloat() ?: throw IllegalStateException()
            val db = e["db"]?.toFloat() ?: throw IllegalStateException()
            val alpha = e["alpha"]?.toFloat() ?: throw IllegalStateException()
            val specularity = e["specularity"]?.toFloat() ?: throw IllegalStateException()
            val sr = e["sr"]?.toFloat() ?: throw IllegalStateException()
            val sg = e["sg"]?.toFloat() ?: throw IllegalStateException()
            val sb = e["sb"]?.toFloat() ?: throw IllegalStateException()
            val mr = e["mr"]?.toFloat() ?: throw IllegalStateException()
            val mg = e["mg"]?.toFloat() ?: throw IllegalStateException()
            val mb = e["mb"]?.toFloat() ?: throw IllegalStateException()
            val toonIndex = e["toonIndex"]?.toByte() ?: throw IllegalStateException()
            val edgeFlag = e["edgeFlag"]?.toByte() ?: throw IllegalStateException()
            val faceVertCount = e["faceVertCount"]?.toInt() ?: throw IllegalStateException()
            val textureFileName: ByteArray = e["textureFileName"]?.toByteArray() ?: throw IllegalStateException()

            assertArrayEquals(floatArrayOf(dr, dg, db), mesh.material!![index].diffuseColor)
            assertEquals(alpha, mesh.material!![index].alpha)
            assertEquals(specularity, mesh.material!![index].specularity)
            assertArrayEquals(floatArrayOf(sr, sg, sb), mesh.material!![index].specularColor)
            assertArrayEquals(floatArrayOf(mr, mg, mb), mesh.material!![index].ambientColor)
            assertEquals(toonIndex, mesh.material!![index].toonIndex)
            assertEquals(edgeFlag, mesh.material!![index].edgeFlag)
            assertEquals(faceVertCount, mesh.material!![index].faceVertCount)
            //assertEquals(textureFileName, pmdStruct.material!![index].textureFileName)
        }


    }

}
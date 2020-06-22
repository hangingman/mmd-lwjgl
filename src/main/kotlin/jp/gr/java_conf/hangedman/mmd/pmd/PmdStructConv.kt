package jp.gr.java_conf.hangedman.mmd.pmd

import jp.gr.java_conf.hangedman.lwjgl.BufferBuilder.buildFloatBuffer
import jp.gr.java_conf.hangedman.lwjgl.BufferBuilder.buildShortBuffer
import java.nio.FloatBuffer
import java.nio.ShortBuffer

fun PmdStruct.verticesBuffer(): FloatBuffer {
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

fun PmdStruct.colorsBuffer(): FloatBuffer {

    // 面頂点リストの適用合計値に対してどの材質リストを使うかを保持する連想配列
    val materialRanged = this.material!!
            .mapIndexed { i, m ->
                val materialRanged = this.material!!.filterIndexed { index, material ->
                    index <= i
                }.map { it.faceVertCount }.sum()
                materialRanged to m
            }

    // 頂点に対してどの材質リストを使うかを保持する連想配列
    val vertexMaterialMap = this.vertex!!
            .mapIndexed { index, _ ->
                val faceVertIndex = this.faceVertIndex!!.indexOfFirst { faceVert -> faceVert == index.toShort() }
                val material = materialRanged.find { m -> m.first >= faceVertIndex }
                index to material!!.second
            }

    // 頂点に対する色を設定する
    val colors = this.vertex!!
            .mapIndexed { i, _ ->
                val floatList = mutableListOf<Float>()
                val m = vertexMaterialMap.find { (range, _) -> i <= range }!!.second
                floatList.addAll(m.diffuseColor.toList())
                floatList.add(m.alpha)
                floatList
            }.flatten().toFloatArray()

    return buildFloatBuffer(colors)
}

fun PmdStruct.normalsBuffer(): FloatBuffer {
    val normals = this.vertex!!
            .map { v -> v.normalVec }
            .flatMap { fArray ->
                mutableListOf<Float>().also {
                    it.addAll(fArray.asList())
                }
            }.toFloatArray()

    return buildFloatBuffer(normals)
}

fun PmdStruct.faceVertPair(): Pair<Int, ShortBuffer> {
    return this.faceVertCount to buildShortBuffer(this.faceVertIndex!!)
}
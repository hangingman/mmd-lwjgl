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

fun PmdStruct.alphaBuffer(): FloatBuffer {

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

fun PmdStruct.diffuseColorsBuffer(): FloatBuffer {

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

fun PmdStruct.ambientColorsBuffer(): FloatBuffer {

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

fun PmdStruct.specularColorsBuffer(): FloatBuffer {

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

fun PmdStruct.shininessBuffer(): FloatBuffer {

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
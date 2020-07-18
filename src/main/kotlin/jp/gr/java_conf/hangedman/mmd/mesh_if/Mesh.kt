package jp.gr.java_conf.hangedman.mmd.mesh_if

import java.nio.FloatBuffer
import java.nio.ShortBuffer

interface Mesh {
    val meshPath: String

    companion object {
        const val NUL: Char = 0x00.toByte().toChar()
    }

    fun verticesBuffer(): FloatBuffer
    fun alphaBuffer(): FloatBuffer
    fun diffuseColorsBuffer(): FloatBuffer
    fun ambientColorsBuffer(): FloatBuffer
    fun specularColorsBuffer(): FloatBuffer
    fun shininessBuffer(): FloatBuffer
    fun edgeFlagBuffer(): FloatBuffer
    fun normalsBuffer(): FloatBuffer

    fun faceVertPair(): Pair<Int, ShortBuffer>

    fun getTexturePaths(): List<String>
    fun texCoordBuffer(): FloatBuffer
    fun texLayerBuffer(): FloatBuffer
    fun sphereModeBuffer(): FloatBuffer  // -1f: スフィアなし、1f: スフィア乗算、2f: スフィア加算

    fun getModelYMax(): Float
    fun getModelYMin(): Float
    fun getModelInfo(): String
}
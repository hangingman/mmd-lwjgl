package jp.gr.java_conf.hangedman.mmd.mesh_if

import java.nio.FloatBuffer
import java.nio.ShortBuffer

interface Mesh {
    fun verticesBuffer(): FloatBuffer
    fun alphaBuffer(): FloatBuffer
    fun diffuseColorsBuffer(): FloatBuffer
    fun ambientColorsBuffer(): FloatBuffer
    fun specularColorsBuffer(): FloatBuffer
    fun shininessBuffer(): FloatBuffer
    fun edgeFlagBuffer(): FloatBuffer
    fun normalsBuffer(): FloatBuffer
    fun faceVertPair(): Pair<Int, ShortBuffer>

    fun getModelYMax(): Float
    fun getModelYMin(): Float
}
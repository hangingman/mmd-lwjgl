package jp.gr.java_conf.hangedman.lwjgl

import org.lwjgl.BufferUtils
import java.nio.FloatBuffer
import java.nio.ShortBuffer

object BufferBuilder {
    fun buildFloatBuffer(array: FloatArray): FloatBuffer {
        return BufferUtils.createFloatBuffer(array.size).apply {
            put(array)
            flip()
        }
    }
    fun buildShortBuffer(array: ShortArray): ShortBuffer {
        return BufferUtils.createShortBuffer(array.size).apply {
            put(array)
            flip()
        }
    }
}
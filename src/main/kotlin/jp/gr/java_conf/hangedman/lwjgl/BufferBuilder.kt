package jp.gr.java_conf.hangedman.lwjgl

import org.lwjgl.BufferUtils
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.nio.ShortBuffer

object BufferBuilder {
    fun buildByteBuffer(array: ByteArray): ByteBuffer {
        return BufferUtils.createByteBuffer(array.size).apply {
            put(array)
            flip()
        }
    }
    fun buildIntBuffer(array: IntArray): IntBuffer {
        return BufferUtils.createIntBuffer(array.size).apply {
            put(array)
            flip()
        }
    }
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
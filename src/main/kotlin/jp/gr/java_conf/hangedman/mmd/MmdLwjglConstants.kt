package jp.gr.java_conf.hangedman.mmd

import org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER
import org.lwjgl.opengl.GL31.GL_TEXTURE_BUFFER
import java.lang.IllegalStateException

object MmdLwjglConstants {
    const val width = 800
    const val height = 640
    const val title = "Load PMD file testing"
}

enum class VboIndex(val asInt: Int) {
    VERTEX(0),
    ALPHA(1),
    DIFFUSE_COLOR(2),
    AMBIENT_COLOR(3),
    SPECULAR_COLOR(4),
    NORMAL(5),
    SHININESS(6),
    EDGE(7),
    TEXTURE_COORD(8),
    TEXTURE_LAYER(9),
    SPHERE_MODE(10);

    fun elementSize(): Int {
        return when(this) {
            VERTEX -> 3
            ALPHA -> 1
            DIFFUSE_COLOR  -> 3
            AMBIENT_COLOR -> 3
            SPECULAR_COLOR -> 3
            NORMAL -> 3
            SHININESS -> 1
            EDGE -> 1
            TEXTURE_COORD -> 2
            TEXTURE_LAYER -> 1
            SPHERE_MODE -> 1
            else -> throw IllegalStateException("Invalid VboIndex Enum")
        }
    }

    fun bufferTarget(): Int {
        return when(this) {
            VERTEX -> GL_ARRAY_BUFFER
            ALPHA -> GL_ARRAY_BUFFER
            DIFFUSE_COLOR  -> GL_ARRAY_BUFFER
            AMBIENT_COLOR -> GL_ARRAY_BUFFER
            SPECULAR_COLOR -> GL_ARRAY_BUFFER
            NORMAL -> GL_ARRAY_BUFFER
            SHININESS -> GL_ARRAY_BUFFER
            EDGE -> GL_ARRAY_BUFFER
            TEXTURE_COORD -> GL_TEXTURE_BUFFER
            else -> throw IllegalStateException("Invalid VboIndex Enum")
        }
    }
}


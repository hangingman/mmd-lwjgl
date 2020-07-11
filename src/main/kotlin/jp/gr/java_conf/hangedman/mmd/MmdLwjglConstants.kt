package jp.gr.java_conf.hangedman.mmd

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
    TEXTURE(8);

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
            TEXTURE -> 2
            else -> throw IllegalStateException("Invalid VboIndex Enum")
        }
    }
}


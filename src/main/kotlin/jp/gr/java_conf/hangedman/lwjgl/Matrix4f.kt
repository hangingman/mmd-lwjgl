package jp.gr.java_conf.hangedman.lwjgl

import org.lwjgl.glfw.GLFW
import org.lwjgl.system.MemoryStack
import kotlin.math.cos
import kotlin.math.sin

// https://www.dcom-web.co.jp/lab/game/lwjgl/rendering_polygons_with_programmable_shaders
// 行列クラス(DirectXと違ってVector、Matrixクラスがない)
// とのことだがJomlを探せばありそうな気がする、とりあえず理解できるまではこのまま
class Matrix4f {
    /**
     *  m0[ 0]  m1[ 4]  m2[ 8]  m3[12]
     *  m4[ 1]  m5[ 5]  m6[ 9]  m7[13]
     *  m8[ 2]  m9[ 6] m10[10] m11[14]
     * m12[ 3] m13[ 7] m14[11] m15[15]
     */
    var value: FloatArray = FloatArray(16)

    // 単位行列
    fun identity(m: Matrix4f) {
        m.value[0] = 1.0f
        m.value[1] = 0.0f
        m.value[2] = 0.0f
        m.value[3] = 0.0f
        m.value[4] = 0.0f
        m.value[5] = 1.0f
        m.value[6] = 0.0f
        m.value[7] = 0.0f
        m.value[8] = 0.0f
        m.value[9] = 0.0f
        m.value[10] = 1.0f
        m.value[11] = 0.0f
        m.value[12] = 0.0f
        m.value[13] = 0.0f
        m.value[14] = 0.0f
        m.value[15] = 1.0f
    }

    // Y軸で回転させる回転行列
    companion object {
        fun rotateY(m: Matrix4f, degree: Float) {
            val radian: Double = Math.toRadians(degree.toDouble())
            val sin: Float = sin(radian.toFloat())
            val cos: Float = cos(radian.toFloat())
            m.value[0] = cos
            m.value[1] = 0.0f
            m.value[2] = -sin
            m.value[3] = 0.0f
            m.value[4] = 0.0f
            m.value[5] = 1.0f
            m.value[6] = 0.0f
            m.value[7] = 0.0f
            m.value[8] = sin
            m.value[9] = 0.0f
            m.value[10] = cos
            m.value[11] = 0.0f
            m.value[12] = 0.0f
            m.value[13] = 0.0f
            m.value[14] = 0.0f
            m.value[15] = 1.0f
        }
    }
    // 正射影行列
    fun orthographic(m: Matrix4f, left: Float, right: Float, bottom: Float, top: Float, near: Float, far: Float) {
        m.value[0] = 2.0f / (right - left)
        m.value[1] = 0.0f
        m.value[2] = 0.0f
        m.value[3] = 0.0f
        m.value[4] = 0.0f
        m.value[5] = 2.0f / (top - bottom)
        m.value[6] = 0.0f
        m.value[7] = 0.0f
        m.value[8] = 0.0f
        m.value[9] = 0.0f
        m.value[10] = -2.0f / (far - near)
        m.value[11] = 0.0f
        m.value[12] = -(right + left) / (right - left)
        m.value[13] = -(top + bottom) / (top - bottom)
        m.value[14] = -(far + near) / (far - near)
        m.value[15] = 1.0f
    }
}

fun createProjectionMatrix(): Matrix4f {
    val stack = MemoryStack.stackPush()
    val window = GLFW.glfwGetCurrentContext()
    val width = stack.mallocInt(1)
    val height = stack.mallocInt(1)
    GLFW.glfwGetFramebufferSize(window, width, height)
    val ratio: Float = width.get().toFloat() / height.get()
    val projection = Matrix4f()
    projection.orthographic(projection, -ratio, ratio, -1f, 1f, -1f, 1f)
    return projection
}
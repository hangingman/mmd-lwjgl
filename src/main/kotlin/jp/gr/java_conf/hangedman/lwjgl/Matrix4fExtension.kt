package jp.gr.java_conf.hangedman.lwjgl

import org.joml.Matrix4f
import org.lwjgl.BufferUtils
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL11.*
import org.lwjgl.system.MemoryStack
import java.nio.FloatBuffer

fun Matrix4f.value(): FloatBuffer {
    val floatBuffer = BufferUtils.createFloatBuffer(16)
    this.get(floatBuffer)
    return floatBuffer
}

fun Matrix4f.createProjectionMatrix(foV: Float): Matrix4f {
    val stack = MemoryStack.stackPush()
    val window = GLFW.glfwGetCurrentContext()
    val width = stack.mallocInt(1)
    val height = stack.mallocInt(1)
    GLFW.glfwGetFramebufferSize(window, width, height)

    // projection（投影）の方法は複数ある
    // とりあえずズームが動いているorthoで

    //val ratio: Float = width.get().toFloat() / height.get()
    //val ratio = foV
    //return Matrix4f().ortho(-ratio, ratio, -1f, 1f, -1f, 1f)

    return Matrix4f().ortho(-foV, foV, -foV, foV, -foV, foV)

    //return Matrix4f().perspective(
    //        Math.toRadians(foV.toDouble()).toFloat(), 4.0f / 3.0f, 0.1f, 100.0f
    //)
}
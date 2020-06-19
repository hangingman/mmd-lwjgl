package jp.gr.java_conf.hangedman.lwjgl

import org.joml.Matrix4f
import org.lwjgl.BufferUtils
import org.lwjgl.glfw.GLFW
import org.lwjgl.system.MemoryStack
import java.nio.FloatBuffer

fun Matrix4f.value(): FloatBuffer {
    val floatBuffer = BufferUtils.createFloatBuffer(16)
    this.get(floatBuffer)
    return floatBuffer
}

fun Matrix4f.createProjectionMatrix(): Matrix4f {
    val stack = MemoryStack.stackPush()
    val window = GLFW.glfwGetCurrentContext()
    val width = stack.mallocInt(1)
    val height = stack.mallocInt(1)
    GLFW.glfwGetFramebufferSize(window, width, height)
    val ratio: Float = width.get().toFloat() / height.get()
    return Matrix4f().ortho(-ratio, ratio, -1f, 1f, -1f, 1f)
}
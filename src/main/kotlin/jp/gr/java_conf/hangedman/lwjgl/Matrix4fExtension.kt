package jp.gr.java_conf.hangedman.lwjgl

import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.BufferUtils
import org.lwjgl.glfw.GLFW
import org.lwjgl.system.MemoryStack
import java.nio.FloatBuffer

fun Matrix4f.value(): FloatBuffer {
    val floatBuffer = BufferUtils.createFloatBuffer(16)
    this.get(floatBuffer)
    return floatBuffer
}

fun Vector3f.value(): FloatBuffer {
    val floatBuffer = BufferUtils.createFloatBuffer(3)
    this.get(floatBuffer)
    return floatBuffer
}

fun getCurrentWH(): Pair<Int, Int> {
    val stack = MemoryStack.stackPush()
    val window = GLFW.glfwGetCurrentContext()
    val widthBuffer = stack.mallocInt(1)
    val heightBuffer = stack.mallocInt(1)
    GLFW.glfwGetFramebufferSize(window, widthBuffer, heightBuffer)
    return widthBuffer.get() to heightBuffer.get()
}

fun createProjectionMatrix(foV: Float): Matrix4f {

    // projection（投影）の方法は複数ある, とりあえずズームが動いているorthoで
    return Matrix4f().ortho(
            -foV,
            foV,
            -foV,
            foV,
            0.1f,
            100f
    )
}

fun Matrix4f.orbitBy(modelCenter: Vector3f, angleY: Float, angleX: Float, focalLength: Float) {
    // Setup both camera's view matrices
    val modelCenterCp = Vector3f(modelCenter)
    val recenter = Matrix4f().translate(modelCenterCp.mul(-1.0f)) //not needed if world origin
    val rotation = Matrix4f()
            .rotateY(angleY) //can be replaced by glm::eulerAngleZXY(yaw, pitch, roll) ...
            .rotateX(angleX)
    val moveBack = Matrix4f().translate(modelCenter) //not needed if world origin
    val transfer = moveBack.mul(rotation).mul(recenter) //order from right to left

    val eye = Vector3f(0f, modelCenter.y, -focalLength).mulProject(transfer)
    val up = Vector3f(0f, 1f, 0f)
    this.setLookAt(eye, modelCenter, up)
}
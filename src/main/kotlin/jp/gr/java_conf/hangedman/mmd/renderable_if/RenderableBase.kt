package jp.gr.java_conf.hangedman.mmd.renderable_if

import jp.gr.java_conf.hangedman.mmd.mesh_if.Mesh
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.glfw.GLFW.*
import kotlin.math.atan

open class RenderableBase(override val windowId: Long) : Renderable {

    // カメラ関連
    var firstTime: Long = System.nanoTime()
    var lastTime: Long = firstTime
    var rotationY = 0
    var rotationX = 1
    var cameraRotate = floatArrayOf(0.0f, 0.0f)
    var rotation = floatArrayOf(0.0f, 0.0f)
    var modelCenter = Vector3f(0f, 0f, 0f)
    var focalLength = 30f  // 焦点距離
    var previousXpos = 0.0
    var previousYpos = 0.0

    // Shader(Model)
    var shader: Int = 0
    var vertShaderObj: Int = 0
    var fragShaderObj: Int = 0

    // Model, View, Projection
    val projMatrix = Matrix4f()
    val viewMatrix = Matrix4f()
    val modelMatrix = Matrix4f()

    override fun initialize(mesh: Mesh?): Renderable { return this }
    override fun render() {}
    override fun updatePos(windowId: Long) {}
    override fun cleanup() {}

    override fun cursorPosCallback(windowId: Long, xpos: Double, ypos: Double) {
        val (xposDelta, yposDelta) = if (glfwGetMouseButton(windowId, GLFW_MOUSE_BUTTON_RIGHT) == GLFW_PRESS) {
            // 右クリックしている場合にモデルを動かす
            previousXpos - xpos to previousYpos - ypos
        } else {
            0.0 to 0.0
        }
        previousXpos = xpos
        previousYpos = ypos

        val angleX = atan(xposDelta / focalLength.toDouble()).toFloat()
        val angleY = atan(yposDelta / focalLength.toDouble()).toFloat()

        cameraRotate[rotationY] = angleX * 20.0f
        cameraRotate[rotationX] = - angleY * 20.0f
    }

    override fun keyCallback(windowId: Long, key: Int, scancode: Int, action: Int, mods: Int) {}
    override fun setScrollCallback(windowId: Long, xoffset: Double, yoffset: Double) {
        focalLength -= yoffset.toFloat()
    }
}
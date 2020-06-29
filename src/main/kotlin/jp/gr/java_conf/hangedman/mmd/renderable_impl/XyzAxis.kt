package jp.gr.java_conf.hangedman.mmd.renderable_impl

import jp.gr.java_conf.hangedman.lwjgl.BufferBuilder.buildFloatBuffer
import jp.gr.java_conf.hangedman.lwjgl.ModelViewProjection.updateMVP
import jp.gr.java_conf.hangedman.lwjgl.ShaderHandler.makeShader
import jp.gr.java_conf.hangedman.mmd.MmdLwjglConstants.width
import jp.gr.java_conf.hangedman.mmd.pmd.PmdStruct
import jp.gr.java_conf.hangedman.mmd.renderable_if.Renderable
import jp.gr.java_conf.hangedman.mmd.shader.AxisShader.axisFragmentSource
import jp.gr.java_conf.hangedman.mmd.shader.AxisShader.axisVertexSource
import org.joml.Vector3f
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL15.glDeleteBuffers
import org.lwjgl.opengl.GL30.*
import org.lwjgl.opengl.GLUtil
import uk.org.lidalia.sysoutslf4j.context.SysOutOverSLF4J

// XYZ軸を表す線を描画する
class XyzAxis(override val windowId: Long) : Renderable {

    // VAO, VBO
    private var vao: Int = 0
    private var vbo: IntArray = IntArray(2)

    // Shader
    private var shader: Int = 0
    private var vertShaderObj: Int = 0
    private var fragShaderObj: Int = 0

    // XYZ軸の位置
    private var position = Vector3f(0f, 0f, 0f)
    private val axisSize = 5.0f
    private var rotation = 1.0f

    override fun initialize(pmdStruct: PmdStruct?): Renderable {
        // コンテキストの作成
        glfwMakeContextCurrent(windowId)
        glfwSwapInterval(1)
        GL.createCapabilities()

        SysOutOverSLF4J.sendSystemOutAndErrToSLF4J()
        GLUtil.setupDebugMessageCallback(System.out)

        // バッファのバインド
        this.vao = glGenVertexArrays()
        glBindVertexArray(this.vao)

        val verticesBuffer = floatArrayOf(
                axisSize, 0.0f, 0.0f,
                0.0f, 0.0f, 0.0f,
                0.0f, axisSize, 0.0f,
                0.0f, 0.0f, 0.0f,
                0.0f, 0.0f, axisSize,
                0.0f, 0.0f, 0.0f
        ).run { buildFloatBuffer(this) }

        val colorsBuffer = floatArrayOf(
                1.0f, 0.0f, 0.0f,
                1.0f, 0.0f, 0.0f,
                0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f
        ).run { buildFloatBuffer(this) }

        this.vbo[0] = glGenBuffers()
        this.vbo[1] = glGenBuffers()

        glBindBuffer(GL_ARRAY_BUFFER, this.vbo[0])
        glBufferData(GL_ARRAY_BUFFER, verticesBuffer, GL_STATIC_DRAW)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0)
        glEnableVertexAttribArray(0)

        glBindBuffer(GL_ARRAY_BUFFER, this.vbo[1])
        glBufferData(GL_ARRAY_BUFFER, colorsBuffer, GL_STATIC_DRAW)
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0)
        glEnableVertexAttribArray(1)

        glBindBuffer(GL_ARRAY_BUFFER, 0)
        glBindVertexArray(0)

        makeShader(axisVertexSource, axisFragmentSource).let { (vertShaderObj, fragShaderObj, shader) ->
            this.vertShaderObj = vertShaderObj
            this.fragShaderObj = fragShaderObj
            this.shader = shader
        }

        return this
    }

    override fun updatePos(windowId: Long) {
        updateMVP(shader, 50f, position)
    }

    override fun render() {
        // 頂点情報のすべての情報を持つVAOをバインドする
        glBindVertexArray(this.vao)
        glEnableVertexAttribArray(0)

        glUseProgram(this.shader)
        updatePos(windowId)

        glDrawArrays(GL_LINES, 0, 3 * 3)

        // 全ての選択を外す
        glUseProgram(0)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)
        glDisableVertexAttribArray(0)
        glBindVertexArray(0)
    }

    override fun cleanup() {
        glDeleteVertexArrays(this.vao)
        glDeleteBuffers(this.vbo[0])
        glDeleteBuffers(this.vbo[1])
        glDeleteShader(this.vertShaderObj)
        glDeleteShader(this.fragShaderObj)
        glDeleteProgram(this.shader)
    }

    override fun cursorPosCallback(windowId: Long, xpos: Double, ypos: Double) {
        if (glfwGetMouseButton(windowId, GLFW_MOUSE_BUTTON_RIGHT) == GLFW_PRESS) {
            // 右クリックしている場合にモデルを動かす
            rotation = (xpos.toFloat() / width - 0.5f) * 2f * Math.PI.toFloat()
        }
    }
}
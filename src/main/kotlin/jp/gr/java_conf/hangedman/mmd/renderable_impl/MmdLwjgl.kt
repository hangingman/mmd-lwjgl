package jp.gr.java_conf.hangedman.mmd.renderable_impl

import jp.gr.java_conf.hangedman.lwjgl.ShaderHandler.makeShader
import jp.gr.java_conf.hangedman.lwjgl.createProjectionMatrix
import jp.gr.java_conf.hangedman.lwjgl.value
import jp.gr.java_conf.hangedman.mmd.MmdLwjglConstants.width
import jp.gr.java_conf.hangedman.mmd.VboIndex
import jp.gr.java_conf.hangedman.mmd.pmd.*
import jp.gr.java_conf.hangedman.mmd.renderable_if.Renderable
import jp.gr.java_conf.hangedman.mmd.shader.ModelShader.modelFragmentSource
import jp.gr.java_conf.hangedman.mmd.shader.ModelShader.modelVertexSource
import org.joml.Math.cos
import org.joml.Math.sin
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWScrollCallback
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL33.*
import org.lwjgl.opengl.GLUtil
import uk.org.lidalia.sysoutslf4j.context.SysOutOverSLF4J

class MmdLwjgl(override val windowId: Long) : Renderable {

    // VAO, VBO, VBOI
    private var vao: Int = 0
    private var vbo: IntArray = IntArray(VboIndex.values().size)
    private var vboi: Int = 0
    private var indicesCount: Int = 0

    // Shader
    private var shader: Int = 0
    private var vertShaderObj: Int = 0
    private var fragShaderObj: Int = 0

    // モデルの位置
    private var position = Vector3f(0f, 0f, 0f)

    // 初期視野
    private var initialFoV = 45.0f
    private var fov = initialFoV
    private var rotation = 1.0f
    private var lastTime = glfwGetTime()

    // VAO, VBO, VBOIの読み込み
    private fun load(pmdStruct: PmdStruct) {

        val verticesBuffer = pmdStruct.verticesBuffer()             // 頂点
        val alphaBuffer = pmdStruct.alphaBuffer()                   // 物体色透過率
        val diffuseColorsBuffer = pmdStruct.diffuseColorsBuffer()   // 物体色
        val ambientColorsBuffer = pmdStruct.ambientColorsBuffer()   // 環境色
        val specularColorsBuffer = pmdStruct.specularColorsBuffer() // 光沢色
        val normalsBuffer = pmdStruct.normalsBuffer()               // 法線
        val shininessBuffer = pmdStruct.shininessBuffer()           // 光沢度
        val edgeFlagBuffer = pmdStruct.edgeFlagBuffer()             // エッジの有無

        val (indicesCount, indicesBuffer) = pmdStruct.faceVertPair()  // 面頂点
        this.indicesCount = indicesCount

        // Vertex Array Objectをメモリ上に作成し選択する(バインド)
        // VAOはデフォルトで16の属性(VBO)を設定できる
        this.vao = glGenVertexArrays()
        glBindVertexArray(this.vao)

        // 新しいVertex Buffer Objectをメモリ上に作成し選択する(バインド), VBOはベクトルの集まり
        mapOf(
                VboIndex.VERTEX to verticesBuffer,
                VboIndex.ALPHA to alphaBuffer,
                VboIndex.DIFFUSE_COLOR to diffuseColorsBuffer,
                VboIndex.AMBIENT_COLOR to ambientColorsBuffer,
                VboIndex.SPECULAR_COLOR to specularColorsBuffer,
                VboIndex.NORMAL to normalsBuffer,
                VboIndex.SHININESS to shininessBuffer,
                VboIndex.EDGE to edgeFlagBuffer
        ).forEach { (index, buffer) ->
            this.vbo[index.asInt] = glGenBuffers()
            glBindBuffer(GL_ARRAY_BUFFER, this.vbo[index.asInt])
            glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW)
            glVertexAttribPointer(index.asInt, index.elementSize(), GL_FLOAT, false, 0, 0)
            glEnableVertexAttribArray(index.asInt)
            glBindBuffer(GL_ARRAY_BUFFER, 0)
        }
        glBindVertexArray(0)

        // Create a new VBO for the indices and select it (bind)
        this.vboi = glGenBuffers()
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, this.vboi)
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL_STATIC_DRAW)
        // Deselect (bind to 0) the VBO
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)
    }

    // 初期化してシェーダーを返す
    override fun initialize(pmdStruct: PmdStruct): MmdLwjgl {
        // コンテキストの作成
        glfwMakeContextCurrent(windowId)
        glfwSwapInterval(1)
        GL.createCapabilities()

        SysOutOverSLF4J.sendSystemOutAndErrToSLF4J()
        GLUtil.setupDebugMessageCallback(System.out)

        // 背景色変更
        glClearColor(0.4f, 0.6f, 0.9f, 0f)

        // Zバッファ
        glEnable(GL_DEPTH_TEST)  // デプステストを有効にする
        glDepthFunc(GL_LESS)     // 前のものよりもカメラに近ければ、フラグメントを受け入れる

        glEnable(GL_CULL_FACE)   // 視点に対して裏を向いている面を表示しないようにする
        glCullFace(GL_BACK)

        glfwSetScrollCallback(windowId, object : GLFWScrollCallback() {
            override fun invoke(windowId: Long, xoffset: Double, yoffset: Double) {
                if (yoffset < 0) {
                    fov *= 1.05f;
                } else {
                    fov *= 1f / 1.05f;
                }
            }
        })
        glfwSetCursorPosCallback(windowId) { _, x, _ ->
            if (glfwGetMouseButton(windowId, GLFW_MOUSE_BUTTON_RIGHT) == GLFW_PRESS) {
                // 右クリックしている場合にモデルを動かす
                rotation = (x.toFloat() / width - 0.5f) * 2f * Math.PI.toFloat()
            }
        }

        // ここから描画情報の読み込み
        load(pmdStruct)

        // 0番目のシェーダーをモデル用に使う
        makeShader(modelVertexSource, modelFragmentSource).let { (vertShaderObj, fragShaderObj, shader) ->
            this.vertShaderObj = vertShaderObj
            this.fragShaderObj = fragShaderObj
            this.shader = shader
        }

        return this
    }

    override fun update(windowId: Long) {
        // キーボードとマウスのインプットからMVP行列を計算する
        computeMatricesFromInputs(windowId)

        // Model行列(描画対象のモデルの座標からOpenGLのワールド座標への相対値)
        // 頂点シェーダーのグローバルGLSL変数"model"に設定
        val uniModel = glGetUniformLocation(this.shader, "model")
        // updateメソッドで求めた回転行列をグローバルGLSL変数に設定
        glUniformMatrix4fv(uniModel, false, Matrix4f().value())

        // View行列(OpenGLのワールド座標からカメラの座標への相対値)
        // 頂点シェーダーのグローバルGLSL変数"view"に設定
        val uniView = glGetUniformLocation(this.shader, "view")
        val viewMatrix = Matrix4f().setLookAt(
                position.x, position.y, position.z,  // ワールド空間でのカメラの位置
                0f, 0f, 0f, // ワールド空間での見たい位置
                0f, 1f, 0f
        )
        glUniformMatrix4fv(uniView, false, viewMatrix.value())

        // Projection行列(カメラの座標から、映し出される（射影）ものへの相対値)
        // 頂点シェーダーのグローバルGLSL変数"projection"に設定
        val projectionMatrix = Matrix4f().createProjectionMatrix(fov)
        val uniProjection = glGetUniformLocation(this.shader, "projection")
        glUniformMatrix4fv(uniProjection, false, projectionMatrix.value())

        // 照明の座標
        val uLightPosition = glGetUniformLocation(this.shader, "uLightPosition")
        glUniform3f(uLightPosition, 20f, 20f, -20f)

        // エッジの太さ, 色
        val uEdgeSize = glGetUniformLocation(this.shader, "uEdgeSize")
        glUniform1f(uEdgeSize, 0.1f)
        val uEdgeColor = glGetUniformLocation(this.shader, "uEdgeColor")
        glUniform3f(uEdgeColor, 0f, 0f, 0f)
    }

    private fun computeMatricesFromInputs(windowId: Long) {

        val currentTime = glfwGetTime()
        val deltaTime = (currentTime - lastTime).toFloat()
        lastTime = currentTime

        position.set(-1f * cos(rotation), 0f, -5.0f * sin(rotation))
    }

    override fun render() {
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        // 頂点情報のすべての情報を持つVAOをバインドする
        glBindVertexArray(this.vao)
        glEnableVertexAttribArray(0)

        // 頂点情報の並びの情報をすべて持つVBO indexをバインドする
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, this.vboi)
        glUseProgram(this.shader)

        // 頂点情報を描画する
        glDrawElements(GL_TRIANGLES, indicesCount, GL_UNSIGNED_SHORT, 0)

        // Put everything back to default (deselect)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)
        glDisableVertexAttribArray(0)
        glBindVertexArray(0)
    }

    override fun cleanup() {
        glDeleteVertexArrays(this.vao)
        VboIndex.values().forEach { index ->
            glDeleteBuffers(this.vbo[index.asInt])
        }
        glDeleteBuffers(this.vboi)

        glDeleteShader(this.vertShaderObj)
        glDeleteShader(this.fragShaderObj)
        glDeleteProgram(this.shader)
    }
}

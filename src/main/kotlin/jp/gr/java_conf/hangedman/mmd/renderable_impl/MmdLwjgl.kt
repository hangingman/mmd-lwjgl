package jp.gr.java_conf.hangedman.mmd.renderable_impl

import jp.gr.java_conf.hangedman.lwjgl.BufferBuilder.buildFloatBuffer
import jp.gr.java_conf.hangedman.lwjgl.ModelViewProjection.updateMVP
import jp.gr.java_conf.hangedman.lwjgl.ShaderHandler.makeShader
import jp.gr.java_conf.hangedman.mmd.MmdLwjglConstants.height
import jp.gr.java_conf.hangedman.mmd.MmdLwjglConstants.width
import jp.gr.java_conf.hangedman.mmd.VboIndex
import jp.gr.java_conf.hangedman.mmd.pmd.*
import jp.gr.java_conf.hangedman.mmd.renderable_if.Renderable
import jp.gr.java_conf.hangedman.mmd.shader.ModelShader.modelFragmentSource
import jp.gr.java_conf.hangedman.mmd.shader.ModelShader.modelVertexSource
import jp.gr.java_conf.hangedman.mmd.shader.SimpleShader.simpleFragmentSource
import jp.gr.java_conf.hangedman.mmd.shader.SimpleShader.simpleVertexSource
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.glfw.GLFW.*
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

    // Shader(Model)
    private var shader: Int = 0
    private var vertShaderObj: Int = 0
    private var fragShaderObj: Int = 0

    // Shader(Camera)
    private var vaoCamera: Int = 0
    private var vboCamera: IntArray = IntArray(2)
    private var shaderCamera: Int = 0
    private var vertShaderObjCamera: Int = 0
    private var fragShaderObjCamera: Int = 0

    // Model, View, Projection
    private val projMatrix = arrayOf(Matrix4f(), Matrix4f())
    private val viewMatrix = arrayOf(Matrix4f(), Matrix4f())
    private val modelMatrix = Matrix4f()

    // カメラ
    private var firstTime: Long = System.nanoTime()
    private var lastTime: Long = firstTime
    private var active = 0
    private var inactive = 1
    private var rotate = 0.0f
    private var rotation = floatArrayOf(0.0f, 0.0f)
    private var center = Vector3f(0f, 0f, 0f)

    // VAO, VBO, VBOIの読み込み
    private fun load(pmdStruct: PmdStruct?) {

        requireNotNull(pmdStruct)
        val verticesBuffer = pmdStruct.verticesBuffer()             // 頂点
        val alphaBuffer = pmdStruct.alphaBuffer()                   // 物体色透過率
        val diffuseColorsBuffer = pmdStruct.diffuseColorsBuffer()   // 物体色
        val ambientColorsBuffer = pmdStruct.ambientColorsBuffer()   // 環境色
        val specularColorsBuffer = pmdStruct.specularColorsBuffer() // 光沢色
        val normalsBuffer = pmdStruct.normalsBuffer()               // 法線
        val shininessBuffer = pmdStruct.shininessBuffer()           // 光沢度
        val edgeFlagBuffer = pmdStruct.edgeFlagBuffer()             // エッジの有無

        // カメラの視点のためPMDモデルの中心を計算する(0, Ymax + Ymin / 2, 0)
        val modelYMax = pmdStruct.vertex!!.map{ v -> v.pos[1] }.max()!!
        val modelYMin = pmdStruct.vertex!!.map{ v -> v.pos[1] }.min()!!
        center = Vector3f(0f, (modelYMax + modelYMin)/2, 0f)

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
    override fun initialize(pmdStruct: PmdStruct?): MmdLwjgl {
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

        // ここから描画情報の読み込み
        load(pmdStruct)

        makeShader(modelVertexSource, modelFragmentSource).let { (vertShaderObj, fragShaderObj, shader) ->
            this.vertShaderObj = vertShaderObj
            this.fragShaderObj = fragShaderObj
            this.shader = shader
        }

        //initFrustum()

        return this
    }

    private fun initFrustum() {
        makeShader(simpleVertexSource, simpleFragmentSource).let { (vertShaderObj, fragShaderObj, shader) ->
            this.vertShaderObjCamera = vertShaderObj
            this.fragShaderObjCamera = fragShaderObj
            this.shaderCamera = shader
        }
    }

    override fun updatePos(windowId: Long) {
        // キーボードとマウスのインプットからMVP行列を計算する
        computeMatricesFromInputs()

        // glUniform系の関数はglUseProgramを実行している間に呼び出す
        val thisTime = System.nanoTime()
        val angle = (thisTime - firstTime) / 1E9f
        val delta = (thisTime - lastTime) / 1E9f
        lastTime = thisTime

        // Process rotation
        rotation[inactive] += rotate * delta

        // Setup both camera's projection matrices
        projMatrix[0].setPerspective(Math.toRadians(40.0).toFloat(), (width/height).toFloat(), 0.1f, 100.0f)
        projMatrix[1].setPerspective(Math.toRadians(30.0).toFloat(), (width/height).toFloat(), 2.0f, 5.0f)

        // Setup both camera's view matrices
        viewMatrix[0].setLookAt(
                0f, center.y, 25f,
                center.x, center.y, center.z,
                0f, 1f, 0f)
                .rotateY(rotation[0])
        viewMatrix[1].setLookAt(
                3f, 1f, 1f,
                0f, 0f, 0f,
                0f, 1f, 0f)
                .rotateY(rotation[1])

        // Apply model transformation to active camera's view
        modelMatrix.rotationY(angle * Math.toRadians(10.0).toFloat())
        updateMVP(shader, modelMatrix, viewMatrix[active], projMatrix[active])

        // 照明の座標
        val uLightPosition = glGetUniformLocation(shader, "uLightPosition")
        glUniform3f(uLightPosition, 20f, 20f, -20f)
        // エッジの太さ, 色
        val uEdgeSize = glGetUniformLocation(shader, "uEdgeSize")
        glUniform1f(uEdgeSize, 0.1f)
        val uEdgeColor = glGetUniformLocation(shader, "uEdgeColor")
        glUniform3f(uEdgeColor, 0f, 0f, 0f)
    }

    private fun computeMatricesFromInputs() {

        val currentTime = glfwGetTime()
        //val deltaTime = (currentTime - lastTime).toFloat()
        //lastTime = currentTime
        //position.set(-1f * cos(rotation), 0f, -5.0f * sin(rotation))
    }

    override fun render() {
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        glViewport(0, 0, width, height)

        // 頂点情報のすべての情報を持つVAOをバインドする
        glBindVertexArray(this.vao)
        glEnableVertexAttribArray(0)

        // 頂点情報の並びの情報をすべて持つVBO indexをバインドする
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, this.vboi)
        glUseProgram(this.shader)
        // MVP行列の更新
        updatePos(windowId)

        // 頂点情報を描画する
        glDrawElements(GL_TRIANGLES, indicesCount, GL_UNSIGNED_SHORT, 0)

        // 全ての選択を外す
        glUseProgram(0)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)
        glDisableVertexAttribArray(0)
        glBindVertexArray(0)

        //renderFrustum()
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

        // camera用
        /**
        glDeleteVertexArrays(this.vaoCamera)
        glDeleteBuffers(this.vboCamera[0])
        glDeleteBuffers(this.vboCamera[1])
        glDeleteShader(this.vertShaderObjCamera)
        glDeleteShader(this.fragShaderObjCamera)
        glDeleteProgram(this.shaderCamera)
        */
    }

    override fun cursorPosCallback(windowId: Long, xpos: Double, ypos: Double) {
//        if (glfwGetMouseButton(windowId, GLFW_MOUSE_BUTTON_RIGHT) == GLFW_PRESS) {
//            // 右クリックしている場合にモデルを動かす
//            rotation = (xpos.toFloat() / width - 0.5f) * 2f * Math.PI.toFloat()
//        }
    }

    override fun keyCallback(windowId: Long, key: Int, scancode: Int, action: Int, mods: Int) {
        if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
            glfwSetWindowShouldClose(windowId, true)
        if (key == GLFW_KEY_C && action == GLFW_RELEASE)
            switchCamera()
        if (key == GLFW_KEY_LEFT && (action == GLFW_PRESS || action == GLFW_REPEAT)) {
            rotate = 1.0f
        } else if (key == GLFW_KEY_LEFT && (action == GLFW_RELEASE)) {
            rotate = 0.0f
        } else if (key == GLFW_KEY_RIGHT && (action == GLFW_PRESS || action == GLFW_REPEAT)) {
            rotate = -1.0f
        } else if (key == GLFW_KEY_RIGHT && (action == GLFW_RELEASE)) {
            rotate = 0.0f
        }
    }

    private fun switchCamera() {
        active = 1 - active
        inactive = 1 - inactive
    }

    private fun renderFrustum() {
        val m = Matrix4f().set(projMatrix[inactive]).mul(viewMatrix[inactive])
        val vertices = mutableListOf<Float>()
        val colors = mutableListOf<Float>()

        // Perspective origin to near plane
        val v = Vector3f()

        for (i in 0 until 4) {
            m.perspectiveOrigin(v)
            vertices.addAll(listOf(v.x, v.y, v.z))
            colors.addAll(listOf(0.2f, 0.2f, 0.2f))
            m.frustumCorner(i, v)
            vertices.addAll(listOf(v.x, v.y, v.z))
            colors.addAll(listOf(0.2f, 0.2f, 0.2f))
        }
        // Near plane
        for (i in 0 until 4+1) {
            m.frustumCorner(i and 3, v)
            vertices.addAll(listOf(v.x, v.y, v.z))
            colors.addAll(listOf(0.8f, 0.2f, 0.2f))
        }
        // Edges
        for (i in 0 until 4) {
            m.frustumCorner(3 - i, v)
            vertices.addAll(listOf(v.x, v.y, v.z))
            colors.addAll(listOf(0.0f, 0.0f, 0.0f))
            m.frustumCorner(4 + ((i + 2) and 3), v)
            vertices.addAll(listOf(v.x, v.y, v.z))
            colors.addAll(listOf(0.0f, 0.0f, 0.0f))
        }
        // Far plane
        for (i in 0 until 4+1) {
            m.frustumCorner(4 + (i and 3), v)
            vertices.addAll(listOf(v.x, v.y, v.z))
            colors.addAll(listOf(0.0f, 0.0f, 0.0f))
        }

        val verticesBuffer = buildFloatBuffer(vertices.toFloatArray())
        val colorsBuffer = buildFloatBuffer(colors.toFloatArray())

        this.vaoCamera = glGenVertexArrays()
        glBindVertexArray(this.vaoCamera)

        this.vboCamera[0] = glGenBuffers()
        glBindBuffer(GL_ARRAY_BUFFER, this.vboCamera[0])
        glBufferData(GL_ARRAY_BUFFER, verticesBuffer, GL_STATIC_DRAW)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0)
        glEnableVertexAttribArray(0)
        glBindBuffer(GL_ARRAY_BUFFER, 0)

        this.vboCamera[1] = glGenBuffers()
        glBindBuffer(GL_ARRAY_BUFFER, this.vboCamera[1])
        glBufferData(GL_ARRAY_BUFFER, colorsBuffer, GL_STATIC_DRAW)
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0)
        glEnableVertexAttribArray(1)
        glBindBuffer(GL_ARRAY_BUFFER, 0)

        glBindVertexArray(0)


        // 頂点情報のすべての情報を持つVAOをバインドする
        glBindVertexArray(this.vaoCamera)
        glEnableVertexAttribArray(0)

        glUseProgram(this.shaderCamera)
        // 頂点情報を描画する
        glDrawArrays(GL_LINES, 0, vertices.size / 3)

        // 全ての選択を外す
        glUseProgram(0)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)
        glDisableVertexAttribArray(0)
        glBindVertexArray(0)
    }
}

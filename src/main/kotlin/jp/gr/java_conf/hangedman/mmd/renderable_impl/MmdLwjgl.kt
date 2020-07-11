package jp.gr.java_conf.hangedman.mmd.renderable_impl

import jp.gr.java_conf.hangedman.lwjgl.ModelViewProjection.updateMVP
import jp.gr.java_conf.hangedman.lwjgl.ShaderHandler.makeShader
import jp.gr.java_conf.hangedman.lwjgl.TextureHandler.initTextures
import jp.gr.java_conf.hangedman.lwjgl.orbitBy
import jp.gr.java_conf.hangedman.mmd.MmdLwjglConstants.height
import jp.gr.java_conf.hangedman.mmd.MmdLwjglConstants.width
import jp.gr.java_conf.hangedman.mmd.VboIndex
import jp.gr.java_conf.hangedman.mmd.mesh_if.Mesh
import jp.gr.java_conf.hangedman.mmd.renderable_if.RenderableBase
import jp.gr.java_conf.hangedman.mmd.shader.ModelShader.modelFragmentSource
import jp.gr.java_conf.hangedman.mmd.shader.ModelShader.modelVertexSource
import org.joml.Vector3f
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL33.*
import org.lwjgl.opengl.GLUtil
import uk.org.lidalia.sysoutslf4j.context.SysOutOverSLF4J

class MmdLwjgl(override val windowId: Long) : RenderableBase(windowId) {

    // VAO, VBO, VBOI
    private var vao: Int = 0
    private var vbo: IntArray = IntArray(VboIndex.values().size)
    private var vboi: Int = 0
    private var indicesCount: Int = 0

    // VAO, VBO, VBOIの読み込み
    private fun load(mesh: Mesh?) {

        requireNotNull(mesh)
        val verticesBuffer = mesh.verticesBuffer()             // 頂点
        val alphaBuffer = mesh.alphaBuffer()                   // 物体色透過率
        val diffuseColorsBuffer = mesh.diffuseColorsBuffer()   // 物体色
        val ambientColorsBuffer = mesh.ambientColorsBuffer()   // 環境色
        val specularColorsBuffer = mesh.specularColorsBuffer() // 光沢色
        val normalsBuffer = mesh.normalsBuffer()               // 法線
        val shininessBuffer = mesh.shininessBuffer()           // 光沢度
        val edgeFlagBuffer = mesh.edgeFlagBuffer()             // エッジの有無

        // カメラの視点のためPMDモデルの中心を計算する(0, Ymax + Ymin / 2, 0)
        val modelYMax = mesh.getModelYMax()
        val modelYMin = mesh.getModelYMin()
        modelCenter = Vector3f(0f, (modelYMax + modelYMin) / 2, 0f)

        val (indicesCount, indicesBuffer) = mesh.faceVertPair()  // 面頂点
        this.indicesCount = indicesCount

        // モデルに設定されているテクスチャを読み取る
        //initTextures(mesh.getTexturePaths())

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
    override fun initialize(mesh: Mesh?): MmdLwjgl {
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
        load(mesh)

        makeShader(modelVertexSource, modelFragmentSource).let { (vertShaderObj, fragShaderObj, shader) ->
            this.vertShaderObj = vertShaderObj
            this.fragShaderObj = fragShaderObj
            this.shader = shader
        }

        return this
    }

    override fun updatePos(windowId: Long) {
        // glUniform系の関数はglUseProgramを実行している間に呼び出す
        val thisTime = System.nanoTime()
        val delta = (thisTime - lastTime) / 1E9f
        lastTime = thisTime

        // Process rotation
        rotation[rotationY] += cameraRotate[rotationY] * delta
        rotation[rotationX] += cameraRotate[rotationX] * delta

        // Setup both camera's projection matrices
        projMatrix.setPerspective(Math.toRadians(40.0).toFloat(), (width/height).toFloat(), 0.1f, 100.0f)
        viewMatrix.orbitBy(modelCenter, rotation[rotationY], rotation[rotationX], focalLength)
        updateMVP(shader, modelMatrix, viewMatrix, projMatrix)

        // 照明の座標
        val uLightPosition = glGetUniformLocation(shader, "uLightPosition")
        glUniform3f(uLightPosition, 20f, 20f, -20f)
        // エッジの太さ, 色
        val uEdgeSize = glGetUniformLocation(shader, "uEdgeSize")
        glUniform1f(uEdgeSize, 0.1f)
        val uEdgeColor = glGetUniformLocation(shader, "uEdgeColor")
        glUniform3f(uEdgeColor, 0f, 0f, 0f)
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

    override fun keyCallback(windowId: Long, key: Int, scancode: Int, action: Int, mods: Int) {
        if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
            glfwSetWindowShouldClose(windowId, true)
    }
}

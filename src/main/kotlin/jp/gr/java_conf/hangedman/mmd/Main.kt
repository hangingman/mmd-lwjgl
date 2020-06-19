package jp.gr.java_conf.hangedman.mmd

import jp.gr.java_conf.hangedman.lwjgl.Matrix4f
import jp.gr.java_conf.hangedman.lwjgl.createProjectionMatrix
import jp.gr.java_conf.hangedman.mmd.Main.Companion.cleanup
import jp.gr.java_conf.hangedman.mmd.Main.Companion.enter
import jp.gr.java_conf.hangedman.mmd.Main.Companion.render
import jp.gr.java_conf.hangedman.mmd.Main.Companion.update
import jp.gr.java_conf.hangedman.mmd.MmdLwjglConstants.fragmentSource
import jp.gr.java_conf.hangedman.mmd.MmdLwjglConstants.height
import jp.gr.java_conf.hangedman.mmd.MmdLwjglConstants.title
import jp.gr.java_conf.hangedman.mmd.MmdLwjglConstants.vertexSource
import jp.gr.java_conf.hangedman.mmd.MmdLwjglConstants.width
import jp.gr.java_conf.hangedman.mmd.pmd.PmdStruct
import org.lwjgl.BufferUtils
import org.lwjgl.Version
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWVidMode
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL33.*
import org.lwjgl.opengl.GLUtil
import org.lwjgl.system.MemoryUtil.NULL
import org.slf4j.LoggerFactory
import uk.org.lidalia.sysoutslf4j.context.SysOutOverSLF4J


fun main(args: Array<String>) {
    println("Hello, LWJGL - ${Version.getVersion()} !")
    println("       OpenGL - ${GL_VERSION} !")
    println("       GLFW - ${glfwGetVersionString()} !")

    val pmdStruct = PmdLoader.loadPmdFile("HatsuneMiku.pmd")
    val window = enter(pmdStruct)

    while (!glfwWindowShouldClose(window)) {
        update()
        render()
        glfwSwapBuffers(window)
        glfwPollEvents()
    }
    // GLFWの終了処理
    cleanup()
    glfwTerminate()
}

class Main {

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
        private var vao: Int = 0
        private var vbo: IntArray = intArrayOf(0, 0, 0) // 頂点, 色, 法線
        private var vboi: Int = 0
        private var indicesCount: Int = 0

        private var shader: Int? = null
        private var vertShaderObj: Int? = null
        private var fragShaderObj: Int? = null
        private var uniModel: Int? = null
        private var modelMatrix = Matrix4f()

        // テスト
        fun createVertex(pmdStruct: PmdStruct) {
            // Vertices, the order is not important.

            /**
            val vertices = floatArrayOf(
            -0.5f, 0.5f, 0f,    // Left top         ID: 0
            -0.5f, -0.5f, 0f,   // Left bottom      ID: 1
            0.5f, -0.5f, 0f,    // Right bottom     ID: 2
            0.5f, 0.5f, 0f      // Right left       ID: 3
            ) */

            // 頂点リスト
            val vertices = pmdStruct.vertex!!
                    .map { v -> v.pos }
                    .flatMap { fArray ->
                        mutableListOf<Float>().also {
                            // TODO: windowの拡大縮小ができるまでは 22 で割って小さくしている
                            it.addAll(fArray.asList().map { p -> (p / 18) }.toList())
                        }
                    }.toFloatArray()

            // 面頂点リストの適用合計値に対してどの材質リストを使うかを保持する連想配列
            val materialRanged = pmdStruct.material!!
                    .mapIndexed { i, m ->
                        val materialRanged = pmdStruct.material!!.filterIndexed { index, material ->
                            index <= i
                        }.map { it.faceVertCount }.sum()
                        materialRanged to m
                    }

            // 頂点に対してどの材質リストを使うかを保持する連想配列
            val vertexMaterialMap = pmdStruct.vertex!!
                    .mapIndexed { index, _ ->
                        val faceVertIndex = pmdStruct.faceVertIndex!!.indexOfFirst { faceVert -> faceVert == index.toShort() }
                        val material = materialRanged.find { m -> m.first >= faceVertIndex }
                        index to material!!.second
                    }

            // 頂点に対する色を設定する
            val colors = pmdStruct.vertex!!
                    .mapIndexed { i, _ ->
                        val floatList = mutableListOf<Float>()
                        val m = vertexMaterialMap.find { (range, _) -> i <= range }!!.second
                        floatList.addAll(m.diffuseColor.toList())
                        floatList.add(m.alpha)
                        floatList
                    }.flatten().toFloatArray()

            val normals = pmdStruct.vertex!!
                    .map { v -> v.normalVec }
                    .flatMap { fArray ->
                        mutableListOf<Float>().also {
                            it.addAll(fArray.asList())
                        }
                    }.toFloatArray()

            // 頂点
            val verticesBuffer = BufferUtils.createFloatBuffer(vertices.size)
            verticesBuffer.put(vertices)
            verticesBuffer.flip()
            println("vertices size " + vertices.size)

            // 色
            val colorsBuffer = BufferUtils.createFloatBuffer(colors.size)
            colorsBuffer.put(colors)
            colorsBuffer.flip()
            println("colors size " + colors.size / 4)

            // 法線
            val normalsBuffer = BufferUtils.createFloatBuffer(normals.size)
            normalsBuffer.put(normals)
            normalsBuffer.flip()

            // OpenGL expects to draw vertices in counter clockwise order by default
            /**
            val indices = shortArrayOf(
            // Left bottom triangle
            0, 1, 2,
            // Right top triangle
            2, 3, 0
            )
            indicesCount = indices.size */

            val indices = pmdStruct.faceVertIndex
            indicesCount = pmdStruct.faceVertCount

            val indicesBuffer = BufferUtils.createShortBuffer(indicesCount)
            indicesBuffer.put(indices)
            indicesBuffer.flip()

            // Create a new Vertex Array Object in memory and select it (bind)
            // A VAO can have up to 16 attributes (VBO's) assigned to it by default
            this.vao = glGenVertexArrays()
            glBindVertexArray(this.vao)

            // Create a new Vertex Buffer Object in memory and select it (bind)
            // A VBO is a collection of Vectors which in this case resemble the location of each vertex.
            this.vbo[0] = glGenBuffers()
            glBindBuffer(GL_ARRAY_BUFFER, this.vbo[0])
            glBufferData(GL_ARRAY_BUFFER, verticesBuffer, GL_STATIC_DRAW)
            // Put the VBO in the attributes list at index 0
            glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0)
            // Deselect (bind to 0) the VBO
            glBindBuffer(GL_ARRAY_BUFFER, 0)

            this.vbo[1] = glGenBuffers()
            glBindBuffer(GL_ARRAY_BUFFER, this.vbo[1])
            glBufferData(GL_ARRAY_BUFFER, colorsBuffer, GL_STATIC_DRAW)
            // Put the VBO in the attributes list at index 1
            glVertexAttribPointer(1, 4, GL_FLOAT, false, 0, 0)
            glEnableVertexAttribArray(1)
            // Deselect (bind to 0) the VBO
            glBindBuffer(GL_ARRAY_BUFFER, 0)

            this.vbo[2] = glGenBuffers()
            glBindBuffer(GL_ARRAY_BUFFER, this.vbo[2])
            glBufferData(GL_ARRAY_BUFFER, normalsBuffer, GL_STATIC_DRAW)
            // Put the VBO in the attributes list at index 2
            glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0)
            glEnableVertexAttribArray(2)
            // Deselect (bind to 0) the VBO
            glBindBuffer(GL_ARRAY_BUFFER, 0)

            // Deselect (bind to 0) the VAO
            glBindVertexArray(0)

            // Create a new VBO for the indices and select it (bind)
            this.vboi = glGenBuffers()
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, this.vboi)
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL_STATIC_DRAW)
            // Deselect (bind to 0) the VBO
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)
        }

        // 初期化してシェーダーを返す
        fun enter(pmdStruct: PmdStruct): Long {
            // GLFW初期化
            glfwInit()
            glfwDefaultWindowHints()
            glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE)
            glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE)
            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3)
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3)
            glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)
            glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE)

            // ウィンドウ生成
            val window: Long = glfwCreateWindow(width, height, title, 0, 0)
            if (window == NULL) {
                // 生成に失敗
                glfwTerminate()
            }

            glfwSetWindowAspectRatio(window, 1, 1)
            val videoMode: GLFWVidMode = glfwGetVideoMode(glfwGetPrimaryMonitor())
                    ?: throw IllegalStateException("Failed to get video mode...")
            glfwSetWindowPos(window,
                    (videoMode.width() - width) / 2,
                    (videoMode.height() - height) / 2
            )

            // コンテキストの作成
            glfwMakeContextCurrent(window)
            glfwSwapInterval(1)
            GL.createCapabilities()

            SysOutOverSLF4J.sendSystemOutAndErrToSLF4J()
            GLUtil.setupDebugMessageCallback(System.out)

            // 背景色変更
            glClearColor(0.4f, 0.6f, 0.9f, 0f)
            glViewport(0, 0, width, height)

            // Zバッファ
            glEnable(GL_DEPTH_TEST)  // デプステストを有効にする
            glDepthFunc(GL_LESS)     // 前のものよりもカメラに近ければ、フラグメントを受け入れる

            // ここから描画情報の読み込み
            createVertex(pmdStruct)
            makeShader(vertexSource, fragmentSource)

            val floatSize = 4
            // 頂点シェーダーのinパラメータ"position"と対応
            val posAttrib = glGetAttribLocation(this.shader!!, "position")
            glEnableVertexAttribArray(posAttrib)
            glVertexAttribPointer(posAttrib, 3, GL_FLOAT, false, 6 * floatSize, 0)

            // 頂点シェーダーのグローバルGLSL変数"model"の位置を保持しておく
            // 毎フレーム設定するので
            this.uniModel = glGetUniformLocation(this.shader!!, "model")

            // 頂点シェーダーのグローバルGLSL変数"view"に設定
            val viewMatrix = Matrix4f()
            viewMatrix.identity(viewMatrix)

            val uniView = glGetUniformLocation(this.shader!!, "view")
            glUniformMatrix4fv(uniView, false, viewMatrix.value)

            // 頂点シェーダーのグローバルGLSL変数"projection"に設定
            val projectionMatrix = createProjectionMatrix()
            val uniProjection = glGetUniformLocation(this.shader!!, "projection")
            glUniformMatrix4fv(uniProjection, false, projectionMatrix.value)

            val lightFloatBuffer = BufferUtils.createFloatBuffer(4)
            lightFloatBuffer.put(floatArrayOf(0f, 1f, 0f, 0f))
            lightFloatBuffer.flip()
            val uniLightDir = glGetUniformLocation(this.shader!!, "wLightDir")
            glUniformMatrix3fv(uniLightDir, false, lightFloatBuffer)

            return window
        }

        private fun makeShader(vertexSource: String, fragmentSource: String) {
            // シェーダーオブジェクト作成
            this.vertShaderObj = glCreateShader(GL_VERTEX_SHADER)
            this.fragShaderObj = glCreateShader(GL_FRAGMENT_SHADER)

            // シェーダーのソースプログラムの読み込み
            readShaderSource(this.vertShaderObj!!, vertexSource)
            readShaderSource(this.fragShaderObj!!, fragmentSource)

            // バーテックスシェーダーのソースプログラムのコンパイル
            glCompileShader(this.vertShaderObj!!)
            if (glGetShaderi(this.vertShaderObj!!, GL_COMPILE_STATUS) != GL_TRUE) {
                throw IllegalStateException("Failed to compile vertex shader...")
            }
            // フラグメントシェーダーのソースプログラムのコンパイル
            glCompileShader(this.fragShaderObj!!)
            if (glGetShaderi(this.fragShaderObj!!, GL_COMPILE_STATUS) != GL_TRUE) {
                throw IllegalStateException("Failed to compile fragment shader...")
            }

            // プログラムオブジェクトの作成
            this.shader = glCreateProgram()
            // シェーダーオブジェクトのシェーダープログラムへの登録
            glAttachShader(this.shader!!, this.vertShaderObj!!)
            glAttachShader(this.shader!!, this.fragShaderObj!!)
            // シェーダーにデータの位置をバインド
            glBindFragDataLocation(this.shader!!, 0, "fragColor")

            // シェーダープログラムのリンクと実行
            glLinkProgram(this.shader!!)
            glUseProgram(this.shader!!)
        }

        private fun readShaderSource(shaderObj: Int, shaderSrc: String) {
            //logger.debug("shader source: \n$shaderSrc")
            glShaderSource(shaderObj, shaderSrc)
        }

        fun update() {
            // 1秒で1回転
            val angle = 360 * (glfwGetTime() % 1).toFloat()
            Matrix4f.rotateY(this.modelMatrix, angle);
        }

        fun render() {
            glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

            // Bind to the VAO that has all the information about the vertices
            glBindVertexArray(this.vao)
            glEnableVertexAttribArray(0)

            // Bind to the index VBO that has all the information about the order of the vertices
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, this.vboi)

            glUseProgram(this.shader!!)
            // updateメソッドで求めた回転行列をグローバルGLSL変数に設定
            glUniformMatrix4fv(this.uniModel!!, false, this.modelMatrix.value)

            // Draw the vertices
            glDrawElements(GL_TRIANGLES, indicesCount, GL_UNSIGNED_SHORT, 0)

            // Put everything back to default (deselect)
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)
            glDisableVertexAttribArray(0)
            glBindVertexArray(0)
        }

        fun cleanup() {
            glDeleteVertexArrays(this.vao)
            glDeleteBuffers(this.vbo[0])
            glDeleteBuffers(this.vbo[1])
            glDeleteBuffers(this.vbo[2])
            glDeleteBuffers(this.vboi)

            glDeleteShader(this.vertShaderObj!!)
            glDeleteShader(this.fragShaderObj!!)
            glDeleteProgram(this.shader!!)
        }
    }
}


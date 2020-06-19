package jp.gr.java_conf.hangedman.mmd

import jp.gr.java_conf.hangedman.lwjgl.createProjectionMatrix
import jp.gr.java_conf.hangedman.lwjgl.value
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
import org.joml.Math.cos
import org.joml.Math.sin
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.BufferUtils
import org.lwjgl.Version
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWScrollCallback
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
    val windowId = enter(pmdStruct)

    while (!glfwWindowShouldClose(windowId)) {
        update(windowId)
        render()
        glfwSwapBuffers(windowId)
        glfwPollEvents()
    }
    // GLFWの終了処理
    cleanup()
    glfwTerminate()
}

class Main {

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)

        // VAO, VBO, VBOI
        private var vao: Int = 0
        private var vbo: IntArray = intArrayOf(0, 0, 0) // 頂点, 色, 法線
        private var vboi: Int = 0
        private var indicesCount: Int = 0

        // Shader
        private var shader: Int? = null
        private var vertShaderObj: Int? = null
        private var fragShaderObj: Int? = null

        // モデルの位置など
        private var position = Vector3f(0f, 0f, 5f)
        // 水平角、-Z方向
        private var horizontalAngle = 3.14f

        // 鉛直角、0、水平線を眺めている
        private var verticalAngle = 0.0f

        // 初期視野
        private var initialFoV = 45.0f
        private var foV = initialFoV
        private var mouseWheelVelocity = 0f
        private var speed = 3.0f // 3 units / second
        private var mouseSpeed = 0.005f
        private var lastTime = glfwGetTime()

        private var direction = Vector3f()
        private var up = Vector3f()

        private var uniModel: Int? = null
        private var modelMatrix = Matrix4f()

        // VAO, VBO, VBOIの読み込み
        fun createVertex(pmdStruct: PmdStruct) {

            // 頂点リスト
            val vertices = pmdStruct.vertex!!
                    .map { v -> v.pos }
                    .flatMap { fArray ->
                        mutableListOf<Float>().also {
                            it.addAll(fArray.asList())
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
            val windowId: Long = glfwCreateWindow(width, height, title, 0, 0)
            if (windowId == NULL) {
                // 生成に失敗
                glfwTerminate()
            }

            glfwSetWindowAspectRatio(windowId, 1, 1)
            val videoMode: GLFWVidMode = glfwGetVideoMode(glfwGetPrimaryMonitor())
                    ?: throw IllegalStateException("Failed to get video mode...")
            glfwSetWindowPos(windowId,
                    (videoMode.width() - width) / 2,
                    (videoMode.height() - height) / 2
            )

            // コンテキストの作成
            glfwMakeContextCurrent(windowId)
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

            glfwSetScrollCallback(windowId, object : GLFWScrollCallback() {
                override fun invoke(windowId: Long, dx: Double, dy: Double) {
                    mouseWheelVelocity = dy.toFloat()
                    foV -= (5 * mouseWheelVelocity)
                    System.out.println("dy: $dy, foV: $foV")
                }
            })

            // ここから描画情報の読み込み
            createVertex(pmdStruct)
            makeShader(vertexSource, fragmentSource)

            val floatSize = 4
            // 頂点シェーダーのinパラメータ"position"と対応
            val posAttrib = glGetAttribLocation(this.shader!!, "position")
            glEnableVertexAttribArray(posAttrib)
            glVertexAttribPointer(posAttrib, 3, GL_FLOAT, false, 6 * floatSize, 0)

            return windowId
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

        fun update(windowId: Long) {
            // キーボードとマウスのインプットからMVP行列を計算する
            computeMatricesFromInputs(windowId)

            // 頂点シェーダーのグローバルGLSL変数"model"の位置を保持しておく
            // 毎フレーム設定するので
            this.uniModel = glGetUniformLocation(this.shader!!, "model")

            // 頂点シェーダーのグローバルGLSL変数"projection"に設定
            val projectionMatrix = Matrix4f().createProjectionMatrix(foV)
            val uniProjection = glGetUniformLocation(this.shader!!, "projection")
            glUniformMatrix4fv(uniProjection, false, projectionMatrix.value())

            // 頂点シェーダーのグローバルGLSL変数"view"に設定
            // TODO: このへんまだうまく動かない
            val viewMatrix = Matrix4f().identity()
            //val viewMatrix = Matrix4f().lookAt(position, position.add(direction), up)

            val uniView = glGetUniformLocation(this.shader!!, "view")
            glUniformMatrix4fv(uniView, false, viewMatrix.value())

            val lightFloatBuffer = BufferUtils.createFloatBuffer(4)
            lightFloatBuffer.put(floatArrayOf(0f, 1f, 0f, 0f))
            lightFloatBuffer.flip()
            val uniLightDir = glGetUniformLocation(this.shader!!, "wLightDir")
            glUniformMatrix3fv(uniLightDir, false, lightFloatBuffer)

            // 1秒で1回転?
            //val angle = 360 * (glfwGetTime() % 1000).toFloat()
            //this.modelMatrix = this.modelMatrix.rotateLocalY(angle)
        }

        private fun computeMatricesFromInputs(windowId: Long) {

            val currentTime = glfwGetTime()
            val deltaTime = (currentTime - lastTime).toFloat()
            lastTime = currentTime

            val windowXBuffer = BufferUtils.createIntBuffer(1)
            val windowYBuffer = BufferUtils.createIntBuffer(1)
            glfwGetWindowSize(windowId, windowXBuffer, windowYBuffer)
            val windowXHalfPos = (windowXBuffer.get(0) / 2).toDouble()
            val windowYHalfPos = (windowYBuffer.get(0) / 2).toDouble()

            // マウスの位置を取得
            val xBuffer = BufferUtils.createDoubleBuffer(1)
            val yBuffer = BufferUtils.createDoubleBuffer(1)
            glfwGetCursorPos(windowId, xBuffer, yBuffer)
            val xpos = xBuffer.get(0)
            val ypos = yBuffer.get(0)

            horizontalAngle += mouseSpeed * deltaTime * (windowXHalfPos - xpos).toFloat()
            verticalAngle += mouseSpeed * deltaTime * (windowYHalfPos - ypos).toFloat()

            this.direction = Vector3f().apply {
                this.x = cos(verticalAngle) * sin(horizontalAngle)
                this.y = sin(verticalAngle)
                this.z = cos(verticalAngle) * cos(horizontalAngle)
            }
            val right = Vector3f().apply {
                this.x = sin(horizontalAngle - 3.14f / 2.0f)
                this.y = 0f
                this.z = cos(horizontalAngle - 3.14f / 2.0f)
            }
            this.up = Vector3f(direction).cross(right)

            // 前へ動きます。
            if (glfwGetKey(windowId, GLFW_KEY_UP) == GLFW_PRESS) {
                position.add(direction.mul(deltaTime * speed))
                logger.info("position ${position.toString()}")
            }
            // 後ろへ動きます。
            if (glfwGetKey(windowId, GLFW_KEY_DOWN) == GLFW_PRESS) {
                position.sub(direction.mul(deltaTime * speed))
                logger.info("position ${position.toString()}")
            }
            // 前を向いたまま、右へ平行移動します。
            if (glfwGetKey(windowId, GLFW_KEY_RIGHT) == GLFW_PRESS) {
                position.add(right.mul(deltaTime * speed))
                logger.info("position ${position.toString()}")
            }
            // 前を向いたまま、左へ平行移動します。
            if (glfwGetKey(windowId, GLFW_KEY_LEFT) == GLFW_PRESS) {
                position.sub(right.mul(deltaTime * speed))
                logger.info("position ${position.toString()}")
            }
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
            glUniformMatrix4fv(this.uniModel!!, false, this.modelMatrix.value())

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


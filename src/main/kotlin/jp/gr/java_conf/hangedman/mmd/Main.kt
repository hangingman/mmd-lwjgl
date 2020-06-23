package jp.gr.java_conf.hangedman.mmd

import jp.gr.java_conf.hangedman.lwjgl.ShaderHandler.makeShader
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
import jp.gr.java_conf.hangedman.mmd.pmd.*
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
        glfwPollEvents()
        glViewport(0, 0, width, height)
        update(windowId)
        render()
        glfwSwapBuffers(windowId)
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
        private var vbo: IntArray = IntArray(VboIndex.values().size)
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
        private var fov = initialFoV
        private var rotation = 0f
        private var mouseWheelVelocity = 0f
        private var speed = 10.0f // 3 units / second
        private var mouseSpeed = 0.005f
        private var lastTime = glfwGetTime()

        private var direction = Vector3f()
        private var up = Vector3f()

        private var uniModel: Int? = null
        private var modelMatrix = Matrix4f()

        // VAO, VBO, VBOIの読み込み
        fun loadPolygonData(pmdStruct: PmdStruct) {

            val verticesBuffer = pmdStruct.verticesBuffer()            // 頂点
            val alphaBuffer = pmdStruct.alphaBuffer()                  // 物体色透過率
            val diffuseColorsBuffer = pmdStruct.diffuseColorsBuffer()  // 物体色
            val ambientColorsBuffer = pmdStruct.ambientColorsBuffer()  // 環境色
            val normalsBuffer = pmdStruct.normalsBuffer()              // 法線

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
                    VboIndex.NORMAL to normalsBuffer
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
                    if (fov < 10.0f) {
                        fov = 10.0f;
                    } else if (fov > 120.0f) {
                        fov = 120.0f;
                    }
                }
            })
            glfwSetCursorPosCallback(windowId) { _, x, _ ->
                rotation = (x.toFloat() / width - 0.5f) * 2f * Math.PI.toFloat()
            }

            // ここから描画情報の読み込み
            loadPolygonData(pmdStruct)
            makeShader(vertexSource, fragmentSource).let { (vertShaderObj, fragShaderObj, shader) ->
                this.vertShaderObj = vertShaderObj
                this.fragShaderObj = fragShaderObj
                this.shader = shader
            }

            val floatSize = 4
            // 頂点シェーダーのinパラメータ"position"と対応
            val posAttrib = glGetAttribLocation(this.shader!!, "position")
            glEnableVertexAttribArray(posAttrib)
            glVertexAttribPointer(posAttrib, 3, GL_FLOAT, false, 6 * floatSize, 0)

            return windowId
        }

        fun update(windowId: Long) {
            // キーボードとマウスのインプットからMVP行列を計算する
            computeMatricesFromInputs(windowId)

            // Model行列(描画対象のモデルの座標からOpenGLのワールド座標への相対値)
            // 頂点シェーダーのグローバルGLSL変数"model"に設定
            this.uniModel = glGetUniformLocation(this.shader!!, "model")
            // 1秒で1回転?
            //val angle = 360 * (glfwGetTime() % 1).toFloat()
            //val angle = (glfwGetTime() - lastTime).toFloat() * 30
            //this.modelMatrix = this.modelMatrix.rotateLocalY(angle)

            // View行列(OpenGLのワールド座標からカメラの座標への相対値)
            // 頂点シェーダーのグローバルGLSL変数"view"に設定
            val uniView = glGetUniformLocation(this.shader!!, "view")
            val viewMatrix = Matrix4f().setLookAt(
                    position.x, position.y, position.z,  // ワールド空間でのカメラの位置
                    0f, 0f, 0f, // ワールド空間での見たい位置
                    0f, 1f, 0f
            )
            glUniformMatrix4fv(uniView, false, viewMatrix.value())

            // Projection行列(カメラの座標から、映し出される（射影）ものへの相対値)
            // 頂点シェーダーのグローバルGLSL変数"projection"に設定
            val projectionMatrix = Matrix4f().createProjectionMatrix(fov)
            val uniProjection = glGetUniformLocation(this.shader!!, "projection")
            glUniformMatrix4fv(uniProjection, false, projectionMatrix.value())

            val lightPositionBuffer = BufferUtils.createFloatBuffer(3).apply {
                put(floatArrayOf(-5f, 5f, 5f))
                flip()
            }
            val uLightPosition = glGetUniformLocation(this.shader!!, "uLightPosition")
            glUniformMatrix3fv(uLightPosition, false, lightPositionBuffer)
        }

        private fun computeMatricesFromInputs(windowId: Long) {

            val currentTime = glfwGetTime()
            val deltaTime = (currentTime - lastTime).toFloat()
            lastTime = currentTime

            position.set(10f * cos(rotation), 10f, 10f * sin(rotation))

            /**
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
            //glfwSetCursorPos(windowId, windowXHalfPos, windowYHalfPos)

            horizontalAngle += mouseSpeed * deltaTime * (windowXHalfPos - xpos).toFloat()
            verticalAngle += mouseSpeed * deltaTime * (windowYHalfPos - ypos).toFloat()

            //println("横の角度: ${horizontalAngle}, 縦の角度 ${verticalAngle}")

            this.direction = Vector3f().apply {
                x = cos(verticalAngle) * sin(horizontalAngle)
                y = sin(verticalAngle)
                z = cos(verticalAngle) * cos(horizontalAngle)
            }

            val right = Vector3f().apply {
                x = sin(horizontalAngle - 3.14f / 2.0f)
                y = 0f
                z = cos(horizontalAngle - 3.14f / 2.0f)
            }

            this.up = Vector3f(direction).cross(right)

            //println("direction: $direction, up: $up, position: $position")

            // 前へ動きます。
            if (glfwGetKey(windowId, GLFW_KEY_UP) == GLFW_PRESS) {
                position = position.add(direction.mul(deltaTime * speed))
                //logger.info("position ${position.toString()}")
            }
            // 後ろへ動きます。
            if (glfwGetKey(windowId, GLFW_KEY_DOWN) == GLFW_PRESS) {
                position = position.sub(direction.mul(deltaTime * speed))
                //logger.info("position ${position.toString()}")
            }
            // 前を向いたまま、右へ平行移動します。
            if (glfwGetKey(windowId, GLFW_KEY_RIGHT) == GLFW_PRESS) {
                position = position.add(right.mul(deltaTime * speed))
                //logger.info("position ${position.toString()}")
            }
            // 前を向いたまま、左へ平行移動します。
            if (glfwGetKey(windowId, GLFW_KEY_LEFT) == GLFW_PRESS) {
                position = position.sub(right.mul(deltaTime * speed))
                //logger.info("position ${position.toString()}")
            }
            */
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
            VboIndex.values().forEach { index ->
                glDeleteBuffers(this.vbo[index.asInt])
            }
            glDeleteBuffers(this.vboi)

            glDeleteShader(this.vertShaderObj!!)
            glDeleteShader(this.fragShaderObj!!)
            glDeleteProgram(this.shader!!)
        }
    }
}


package jp.gr.java_conf.hangedman.mmd

import jp.gr.java_conf.hangedman.mmd.MmdCljConstants.fragmentSource
import jp.gr.java_conf.hangedman.mmd.MmdCljConstants.height
import jp.gr.java_conf.hangedman.mmd.MmdCljConstants.title
import jp.gr.java_conf.hangedman.mmd.MmdCljConstants.vertexSource
import jp.gr.java_conf.hangedman.mmd.MmdCljConstants.width
import jp.gr.java_conf.hangedman.mmd.Main.Companion.enter
import jp.gr.java_conf.hangedman.mmd.Main.Companion.render
import jp.gr.java_conf.hangedman.mmd.MmdCljConstants.matModel
import jp.gr.java_conf.hangedman.mmd.MmdCljConstants.matProj
import jp.gr.java_conf.hangedman.mmd.MmdCljConstants.matView
import org.lwjgl.BufferUtils
import org.lwjgl.Version
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWVidMode
import org.lwjgl.opengl.*
import org.lwjgl.system.MemoryUtil.NULL
import org.slf4j.LoggerFactory
import uk.org.lidalia.sysoutslf4j.context.SysOutOverSLF4J


fun main(args: Array<String>) {
    println("Hello, LWJGL - ${Version.getVersion()} !")
    println("       OpenGL - ${GL11.GL_VERSION} !")
    println("       GLFW - ${GLFW.glfwGetVersionString()} !")

    val (window, shader, attribVertex) = enter()
    while (!GLFW.glfwWindowShouldClose(window)) {
        render(window, shader)
    }
    // GLFWの終了処理
    GLFW.glfwTerminate()
}

class Main {

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)

        // 初期化してシェーダーを返す
        fun enter(): Triple<Long, Int, Int> {
            // GLFW初期化
            GLFW.glfwInit()
            GLFW.glfwDefaultWindowHints()
            GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_TRUE)
            GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE)
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3)
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 2)
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE)
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE)

            // ウィンドウ生成
            val window: Long = GLFW.glfwCreateWindow(width, height, title, 0, 0)
            if (window == NULL) {
                // 生成に失敗
                GLFW.glfwTerminate()
            }

            GLFW.glfwSetWindowAspectRatio(window, 1, 1)
            val videoMode: GLFWVidMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor())
                    ?: throw IllegalStateException("Failed to get video mode...")
            GLFW.glfwSetWindowPos(window,
                    (videoMode.width() - width) / 2,
                    (videoMode.height() - height) / 2
            )

            // コンテキストの作成
            GLFW.glfwMakeContextCurrent(window)
            GLFW.glfwSwapInterval(1)
            GL.createCapabilities()

            SysOutOverSLF4J.sendSystemOutAndErrToSLF4J()
            GLUtil.setupDebugMessageCallback(System.out)

            val shader = makeShader(vertexSource, fragmentSource)
            val attribVertex = GL20.glGetAttribLocation(shader, "vertex")
            val fb = BufferUtils.createFloatBuffer(16)

            GL20.glUniformMatrix4fv(GL20.glGetUniformLocation(shader,"projection"), false, matProj.get(fb))
            GL20.glUniformMatrix4fv(GL20.glGetUniformLocation(shader,"view"), false, matView.get(fb))
            GL20.glUniformMatrix4fv(GL20.glGetUniformLocation(shader,"model"), false, matModel.get(fb))
            GL20.glUseProgram(0)
            return Triple(window, shader, attribVertex)
        }

        fun render(window: Long, shader: Int) {
            // ダブルバッファのスワップ
            GLFW.glfwSwapBuffers(window)
            GLFW.glfwPollEvents()
        }

        private fun makeShader(vertexSource: String, fragmentSource: String): Int {
            // シェーダーオブジェクト作成
            val vertShaderObj = GL20.glCreateShader(GL20.GL_VERTEX_SHADER)
            val fragShaderObj = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER)

            // シェーダーのソースプログラムの読み込み
            readShaderSource(vertShaderObj, vertexSource)
            readShaderSource(fragShaderObj, fragmentSource)

            // バーテックスシェーダーのソースプログラムのコンパイル
            GL20.glCompileShader(vertShaderObj)
            // フラグメントシェーダーのソースプログラムのコンパイル
            GL20.glCompileShader(fragShaderObj)

            // プログラムオブジェクトの作成
            val shader = GL20.glCreateProgram()
            // シェーダーオブジェクトのシェーダープログラムへの登録
            GL20.glAttachShader(shader, vertShaderObj)
            GL20.glAttachShader(shader, fragShaderObj)
            // シェーダーオブジェクトの削除
            GL20.glDeleteShader(vertShaderObj)
            GL20.glDeleteShader(fragShaderObj)
            // シェーダーにデータの位置をバインド
            GL30.glBindFragDataLocation(shader, 0, "fragColor")
            // シェーダープログラムのリンク
            GL20.glLinkProgram(shader)
            GL20.glUseProgram(shader)

            return shader
        }

        private fun readShaderSource(shaderObj: Int, shaderSrc: String) {
            logger.debug("shader source: \n$shaderSrc")
            GL20.glShaderSource(shaderObj, shaderSrc)
        }
    }
}


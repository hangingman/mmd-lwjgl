package jp.gr.java_conf.hangedman.mmd

import jp.gr.java_conf.hangedman.mmd.Main.Companion.enter
import jp.gr.java_conf.hangedman.mmd.Main.Companion.render
import org.lwjgl.Version
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWVidMode
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GLUtil
import org.lwjgl.system.MemoryUtil.NULL
import org.slf4j.LoggerFactory
import uk.org.lidalia.sysoutslf4j.context.SysOutOverSLF4J


fun main(args: Array<String>) {
    println("Hello, LWJGL - ${Version.getVersion()} !")
    println("       OpenGL - ${GL11.GL_VERSION} !")
    println("       GLFW - ${GLFW.glfwGetVersionString()} !")

    val window = enter()
    while (! GLFW.glfwWindowShouldClose(window)) {
        render(window)
    }
    // GLFWの終了処理
    GLFW.glfwTerminate()
}

class Main {

    companion object {
        private val logger = LoggerFactory.getLogger(this.javaClass)
        const val width = 640
        const val height = 480
        const val title = "Load PMD file testing"

        // 初期化してシェーダーを返す
        fun enter(): Long {
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

            return window
        }

        fun render(window: Long) {
            // ダブルバッファのスワップ
            GLFW.glfwSwapBuffers(window)
            GLFW.glfwPollEvents()
        }
    }
}


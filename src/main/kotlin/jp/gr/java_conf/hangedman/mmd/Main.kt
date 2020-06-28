package jp.gr.java_conf.hangedman.mmd

import jp.gr.java_conf.hangedman.mmd.MmdLwjglConstants.height
import jp.gr.java_conf.hangedman.mmd.MmdLwjglConstants.title
import jp.gr.java_conf.hangedman.mmd.MmdLwjglConstants.width
import jp.gr.java_conf.hangedman.mmd.MmdLwjglOptionParser.parse
import jp.gr.java_conf.hangedman.mmd.renderable_impl.MmdLwjgl
import jp.gr.java_conf.hangedman.mmd.renderable_impl.XyzAxis
import org.lwjgl.Version
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWVidMode
import org.lwjgl.opengl.GL33.GL_VERSION
import org.lwjgl.system.Configuration
import org.lwjgl.system.MemoryUtil.NULL


fun main(args: Array<String>) {

    val cmd = parse(args)
    System.setProperty("org.lwjgl.system.stackSize", "1024")

    println("Hello, LWJGL - ${Version.getVersion()} !")
    println("         stack size - ${Configuration.STACK_SIZE.get()}kb !")
    println("       OpenGL - ${GL_VERSION} !")
    println("       GLFW - ${glfwGetVersionString()} !")

    // windowハンドラを得る
    val windowId = initWindow()

    val modelPath = if (cmd.hasOption("m")) cmd.getOptionValue("m") else "HatsuneMiku.pmd"
    val pmdStruct = PmdLoader.loadPmdFile(modelPath)

    // 複数のメッシュを描画する
    val renderables = listOf(
            MmdLwjgl(windowId).initialize(pmdStruct),
            XyzAxis(windowId).initialize()
    )

    while (!glfwWindowShouldClose(windowId)) {
        glfwPollEvents()
        renderables.forEach { it.render() }
        glfwSwapBuffers(windowId)
    }
    // GLFWの終了処理
    renderables.forEach { it.cleanup() }
    glfwTerminate()
}

fun initWindow(): Long {
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
    return windowId
}


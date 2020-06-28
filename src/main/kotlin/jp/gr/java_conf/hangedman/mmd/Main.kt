package jp.gr.java_conf.hangedman.mmd

import jp.gr.java_conf.hangedman.mmd.MmdLwjgl.Companion.cleanup
import jp.gr.java_conf.hangedman.mmd.MmdLwjgl.Companion.initialize
import jp.gr.java_conf.hangedman.mmd.MmdLwjgl.Companion.render
import jp.gr.java_conf.hangedman.mmd.MmdLwjgl.Companion.update
import jp.gr.java_conf.hangedman.mmd.MmdLwjglOptionParser.parse
import org.lwjgl.Version
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL33.GL_VERSION
import org.lwjgl.system.Configuration


fun main(args: Array<String>) {

    val cmd = parse(args)
    System.setProperty("org.lwjgl.system.stackSize", "1024")

    println("Hello, LWJGL - ${Version.getVersion()} !")
    println("         stack size - ${Configuration.STACK_SIZE.get()}kb !")
    println("       OpenGL - ${GL_VERSION} !")
    println("       GLFW - ${glfwGetVersionString()} !")

    val modelPath = if (cmd.hasOption("m")) cmd.getOptionValue("m") else "HatsuneMiku.pmd"
    val pmdStruct = PmdLoader.loadPmdFile(modelPath)
    val windowId = initialize(pmdStruct)

    while (!glfwWindowShouldClose(windowId)) {
        glfwPollEvents()
        update(windowId)
        render()
        glfwSwapBuffers(windowId)
    }
    // GLFWの終了処理
    cleanup()
    glfwTerminate()
}


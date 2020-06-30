package jp.gr.java_conf.hangedman.mmd.renderable_if

import jp.gr.java_conf.hangedman.mmd.pmd.PmdStruct

interface Renderable {
    val windowId: Long

    fun initialize(pmdStruct: PmdStruct? = null): Renderable
    fun render()
    fun updatePos(windowId: Long)
    fun cleanup()

    fun cursorPosCallback(windowId: Long, xpos: Double, ypos: Double)
    fun keyCallback(windowId: Long, key: Int, scancode: Int, action: Int, mods: Int)
}
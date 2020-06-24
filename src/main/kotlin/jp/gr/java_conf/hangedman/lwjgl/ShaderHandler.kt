package jp.gr.java_conf.hangedman.lwjgl

import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL30.glBindFragDataLocation

object ShaderHandler {
    fun makeShader(vertexSource: String, fragmentSource: String): Triple<Int, Int, Int> {
        // シェーダーオブジェクト作成
        val vertexShader = glCreateShader(GL_VERTEX_SHADER)
        val fragmentShader = glCreateShader(GL_FRAGMENT_SHADER)

        // シェーダーのソースプログラムの読み込み
        readShaderSource(vertexShader, vertexSource)
        readShaderSource(fragmentShader, fragmentSource)

        // バーテックスシェーダーのソースプログラムのコンパイル
        glCompileShader(vertexShader)
        if (glGetShaderi(vertexShader, GL_COMPILE_STATUS) != GL_TRUE) {
            val message = glGetShaderInfoLog(vertexShader, glGetShaderi(vertexShader, GL_INFO_LOG_LENGTH))
            throw IllegalStateException("vertex shader is invalid\n$message")
        }
        // フラグメントシェーダーのソースプログラムのコンパイル
        glCompileShader(fragmentShader)
        if (glGetShaderi(fragmentShader, GL_COMPILE_STATUS) != GL_TRUE) {
            val message = glGetShaderInfoLog(fragmentShader, glGetShaderi(fragmentShader, GL_INFO_LOG_LENGTH))
            throw IllegalStateException("fragment shader is invalid\n$message")
        }

        // プログラムオブジェクトの作成
        val shader = glCreateProgram()
        // シェーダーオブジェクトのシェーダープログラムへの登録
        glAttachShader(shader, vertexShader)
        glAttachShader(shader, fragmentShader)
        // シェーダーにデータの位置をバインド
        glBindFragDataLocation(shader, 0, "fragColor")
        // シェーダープログラムのリンクと実行
        glLinkProgram(shader)
        if (glGetProgrami(shader, GL_LINK_STATUS) != GL_TRUE) {
            val message = glGetProgramInfoLog(shader, glGetProgrami(shader, GL_INFO_LOG_LENGTH))
            throw IllegalStateException("linking shader is invalid\n$message")
        }
        glUseProgram(shader)

        return Triple(vertexShader, fragmentShader, shader)
    }

    private fun readShaderSource(shaderObj: Int, shaderSrc: String) {
        //logger.debug("shader source: \n$shaderSrc")
        glShaderSource(shaderObj, shaderSrc)
    }
}
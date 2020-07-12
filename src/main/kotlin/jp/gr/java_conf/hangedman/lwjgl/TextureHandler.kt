package jp.gr.java_conf.hangedman.lwjgl

import org.lwjgl.BufferUtils
import org.lwjgl.opengl.ARBSamplerObjects.glGenSamplers
import org.lwjgl.opengl.ARBSamplerObjects.glSamplerParameteri
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL14.GL_MIRRORED_REPEAT
import org.lwjgl.stb.STBImage
import java.nio.ByteBuffer

object TextureHandler {

    fun initTextures(textures: List<String>): Pair<Int, Int> {

        // Generate Texture
        val textureId = glGenTextures()
        if (glGetError() != GL_NO_ERROR) {
            throw IllegalStateException("glGenTextures に失敗")
        }

        textures.forEach { initTexture(textureId, it) }

        // set Sampler
        val samplerId = glGenSamplers()
        glSamplerParameteri(samplerId, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glSamplerParameteri(samplerId, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glSamplerParameteri(samplerId, GL_TEXTURE_WRAP_S, GL_MIRRORED_REPEAT)
        glSamplerParameteri(samplerId, GL_TEXTURE_WRAP_T, GL_MIRRORED_REPEAT)

        return textureId to samplerId
    }

    private fun initTexture(textureId: Int, texture: String) {

        // Load Image using stb
        val width = BufferUtils.createIntBuffer(1)
        val height = BufferUtils.createIntBuffer(1)
        val comp = BufferUtils.createIntBuffer(1)

        val img: ByteBuffer = STBImage.stbi_load(texture, width, height, comp, STBImage.STBI_default)
                ?: throw IllegalStateException("テクスチャ $texture が読み込めませんでした")

        glBindTexture(GL_TEXTURE_2D, textureId)
        if (comp.get(0) == 3) {
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, width.get(0), height.get(0), 0, GL_RGB, GL_UNSIGNED_BYTE, img)
        } else if (comp.get(0) == 4) {
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width.get(0), height.get(0), 0, GL_RGBA, GL_UNSIGNED_BYTE, img)
        }
        STBImage.stbi_image_free(img)
    }
}
package jp.gr.java_conf.hangedman.lwjgl

import org.lwjgl.BufferUtils
import org.lwjgl.opengl.ARBSamplerObjects.glGenSamplers
import org.lwjgl.opengl.ARBSamplerObjects.glSamplerParameteri
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL14.GL_MIRRORED_REPEAT
import org.lwjgl.stb.STBImage
import java.nio.ByteBuffer

object TextureHandler {

    fun initTextures(textures: List<String>): List<Int> {
        return textures.map { initTexture(it) }
    }

    private fun initTexture(texture: String): Int {

        // Load Image using stb
        val width = BufferUtils.createIntBuffer(1)
        val height = BufferUtils.createIntBuffer(1)
        val comp = BufferUtils.createIntBuffer(1)

        val img: ByteBuffer = STBImage.stbi_load(texture, width, height, comp, STBImage.STBI_default)
                ?: throw IllegalStateException("テクスチャ $texture が読み込めませんでした")

        // Generate Texture
        val textureID = glGenTextures()
        glBindTexture(GL_TEXTURE_2D, textureID)
        if (comp.get(0) == 3) {
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, width.get(0), height.get(0), 0,
                    GL_RGB, GL_UNSIGNED_BYTE, img)
        } else if (comp.get(0) == 4) {
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width.get(0), height.get(0), 0,
                    GL_RGBA, GL_UNSIGNED_BYTE, img)
        }
        STBImage.stbi_image_free(img)

        // set Sampler
        val samplerId = glGenSamplers()
        glSamplerParameteri(samplerId, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glSamplerParameteri(samplerId, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glSamplerParameteri(samplerId, GL_TEXTURE_WRAP_S, GL_MIRRORED_REPEAT)
        glSamplerParameteri(samplerId, GL_TEXTURE_WRAP_T, GL_MIRRORED_REPEAT)
        return samplerId
    }
}
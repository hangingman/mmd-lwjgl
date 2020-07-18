package jp.gr.java_conf.hangedman.lwjgl

import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL14.GL_MIRRORED_REPEAT
import org.lwjgl.opengl.GL30.GL_TEXTURE_2D_ARRAY
import org.lwjgl.opengl.GL45.*
import org.lwjgl.stb.STBImage
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer

object TextureHandler {

    private val logger = LoggerFactory.getLogger(this::class.java)

    data class Texture(
            val index: Int,
            val width: Int,
            val height: Int,
            val comp: Int,
            val img: ByteBuffer
    )

    fun initTextures(texturePaths: List<String>): Int {

        // テクスチャ情報をOpenGLに設定する
        val tex = glCreateTextures(GL_TEXTURE_2D_ARRAY)
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1)

        val textures = texturePaths.mapIndexed { idx, texture ->
            loadTexture(idx, tex, texture)
        }

        glTextureStorage3D(
                tex,
                1,
                GL_RGBA8,
                textures.maxBy { it.width }!!.width,
                textures.maxBy { it.height }!!.height,
                textures.size
        )

        textures.forEach { texture ->
            glTextureSubImage3D(tex,
                    0,
                    0, 0,
                    texture.index,
                    texture.width, texture.height,
                    1,
                    if (texture.comp==3) GL_RGB else GL_RGBA,
                    GL_UNSIGNED_BYTE,
                    texture.img
            )
        }

        glGenerateTextureMipmap(tex)
        glTexParameteri(tex, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(tex, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glTexParameteri(tex, GL_TEXTURE_WRAP_S, GL_MIRRORED_REPEAT)
        glTexParameteri(tex, GL_TEXTURE_WRAP_T, GL_MIRRORED_REPEAT)

        return tex
    }

    private fun loadTexture(idx: Int, tex: Int, texture: String): Texture {

        val width = BufferUtils.createIntBuffer(1)
        val height = BufferUtils.createIntBuffer(1)
        val comp = BufferUtils.createIntBuffer(1)
        val img: ByteBuffer = STBImage.stbi_load(texture, width, height, comp, STBImage.STBI_default)
                ?: throw IllegalStateException("テクスチャ $texture が読み込めませんでした")

        logger.debug("$texture 読み込みOK")
        val texObj = Texture(idx, width.get(0), height.get(0), comp.get(0), img)

        STBImage.stbi_image_free(img)

        return texObj
    }
}


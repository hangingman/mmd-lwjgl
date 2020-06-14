package jp.gr.java_conf.hangedman.mmd.pmd

import com.igormaznitsa.jbbp.mapper.Bin

@Bin
class PmdStruct {
    var magic: ByteArray? = null
    var version: Float = 0F
    var modelName: ByteArray? = null
    var comment: ByteArray? = null
}
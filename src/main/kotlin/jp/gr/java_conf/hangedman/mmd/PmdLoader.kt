package jp.gr.java_conf.hangedman.mmd

import jp.gr.java_conf.hangedman.mmd.pmd.PmdParser
import jp.gr.java_conf.hangedman.mmd.pmd.PmdStruct
import org.apache.commons.lang3.time.DurationFormatUtils
import org.apache.commons.lang3.time.StopWatch
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStream

object PmdLoader {

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun loadPmdFile(pmdFile: String): PmdStruct {
        val stopWatch = StopWatch()
        stopWatch.start()
        logger.info("load file: $pmdFile")

        val stream = getResourceAsStream(pmdFile)
        val pmdStruct = PmdParser.parse(stream)

        // show time
        stopWatch.stop()
        logger.info("Loading time: ${millTimeFormat(0, stopWatch.time)}, Hello MMD!")

        return pmdStruct
    }

    internal fun getResourceAsStream(file: String): InputStream {
        if (File(file).exists()) {
            return File(file).inputStream()
        }

        return this::class.java.classLoader.getResourceAsStream(file)
                ?: throw IllegalStateException("Failed to load file $file")
    }

    private fun millTimeFormat(startTime: Long, endTime: Long): String {
        return DurationFormatUtils.formatPeriod(startTime, endTime, "HH:mm:ss.SSS")
    }
}
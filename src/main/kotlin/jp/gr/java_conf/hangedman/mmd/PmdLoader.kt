package jp.gr.java_conf.hangedman.mmd

import org.apache.commons.lang3.time.DurationFormatUtils
import org.apache.commons.lang3.time.StopWatch
import org.slf4j.LoggerFactory
import java.io.InputStream

object PmdLoader {

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun loadPmdFile(pmdFile: String) {
        val stopWatch = StopWatch()
        stopWatch.start()
        logger.debug("load file: $pmdFile")

        val stream = getResourceAsStream(pmdFile)

        // show time
        stopWatch.stop()
        logger.debug("Loading time: ${millTimeFormat(0, stopWatch.time)}, Hello MMD!")
    }

    internal fun getResourceAsStream(file: String): InputStream {
        return this::class.java.classLoader.getResourceAsStream(file)
                ?: throw IllegalStateException("Failed to load file $file")
    }

    private fun millTimeFormat(startTime: Long, endTime: Long): String {
        return DurationFormatUtils.formatPeriod(startTime, endTime, "HH:mm:ss.SSS")
    }
}
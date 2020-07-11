package jp.gr.java_conf.hangedman.mmd

import jp.gr.java_conf.hangedman.mmd.mesh.pmd.PmdParser
import jp.gr.java_conf.hangedman.mmd.mesh.pmx.PmxParser
import jp.gr.java_conf.hangedman.mmd.mesh_if.Mesh
import org.apache.commons.lang3.time.DurationFormatUtils
import org.apache.commons.lang3.time.StopWatch
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStream

object MeshLoader {

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun loadMeshFile(meshFile: String): Mesh {
        val stopWatch = StopWatch()
        stopWatch.start()
        logger.info("load file: $meshFile")

        val mesh: Mesh = when (val ext = File(meshFile).extension) {
            "pmd" -> PmdParser.parse(meshFile)
            "pmx" -> PmxParser.parse(meshFile)
            else -> {
                throw IllegalStateException("$ext format is not supported now")
            }
        }

        // show time
        stopWatch.stop()
        logger.info("Loading time: ${millTimeFormat(0, stopWatch.time)}, Hello MMD!")

        return mesh
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
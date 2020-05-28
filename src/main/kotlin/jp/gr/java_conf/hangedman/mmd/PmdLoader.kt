package jp.gr.java_conf.hangedman.mmd

import org.apache.commons.lang3.time.DurationFormatUtils
import org.apache.commons.lang3.time.StopWatch
import org.jruby.util.JRubyClassLoader
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStream
import javax.script.ScriptContext
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager

object PmdLoader {

    private val logger = LoggerFactory.getLogger(this::class.java)
    private val scriptEngineManager = ScriptEngineManager()
    private var rubyEngine: ScriptEngine

    init {
        rubyEngine = scriptEngineManager.getEngineByName("jruby")
        rubyEngine.context.setAttribute("label", 1.0F, ScriptContext.ENGINE_SCOPE)
        val jar: File = File("build/libs/mmd-clj-jruby-1.0-SNAPSHOT.jar")

//        rubyEngine.c
//        JRubyClassLoader
//        class_loader = JRuby.runtime.jruby_class_loader
//        class_loader.add_url(java.io.File.new('path/to/jarSome.jar')
    }

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

    fun pmdHeader() {

        rubyEngine.eval("""
          require "bindata"

          class PMD < BinData::Record
            endian :little
        
            # ヘッダ
            string :magic, :read_length => 3
            float :version
            string :model_name, :read_length => 20
            string :comment, :read_length => 256
          end
        """.trimIndent())
    }
}
package jp.gr.java_conf.hangedman.mmd

import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Options

object MmdLwjglOptionParser {
    val options = Options().addOption("m", true, "specify pmd model file")
    val parser = DefaultParser()

    fun parse(args: Array<String>): CommandLine {
        return parser.parse(options, args)
    }
}
/*
 * Copyright (C) 2024 Bajdik Márton
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA  02110-1301, USA.
 */

package me.mbajdik.webcompiler.cli

import me.mbajdik.webcompiler.make.MakeConfig
import me.mbajdik.webcompiler.state.Logger
import me.mbajdik.webcompiler.state.Manager
import me.mbajdik.webcompiler.task.helpers.WebLocalFileHandler
import me.mbajdik.webcompiler.task.tasks.HTMLProcessTask
import me.mbajdik.webcompiler.util.FileUtilities
import me.mbajdik.webcompiler.util.TerminalUtils
import org.apache.commons.cli.*
import java.io.File
import java.net.URI
import kotlin.system.exitProcess

object WMakeCompile {
    fun command(args: Array<String>) {
        val options = Options();

        val optionRoot = Option.builder("r")
            .longOpt("root")
            .hasArg()
            .desc("The project root, CWD is used by default")
            .build();

        val optionOutput = Option.builder("o")
            .longOpt("save")
            .hasArg()
            .desc("Where to save the output, STDOUT is used by default")
            .build();

        val optionNoMinify = Option.builder("n")
            .longOpt("no-minify")
            .desc("Don't minify the output of the HTML compiler")
            .build();

        val optionNoMinifyJS = Option.builder()
            .longOpt("no-minify-js")
            .desc("Don't minify the JavaScript in the compiled HTML")
            .build();

        val optionNoMinifyCSS = Option.builder()
            .longOpt("no-minify-css")
            .desc("Don't minify the CSS in the compiled HTML")
            .build();

        val optionLogFile = Option.builder()
            .longOpt("logfile")
            .hasArg()
            .desc("The file to save the log to")
            .build();

        val optionLogLevel = Option.builder("l")
            .longOpt("loglevel")
            .hasArg()
            .desc("Specify the log level (e.g.: for errors only: 0)")
            .build();

        val optionHelp = Option.builder("h")
            .longOpt("help")
            .desc("Display this message")
            .build();

        options.addOption(optionRoot);
        options.addOption(optionOutput);
        options.addOption(optionNoMinify);
        options.addOption(optionNoMinifyJS);
        options.addOption(optionNoMinifyCSS);
        options.addOption(optionLogFile);
        options.addOption(optionLogLevel);
        options.addOption(optionHelp);

        val parser = DefaultParser();
        val parsed = try { parser.parse(options, args)!! } catch (e: ParseException) { null }
        val where = if (parsed != null && parsed.argList.size > 0) parsed.argList[0] else null;

        if (parsed == null || where == null || parsed.hasOption(optionHelp)) {
            val formatter = HelpFormatter();
            formatter.printHelp("wmake compile [OPTIONS...] [FILE]", options);
            exitProcess(1);
        }

        val logFile: File? = if (parsed.hasOption(optionLogFile)) File(parsed.getOptionValue(optionLogFile)) else null;
        val logLevel = parsed.getOptionValue(optionLogLevel)?.toIntOrNull() ?: 2
        val freeSTDOUT = parsed.hasOption(optionOutput);
        val quiet = !freeSTDOUT;

        val manager = Manager(
            logger = Logger(logFile = logFile, logLevel = logLevel),
            freeSTDOUT = freeSTDOUT,
            quiet = quiet,
        )


        val scheme = URI(where).scheme;
        val handler = if (scheme != null && scheme.isNotEmpty()) {
                WebLocalFileHandler.remote(where)
            } else {
                val file = File(where)
                val rootDir =
                    if (parsed.hasOption(optionRoot))
                        File(parsed.getOptionValue(optionRoot))
                    else
                        askUnspecifiedRoot(freeSTDOUT, file)

                val filePath = rootDir.toURI().relativize(file.toURI()).path

                WebLocalFileHandler.local(rootDir.toString(), filePath);
            }

        val task = HTMLProcessTask(manager, handler, listOf(), listOf(), null, MakeConfig.AutoTitleMode.NONE);

        manager.init();
        val out =
            if (parsed.hasOption(optionNoMinify))
                task.process();
            else
                task.minifiedProcess(
                    minifyJS = !parsed.hasOption(optionNoMinifyJS),
                    minifyCSS = !parsed.hasOption(optionNoMinifyCSS)
                );


        if (freeSTDOUT) {
            FileUtilities.writeToFileSafe(
                bytes = out.toByteArray(),
                file = File(parsed.getOptionValue(optionOutput)),
                freeSTDOUT = true
            );
        } else {
            println(out);
        }

        manager.exit(false);
    }

    private fun askUnspecifiedRoot(freeSTDOUT: Boolean, file: File): File {
        return if (freeSTDOUT && file.parentFile != null) {
            val prompt = "No root directory was given, set the root to ${file.parent}?";

            if (TerminalUtils.yesOrNo(default = true, prompt = prompt)) {
                file.parentFile
            } else {
                File(".")
            }
        } else {
            File(".")
        }
    }
}
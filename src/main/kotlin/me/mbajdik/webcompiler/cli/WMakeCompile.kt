/*
 * Copyright (C) 2023 Bajdik Márton
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

import me.mbajdik.webcompiler.compiler.minifier.HTMLMinifierCompat
import me.mbajdik.webcompiler.state.Logger
import me.mbajdik.webcompiler.state.Manager
import me.mbajdik.webcompiler.task.helpers.WebLocalFileHandler
import me.mbajdik.webcompiler.task.tasks.HTMLProcessTask
import me.mbajdik.webcompiler.util.FileUtilities
import org.apache.commons.cli.*
import java.io.File
import kotlin.system.exitProcess

object WMakeCompile {
    fun command(args: Array<String>) {
        val options = Options();

        // optional root - cwd by default
        val optionRoot = Option.builder("r")
            .longOpt("root")
            .hasArg()
            .desc("The project root, CWD is used by default")
            .build();

        // optional output file
        val optionOutput = Option.builder("o")
            .longOpt("save")
            .hasArg()
            .desc("Where to save the output, STDOUT is used by default")
            .build();

        // optional
        val optionOptions = Option.builder("m")
            .longOpt("options")
            .hasArgs()
            .valueSeparator(';')
            .desc("Options to pass over to the minifier")
            .build();

        // not even recommended
        val optionNoMinify = Option.builder("n")
            .longOpt("no-minify")
            .desc("Don't minify the output of the HTML compiler")
            .build();

        // compile once - debug everywhere
        val optionLogFile = Option.builder()
            .longOpt("logfile")
            .hasArg()
            .desc("The file to save the log to")
            .build();

        // in the weird case the user wants more debug information
        val optionLogLevel = Option.builder("l")
            .longOpt("loglevel")
            .hasArg()
            .desc("Specify the log level (e.g.: for errors only: 0)")
            .build();

        // print this help
        val optionHelp = Option.builder("h")
            .longOpt("help")
            .desc("Display this message")
            .build();

        options.addOption(optionRoot);
        options.addOption(optionOutput);
        options.addOption(optionOptions);
        options.addOption(optionNoMinify);
        options.addOption(optionLogFile);
        options.addOption(optionLogLevel);
        options.addOption(optionHelp);

        val parser = DefaultParser();
        val parsed = try { parser.parse(options, args)!! } catch (e: ParseException) { null }
        val file = if (parsed != null && parsed.argList.size > 0) parsed.argList[0] else null;

        // printing (error)+help message
        if (parsed == null || file == null || parsed.hasOption(optionHelp)) {
            val formatter = HelpFormatter();
            formatter.printHelp("wmake compile [OPTIONS...] [FILE]", options);
            exitProcess(1);
        }

        val freeSTDOUT = parsed.hasOption(optionOutput);
        val logfile: File? = if (parsed.hasOption(optionLogFile)) File(parsed.getOptionValue(optionLogFile)) else null;

        val manager = Manager(
            logger = Logger(logFile = logfile, logLevel = parsed.getOptionValue(optionLogLevel)?.toIntOrNull() ?: 2),
            freeSTDOUT = freeSTDOUT,
            cli = freeSTDOUT,
        )

        val rootDir =
            if (parsed.hasOption(optionRoot))
                parsed.getOptionValue(optionRoot)
            else
                System.getProperty("user.dir")

        val handler = WebLocalFileHandler.local(rootDir, file);
        val task = HTMLProcessTask(manager, handler);
        val minifyOpts =
            if (parsed.hasOption(optionOptions))
                parsed.getOptionValues(optionOptions).toList()
            else
                HTMLMinifierCompat.DEFAULT_OPTIONS;

        // Starting actual compile timer
        manager.init();

        // Finally executing
        val processed = task.process()
        val out =
            if (parsed.hasOption(optionNoMinify))
                processed // using cached
            else
                task.minify(minifyOpts);


        // Giving output
        if (freeSTDOUT) {
            FileUtilities.writeToFileSafe(out.toByteArray(), File(parsed.getOptionValue(optionOutput)), true);
        } else {
            println(out);
        }

        manager.exit(false);
    }
}
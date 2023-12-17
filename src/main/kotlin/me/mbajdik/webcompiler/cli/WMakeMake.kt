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

import com.google.gson.JsonParser
import me.mbajdik.webcompiler.make.HookRunner
import me.mbajdik.webcompiler.make.MakeConfig
import me.mbajdik.webcompiler.make.MakeProcessor
import me.mbajdik.webcompiler.state.Logger
import me.mbajdik.webcompiler.state.Manager
import me.mbajdik.webcompiler.task.helpers.WebLocalFileHandler
import me.mbajdik.webcompiler.util.ANSI
import me.mbajdik.webcompiler.util.FileUtilities
import me.mbajdik.webcompiler.util.PathUtilities
import org.apache.commons.cli.*
import java.io.File
import java.nio.file.Files
import kotlin.system.exitProcess

object WMakeMake {
    fun command(args: Array<String>) {
        val options = Options();

        // optional root - cwd by default
        val optionRoot = Option.builder("r")
            .longOpt("root")
            .hasArg()
            .desc("The project root, wmake.json is used by default (or the CWD if it's not supplied)")
            .build();

        // optional output file
        val optionOutputDir = Option.builder("o")
            .longOpt("save-dir")
            .hasArg()
            .desc("The directory to save the output to, if not in wmake.json (otherwise in [root]/.target/)")
            .build();

        // optional output file
        val optionZipOutput = Option.builder("z")
            .longOpt("zip")
            .hasArg()
            .desc("The zip file to save the output to, otherwise saved in a directory")
            .build();

        // compile once - debug everywhere
        val optionLogFile = Option.builder()
            .longOpt("logfile")
            .hasArg()
            .desc("The file to save the log to, otherwise ([root]/.wmake/logs/[date].log")
            .build();

        // in the weird case the user wants more debug information
        val optionLogLevel = Option.builder("l")
            .longOpt("loglevel")
            .hasArg()
            .desc("Specify the log level (e.g.: for errors only: 0)")
            .build();

        // changing threads for debug
        val optionThreads = Option.builder("t")
            .longOpt("threads")
            .hasArg()
            .desc("Specify the number of threads for the compilation")
            .build();

        // print this help
        val optionHelp = Option.builder("h")
            .longOpt("help")
            .desc("Display this message")
            .build();

        options.addOption(optionRoot);
        options.addOption(optionOutputDir);
        options.addOption(optionZipOutput);
        options.addOption(optionLogFile);
        options.addOption(optionLogLevel);
        options.addOption(optionHelp);

        val parser = DefaultParser();
        val parsed = try { parser.parse(options, args)!! } catch (e: ParseException) { null }
        val where = if (parsed != null && parsed.argList.size > 0) parsed.argList[0] else null;

        // printing (error)+help message
        if (parsed == null || parsed.hasOption(optionHelp)) {
            val formatter = HelpFormatter();
            formatter.printHelp("wmake make [OPTIONS...] [LOCATION]", options);
            exitProcess(1);
        }

        val makefile =
            if (where != null)
                if (File(where).name != "wmake.json")
                    File(where, "wmake.json")
                else
                    File(where)
            else
                File("wmake.json"); // in CWD

        if (!makefile.exists()) {
            println(ANSI.red("There is no wmake project here, use \"wmake new\" to create one"))
            exitProcess(1);
        }

        val rootDir =
            if (parsed.hasOption(optionRoot))
                File(parsed.getOptionValue(optionRoot))
            else
                makefile.parentFile ?: File(".");

        val makeDir = File(rootDir, ".wmake");
        val makeLogDir = File(makeDir, "log");
        val logfile =
            if (parsed.hasOption(optionLogFile))
                File(parsed.getOptionValue(optionLogFile))
            else
                File(makeLogDir, "${System.currentTimeMillis()}.log")

        val manager = Manager(
            logger = Logger(
                logFile = logfile,
                logLevel = parsed.getOptionValue(optionLogLevel)?.toIntOrNull() ?: 2
            )
        )

        val configHandler = WebLocalFileHandler.local(
            root = rootDir.toString(),
            path = makefile.toPath().toString()
        );
        val config = MakeConfig(
            JsonParser.parseString(configHandler.fileContentsString(manager)).asJsonObject
        );

        val processor = MakeProcessor(
            manager = manager,
            config = config,
            root = rootDir.toString(),
            explicitThreads = parsed.getOptionValue(optionThreads)?.toIntOrNull()
        );

        // Starting actual compile timer
        manager.init();

        // Pre-build hooks
        HookRunner.run(manager, config.hookRunner, config.preBuildHooks, HookRunner.HookType.PRE_BUILD);

        // Executing and Saving
        if (parsed.hasOption(optionZipOutput)) {
            val zip = processor.processedZip();

            FileUtilities.writeToFileSafe(zip, File(parsed.getOptionValue(optionZipOutput)), true);
        } else {
            val dirs = processor.processed();
            val saveDir =
                if (parsed.hasOption(optionOutputDir))
                    File(parsed.getOptionValue(optionOutputDir))
                else
                    File(makeDir, "target");

            saveDir.deleteRecursively();

            for (path in dirs.keys) {
                val savePath = PathUtilities.joinPathListWithRoot(saveDir.toString(), path);

                savePath.parentFile.mkdirs();
                Files.write(savePath.toPath(), dirs[path] ?: byteArrayOf())
            }
        }

        // Pro-build hooks
        HookRunner.run(manager, config.hookRunner, config.proBuildHooks, HookRunner.HookType.POST_BUILD);

        manager.exit(false);
    }
}
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

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import me.mbajdik.webcompiler.make.MakeConfig
import me.mbajdik.webcompiler.make.MakeProcessor
import me.mbajdik.webcompiler.state.Logger
import me.mbajdik.webcompiler.state.Manager
import me.mbajdik.webcompiler.task.helpers.WebLocalFileHandler
import me.mbajdik.webcompiler.task.tasks.HookListRunnerTask
import me.mbajdik.webcompiler.util.ANSI
import me.mbajdik.webcompiler.util.FileUtilities
import org.apache.commons.cli.*
import java.io.File
import java.nio.file.Files
import kotlin.system.exitProcess

object WMakeMake {
    fun command(args: Array<String>, foundProjectRoot: String? = null) {
        val options = Options();

        val optionRoot = Option.builder("r")
            .longOpt("root")
            .hasArg()
            .desc("The project root, wmake.json is used by default (or the CWD if it's not supplied)")
            .build();

        val optionOutputDir = Option.builder("o")
            .longOpt("save-dir")
            .hasArg()
            .desc("The directory to save the output to, if not in wmake.json (otherwise in [root]/.target/)")
            .build();

        val optionZipOutput = Option.builder("z")
            .longOpt("zip")
            .hasArg()
            .desc("The zip file to save the output to, otherwise saved in a directory")
            .build();

        val optionLogFile = Option.builder()
            .longOpt("logfile")
            .hasArg()
            .desc("The file to save the log to, otherwise ([root]/.wmake/logs/[date].log)")
            .build();

        val optionLogLevel = Option.builder("l")
            .longOpt("loglevel")
            .hasArg()
            .desc("Specify the log level (e.g.: for errors only: 0)")
            .build();

        val optionNoLogging = Option.builder()
            .longOpt("no-log")
            .desc("Fully disables logging")
            .build();

        val optionThreads = Option.builder("t")
            .longOpt("threads")
            .hasArg()
            .desc("Specify the number of threads for the compilation")
            .build();

        val optionQuiet = Option.builder("q")
            .longOpt("quiet")
            .desc("Will not output anything unless an error occurs")
            .build();

        val optionHelp = Option.builder("h")
            .longOpt("help")
            .desc("Display this message")
            .build();

        options.addOption(optionRoot);
        options.addOption(optionOutputDir);
        options.addOption(optionZipOutput);
        options.addOption(optionLogFile);
        options.addOption(optionLogLevel);
        options.addOption(optionNoLogging);
        options.addOption(optionQuiet);
        options.addOption(optionHelp)

        val parser = DefaultParser();
        val parsed = try { parser.parse(options, args)!! } catch (e: ParseException) { null }
        val where =
            if (parsed != null && parsed.argList.size > 0)
                parsed.argList[0]
            else
                foundProjectRoot ?: findParentProject(File("."))?.toString()

        // printing (error)+help message
        if (parsed == null || parsed.hasOption(optionHelp)) {
            val formatter = HelpFormatter();
            formatter.printHelp("wmake make [OPTIONS...] [LOCATION]", options);
            exitProcess(1);
        }


        // only directories are allowed - cannot give wmake.json
        if (where != null && !File(where).isDirectory) {
            println(ANSI.red("The given make location is not a directory! Run \"wmake make --help\" for usage info"))
            exitProcess(1)
        }

        val makefile =
            if (where != null)
                File(where, "wmake.json")
            else
                File("wmake.json"); // in CWD


        if (!makefile.exists()) {
            println(ANSI.red("There is no wmake project here, use \"wmake new\" to create one"))
            exitProcess(1);
        }

        val projectRootDir = makefile.parentFile ?: File(".");
        val makeDir = File(projectRootDir, ".wmake");
        val makeLogDir = File(makeDir, "log");
        val logfile =
            if (!parsed.hasOption(optionNoLogging))
                if (parsed.hasOption(optionLogFile))
                    File(parsed.getOptionValue(optionLogFile))
                else
                    File(makeLogDir, "${System.currentTimeMillis()}.log")
            else
                null

        val manager = Manager(
            logger = Logger(
                logFile = logfile,
                logLevel = parsed.getOptionValue(optionLogLevel)?.toIntOrNull() ?: 2
            ),
            quiet = parsed.hasOption(optionQuiet)
        )

        val configHandler = WebLocalFileHandler.local(
            root = makefile.parent ?: ".",
            path = makefile.name
        )

        val configJson = JsonParser.parseString(configHandler.fileContentsString(manager))
        if (configJson == null || configJson !is JsonObject) {
            println(ANSI.red("The wmake.json is invalid! (either empty or invalid JSON syntax is used)"))
            exitProcess(1);
        }

        val config = MakeConfig(
            configJson.asJsonObject
        )

        val makeRootDir =
            if (parsed.hasOption(optionRoot))
                File(parsed.getOptionValue(optionRoot))
            else
                if (config.root != null)
                    File(projectRootDir, config.root)
                else
                    projectRootDir

        val processor = MakeProcessor(
            manager = manager,
            config = config,
            root = makeRootDir.toString(),
            explicitThreads = parsed.getOptionValue(optionThreads)?.toIntOrNull()
        );

        // Starting actual compile timer
        manager.init();

        // Pre-build hooks
        HookListRunnerTask(
            manager = manager,
            runner = config.hookRunner,
            hooks = config.preBuildHooks,
            type = HookListRunnerTask.HookType.PRE_BUILD
        ).run();

        // Executing and Saving
        if (parsed.hasOption(optionZipOutput) || config.outputType == MakeConfig.OutputType.ZIP) {
            val zipOut = parsed.getOptionValue(optionZipOutput);
            val outFile =
                if (zipOut != null)
                    File(zipOut)
                else
                    if (config.output != null)
                        File(config.output)
                    else
                        File(makeDir, "target.zip")

            outFile.delete();

            val zip = processor.processedZip();

            FileUtilities.writeToFileSafe(zip, outFile, freeSTDOUT = true, force = true);
        } else {
            val dirs = processor.processed();
            val saveDir =
                if (parsed.hasOption(optionOutputDir))
                    File(parsed.getOptionValue(optionOutputDir))
                else
                    if (config.output != null)
                        File(config.output)
                    else
                        File(makeDir, "target");

            saveDir.deleteRecursively();

            for (path in dirs.keys) {
                val savePath = path.osFileWithRoot(saveDir)

                savePath.parentFile?.mkdirs();
                Files.write(savePath.toPath(), dirs[path] ?: byteArrayOf())
            }
        }

        // Post-build hooks
        HookListRunnerTask(
            manager = manager,
            runner = config.hookRunner,
            hooks = config.proBuildHooks,
            type = HookListRunnerTask.HookType.POST_BUILD
        ).run();

        manager.exit(false);
    }

    fun findParentProject(place: File): File? {
        val abs = place.absoluteFile;
        val parent = abs.parentFile;
        val makefile = File(parent, "wmake.json")

        try {
            if (parent != null && parent.canWrite()) {
                return if (makefile.exists() && makefile.isFile) {
                    parent;
                } else {
                    findParentProject(parent)
                }
            }
        } catch (e: Exception) {
            return null;
        }

        return null;
    }
}
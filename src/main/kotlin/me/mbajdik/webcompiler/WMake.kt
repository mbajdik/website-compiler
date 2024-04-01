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

package me.mbajdik.webcompiler

import me.mbajdik.webcompiler.cli.WMakeCompile
import me.mbajdik.webcompiler.cli.WMakeDumpConfig
import me.mbajdik.webcompiler.cli.WMakeMake
import me.mbajdik.webcompiler.util.ANSI
import java.io.File
import kotlin.system.exitProcess

object WMake {
    private val HELP_MESSAGE = """
        Usage: wmake [make|build|new|create|compile] [SUBCOMMAND OPTIONS]
        
        DEFAULT SUBCOMMAND: make (automatically executed if in a project)
        
        COMMANDS
            make, build:    Builds a wmake project
            new, create:    Dumps the default configuration - usage: "wmake new [location?]"
            compile:        Compiles a single file
            help:           Displays this message
    """.trimIndent()

    @JvmStatic
    fun main(args: Array<String>) {
        if (args.isEmpty()) {
            val projectRoot = WMakeMake.findParentProject(File("."))?.toString();

            if (projectRoot != null && File(projectRoot, "wmake.json").exists()) {
                WMakeMake.command(emptyArray(), projectRoot);
            } else {
                printHelp("No command was given and not in a wmake project, use \"wmake new\" to create a new project")
            }
        };

        val newArgs = args.slice(1 until args.size).toTypedArray();
        when (args[0]) {
            "make", "build" -> WMakeMake.command(newArgs);
            "new", "create" -> WMakeDumpConfig.command(newArgs)
            "compile" -> WMakeCompile.command(newArgs);
            "help" -> printHelp()
            else -> printHelp("Unknown command: ${args[0]}")
        }
    }

    private fun printHelp(error: String? = null) {
        if (error != null) println(ANSI.red(error) + "\n");
        println(HELP_MESSAGE);
        exitProcess(if (error == null) 0 else 1);
    }
}
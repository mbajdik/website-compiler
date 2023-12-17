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

import me.mbajdik.webcompiler.util.ANSI
import me.mbajdik.webcompiler.util.FileUtilities
import me.mbajdik.webcompiler.util.TerminalUtils
import java.io.File
import kotlin.system.exitProcess

object WMakeDumpConfig {
    fun command(args: Array<String>) {
        val location =
            if (args.isEmpty())
                File(".")
            else
                File(args[0])

        val file = File(location, "wmake.json")

        if (file.exists() && TerminalUtils.yesOrNo(false, "There's already a wmake project here")) {
            exitProcess(0);
        }

        try {
            val inStream = javaClass.getResourceAsStream("/default_config.json");

            if (inStream == null) {
                println(ANSI.red("Couldn't load default configuration"))
                exitProcess(1);
            }

            FileUtilities.writeToFileSafe(inStream.readAllBytes(), file, true);
        } catch (e: Exception) {
            println(ANSI.red("An exception occurred while writing the default configuration to $file"))
            e.printStackTrace();
        }
    }
}
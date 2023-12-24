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

package me.mbajdik.webcompiler.compiler.minifier

import me.mbajdik.webcompiler.state.data.ErrorMessage
import me.mbajdik.webcompiler.task.tasks.MinifyTask
import me.mbajdik.webcompiler.util.EnvironmentUtilities

object HTMLMinifierCompat {
    val DEFAULT_OPTIONS = listOf("--collapse-whitespace", "--minify-css", "--minify-js")

    fun minifyHTML(task: MinifyTask): String {
        val probableNodePath = EnvironmentUtilities.getFullExecutablePath("node")
        val probableScriptPath = EnvironmentUtilities.getFullExecutablePath("html-minifier");

        if (probableNodePath == null) createNotInPathError(task, "node");
        if (probableScriptPath == null) createNotInPathError(task, "html-minifier");

        val process = Runtime.getRuntime().exec(
            arrayOf(
                probableNodePath,
                probableScriptPath,
            ) + task.options.toTypedArray()
        )

        process.outputStream.write(task.unminified.toByteArray())
        process.outputStream.close()
        process.waitFor()

        val errorBytes = process.errorStream.readAllBytes()
        if (errorBytes.isNotEmpty()) {
            task.manager.pushErrorMessage(
                task,
                ErrorMessage.MessageType.ERROR,
                "couldn't minify HTML",
                null,
                listOf(String(errorBytes))
            )
        }

        return String(process.inputStream.readAllBytes())
    }

    private fun createNotInPathError(task: MinifyTask, bin: String) {
        task.manager.pushErrorMessage(
            task,
            ErrorMessage.MessageType.ERROR,
            "Couldn't find a required executable in PATH",
            null,
            listOf("Binary: $bin")
        )
    }
}
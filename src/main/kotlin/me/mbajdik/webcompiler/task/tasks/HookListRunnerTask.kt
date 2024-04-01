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

package me.mbajdik.webcompiler.task.tasks

import me.mbajdik.webcompiler.state.Manager
import me.mbajdik.webcompiler.state.data.ErrorMessage
import me.mbajdik.webcompiler.task.Task
import java.io.InputStreamReader
import java.util.*

class HookListRunnerTask(
    val manager: Manager,
    val runner: List<String>,
    val hooks: List<List<String>>,
    val type: HookType,
) : Task() {
    override fun getDisplayPath(): String = ""; // Should never throw an error
    override fun getTaskTypeName(): String = "${type.text} hooks";

    fun run() {
        for (hook in hooks) {
            val subtask = HookRunnerTask(manager, runner, hook, type);

            subtask.run();
        }
    }


    class HookRunnerTask(
        val manager: Manager,
        val runner: List<String>,
        val hook: List<String>,
        val type: HookType,
    ): Task() {
        override fun getDisplayPath(): String = hook.joinToString(" ")
        override fun getTaskTypeName(): String = "${type.text} hook";

        fun run() {
            val process = Runtime.getRuntime().exec(
                runner.toTypedArray() + hook.toTypedArray()
            )

            // Redirecting STDOUT
            val inStream = process.inputStream
            val reader = InputStreamReader(inStream)
            val scan = Scanner(reader)
            while (scan.hasNextLine()) {
                println(scan.nextLine())
            }

            val errorBytes = process.errorStream.readAllBytes()
            if (errorBytes.isNotEmpty()) {
                manager.pushErrorMessage(
                    task = this,
                    type = ErrorMessage.MessageType.ERROR,
                    message = "probable error occurred while running a hook",
                    description = listOf(
                        "Hook runner: ${runner.joinToString(" ")}",
                        String(errorBytes)
                    )
                )
            }
        }
    }

    enum class HookType(val text: String) { PRE_BUILD("Pre-build"), POST_BUILD("Post-build") }
}
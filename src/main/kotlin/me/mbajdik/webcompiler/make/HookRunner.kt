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

package me.mbajdik.webcompiler.make

import me.mbajdik.webcompiler.state.Manager
import me.mbajdik.webcompiler.state.data.ErrorMessage
import me.mbajdik.webcompiler.task.tasks.HookRunnerTask

object HookRunner {
    fun run(manager: Manager, runner: List<String>, hooks: List<List<String>>, type: HookType) {
        for (hook in hooks) {
            val task = HookRunnerTask(type, runner, hook);

            val process = Runtime.getRuntime().exec(
                runner.toTypedArray() + hook.toTypedArray()
            )

            // Redirecting STDOUT
            while (process.isAlive) {
                print(String(process.inputStream.readAllBytes()))
            }

            print(String(process.inputStream.readAllBytes()))

            val errorBytes = process.errorStream.readAllBytes()
            if (errorBytes.isNotEmpty()) {
                manager.pushErrorMessage(
                    task,
                    ErrorMessage.MessageType.ERROR,
                    "probable error occurred while running a hook",
                    null,
                    listOf(String(errorBytes))
                )
            }
        }
    }

    enum class HookType(val text: String) { PRE_BUILD("Pre-build"), PRO_BUILD("Pro-build") }
}
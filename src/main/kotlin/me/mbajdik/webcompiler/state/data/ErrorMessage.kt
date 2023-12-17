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

package me.mbajdik.webcompiler.state.data

import me.mbajdik.webcompiler.task.helpers.DebugInformationSupplier

data class ErrorMessage(
    val task: DebugInformationSupplier,
    val type: MessageType = MessageType.WARNING,
    val message: String = "an error occurred",
    val snippet: CodeSnippet? = null,
    val description: List<String>? = null,
    val exception: Throwable? = null,
) {
    override fun toString(): String {
        return toString(false);
    }

    fun toString(colored: Boolean = false): String {
        val typeText = if (colored) type.colored else type.loggable;
        val descLines =
            listOf("Task: ${DebugInformationSupplier.supplierToString(task)}") +
                    (if (exception != null) listOf("Exception: ${exception.javaClass.name}") else emptyList()) +
                    (description ?: emptyList())

        return "$typeText $message" + "\n" +
                descLines.joinToString("\n") { s -> TAB + s } + "\n" +
                (if (snippet == null) "" else "\n$TAB${snippet.code}\n$TAB${" ".repeat(snippet.errorIndex)}^");
    }

    enum class MessageType(val colored: String, val loggable: String, val logLevel: Int) {
        INFORMATION("\u001B[0;36mINFORMATION\u001B[0m", "INFORMATION", 2),
        WARNING("\u001B[0;33mWARNING\u001B[0m", "WARNING", 1),
        INTERNAL_ERROR("\u001B[0;35mINTERNAL ERROR\u001B[0m", "INTERNAL_ERROR", 0),
        ERROR("\u001B[0;31mERROR\u001B[0m", "ERROR", 0),
    }

    data class CodeSnippet(
        val code: String,
        val errorIndex: Int
    )

    companion object {
        private val TAB = " ".repeat(4);
    }
}

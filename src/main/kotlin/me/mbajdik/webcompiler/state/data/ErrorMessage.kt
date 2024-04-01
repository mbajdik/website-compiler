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

package me.mbajdik.webcompiler.state.data

import me.mbajdik.webcompiler.task.helpers.DebugInformationSupplier
import me.mbajdik.webcompiler.util.ANSI

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
                (if (snippet == null) "" else "$snippet");
    }

    enum class MessageType(val colored: String, val loggable: String, val logLevel: Int) {
        INFORMATION("\u001B[0;36mINFORMATION\u001B[0m", "INFORMATION", 2),
        WARNING("\u001B[0;33mWARNING\u001B[0m", "WARNING", 1),
        INTERNAL_ERROR("\u001B[0;35mINTERNAL ERROR\u001B[0m", "INTERNAL_ERROR", 0),
        ERROR("\u001B[0;31mERROR\u001B[0m", "ERROR", 0),
    }

    data class CodeSnippet(
        val code: String,
        val errorIndex: Int,
        val lineNum: Int? = null,
        val columnNum: Int? = null,
    ) {
        override fun toString(): String {
            val linePrefix = if (lineNum != null) ANSI.gray("$lineNum | ") else ANSI.gray(" | ");
            val linePrefixLen = if (lineNum != null) "$lineNum | ".length else 3;

            val lines = listOf(
                linePrefix + code,
                " ".repeat(errorIndex + linePrefixLen) + ANSI.gray("^${columnNum ?: ""}")
            )

            return lines.joinToString("\n");
        }

        companion object {
            fun fromCode(code: String, globalIndex: Int): CodeSnippet {
                var fromNewline = 1;
                var lineNum = 1; // Not starting from 0, but from 1

                val beforeBuf = StringBuilder(code[globalIndex].toString());
                val afterBuf = StringBuilder()

                var skip = 0;
                for (i in globalIndex - 1 downTo 0) {
                    if (skip > 0) {
                        skip--;
                        continue;
                    }

                    val c = code[i];
                    val prev = if (i > 0) code[i - 1] else null;

                    // LF
                    if (c == '\n') {
                        // CRLF
                        if (prev == '\r') {
                            lineNum++;
                            skip += 1;
                            continue;
                        } else {
                            lineNum++;
                            continue;
                        }
                    }

                    if (lineNum == 1) {
                        if (c == '\u0009') {
                            beforeBuf.append(TAB)
                            fromNewline += 4;
                        }

                        beforeBuf.append(c);
                        fromNewline++;
                    }
                }

                afterLoop@ for (i in globalIndex + 1 until code.length) {
                    val c = code[i];
                    if (c == '\n' || c == '\r') break@afterLoop;
                    afterBuf.append(c);
                }

                return CodeSnippet(
                    code = beforeBuf.reverse().toString() + afterBuf.toString(),
                    errorIndex = fromNewline - 1,
                    lineNum = lineNum,
                    columnNum = fromNewline
                );
            }
        }
    }

    companion object {
        private const val TAB_SIZE = 4; // If you use anything else, just recompile
        private val TAB = " ".repeat(TAB_SIZE);
    }
}

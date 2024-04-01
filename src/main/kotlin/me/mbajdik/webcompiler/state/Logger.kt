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

package me.mbajdik.webcompiler.state

import me.mbajdik.webcompiler.state.data.ErrorMessage
import me.mbajdik.webcompiler.util.FileUtilities
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/*
 * Log levels:
 *      0 -> only (internal) errors
 *      1 -> all kinds of warnings, except information
 *      2 -> all messages
 *      3 -> during-execution logging
 */
class Logger constructor(
    private val logLevel: Int = LogLevel.INFORMATION.ordinal,
    private val logFile: File? = null
) {
    constructor() : this(logLevel = 3, null);

    constructor(logLevel: LogLevel = LogLevel.INFORMATION, logFile: File? = null,)
            : this(logLevel = logLevel.ordinal, logFile = logFile)


    private val stringWriter: StringWriter = StringWriter();
    private val writer: PrintWriter = PrintWriter(stringWriter);


    fun log(message: String) {
        log(message, 3)
    }

    fun log(message: String, level: Int) {
        if (level > logLevel) return;

        writer.println(message);
    }

    fun log(message: String, level: LogLevel) {
        log(message, level.ordinal)
    }

    private fun logStacktrace(e: Throwable) {
        e.printStackTrace(writer);
    }

    fun logErrorMessage(message: ErrorMessage) {
        if (message.type.logLevel > logLevel) return;

        writer.println(message.toString())
        if (message.exception != null) logStacktrace(message.exception);
    }

    fun write() {
        if (logFile == null || stringWriter.buffer.isEmpty()) return;
        FileUtilities.writeToFileSafe(stringWriter.buffer.toString().toByteArray(), logFile);
    }



    data class LogEntry(
        val message: String,
        val level: Int,
    )

    enum class LogLevel {
        ERRORS,
        WARNINGS,
        INFORMATION,
        DEBUG,
    }
}
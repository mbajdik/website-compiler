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
import me.mbajdik.webcompiler.util.ANSI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class Statistics {
    private var START_TIME: Long = -1;
    private var END_TIME: Long = -1;

    var HTML_COMPILED: AtomicInteger = AtomicInteger(0);

    var ERROR_TYPES_COUNT: ConcurrentHashMap<ErrorMessage.MessageType, Int> = ConcurrentHashMap();

    fun start() {
        START_TIME = System.currentTimeMillis();
        END_TIME = -1;

        HTML_COMPILED = AtomicInteger(0);

        ERROR_TYPES_COUNT = ConcurrentHashMap();
    }

    fun stop() {
        if (END_TIME != -1L) return;
        END_TIME = System.currentTimeMillis();
    }

    fun printStatistics(error: Boolean) {
        stop();

        val warningsNum = getErrorOccurs(ErrorMessage.MessageType.WARNING, ANSI::yellow);
        val internalErrNum = getErrorOccurs(ErrorMessage.MessageType.INTERNAL_ERROR, ANSI::purple);

        val internalErrors = getErrorAmount(ErrorMessage.MessageType.INTERNAL_ERROR)

        val exitStatus =
            if (error) getFancyMessage("FAIL", ANSI::red)
            else getFancyMessage(
                "SUCCESS",
                if (internalErrors == 0) ANSI::green else ANSI::purple
            )

        val timeDeltaSecs = (END_TIME - START_TIME) / 1000.0

        val lines = listOf(
            *exitStatus.toTypedArray(),
            "",
            //"HTML files compiled: $HTML_COMPILED",
            "Warnings: $warningsNum",
            "Internal errors: $internalErrNum",
            if (!error && internalErrors != 0)
                "A non-fatal internal error occurred, but the output might not be what you want" else null,
            "",
            "Time took: ${if (START_TIME == -1L) "not measured" else String.format("%.3f", timeDeltaSecs) + "s"}"
        )

        println(lines.filterNotNull().joinToString("\n"));
    }

    private fun getFancyMessage(msg: String, modifier: (String) -> String): List<String> {
        val margin = 2;
        val lines = listOf(
            "┏${"━".repeat(msg.length + margin * 2)}┓",
            "┃${space(margin)}$msg${space(margin)}┃",
            "┗${"━".repeat(msg.length + margin * 2)}┛"
        )
        return lines.map(modifier);
    }

    private fun getErrorOccurs(type: ErrorMessage.MessageType, transform: (String) -> String): String {
        val occurrences = getErrorAmount(type);
        return if (occurrences == 0) ANSI.green("0") else transform(""+ERROR_TYPES_COUNT[type])
    }

    private fun getErrorAmount(type: ErrorMessage.MessageType): Int = ERROR_TYPES_COUNT[type] ?: 0;

    companion object {
        fun space(n: Int): String = " ".repeat(n);
    }
}
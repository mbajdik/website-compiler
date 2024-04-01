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

package me.mbajdik.webcompiler.util

object TerminalUtils {
    fun yesOrNo(default: Boolean = true, prompt: String, force: Boolean = false): Boolean {
        var valid = false;
        var value = false;
        while (!valid) {
            print("$prompt [${if (default) "Y/n" else "y/N"}] ");
            val userIn = readln();

            if (userIn.length < 2) {
                when (userIn) {
                    "y", "Y" -> {valid = true; value = true};
                    "n", "N" -> {valid = true; value = false};
                    "" -> { if (!force) {valid = true; value = default} }
                }
            }
        }

        return value
    }
}
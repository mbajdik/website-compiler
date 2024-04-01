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

object ANSI {
    val BLACK = "\u001B[30m"
    val RED = "\u001B[31m"
    val GREEN = "\u001B[32m"
    val YELLOW = "\u001B[33m"
    val BLUE = "\u001B[34m"
    val PURPLE = "\u001B[35m"
    val CYAN = "\u001B[36m"
    val WHITE = "\u001B[37m"
    val GRAY = "\u001B[90m"

    val RESET = "\u001B[0m"

    private fun surround(code: String, s: String): String = "$code$s$RESET"
    fun black(s: String): String = surround(BLACK, s);
    fun red(s: String): String = surround(RED, s);
    fun green(s: String): String = surround(GREEN, s);
    fun yellow(s: String): String = surround(YELLOW, s);
    fun blue(s: String): String = surround(BLUE, s);
    fun purple(s: String): String = surround(PURPLE, s);
    fun cyan(s: String): String = surround(CYAN, s);
    fun white(s: String): String = surround(WHITE, s);
    fun gray(s: String): String = surround(GRAY, s);
}
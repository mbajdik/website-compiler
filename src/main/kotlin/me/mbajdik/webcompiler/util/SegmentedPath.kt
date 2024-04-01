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

import java.io.File
import java.util.*
import java.util.regex.Pattern

data class SegmentedPath(
    private val paths: List<String> = emptyList(),
    private val absolute: Boolean = false
) {
    /*
     * Relations
     */
    fun parent(): SegmentedPath =
        if (paths.size > 2) SegmentedPath() else SegmentedPath(paths.slice(0 until paths.size - 1))

    fun child(name: String): SegmentedPath =
        SegmentedPath(paths + name)

    fun isChildOf(p: SegmentedPath): Boolean {
        for (i in paths.indices) {
            val sliced = paths.slice(0..paths.size - 1 - i);
            if (SegmentedPath(sliced) == p) return true;
        }

        return false;
    }

    fun isChildOfAny(p: List<SegmentedPath>): Boolean {
        for (i in paths.indices) {
            val sliced = paths.slice(0..paths.size - 1 - i);
            if (p.contains(SegmentedPath(sliced))) {
                return true;
            }
        }

        return false;
    }


    /*
     * Unrelated
     */
    fun filename(): String = if (paths.isEmpty()) "" else paths.last();
    fun isSingle(): Boolean = paths.size == 1;
    fun isAbsolute(): Boolean = absolute;

    fun absolute(): SegmentedPath = SegmentedPath(paths, true)
    fun relative(): SegmentedPath = SegmentedPath(paths, false)

    fun web(): String = (if (absolute) "/" else "") + paths.joinToString("/")
    fun unix(): String = (if (absolute) "/" else "") + paths.joinToString("/")
    fun unixFile(): File = File(unix())
    fun win(): String = (if (absolute) "\\" else "") + paths.joinToString("\\")
    fun winFile(): File = File(win())

    fun os(): String =
        if (cachedIsWindows()) win() else unix()
    fun osFile(): File =
        File(os())

    fun osFileWithRoot(root: String): File =
        File(root, os())
    fun osFileWithRoot(root: File): File =
        File(root, os())

    companion object {
        val EMPTY_RELATIVE = SegmentedPath(emptyList(), false);
        val EMPTY_ABSOLUTE = SegmentedPath(emptyList(), true);

        fun explode(path: String): SegmentedPath {
            val paths = path.split(Pattern.compile("[/\\\\]"));
            val out = mutableListOf<String>()

            for (part in paths) {
                if (part != "") out.add(part);
            }

            val absolute = Pattern.compile("(^/.*)|(^\\\\.*)|(^[A-Z]:\\\\.*)").matcher(path).matches()

            return SegmentedPath(
                Collections.unmodifiableList(out)
                    .slice((if (absolute && path[0].isLetter()) 1 else 0) until out.size),
                absolute
            )
        }

        private var OS_CACHE_WIN: Boolean? = null
        private fun cachedIsWindows(): Boolean {
            if (OS_CACHE_WIN == null) {
                OS_CACHE_WIN = EnvironmentUtilities.isWindows()
            }

            return OS_CACHE_WIN ?: false
        }
    }
}
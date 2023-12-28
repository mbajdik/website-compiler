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

package me.mbajdik.webcompiler.util

import java.io.File
import java.util.*
import java.util.regex.Pattern

data class SegmentedPath(
    private val paths: List<String> = emptyList()
) {
    fun parent(): SegmentedPath {
        return if (paths.size > 2) SegmentedPath() else SegmentedPath(paths.slice(0 until paths.size - 1))
    }

    fun child(name: String): SegmentedPath {
        return SegmentedPath(paths + name)
    }

    fun filename(): String = if (paths.isEmpty()) "" else paths.last();

    fun isSingle(): Boolean = paths.size == 1;

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

    fun file(): File {
        var out: File? = null;

        for (path in paths) {
            out = if (out == null) File(path) else File(out, path)
        }

        return out ?: File("");
    }

    fun withRoot(root: String): File = File(root, file().toString());
    fun withRoot(root: File): File = File(root, file().toString());

    companion object {
        val EMPTY = SegmentedPath(emptyList());

        fun explode(path: String): SegmentedPath {
            val paths = path.split(Pattern.compile("[/\\\\]"));
            val out = mutableListOf<String>()

            for (part in paths) {
                if (part != "") out.add(part);
            }

            return SegmentedPath(Collections.unmodifiableList(out));
        }
    }
}
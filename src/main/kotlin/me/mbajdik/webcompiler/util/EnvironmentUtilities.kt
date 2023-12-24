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
import kotlin.collections.HashMap

object EnvironmentUtilities {
    private var PATH_CACHE: HashMap<String, String>? = null;


    fun getFullExecutablePath(exec: String): String? {
        if (PATH_CACHE == null) walkPath();

        return PATH_CACHE!![exec];
    }


    private fun walkPath() {
        val osName = System.getProperty("os.name").lowercase(Locale.getDefault());
        val pathVar = if (osName.contains("windows")) System.getenv("Path") else System.getenv("PATH");

        pathVar ?: return;

        val pathDirs = pathVar.split(":");
        val cache = hashMapOf<String, String>()

        for (dir in pathDirs) {
            val children = File(dir).listFiles() ?: continue;

            for (member in children) {
                if (!member.isFile || !member.canExecute()) continue;

                cache[member.name] = member.toPath().toAbsolutePath().toString();
            }
        }

        PATH_CACHE = cache;
    }
}
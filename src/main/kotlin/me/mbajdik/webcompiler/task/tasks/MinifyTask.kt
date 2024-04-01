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

package me.mbajdik.webcompiler.task.tasks

import me.mbajdik.webcompiler.compiler.minifier.HTMLMinifierCompat
import me.mbajdik.webcompiler.state.Manager
import me.mbajdik.webcompiler.task.Task
import me.mbajdik.webcompiler.task.helpers.WebLocalFileHandler

class MinifyTask(
    val manager: Manager,
    private val displayPath: String,
    val unminified: String,
    val nodePath: String?,
    val minifierPath: String?,

    val minifyJS: Boolean,
    val minifyCSS: Boolean
): Task() {
    private var minified: String? = null;

    // Alternate - just for fun
    constructor(
        manager: Manager,
        handler: WebLocalFileHandler,
        unminified: String,
        nodePath: String?,
        minifierPath: String?,

        minifyJS: Boolean,
        minifyCSS: Boolean
    ) : this(manager, handler.getDisplayPath(), unminified, nodePath, minifierPath, minifyJS, minifyCSS)

    override fun getDisplayPath(): String = displayPath;
    override fun getTaskTypeName(): String = "HTML minify"


    fun minify(): String {
        if (minified == null) minified = HTMLMinifierCompat.minifyHTML(this);
        return minified!!;
    }
}
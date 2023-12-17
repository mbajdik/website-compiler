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

package me.mbajdik.webcompiler.task.tasks

import me.mbajdik.webcompiler.state.Statistics
import me.mbajdik.webcompiler.compiler.minifier.HTMLMinifierCompat
import me.mbajdik.webcompiler.compiler.processor.HTML
import me.mbajdik.webcompiler.state.Manager
import me.mbajdik.webcompiler.task.helpers.WebLocalFileHandler

class HTMLProcessTask constructor(
    val manager: Manager,
    val handler: WebLocalFileHandler
): CompileTask(manager, handler) {
    private var processed: String? = null;
    private var minified: String? = null;

    override fun subtask(path: String): HTMLProcessTask = HTMLProcessTask(manager, handler.fileRelative(path))
    override fun getTaskTypeName(): String = "HTML compile"


    fun process(): String {
        if (processed == null) {
            processed = HTML.process(this)
            manager.statistics.HTML_COMPILED.incrementAndGet();
        };
        return processed!!;
    }

    fun minify(options: List<String>): String {
        if (processed == null) process();
        if (minified == null) minified = HTMLMinifierCompat.minifyHTML(MinifyTask(manager, handler, processed!!, options));
        return minified!!;
    }
}
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
import me.mbajdik.webcompiler.compiler.processor.HTML
import me.mbajdik.webcompiler.make.MakeConfig
import me.mbajdik.webcompiler.state.Manager
import me.mbajdik.webcompiler.task.helpers.WebLocalFileHandler
import me.mbajdik.webcompiler.util.SegmentedPath

class HTMLProcessTask constructor(
    val manager: Manager,
    val handler: WebLocalFileHandler,

    val addJS: List<String>,
    val addCSS: List<String>,
    val addToHeader: List<String>,

    val footerHTML: String?,
    val autoTitle: MakeConfig.AutoTitleMode,
    val encoding: String?
): CompileTask(manager, handler) {
    init {
        if (handler.isLocal()) manager.setSeenSite(SegmentedPath.explode(handler.path()).relative())
    }

    override fun subtask(path: String): HTMLProcessTask = HTMLProcessTask(
        manager = manager,
        handler = handler.fileRelative(path),
        addJS = addJS,
        addCSS = addCSS,
        addToHeader = addToHeader,
        footerHTML = footerHTML,
        autoTitle = autoTitle,
        encoding = encoding
    )

    override fun getTaskTypeName(): String = "HTML compile"


    fun process(): String {
        manager.statistics.HTML_COMPILED.incrementAndGet()

        return HTML.process(this);
    }

    fun minifiedProcess(
        nodePath: String? = null,
        minifierPath: String? = null,

        minifyJS: Boolean = true,
        minifyCSS: Boolean = true
    ): String {
        val task = MinifyTask(
            manager = manager,
            handler = handler,
            unminified = process(),
            nodePath = nodePath,
            minifierPath = minifierPath,

            minifyJS = minifyJS,
            minifyCSS = minifyCSS
        )

        return HTMLMinifierCompat.minifyHTML(task)
    }
}
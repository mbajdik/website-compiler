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

import me.mbajdik.webcompiler.state.Manager
import me.mbajdik.webcompiler.task.helpers.WebLocalFileHandler
import me.mbajdik.webcompiler.util.SegmentedPath

class JavascriptProcessTask constructor(
    val manager: Manager,
    val handler: WebLocalFileHandler
): CompileTask(manager, handler) {
    init {
        if (handler.isLocal()) manager.setSeenSite(SegmentedPath.explode(handler.path()).relative())
    }

    override fun subtask(path: String): JavascriptProcessTask = JavascriptProcessTask(manager, handler.fileRelative(path));
    override fun getTaskTypeName(): String = "JS import"


    // Adding a semicolon for good measure (will probably not cause a problem, since breaking JS files is an awful thing
    fun process(): String = handler.fileContentsString(manager) + ";";
}
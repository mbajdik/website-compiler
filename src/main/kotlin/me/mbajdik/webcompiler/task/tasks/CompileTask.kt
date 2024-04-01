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
import me.mbajdik.webcompiler.task.Task
import me.mbajdik.webcompiler.task.helpers.WebLocalFileHandler

abstract class CompileTask constructor(
    private val manager: Manager,
    private val handler: WebLocalFileHandler,
) : Task() {
    abstract fun subtask(path: String): CompileTask;

    override fun getDisplayPath(): String = handler.getDisplayPath();
    override fun toString(): String = " task in ${getDisplayPath()}"

    fun subtaskCSS(path: String): CSSProcessTask = CSSProcessTask(manager, handler).subtask(path)
    fun subtaskJS(path: String): JavascriptProcessTask = JavascriptProcessTask(manager, handler).subtask(path)

    companion object {
        //fun html(manager: Manager, handler: WebLocalFileHandler): HTMLProcessTask = HTMLProcessTask(manager, handler);
        fun css(manager: Manager, handler: WebLocalFileHandler): CSSProcessTask = CSSProcessTask(manager, handler);
    }
}
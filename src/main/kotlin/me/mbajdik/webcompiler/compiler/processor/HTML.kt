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

package me.mbajdik.webcompiler.compiler.processor

import me.mbajdik.webcompiler.state.data.ErrorMessage
import me.mbajdik.webcompiler.task.tasks.HTMLProcessTask
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

object HTML {
    fun process(task: HTMLProcessTask): String {
        val document = Jsoup.parse(task.handler.fileContentsString(task.manager));
        val modified = processNode(task, document).toString();

        return modified;
    }

    private fun processNode(task: HTMLProcessTask, node: Element): Element {
        when (node.tagName()) {
            "link" -> {
                if (node.attributes().hasKey("rel") && node.attributes()["rel"] == "stylesheet" && node.attributes().hasKey("href")) {
                    val href = node.attributes()["href"]

                    node.tagName("style")
                    node.attributes().remove("rel")
                    node.attributes().remove("href")

                    val subtask = task.subtaskCSS(href);

                    val output = subtask.process();
                    node.html(output.output)

                    for (embedded in output.embedded) {
                        val styleElem = Element("style")
                        styleElem.html(embedded)

                        if (node.parent() != null) node.parent()!!.appendChild(styleElem)
                    }
                }

                return node
            }
            "script" -> {
                if (node.attributes().hasKey("src")) {
                    if (node.attributes().hasKey("type") && node.attributes().get("type") == "module") {
                        task.manager.pushErrorMessage(
                            task,
                            ErrorMessage.MessageType.INTERNAL_ERROR,
                            "JavaScript modules are unsupported (inconsistent)",
                            ErrorMessage.CodeSnippet("type=\"module\"", 6), // a bit artificial
                            null,
                            null,
                            false
                        );
                    }

                    val src = node.attributes()["src"]

                    node.attributes().remove("src")

                    val subtask = task.subtaskJS(src);
                    node.html(subtask.process());
                }

                return node
            }
            else -> {
                val modified = mutableListOf<Element?>()
                for (element in node.children()) {
                    modified.add(processNode(task, element))
                }

                node.children().clear()
                node.children().addAll(modified)

                return node
            }
        }
    }
}
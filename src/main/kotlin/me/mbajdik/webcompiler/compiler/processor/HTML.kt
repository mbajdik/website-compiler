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

package me.mbajdik.webcompiler.compiler.processor

import me.mbajdik.webcompiler.make.MakeConfig
import me.mbajdik.webcompiler.state.data.ErrorMessage
import me.mbajdik.webcompiler.task.tasks.HTMLProcessTask
import me.mbajdik.webcompiler.util.SegmentedPath
import org.jsoup.Jsoup
import org.jsoup.nodes.Comment
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node

object HTML {
    fun process(task: HTMLProcessTask): String {
        val document = Jsoup.parse(task.handler.fileContentsString(task.manager));
        val rootState = CompilerState(
            addedSources = task.addJS.isEmpty() && task.addCSS.isEmpty(),
            addedFooter = task.footerHTML == null,
            addedEncoding = task.encoding == null,
            addedHeaderElements = task.addToHeader.isEmpty(),
            foundTitle = null
        )

        val imported = processImport(task, document)

        if (imported !is Element) task.manager.pushErrorMessage(
            task = task,
            type = ErrorMessage.MessageType.ERROR,
            message = "Root of HTML is a comment",
            snippet = null,
            exit = true
        );

        val modified = processNode(task, imported as Element, rootState);

        var rebuilt = modified;

        if (!rootState.addedSources) rebuilt = addElementListToParentRoot(
            parentTag = "head",
            doc = rebuilt,
            elementList = getSourceNodes(task),
            prepend = false
        )

        if (!rootState.addedFooter && task.footerHTML != null) rebuilt = addElementListToParentRoot(
            parentTag = "body",
            doc = rebuilt,
            elementList = listOf(getFooterNode(task)),
            prepend = false
        )

        if (!rootState.addedFooter && task.encoding != null) rebuilt = addElementListToParentRoot(
            parentTag = "head",
            doc = rebuilt,
            elementList = listOf(
                Element("meta").apply {
                    attr("charset", task.encoding)
                }
            ),
            prepend = false
        )

        if (!rootState.addedHeaderElements) rebuilt = addListToParentRoot(
            parentTag = "head",
            doc = rebuilt,
            addableList = task.addToHeader,
            adder = {parent, addable -> parent.append(addable)}
        )

        if (task.autoTitle != MakeConfig.AutoTitleMode.NONE && rootState.foundTitle != null) {
            if (rootState.foundH1Tag && !rootState.foundTitleTag &&
                (task.autoTitle == MakeConfig.AutoTitleMode.TITLE || task.autoTitle == MakeConfig.AutoTitleMode.BOTH)) {

                rebuilt = addTitleToHead(rebuilt, rootState.foundTitle!!)
            } else if (rootState.foundTitleTag && !rootState.foundH1Tag &&
                (task.autoTitle == MakeConfig.AutoTitleMode.H1 || task.autoTitle == MakeConfig.AutoTitleMode.BOTH)) {

                rebuilt = addH1TitleToBody(rebuilt, rootState.foundTitle!!)
            }
        }

        return rebuilt.toString();
    }

    private fun processImport(task: HTMLProcessTask, node: Node): Node {
        for (element in node.childNodes()) {
            if (element is Comment && element.data.startsWith("@import-html")) {
                if (node.ownerDocument() != null) task.manager.pushErrorMessage(
                    task = task,
                    type = ErrorMessage.MessageType.ERROR,
                    message = "Cannot import entire document, only body-fragments",
                    exit = true
                )

                val split = element.data.split(" ");

                if (split.size == 1) task.manager.pushErrorMessage(
                    task = task,
                    type = ErrorMessage.MessageType.ERROR,
                    message = "Import statement has no arguments",
                    snippet = ErrorMessage.CodeSnippet.fromCode(element.data, 0),
                    exit = true
                )

                val dest = split.slice(1 until split.size).joinToString(" ");

                val subHandler = task.handler.fileRelative(dest);
                val elementsToInclude = Jsoup.parseBodyFragment(subHandler.fileContentsString(task.manager)).body().children();

                elementsToInclude.forEach { element.before(it) }
                element.remove()

                if (subHandler.isLocal()) task.manager.setImported(SegmentedPath.explode(subHandler.path()).relative())

                continue;
            }

            processImport(task, element)
        }

        return node;
    }

    private fun processNode(task: HTMLProcessTask, node: Element, state: CompilerState): Element {
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
                            task = task,
                            type = ErrorMessage.MessageType.INTERNAL_ERROR,
                            message = "JavaScript modules are unsupported (import inconsistency)",
                            snippet = ErrorMessage.CodeSnippet("type=\"module\"", 6), // a bit artificial
                            exit = false
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
                if (node.tagName() == "title") {
                    state.foundTitleTag = true;
                    if (state.foundTitle == null) state.foundTitle = node.html();
                }

                if (node.tagName() == "h1") {
                    state.foundH1Tag = true;
                    if (state.foundTitle == null) state.foundTitle = node.html();
                }

                if (!state.addedSources && node.tagName() == "head") {
                    node.appendChildren(getSourceNodes(task));
                    state.addedSources = true;
                }

                if (!state.addedFooter && task.footerHTML != null && node.tagName() == "body") {
                    node.appendChild(getFooterNode(task));
                    state.addedFooter = true;
                }

                if (!state.addedEncoding && task.encoding != null && node.tagName() == "head") {
                    node.appendChild(getEncodingNode(task));
                    state.addedEncoding = true;
                }

                if (!state.addedHeaderElements && node.tagName() == "head") {
                    task.addToHeader.forEach { node.append(it) };
                    state.addedHeaderElements = true;
                }

                val modified = mutableListOf<Element?>()
                for (element in node.children()) {
                    val res = processNode(task, element, state)

                    modified.add(res)
                }

                node.children().clear()
                node.children().addAll(modified)

                return node
            }
        }
    }

    private fun getSourceNodes(task: HTMLProcessTask): List<Element> {
        val out = mutableListOf<Element>()

        for (jsSource in task.addJS) {
            val subtaskJS = task.subtaskJS(jsSource);

            val scriptElem = Element("script");
            scriptElem.html(subtaskJS.process());

            out.add(scriptElem);
        }

        for (cssSource in task.addCSS) {
            val subtaskCSS = task.subtaskCSS(cssSource);
            val outputCSS = subtaskCSS.process();

            for (stylesheet in listOf(outputCSS.output) + outputCSS.embedded) {
                val styleElem = Element("style");
                styleElem.html(stylesheet);

                out.add(styleElem);
            }
        }

        return out
    }

    private fun getFooterNode(task: HTMLProcessTask): Element {
        if (task.footerHTML == null) return Element("span");

        val footerElem = Element("footer");
        footerElem.html(task.footerHTML);

        return footerElem;
    }

    private fun getEncodingNode(task: HTMLProcessTask): Element {
        if (task.encoding == null) return Element("span");

        val metaElem = Element("meta");
        metaElem.attr("charset", task.encoding)

        return metaElem;
    }

    private fun addH1TitleToBody(doc: Element, titleHTML: String): Element =
        addElementToParentInRoot("body", doc, "h1", titleHTML, true);

    private fun addTitleToHead(doc: Element, titleHTML: String): Element =
        addElementToParentInRoot("head", doc, "title", titleHTML, false);

    private fun addElementToParentInRoot(
        parentTag: String,
        doc: Element,
        elementTag: String,
        elementHTML: String,
        prepend: Boolean
    ): Element = addElementListToParentRoot(
        parentTag = parentTag,
        doc = doc,
        elementList = listOf(Element(elementTag).apply { html(elementHTML) }),
        prepend = prepend
    )

    private fun addElementListToParentRoot(
        parentTag: String,
        doc: Element,
        elementList: List<Element>,
        prepend: Boolean
    ): Element = addListToParentRoot(
        parentTag = parentTag,
        doc = doc,
        addableList = elementList,
        adder = if (prepend) {parent, addable -> parent.prependChild(addable)} else {parent, addable -> parent.appendChild(addable)}
    )

    private fun <T> addListToParentRoot(
        parentTag: String,
        doc: Element,
        addableList: List<T>,
        adder: (Element, T) -> Unit,
    ): Element {
        var added = false;

        fun recurseAdd(node: Element): Element {
            if (!added) {
                if (node.tagName() == parentTag) {
                    addableList.forEach { adder(node, it) }
                    added = true;

                    return node;
                } else {
                    val newChildren = mutableListOf<Element>()
                    for (child in node.children()) {
                        newChildren.add(recurseAdd(child))
                    }

                    node.children().clear();
                    node.children().addAll(newChildren);

                    return node;
                }
            } else return node;
        }

        val firstIteration = recurseAdd(doc);

        if (!added) {
            val newChildren = mutableListOf<Element>()
            for (node in firstIteration.children()) {
                if (node.tagName() == "html") {
                    val newParent = Element(parentTag);

                    addableList.forEach { adder(node, it) }

                    node.appendChild(newParent);

                    newChildren.add(node);
                    break;
                } else newChildren.add(node);
            }

            firstIteration.children().clear();
            firstIteration.children().addAll(newChildren);
        }

        return firstIteration;
    }



    data class CompilerState(
        var addedSources: Boolean = false,
        var addedFooter: Boolean = false,
        var addedEncoding: Boolean = false,
        var addedHeaderElements: Boolean = false,

        var foundTitle: String? = null,
        var foundH1Tag: Boolean = false,
        var foundTitleTag: Boolean = false,
    )
}
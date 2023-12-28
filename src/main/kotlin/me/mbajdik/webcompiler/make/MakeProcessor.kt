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

package me.mbajdik.webcompiler.make

import me.mbajdik.webcompiler.state.Manager
import me.mbajdik.webcompiler.task.helpers.WebLocalFileHandler
import me.mbajdik.webcompiler.task.tasks.HTMLProcessTask
import me.mbajdik.webcompiler.util.JSONUtil
import me.mbajdik.webcompiler.util.SegmentedPath
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.collections.HashMap

class MakeProcessor(
    private val manager: Manager,
    private val config: MakeConfig,
    private val root: String,
    private val explicitThreads: Int? = null,
) {
    fun processed(): HashMap<SegmentedPath, ByteArray> = processFiles();

    fun processedZip(): ByteArray {
        val bos = ByteArrayOutputStream();
        val zos = ZipOutputStream(bos);

        for ((path, contents) in processed()) {
            val pathString = path.file().toString();
            val e = ZipEntry(pathString);

            zos.putNextEntry(e);
            zos.write(contents, 0, contents.size);
            zos.closeEntry();
        }

        zos.close();

        return bos.toByteArray();
    }

    private fun processFiles(): HashMap<SegmentedPath, ByteArray> {
        val collected = recurseCollect(SegmentedPath.EMPTY);

        val finalThreads = explicitThreads ?: config.threads

        val taskerHTML = ConcurrentLinkedQueue(collected.html);
        val taskerOther = ConcurrentLinkedQueue(collected.other);
        val receiver = ConcurrentHashMap<SegmentedPath, ByteArray>();

        val runningThreads = AtomicInteger(0);

        fun processorThread(main: Boolean = false) {
            runningThreads.incrementAndGet();
            if (!main) manager.pushThread(Thread.currentThread());

            while (!taskerHTML.isEmpty()) {
                val path = taskerHTML.poll() ?: continue;
                receiver[path] = handleCompile(path);
            }

            while (!taskerOther.isEmpty()) {
                val path = taskerOther.poll() ?: continue;

                if (config.unusedMode() && manager.getSeenSite(path)) continue;

                receiver[path] = handleOther(path);
            }

            if (!main) manager.popThread(Thread.currentThread());
            runningThreads.decrementAndGet();
        }

        for (tasks in 0 until finalThreads - 1) {
            val thread = Thread {
                processorThread();
            };

            thread.start();
        }

        manager.pushThread(Thread.currentThread());
        processorThread(main = true);

        while (runningThreads.get() != 0) {/*Wait*/} // Should have completed

        manager.popThread(Thread.currentThread())

        return HashMap(receiver);
    }

    private fun recurseCollect(parentPath: SegmentedPath): MakeTaskCollection {
        val children = parentPath.withRoot(root).listFiles() ?: emptyArray<File>();

        val outHTML = mutableListOf<SegmentedPath>()
        val outOther = mutableListOf<SegmentedPath>()

        for (child in children) {
            if (config.ignoreHidden && isFileHidden(child)) continue;

            val path = parentPath.child(child.name);

            // disregarding the makefile - only this for now
            if (isIgnored(path)) continue;

            if (child.isFile) {
                val includeHTML = isHTML(child.name) && checkIncludeHTML(path);
                val includeOther = checkIncludeOther(path, child);

                if (!includeHTML && !includeOther) continue;

                if (includeHTML) {
                    outHTML.add(path)
                } else {
                    outOther.add(path)
                }
            } else if (child.isDirectory) {
                val subCollection = recurseCollect(path);

                outHTML.addAll(subCollection.html);
                outOther.addAll(subCollection.other);
            }
        }

        return MakeTaskCollection(
            html = Collections.unmodifiableList(outHTML),
            other = Collections.unmodifiableList(outOther)
        );
    }


    private fun handleCompile(path: SegmentedPath): ByteArray {
        val handler = WebLocalFileHandler.local(
            root = root,
            path = path.file().toString()
        )

        val task = HTMLProcessTask(manager = manager, handler = handler)

        val contents = if (config.minifyHTML) task.minifiedProcess(
            options = config.minifierOptions,
            nodePath = config.nodePath,
            minifierPath = config.minifierPath,
        ) else task.process();

        return contents.toByteArray();
    }

    private fun handleOther(path: SegmentedPath): ByteArray {
        val handler = WebLocalFileHandler.local(
            root = root,
            path = path.file().toString()
        )

        val bytes = handler.fileBytes(manager);

        return if (getFileExtension(path.filename()) == "json" && config.otherMinifyJSON) {
            JSONUtil.General.minifyJSON(String(bytes)).toByteArray();
        } else {
            bytes;
        }
    }

    private fun checkIncludeHTML(childPath: SegmentedPath): Boolean {
        var include = false;
        when (config.mode) {
            MakeConfig.Mode.TRAVERSE -> {include = true}
            MakeConfig.Mode.ROOT_ONLY -> {if (childPath.isSingle()) include = true;}
            MakeConfig.Mode.MANUAL -> {
                include = childPath.isChildOfAny(config.manualFiles);
            }
        }
        return include;
    }

    private fun checkIncludeOther(childPath: SegmentedPath, child: File): Boolean {
        var includeOther = false;
        when (config.otherMode) {
            MakeConfig.OtherMode.ALL -> {includeOther = true};
            MakeConfig.OtherMode.UNUSED_SITE -> {includeOther = true};
            MakeConfig.OtherMode.NO_SITE -> {if (!isSiteFile(child.name)) includeOther = true};
            MakeConfig.OtherMode.ASSETS -> {if (isAssetFile(child.name)) includeOther = true};
            MakeConfig.OtherMode.MANUAL -> {
                var validManual = false;
                when (config.otherManualMode) {
                    MakeConfig.OtherManualMode.ALL -> {validManual = true};
                    MakeConfig.OtherManualMode.UNUSED_SITE -> {validManual = true};
                    MakeConfig.OtherManualMode.NO_SITE -> {if (!isSiteFile(child.name)) validManual = true};
                    MakeConfig.OtherManualMode.ASSETS -> {if (isAssetFile(child.name)) validManual = true};
                }

                if (validManual) {
                    includeOther = childPath.isChildOfAny(config.otherManualFiles);
                }
            };
            MakeConfig.OtherMode.NONE -> {};
        }

        return includeOther;
    }

    data class MakeTaskCollection(val html: List<SegmentedPath>, val other: List<SegmentedPath>)

    companion object {
        private val HTMLs = listOf("html", "shtml");
        private val SITE_FILES = listOf("html", "shtml", "css", "js");
        private val ASSET_FILES = listOf("png", "jpg", "jpeg", "ico", "gif", "svg", "zip", "json", "toml", "xml", "yml", "yaml");
        private val IGNORED: List<SegmentedPath> = listOf(SegmentedPath.explode("wmake.json"))

        fun isHTML(name: String): Boolean {
            val ext = getFileExtension(name);
            return HTMLs.contains(ext);
        }

        fun isSiteFile(name: String): Boolean {
            val ext = getFileExtension(name);
            return SITE_FILES.contains(ext);
        }

        fun isAssetFile(name: String): Boolean {
            val ext = getFileExtension(name);
            return ASSET_FILES.contains(ext);
        }

        fun isIgnored(paths: SegmentedPath): Boolean {
            return IGNORED.contains(paths);
        }

        fun getFileExtension(name: String): String {
            val i = name.lastIndexOf(".");
            if (i > 0 && name.length > i+1) return name.substring(i+1)
            return "";
        }

        fun isFileHidden(file: File): Boolean {
            return file.name.startsWith(".") || file.isHidden;
        }
    }
}
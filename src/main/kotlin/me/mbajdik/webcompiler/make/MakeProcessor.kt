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
import me.mbajdik.webcompiler.util.PathUtilities
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class MakeProcessor(
    private val manager: Manager,
    private val config: MakeConfig,
    private val root: String,
    private val explicitThreads: Int? = null,
) {
    fun processed(): HashMap<List<String>, ByteArray> = processFiles();

    fun processedZip(): ByteArray {
        val baos = ByteArrayOutputStream();
        val zos = ZipOutputStream(baos);

        for ((paths, contents) in processed()) {
            val path = PathUtilities.joinPathList(paths).path.toString();
            val e = ZipEntry(path);

            zos.putNextEntry(e);
            zos.write(contents, 0, contents.size);
            zos.closeEntry();
        }

        zos.close();

        return baos.toByteArray();
    }

    private fun processFiles(): HashMap<List<String>, ByteArray> {
        val collected = recurseCollect(emptyList());

        val finalThreads = explicitThreads ?: config.threads

        val tasker = ConcurrentLinkedQueue(collected);
        val receiver = ConcurrentHashMap<List<String>, ByteArray>();

        fun processorThread(main: Boolean = false) {
            while (!tasker.isEmpty()) {
                if (!main) manager.pushThread(Thread.currentThread());

                val task = tasker.poll();
                receiver[task.paths] = handleFile(task.paths, task.type);

                if (!main) manager.popThread(Thread.currentThread());
            }
        }

        for (tasks in 0 until finalThreads - 1) {
            val thread = Thread {
                processorThread();
            };

            thread.start();
        }

        manager.pushThread(Thread.currentThread());
        processorThread(main = true);

        while (receiver.keys.size != collected.size) {/*Wait*/} // Should have completed

        return HashMap(receiver);
    }

    private fun recurseCollect(parentPaths: List<String>): List<MakeTask> {
        val children = PathUtilities.joinPathListWithRoot(root, parentPaths).listFiles() ?: emptyArray<File>();
        val out = mutableListOf<MakeTask>()

        for (child in children) {
            if (config.ignoreHidden && PathUtilities.isFileHidden(child)) continue;

            val childPaths = parentPaths + child.name;

            // disregarding the makefile - only this for now
            if (isIgnored(childPaths)) continue;

            if (child.isFile) {
                val includeHTML = isHTML(child.name) && checkIncludeHTML(childPaths);
                val includeOther = checkIncludeOther(childPaths, child, includeHTML);

                if (!includeHTML && !includeOther) continue;

                val type = if (includeHTML) MakeType.COMPILE else MakeType.OTHER;

                out += MakeTask(childPaths, type);
            } else if (child.isDirectory) {
                out += recurseCollect(childPaths);
            }
        }

        return out;
    }

    private fun handleFile(childPaths: List<String>, type: MakeType): ByteArray {
        when (type) {
            MakeType.COMPILE -> {
                val handler = WebLocalFileHandler.local(root, PathUtilities.joinPathList(childPaths).toString());
                val task = HTMLProcessTask(manager, handler)
                val processed = task.process();
                val contents = if (config.minifyHTML) task.minify(config.minifierOptions) else processed;
                return contents.toByteArray();
            }
            MakeType.OTHER -> {
                val handler = WebLocalFileHandler.local(root, PathUtilities.joinPathList(childPaths).toString());
                val bytes = handler.fileBytes(manager);

                return if (PathUtilities.getFileExtension(childPaths.last()) == "json" && config.otherMinifyJSON) {
                    JSONUtil.General.minifyJSON(String(bytes)).toByteArray();
                } else {
                    bytes;
                }
            }
        }
    }

    private fun checkIncludeHTML(childPaths: List<String>): Boolean {
        var include = false;
        when (config.mode) {
            MakeConfig.Mode.TRAVERSE -> {include = true}
            MakeConfig.Mode.ROOT_ONLY -> {if (childPaths.size == 1) include = true;}
            MakeConfig.Mode.MANUAL -> {
                include = isParentDirValid(config.manualFiles, childPaths);
                /*if (config.manualFiles.contains(childPaths)) {
                    include = true;
                }*/
            }
        }
        return include;
    }

    private fun checkIncludeOther(childPaths: List<String>, child: File, includesHTML: Boolean): Boolean {
        var includeOther = false;
        when (config.otherMode) {
            MakeConfig.OtherMode.ALL -> {includeOther = true};
            MakeConfig.OtherMode.NO_SITE -> {if (!isSiteFile(child.name)) includeOther = true};
            MakeConfig.OtherMode.ASSETS -> {if (isAssetFile(child.name)) includeOther = true};
            MakeConfig.OtherMode.MANUAL -> {
                var validManual = false;
                when (config.otherManualMode) {
                    MakeConfig.OtherManualMode.ALL -> {validManual = true};
                    MakeConfig.OtherManualMode.NO_SITE -> {if (!isSiteFile(child.name)) validManual = true};
                    MakeConfig.OtherManualMode.ASSETS -> {if (isAssetFile(child.name)) validManual = true};
                }

                if (validManual) {
                    includeOther = isParentDirValid(config.otherManualFiles, childPaths);
                    /*parentLoop@ for (i in childPaths.indices) {
                        val sliced = childPaths.slice(0..childPaths.size - 1 - i);
                        if (config.otherManualFiles.contains(sliced)) {
                            includeOther = true
                            break@parentLoop;
                        };
                    }*/
                }
            };
            MakeConfig.OtherMode.NONE -> {};
        }

        return includeOther;
    }

    enum class MakeType { COMPILE, OTHER }
    data class MakeTask(val paths: List<String>, val type: MakeType);

    companion object {
        private val HTMLs = listOf("html", "shtml");
        private val SITE_FILES = listOf("html", "shtml", "css", "js");
        private val ASSET_FILES = listOf("png", "jpg", "jpeg", "ico", "gif", "svg", "zip", "json", "toml", "xml", "yml", "yaml");
        private val IGNORED: List<List<String>> = listOf(listOf("wmake.json"))

        fun isHTML(name: String): Boolean {
            val ext = PathUtilities.getFileExtension(name);
            return HTMLs.contains(ext);
        }

        fun isSiteFile(name: String): Boolean {
            val ext = PathUtilities.getFileExtension(name);
            return SITE_FILES.contains(ext);
        }

        fun isAssetFile(name: String): Boolean {
            val ext = PathUtilities.getFileExtension(name);
            return ASSET_FILES.contains(ext);
        }

        fun isIgnored(paths: List<String>): Boolean {
            return IGNORED.contains(paths);
        }

        fun isParentDirValid(allowed: List<List<String>>, current: List<String>): Boolean {
            for (i in current.indices) {
                val sliced = current.slice(0..current.size - 1 - i);
                if (allowed.contains(sliced)) {
                    return true;
                }
            }

            return false;
        }
    }
}
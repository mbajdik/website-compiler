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

package me.mbajdik.webcompiler.state

import me.mbajdik.webcompiler.state.data.ErrorMessage
import me.mbajdik.webcompiler.state.data.PanicCallback
import me.mbajdik.webcompiler.task.helpers.DebugInformationSupplier
import me.mbajdik.webcompiler.task.helpers.WebLocalFileHandler
import me.mbajdik.webcompiler.util.SegmentedPath
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.exitProcess

class Manager constructor(
    val logger: Logger = Logger(),
    val statistics: Statistics = Statistics(),

    val freeSTDOUT: Boolean = true,
    val quiet: Boolean = true,
    val cli: Boolean = true,

    private val panicActive: AtomicInteger = AtomicInteger(0),
    private val panicCallbacks: MutableList<PanicCallback> = mutableListOf(),

    private val errorQueue: ConcurrentLinkedQueue<ErrorMessage> = ConcurrentLinkedQueue(),
    private val usedThreads: ConcurrentHashMap<Thread, Unit> = ConcurrentHashMap(),
    private val cachedFiles: ConcurrentHashMap<WebLocalFileHandler, ByteArray> = ConcurrentHashMap(),
    private val seenSiteFiles: ConcurrentHashMap<SegmentedPath, Unit> = ConcurrentHashMap(),
    private val imported: ConcurrentHashMap<SegmentedPath, Unit> = ConcurrentHashMap()
) {
    fun pushErrorMessage(
        task: DebugInformationSupplier,
        type: ErrorMessage.MessageType = ErrorMessage.MessageType.WARNING,
        message: String = "an error occurred",
        snippet: ErrorMessage.CodeSnippet? = null,
        description: List<String>? = null,
        exception: Throwable? = null,
        exit: Boolean = false
    ) {
        val msg = ErrorMessage(task, type, message, snippet, description, exception);

        errorQueue.add(msg)
        statistics.ERROR_TYPES_COUNT.putIfAbsent(type, 0)
        statistics.ERROR_TYPES_COUNT[type] = (statistics.ERROR_TYPES_COUNT[type] ?: 0) + 1;
        logger.logErrorMessage(msg)

        if (exit || type == ErrorMessage.MessageType.ERROR) panicError(msg);
    }

    private fun stringifyErrorQueue(): String {
        val stringWriter = StringWriter();
        val writer = PrintWriter(stringWriter);

        for (message in errorQueue) {
            val msg = message.toString(true);

            writer.println(msg);
        }

        return stringWriter.buffer.toString()
    }

    private fun printErrorQueue() {
        val queue = stringifyErrorQueue();

        if (!freeSTDOUT) {
            System.err.print(queue)
        } else {
            print(queue)
        }

        if (!errorQueue.isEmpty()) println();
    }

    private fun panicError(message: ErrorMessage) {
        // Two threads were killing each other
        if (panicActive.incrementAndGet() > 1) {
            while (true) {/*Hang thread*/}
        }

        // Stopping all threads - in this case it's NOT THAT dangerous
        for (thread in usedThreads.keys()) {
            if (thread != Thread.currentThread() && thread.isAlive) {
                thread.stop();
            }
        }

        for (cb in panicCallbacks) cb.onErrorPanic(this, message);
        exit(true)
    }

    fun addPanicCallback(cb: PanicCallback) = panicCallbacks.add(cb);


    fun init() {
        statistics.start();
    }

    fun exit(error: Boolean = false) {
        statistics.stop();
        logger.write();

        if (cli) {
            if (error || !quiet) {
                printErrorQueue()
                if (freeSTDOUT) statistics.printStatistics(error)
            }

            exitProcess(if (error) 1 else 0)
        }
    }


    fun cacheFile(file: WebLocalFileHandler, contents: ByteArray) = cachedFiles.put(file, contents);
    fun readCached(file: WebLocalFileHandler): ByteArray? = cachedFiles[file];


    fun pushThread(thread: Thread) = usedThreads.put(thread, Unit);
    fun popThread(thread: Thread) = usedThreads.remove(thread);


    fun setSeenSite(path: SegmentedPath) = seenSiteFiles.put(path, Unit);
    fun getSeenSite(path: SegmentedPath): Boolean = seenSiteFiles.containsKey(path.relative());


    fun setImported(path: SegmentedPath) = imported.put(path.relative(), Unit);
    fun getImported(path: SegmentedPath): Boolean = imported.containsKey(path.relative());
    fun imported(): List<SegmentedPath> = HashMap(imported).keys.toList();
}
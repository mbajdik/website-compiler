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

package me.mbajdik.webcompiler.util

import java.io.File
import java.nio.file.Files
import java.nio.file.Path

object FileUtilities {
    fun writeToFileSafe(bytes: ByteArray, file: File, freeSTDOUT: Boolean = false) {
        if (file.exists()) {
            if (file.isDirectory) {
                val msg = "Output file is a directory: $file";

                if (freeSTDOUT) {
                    println(msg)
                } else {
                    System.err.println(msg);
                }
            } else if (file.isFile) {
                if (freeSTDOUT) {
                    if (TerminalUtils.yesOrNo(true, "File already exists ($file), replace?")) {
                        tryWriteToFile(file, bytes, true);
                    } else {
                        println("Skipped saving to file ($file)")
                    }
                } else {
                    System.err.println("Output file already exists: $file")
                }
            }
        } else {
            tryWriteToFile(file, bytes, freeSTDOUT);
        }
    }

    fun tryWriteToFile(file: File, bytes: ByteArray, freeSTDOUT: Boolean) {
        makeParentSafe(file);

        try {
            Files.write(file.toPath(), bytes);
        } catch (e: Exception) {
            val msg = "A(n) ${e.javaClass.name} occurred while saving to file ($file)";

            if (freeSTDOUT) {
                println(msg)
            } else {
                System.err.println(msg);
            }
        }
    }

    fun makeParentSafe(file: File) {
        if (file.parentFile != null) file.parentFile.mkdirs();
    }
}
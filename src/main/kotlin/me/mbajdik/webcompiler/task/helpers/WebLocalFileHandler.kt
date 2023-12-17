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

package me.mbajdik.webcompiler.task.helpers

import me.mbajdik.webcompiler.state.Manager
import me.mbajdik.webcompiler.state.data.ErrorMessage
import me.mbajdik.webcompiler.util.PathUtilities
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.net.MalformedURLException
import java.net.URI
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path

class WebLocalFileHandler private constructor(
    private val baseURI: URI?,
    private val absoluteRootPath: String?,
    private val path: String,
) : DebugInformationSupplier {
    private fun getLocalPath(): Path {
        return File(absoluteRootPath!!, path).toPath();
    }

    fun fileBytes(manager: Manager): ByteArray {
        // Supported protocol(s): HTTP, HTTPS
        if (baseURI != null) {
            val fullURI = PathUtilities.uriWithPath(baseURI, path);
            when (baseURI.scheme) {
                "http", "https" -> {
                    val bos = ByteArrayOutputStream()
                    // try with HTTP if HTTPS is used
                    val uri = PathUtilities.uriWithScheme(fullURI, "http");

                    try {
                        val url = uri.toURL()
                        val stream = url.openStream()
                        stream.transferTo(bos);
                    } catch (e: MalformedURLException) {
                        manager.pushErrorMessage(
                            this,
                            ErrorMessage.MessageType.INTERNAL_ERROR,
                            "a URL was not formatted accordingly",
                            null,
                            listOf("URI: $uri"),
                            e,
                            true,
                        )
                    } catch (e: IOException) {
                        manager.pushErrorMessage(
                            this,
                            ErrorMessage.MessageType.ERROR,
                            "file couldn't be downloaded due to an IOException",
                            null,
                            null,
                            e,
                        )
                    }

                    return bos.toByteArray();
                }
                else -> {
                    manager.pushErrorMessage(
                        this,
                        ErrorMessage.MessageType.ERROR,
                        "incompatible protocol (only HTTP(S) is supported)",
                        null,
                        listOf("URI: $fullURI", "Protocol: ${fullURI.scheme}"),
                        null,
                    )
                }
            }
        } else {
            val filepath = getLocalPath();
            return try {
                Files.readAllBytes(filepath);
            } catch (e: NoSuchFileException) {
                manager.pushErrorMessage(
                    this,
                    ErrorMessage.MessageType.ERROR,
                    "file was not found",
                    null,
                    listOf("Path: $filepath"),
                    e,
                )
                ByteArray(0)
            } catch (e: IOException) {
                manager.pushErrorMessage(
                    this,
                    ErrorMessage.MessageType.ERROR,
                    "file couldn't be read due to an IOException",
                    null,
                    listOf("Path: $filepath"),
                    e,
                )
                ByteArray(0)
            }
        }

        return ByteArray(0);
    }

    fun fileContentsString(manager: Manager): String {
        return String(fileBytes(manager))
    }


    fun fileRelative(newPath: String): WebLocalFileHandler {
        val newURI = URI(newPath);

        // Check if anything of significance changed
        return if
            // Scenario 1: already a remote location, the authority changed
           ((baseURI != null && !PathUtilities.uriPathlessEquals(baseURI, newURI) && !PathUtilities.onlyPathURI(newURI)) ||
            // Scenario 2: local location, a remote location is needed
            (baseURI == null && !(newURI.scheme == null || newURI.scheme == "")))
        {
            WebLocalFileHandler(
                PathUtilities.pathlessURI(newURI),
                null,
                newURI.path
            );
        } else {
            // If nothing changed - just concatenate the paths
            WebLocalFileHandler(
                baseURI,
                absoluteRootPath,
                PathUtilities.joinPaths(path, newURI.path)
            );
        }
    }

    override fun getDisplayPath(): String = if (baseURI == null) path else PathUtilities.uriWithPath(baseURI, path).toString()
    override fun getTaskTypeName(): String = "File handler";

    companion object {
        fun local(root: String, path: String): WebLocalFileHandler {
            return WebLocalFileHandler(null, root, path);
        }

        fun remote(uriString: String): WebLocalFileHandler {
            val uri = URI(uriString);
            return WebLocalFileHandler(PathUtilities.pathlessURI(uri), null, uri.path);
        }
    }
}
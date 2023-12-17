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
import java.net.URI
import java.util.Collections
import java.util.regex.Pattern

object PathUtilities {
    fun onlyPathURI(uri: URI): Boolean {
        return uri.scheme == null && uri.authority == null && uri.query == null && uri.fragment == null;
    }

    fun uriWithScheme(uri: URI, scheme: String): URI {
        return URI(scheme, uri.authority, uri.path, uri.query, uri.fragment);
    }

    fun uriWithPath(uri: URI, path: String): URI {
        return URI(uri.scheme, uri.authority, path, uri.query, uri.fragment);
    }

    fun pathlessURI(old: URI): URI {
        return URI(old.scheme, old.authority, null, old.query, old.fragment)
    }

    fun uriPathlessEquals(a: URI?, b: URI?): Boolean {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.scheme == b.scheme && a.authority == b.authority && a.query == b.query && a.fragment == b.fragment;
    }




    fun joinPaths(current: String, new: String): String {
        return if (new.startsWith("/")) new else File(if (current.endsWith("/")) current else File(current).parent, new).toPath().toString();
    }

    fun joinPathList(paths: List<String>): File {
        var out: File? = null;

        for (path in paths) {
            out = if (out == null) File(path) else File(out, path)
        }

        return out ?: File("");
    }

    fun joinPathListWithRoot(root: String, paths: List<String>): File {
        return File(root, joinPathList(paths).path.toString());
    }

    fun pathToList(path: String): List<String> {
        val paths = path.split(Pattern.compile("[/\\\\]"));
        val out = mutableListOf<String>()

        for (part in paths) {
            if (part != "") out.add(part);
        }

        return Collections.unmodifiableList(out);
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
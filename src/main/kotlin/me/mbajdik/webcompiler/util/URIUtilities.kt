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

package me.mbajdik.webcompiler.util

import java.net.URI

object URIUtilities {
    fun onlyPathURI(uri: java.net.URI): Boolean {
        return uri.scheme == null && uri.authority == null && uri.query == null && uri.fragment == null;
    }

    fun uriWithScheme(uri: java.net.URI, scheme: String): java.net.URI {
        return URI(scheme, uri.authority, uri.path, uri.query, uri.fragment);
    }

    fun uriWithPath(uri: java.net.URI, path: String): java.net.URI {
        return URI(uri.scheme, uri.authority, path, uri.query, uri.fragment);
    }

    fun pathlessURI(old: java.net.URI): java.net.URI {
        return URI(old.scheme, old.authority, null, old.query, old.fragment)
    }

    fun uriPathlessEquals(a: java.net.URI?, b: java.net.URI?): Boolean {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.scheme == b.scheme && a.authority == b.authority && a.query == b.query && a.fragment == b.fragment;
    }
}
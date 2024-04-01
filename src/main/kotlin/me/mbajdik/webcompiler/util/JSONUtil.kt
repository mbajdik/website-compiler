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

import com.google.gson.*
import java.lang.reflect.Type
import java.util.*


object JSONUtil {
    // Deserialization part
    object De {
        fun safeObjectRoute(obj: JsonObject?, vararg route: String): JsonElement? {
            var current = obj;
            for (key in route) {
                val sub = obj?.get(key);
                if (sub !is JsonObject) return null; // could throw error

                current = sub;
            }
            return current;
        }

        fun safeJsonObject(elem: JsonElement?): JsonObject? {
            return if (elem is JsonObject) elem else null;
        }

        fun safeBoolean(elem: JsonElement?, default: Boolean): Boolean {
            return if (elem != null && elem is JsonPrimitive && elem.isBoolean) elem.asBoolean else default;
        }

        fun safeInt(elem: JsonElement?, default: Int): Int {
            return if (elem != null && elem is JsonPrimitive && elem.isNumber) elem.asInt else default;
        }

        fun safeString(elem: JsonElement?): String? {
            return if (elem != null && elem is JsonPrimitive && elem.isString) elem.asString else null;
        }

        fun safeArray(elem: JsonElement?): List<String> {
            return if (elem != null && elem.isJsonArray) arrayToStringList(elem.asJsonArray) else emptyList();
        }

        inline fun <reified T : Enum<T>> safeEnum(elem: JsonElement?, default: T): T {
            return if (elem != null && elem is JsonPrimitive && elem.isString) {
                try {
                    enumValueOf(elem.asString.uppercase())
                } catch (e: Exception) {
                    default
                }
            } else default;
        }

        fun safeArrayArray(elem: JsonElement?): List<List<String>> {
            if (elem == null || elem !is JsonArray) return emptyList();

            val out = mutableListOf<List<String>>();
            for (subElem in elem) {
                if (!isSafeArray(subElem)) continue;

                out.add(safeArray(subElem));
            }

            return Collections.unmodifiableList(out);
        }

        fun isSafeArray(elem: JsonElement?): Boolean {
            return elem != null && elem is JsonArray;
        }

        private fun arrayToStringList(array: JsonArray): List<String> {
            val output = mutableListOf<String>()

            for (option in array) {
                if (option is JsonPrimitive && option.isString) output.add(option.asString);
            }

            return output;
        }
    }

    object General {
        fun minifyJSON(input: String): String {
            val serializer = JsonSerializer { src: String, _: Type, _: JsonSerializationContext ->
                JsonPrimitive(src.trim());
            };

            val gson = GsonBuilder().registerTypeAdapter(String::class.java, serializer).create();
            val jsonElement = gson.fromJson(input, JsonElement::class.java);
            return gson.toJson(jsonElement);
        }
    }
}
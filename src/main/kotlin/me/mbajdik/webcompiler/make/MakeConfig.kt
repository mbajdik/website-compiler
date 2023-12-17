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

import com.google.gson.JsonObject
import me.mbajdik.webcompiler.util.JSONUtil
import me.mbajdik.webcompiler.util.PathUtilities
import java.util.Collections

class MakeConfig(json: JsonObject) {
    val minifyHTML: Boolean;
    val minifierOptions: List<String>;

    val mode: Mode;
    val manualFiles: List<List<String>>;
    val ignoreHidden: Boolean;
    val threads: Int;

    val hookRunner: List<String>;
    val preBuildHooks: List<List<String>>;
    val proBuildHooks: List<List<String>>;

    val otherMode: OtherMode;
    val otherManualFiles: List<List<String>>;
    val otherManualMode: OtherManualMode;
    val otherMinifyJSON: Boolean;

    init {
        val jsonMinifier = JSONUtil.De.safeJsonObject(JSONUtil.De.safeObjectRoute(json, "minifier"));
        val jsonMinifierOptions = jsonMinifier?.get("options");

        this.minifyHTML = JSONUtil.De.isSafeArray(jsonMinifierOptions);
        this.minifierOptions = if (minifyHTML) JSONUtil.De.safeArray(jsonMinifierOptions) else emptyList()



        val jsonMake = JSONUtil.De.safeJsonObject(JSONUtil.De.safeObjectRoute(json, "make"));
        val jsonMakeMode = jsonMake?.get("mode");
        val jsonMakeManual = jsonMake?.get("manual");
        val jsonMakeIgnoreHidden = jsonMake?.get("ignore_hidden");
        val jsonMakeThreads = jsonMake?.get("threads");

        this.mode = JSONUtil.De.safeEnum(jsonMakeMode, Mode.ROOT_ONLY);
        this.manualFiles = processPathList(JSONUtil.De.safeArray(jsonMakeManual));
        this.ignoreHidden = JSONUtil.De.safeBoolean(jsonMakeIgnoreHidden, true);
        this.threads = JSONUtil.De.safeInt(jsonMakeThreads, 1);



        val jsonMakeHooks = JSONUtil.De.safeJsonObject(JSONUtil.De.safeObjectRoute(jsonMake, "hooks"));
        val jsonMakeHooksPreBuild = jsonMakeHooks?.get("pre_build");
        val jsonMakeHooksProBuild = jsonMakeHooks?.get("post_build");
        val jsonMakeHookRunner = jsonMake?.get("hook_runner");

        this.hookRunner = JSONUtil.De.safeArray(jsonMakeHookRunner);
        this.preBuildHooks = JSONUtil.De.safeArrayArray(jsonMakeHooksPreBuild);
        this.proBuildHooks = JSONUtil.De.safeArrayArray(jsonMakeHooksProBuild);



        val jsonMakeOther = JSONUtil.De.safeJsonObject(JSONUtil.De.safeObjectRoute(jsonMake, "other"));
        val jsonMakeOtherMode = jsonMakeOther?.get("mode");
        val jsonMakeOtherManual = jsonMakeOther?.get("manual");
        val jsonMakeOtherManualMode = jsonMakeOther?.get("manual_mode");
        val jsonMakeOtherMinifyJSON = jsonMakeOther?.get("minify_json");

        this.otherMode = JSONUtil.De.safeEnum(jsonMakeOtherMode, OtherMode.NO_SITE);
        this.otherManualFiles = processPathList(JSONUtil.De.safeArray(jsonMakeOtherManual));
        this.otherManualMode = JSONUtil.De.safeEnum(jsonMakeOtherManualMode, OtherManualMode.ALL);
        this.otherMinifyJSON = JSONUtil.De.safeBoolean(jsonMakeOtherMinifyJSON, true);
    }

    enum class Mode { TRAVERSE, ROOT_ONLY, MANUAL }
    enum class OtherMode { ALL, NO_SITE, ASSETS, MANUAL, NONE }
    enum class OtherManualMode { ALL, NO_SITE, ASSETS }

    companion object {
        fun processPathList(fulls: List<String>): List<List<String>> {
            val out = mutableListOf<List<String>>()

            for (full in fulls) {
                out.add(PathUtilities.pathToList(full));
            }

            return Collections.unmodifiableList(out);
        }
    }
}

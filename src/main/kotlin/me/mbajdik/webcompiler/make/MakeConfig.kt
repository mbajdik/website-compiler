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
import me.mbajdik.webcompiler.util.URIUtilities
import me.mbajdik.webcompiler.util.SegmentedPath
import java.util.Collections

class MakeConfig(json: JsonObject) {
    val nodePath: String?;
    val minifierPath: String?;
    val minifyHTML: Boolean;
    val minifierOptions: List<String>;

    val root: String?;
    val outputType: OutputType;
    val output: String?;
    val mode: Mode;
    val manualFiles: List<SegmentedPath>;
    val ignoreHidden: Boolean;
    val threads: Int;

    val hookRunner: List<String>;
    val preBuildHooks: List<List<String>>;
    val proBuildHooks: List<List<String>>;

    val otherMode: OtherMode;
    val otherManualFiles: List<SegmentedPath>;
    val otherManualMode: OtherManualMode;
    val otherMinifyJSON: Boolean;

    init {
        val jsonMinifier = JSONUtil.De.safeJsonObject(JSONUtil.De.safeObjectRoute(json, "minifier"));
        val jsonMinifierOptions = jsonMinifier?.get("options");
        val jsonMinifierNodePath = jsonMinifier?.get("node_path");
        val jsonMinifierMinifierPath = jsonMinifier?.get("minifier_path");

        this.minifyHTML = JSONUtil.De.isSafeArray(jsonMinifierOptions);
        this.minifierOptions = if (minifyHTML) JSONUtil.De.safeArray(jsonMinifierOptions) else emptyList()
        this.nodePath = userRelativePath(JSONUtil.De.safeString(jsonMinifierNodePath));
        this.minifierPath = userRelativePath(JSONUtil.De.safeString(jsonMinifierMinifierPath));



        val jsonMake = JSONUtil.De.safeJsonObject(JSONUtil.De.safeObjectRoute(json, "make"));
        val jsonRoot = jsonMake?.get("root");
        val jsonOutputType = jsonMake?.get("output_type");
        val jsonOutput = jsonMake?.get("output");
        val jsonMakeMode = jsonMake?.get("mode");
        val jsonMakeManual = jsonMake?.get("manual");
        val jsonMakeIgnoreHidden = jsonMake?.get("ignore_hidden");
        val jsonMakeThreads = jsonMake?.get("threads");

        this.root = JSONUtil.De.safeString(jsonRoot);
        this.outputType = JSONUtil.De.safeEnum(jsonOutputType, OutputType.DIR);
        this.output = JSONUtil.De.safeString(jsonOutput);
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

    fun unusedMode(): Boolean {
        val baseUnused = otherMode == OtherMode.UNUSED_SITE
        val baseManual = otherMode == OtherMode.MANUAL
        val manualUnused = otherManualMode == OtherManualMode.UNUSED_SITE

        return baseUnused || (baseManual && manualUnused);
    }

    enum class OutputType { DIR, ZIP }
    enum class Mode { TRAVERSE, ROOT_ONLY, MANUAL }
    enum class OtherMode { ALL, UNUSED_SITE, NO_SITE, ASSETS, MANUAL, NONE }
    enum class OtherManualMode { ALL, UNUSED_SITE, NO_SITE, ASSETS }

    companion object {
        fun processPathList(fulls: List<String>): List<SegmentedPath> {
            val out = mutableListOf<SegmentedPath>()

            for (full in fulls) {
                out.add(SegmentedPath.explode(full));
            }

            return Collections.unmodifiableList(out);
        }

        fun userRelativePath(path: String?): String? {
            if (path == null) return null;

            return if (path.startsWith("~"))
                "${System.getProperty("user.home")}${path.substring(1)}"
            else path
        }
    }
}

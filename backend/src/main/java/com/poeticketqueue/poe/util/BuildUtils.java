package com.poeticketqueue.poe.util;

import com.poeticketqueue.poe.build.Build;
import com.poeticketqueue.poe.build.BuildType;
import com.poeticketqueue.poe.importer.BuildImporterResult;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class BuildUtils {

    public static List<Build> extractBuildsFromRawImportData(List<BuildImporterResult> importResults, Consumer<String> publishMessageCallback) {
        List<Build> results = new ArrayList<>();
        for (BuildImporterResult importResult : importResults) {
            publishMessageCallback.accept("Loading: " + importResult.rawBuildImportData());
            BuildType buildType = BuildType.deriveFromString(importResult.rawBuildImportData());
            if (buildType == null) {
                throw new IllegalArgumentException("Could not determine build type for: " + importResult.rawBuildImportData());
            }
            results.add(Build.of(buildType, importResult, false));
        }
        return results;
    }

    public static List<Build> extractBuildsFromRawImportData(List<BuildImporterResult> importResults) {
        return extractBuildsFromRawImportData(importResults, s -> {});
    }
}

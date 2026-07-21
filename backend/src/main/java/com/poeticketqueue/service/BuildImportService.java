package com.poeticketqueue.service;

import com.poeticketqueue.model.QueuedBuild;
import com.poeticketqueue.poe.build.Build;
import com.poeticketqueue.poe.build.BuildType;
import com.poeticketqueue.poe.build.PoeVersion;
import com.poeticketqueue.poe.build.PoeVersionDelegatingGameVersion;
import com.poeticketqueue.poe.importer.BuildImporterResult;
import com.poeticketqueue.poe.item.Item;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class BuildImportService {

    private static final Logger log = LoggerFactory.getLogger(BuildImportService.class);

    private static final Set<String> ALLOWED_HOSTS = Set.of("pobb.in", "www.pobb.in");

    private static boolean isAllowedPobbinUrl(String url) {
        try {
            URI uri = URI.create(url.trim());
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null) {
                return false;
            }
            if (!scheme.equalsIgnoreCase("https")) {
                return false;
            }
            if (uri.getUserInfo() != null) {
                return false;
            }
            return ALLOWED_HOSTS.contains(host.toLowerCase());
        } catch (Exception e) {
            return false;
        }
    }

    public BuildImportOutcome importBuild(String url, PoeVersion groupVersion) {
        if (StringUtils.isBlank(url)) {
            return BuildImportOutcome.invalidUrl("Please paste a pobb.in build URL.");
        }

        if (!isAllowedPobbinUrl(url)) {
            return BuildImportOutcome.invalidUrl("Only pobb.in build links are supported.");
        }

        BuildType type = BuildType.deriveFromString(url);
        if (type == null) {
            return BuildImportOutcome.invalidUrl("Only pobb.in build links are supported.");
        }

        Build build;
        try {
            build = Build.of(type, new BuildImporterResult(null, url));
        } catch (Exception e) {
            log.warn("importBuild() -- failed to parse build from {}", url, e);
            return BuildImportOutcome.invalidUrl("Could not read that build from pobb.in. Check the link and try again.");
        }

        if (!(build instanceof PoeVersionDelegatingGameVersion versioned)) {
            return BuildImportOutcome.invalidUrl("Could not determine the Path of Exile version of that build.");
        }
        PoeVersion detected = versioned.poeVersion();

        if (detected != groupVersion) {
            return BuildImportOutcome.versionMismatch("This build is for Path of Exile " + label(detected)
                    + " but the group is Path of Exile " + label(groupVersion) + ".");
        }

        List<Item> items = new ArrayList<>(build.getItems());
        return BuildImportOutcome.success(new QueuedBuild(build.getName(), url, detected, items));
    }

    private static String label(PoeVersion v) {
        if (v == PoeVersion.POE2) {
            return "2";
        }
        return "1";
    }

    public record BuildImportOutcome(Status status, QueuedBuild build, String message) {
        public enum Status { SUCCESS, VERSION_MISMATCH, INVALID_URL }

        static BuildImportOutcome success(QueuedBuild build) {
            return new BuildImportOutcome(Status.SUCCESS, build, null);
        }

        static BuildImportOutcome versionMismatch(String msg) {
            return new BuildImportOutcome(Status.VERSION_MISMATCH, null, msg);
        }

        static BuildImportOutcome invalidUrl(String msg) {
            return new BuildImportOutcome(Status.INVALID_URL, null, msg);
        }
    }
}

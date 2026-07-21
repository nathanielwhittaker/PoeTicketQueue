package com.poeticketqueue.poe.importer;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Fetches pobb.in build pages while guarding against redirect-based SSRF.
 *
 * <p>Unlike {@link com.poeticketqueue.poe.util.Web}, this fetcher disables OkHttp's automatic
 * redirect-following and instead validates every hop (initial URL AND every redirect target)
 * against the same host allowlist, using the SAME URL parser ({@link HttpUrl}) that the request
 * itself uses. This closes both known SSRF vectors: (1) a 3xx redirect to a non-allowlisted host
 * being followed unvalidated, and (2) a parser differential between the validator and the fetcher.
 */
public final class PobbinImportFetcher {

    private static final Logger log = LoggerFactory.getLogger(PobbinImportFetcher.class);

    public static final Set<String> ALLOWED_HOSTS = Set.of("pobb.in", "www.pobb.in");

    private static final int MAX_REDIRECTS = 5;

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36";

    private static final OkHttpClient IMPORT_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            // SECURITY-CRITICAL: do not remove. Disabling these re-enables OkHttp's automatic
            // redirect-following, which would follow a 3xx Location header without per-hop
            // allowlist validation, reopening the redirect-based SSRF this fetcher guards against.
            .followRedirects(false)
            .followSslRedirects(false)
            .build();

    private PobbinImportFetcher() {
    }

    // Accepted residual risk: this is a string-only validation of the URL's host. There is a
    // TOCTOU window between this check and the eventual DNS resolution/connect performed by
    // OkHttp, so a DNS-rebinding attack against pobb.in's own DNS is not defended against here —
    // we trust pobb.in's DNS not to resolve to an internal address.
    public static boolean isAllowedUrl(String url) {
        if (url == null) {
            return false;
        }
        HttpUrl httpUrl = HttpUrl.parse(url.trim());
        if (httpUrl == null) {
            return false;
        }
        if (!"https".equalsIgnoreCase(httpUrl.scheme())) {
            return false;
        }
        if (!httpUrl.username().isEmpty() || !httpUrl.password().isEmpty()) {
            return false;
        }
        return ALLOWED_HOSTS.contains(httpUrl.host().toLowerCase(Locale.ROOT));
    }

    public static String fetch(String url) {
        return fetch(IMPORT_CLIENT, url);
    }

    static String fetch(OkHttpClient client, String url) {
        String currentUrl = url;
        int redirectsFollowed = 0;

        while (true) {
            if (!isAllowedUrl(currentUrl)) {
                log.warn("fetch() -- rejecting disallowed pobb.in URL: {}", currentUrl);
                return null;
            }

            Request request = new Request.Builder()
                    .url(currentUrl)
                    .header("User-Agent", USER_AGENT)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isRedirect()) {
                    if (++redirectsFollowed > MAX_REDIRECTS) {
                        log.warn("fetch() -- exceeded max redirects ({}) fetching pobb.in URL: {}", MAX_REDIRECTS, url);
                        return null;
                    }
                    String location = response.header("Location");
                    if (location == null) {
                        return null;
                    }
                    HttpUrl resolved = response.request().url().resolve(location);
                    if (resolved == null) {
                        return null;
                    }
                    currentUrl = resolved.toString();
                    continue;
                }

                return response.body() != null ? response.body().string() : null;
            } catch (IOException e) {
                return null;
            }
        }
    }
}

package com.poeticketqueue.poe.importer;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Deterministic, network-FREE tests for {@link PobbinImportFetcher}.
 *
 * <p>Redirect behavior is exercised via a synthetic {@link Interceptor} that returns in-process
 * {@link Response} objects (no real socket, no {@code MockWebServer} — okhttp's {@code mockwebserver}
 * artifact is not on the classpath).
 */
class PobbinImportFetcherTest {

    private static Response redirectResponse(okhttp3.Request request, String location) {
        return new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(302)
                .message("Found")
                .header("Location", location)
                .body(ResponseBody.create(MediaType.get("text/plain"), ""))
                .build();
    }

    private static Response okResponse(okhttp3.Request request, String body) {
        return new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(ResponseBody.create(MediaType.get("text/html; charset=utf-8"), body))
                .build();
    }

    @Test
    void redirectToNonAllowlistedInternalHost_isRejected_andInternalHostIsNeverRequested() {
        AtomicInteger internalHostCalls = new AtomicInteger();

        Interceptor interceptor = chain -> {
            HttpUrl url = chain.request().url();
            if (url.host().equals("169.254.169.254")) {
                internalHostCalls.incrementAndGet();
                return okResponse(chain.request(), "should never be reached");
            }
            return redirectResponse(chain.request(), "http://169.254.169.254/latest/meta-data/");
        };
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

        String result = PobbinImportFetcher.fetch(client, "https://pobb.in/abc");

        assertThat(result).isNull();
        assertThat(internalHostCalls.get()).isZero();
    }

    @Test
    void redirectToNonAllowlistedExternalHost_isRejected() {
        Interceptor interceptor = chain ->
                redirectResponse(chain.request(), "https://evil.example.com/");
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

        String result = PobbinImportFetcher.fetch(client, "https://pobb.in/abc");

        assertThat(result).isNull();
    }

    @Test
    void normalPobbinFetch_returnsBody() {
        String expectedBody = "<html>build page</html>";
        Interceptor interceptor = chain -> okResponse(chain.request(), expectedBody);
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

        String result = PobbinImportFetcher.fetch(client, "https://pobb.in/abc");

        assertThat(result).isEqualTo(expectedBody);
    }

    @Test
    void sameHostRedirectChain_isFollowed_andReturnsFinalBody() {
        String expectedBody = "<html>final page</html>";
        Interceptor interceptor = chain -> {
            HttpUrl url = chain.request().url();
            if (url.encodedPath().equals("/abc")) {
                return redirectResponse(chain.request(), "https://pobb.in/abc/final");
            }
            return okResponse(chain.request(), expectedBody);
        };
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

        String result = PobbinImportFetcher.fetch(client, "https://pobb.in/abc");

        assertThat(result).isEqualTo(expectedBody);
    }

    @Test
    void relativeRedirectLocation_isResolvedAgainstBase_staysOnPobbin_andReturnsFinalBody() {
        String expectedBody = "<html>relative redirect target</html>";
        Interceptor interceptor = chain -> {
            HttpUrl url = chain.request().url();
            if (url.encodedPath().equals("/abc")) {
                return redirectResponse(chain.request(), "/O_other");
            }
            return okResponse(chain.request(), expectedBody);
        };
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

        String result = PobbinImportFetcher.fetch(client, "https://pobb.in/abc");

        assertThat(result).isEqualTo(expectedBody);
    }

    @Test
    void redirectWithoutLocationHeader_returnsNull() {
        Interceptor interceptor = chain -> new Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(302)
                .message("Found")
                .body(ResponseBody.create(MediaType.get("text/plain"), ""))
                .build();
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

        String result = PobbinImportFetcher.fetch(client, "https://pobb.in/abc");

        assertThat(result).isNull();
    }

    @Test
    void tooManyRedirects_isRejected() {
        Interceptor interceptor = chain ->
                redirectResponse(chain.request(), chain.request().url().toString() + "/");
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

        String result = PobbinImportFetcher.fetch(client, "https://pobb.in/abc");

        assertThat(result).isNull();
    }

    @Test
    void isAllowedUrl_acceptsPlainPobbinUrl() {
        assertThat(PobbinImportFetcher.isAllowedUrl("https://pobb.in/xyz")).isTrue();
    }

    @Test
    void isAllowedUrl_acceptsWwwPobbinUrl() {
        assertThat(PobbinImportFetcher.isAllowedUrl("https://www.pobb.in/xyz")).isTrue();
    }

    @Test
    void isAllowedUrl_rejectsNonPobbinHostWithPobbinInQuery() {
        assertThat(PobbinImportFetcher.isAllowedUrl("https://evil.example.com/?x=pobb.in")).isFalse();
    }

    @Test
    void isAllowedUrl_rejectsInternalMetadataHostWithPobbinInFragment() {
        assertThat(PobbinImportFetcher.isAllowedUrl("http://169.254.169.254/#pobb.in")).isFalse();
    }

    @Test
    void isAllowedUrl_rejectsUserInfoTrickPointingAtInternalHost() {
        assertThat(PobbinImportFetcher.isAllowedUrl("https://pobb.in@169.254.169.254/")).isFalse();
    }

    @Test
    void isAllowedUrl_rejectsHttpSchemeForRealHost() {
        assertThat(PobbinImportFetcher.isAllowedUrl("http://pobb.in/O_rfho9bgUap")).isFalse();
    }

    @Test
    void isAllowedUrl_rejectsLookalikeSubdomain() {
        assertThat(PobbinImportFetcher.isAllowedUrl("https://pobb.in.attacker.com/x")).isFalse();
    }
}

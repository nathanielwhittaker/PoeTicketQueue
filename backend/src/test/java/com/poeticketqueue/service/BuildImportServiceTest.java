package com.poeticketqueue.service;

import com.poeticketqueue.poe.build.PoeVersion;
import com.poeticketqueue.service.BuildImportService.BuildImportOutcome;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Deterministic, network-FREE contract tests for {@link BuildImportService#importBuild(String, PoeVersion)}.
 *
 * <p>These cover ONLY the input-validation branches that return an outcome BEFORE any outbound
 * fetch is attempted: the blank-URL guard and the SSRF host-allowlist ({@code isAllowedPobbinUrl}).
 * Per the production code, {@code StringUtils.isBlank(url)} and {@code isAllowedPobbinUrl(url)} both
 * run and short-circuit before {@code BuildType.deriveFromString} / {@code Build.of}, so no network
 * call occurs on any of these inputs — they are fast and repeatable.
 *
 * <p>The SUCCESS and VERSION_MISMATCH paths require a live pobb.in fetch and are intentionally NOT
 * asserted here; they are covered by live verification.
 *
 * <p>The service is a stateless {@code @Service} with a no-arg constructor, so it is exercised
 * directly with {@code new BuildImportService()} — no Spring context, no mocking.
 */
class BuildImportServiceTest {

    private final BuildImportService service = new BuildImportService();

    private void assertRejected(String url, String expectedMessage) {
        BuildImportOutcome outcome = service.importBuild(url, PoeVersion.POE1);

        assertThat(outcome.status()).isEqualTo(BuildImportOutcome.Status.INVALID_URL);
        assertThat(outcome.message()).isEqualTo(expectedMessage);
        assertThat(outcome.build()).isNull();
    }

    @Test
    void emptyUrl_isRejected_withPastePrompt() {
        assertRejected("", "Please paste a pobb.in build URL.");
    }

    @Test
    void blankWhitespaceUrl_isRejected_withPastePrompt() {
        assertRejected("   ", "Please paste a pobb.in build URL.");
    }

    @Test
    void nonPobbinHostWithPobbinInQuery_isRejected_asUnsupportedLink() {
        // Substring-based "contains pobb.in" checks would WRONGLY accept this; the host allowlist rejects it.
        assertRejected("https://evil.example.com/?x=pobb.in", "Only pobb.in build links are supported.");
    }

    @Test
    void internalMetadataHostWithPobbinInFragment_isRejected_asUnsupportedLink() {
        // 169.254.169.254 is the cloud metadata endpoint — classic SSRF target; pobb.in in the fragment must not save it.
        assertRejected("http://169.254.169.254/#pobb.in", "Only pobb.in build links are supported.");
    }

    @Test
    void userInfoTrickPointingAtInternalHost_isRejected_asUnsupportedLink() {
        // "pobb.in@" is userinfo, not the host — the real host is 169.254.169.254; getUserInfo() != null rejects it.
        assertRejected("https://pobb.in@169.254.169.254/", "Only pobb.in build links are supported.");
    }

    @Test
    void httpSchemeForRealHost_isRejected_asUnsupportedLink() {
        // The allowlist requires https; a plain http link to the real host is rejected.
        assertRejected("http://pobb.in/O_rfho9bgUap", "Only pobb.in build links are supported.");
    }

    @Test
    void lookalikeSubdomain_isRejected_asUnsupportedLink() {
        // pobb.in.attacker.com has host "pobb.in.attacker.com", not in the allowlist.
        assertRejected("https://pobb.in.attacker.com/x", "Only pobb.in build links are supported.");
    }
}

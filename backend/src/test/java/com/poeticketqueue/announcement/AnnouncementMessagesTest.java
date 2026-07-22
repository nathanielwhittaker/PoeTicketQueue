package com.poeticketqueue.announcement;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The provider-neutral message rendering is the text every third-party impl sends.
 * These lock the exact wording produced for each event type.
 */
class AnnouncementMessagesTest {

    @Test
    void queued_withActor_namesTheQueuer() {
        String text = AnnouncementMessages.render(AnnouncementEvent.queued("Precious Ring", "Alice"));
        assertThat(text).isEqualTo("Alice queued Precious Ring");
    }

    @Test
    void queued_withoutActor_isImpersonal() {
        String text = AnnouncementMessages.render(AnnouncementEvent.queued("Precious Ring", null));
        assertThat(text).isEqualTo("Precious Ring was queued");
    }

    @Test
    void rejected_readsAsRejected() {
        String text = AnnouncementMessages.render(AnnouncementEvent.rejected("Precious Ring"));
        assertThat(text).isEqualTo("Precious Ring was rejected");
    }

    @Test
    void purchased_readsAsPurchased() {
        String text = AnnouncementMessages.render(AnnouncementEvent.purchased("Precious Ring"));
        assertThat(text).isEqualTo("Precious Ring was purchased");
    }
}

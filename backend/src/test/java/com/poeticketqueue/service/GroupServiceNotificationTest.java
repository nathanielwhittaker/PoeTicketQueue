package com.poeticketqueue.service;

import com.poeticketqueue.model.Group;
import com.poeticketqueue.model.PurchaseNotification;
import com.poeticketqueue.model.QueuedBuild;
import com.poeticketqueue.poe.build.PoeVersion;
import com.poeticketqueue.poe.item.Item;
import com.poeticketqueue.util.GroupCodeGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Acceptance tests for the purchase-notification (kaching) feature.
 *
 * A notification is created when a trader marks a queued/build item as BOUGHT, is
 * targeted at the screen name of the member who QUEUED that item, is delivered ONLY
 * to that queuer (never the buyer or other members), and is cleared on ack so it is
 * delivered exactly once (no replay across polls / reload).
 */
class GroupServiceNotificationTest {

    private static final String QUEUER = "Alice";
    private static final String BUYER = "Bob";
    private static final String OTHER = "Carol";

    private GroupService groupService;
    private Group group;

    @BeforeEach
    void setUp() {
        groupService = new GroupService(new GroupCodeGenerator());
        group = groupService.createGroup("Test Group", BUYER, PoeVersion.POE1, "Standard");
    }

    private Item queuedItem(String name, String queuedBy) {
        Item item = new Item(name, "RARE", "Ring");
        item.queuedBy = queuedBy;
        return item;
    }

    @Test
    void boughtItem_createsNotificationTargetedAtQueuer_deliveredOnlyToQueuer() {
        groupService.addItem(group.getCode(), queuedItem("Precious Ring", QUEUER));

        groupService.boughtItem(group.getCode(), 0);

        List<PurchaseNotification> forQueuer = groupService.notificationsFor(group, QUEUER);
        assertThat(forQueuer).hasSize(1);
        PurchaseNotification n = forQueuer.get(0);
        assertThat(n.getTargetScreenName()).isEqualTo(QUEUER);
        assertThat(n.getId()).isNotBlank();
        assertThat(n.getItemLabel()).contains("Precious Ring");
        assertThat(n.getTimestamp()).isGreaterThan(0L);

        // The buyer and unrelated members must NOT receive it.
        assertThat(groupService.notificationsFor(group, BUYER)).isEmpty();
        assertThat(groupService.notificationsFor(group, OTHER)).isEmpty();
    }

    @Test
    void boughtItem_removesItemFromQueue() {
        groupService.addItem(group.getCode(), queuedItem("Precious Ring", QUEUER));

        groupService.boughtItem(group.getCode(), 0);

        assertThat(group.getItemQueue()).isEmpty();
    }

    @Test
    void ack_clearsQueuersNotification_soItIsDeliveredExactlyOnce() {
        groupService.addItem(group.getCode(), queuedItem("Precious Ring", QUEUER));
        groupService.boughtItem(group.getCode(), 0);
        String id = groupService.notificationsFor(group, QUEUER).get(0).getId();

        groupService.ackNotifications(group.getCode(), QUEUER, List.of(id));

        // No replay on the next poll.
        assertThat(groupService.notificationsFor(group, QUEUER)).isEmpty();
    }

    @Test
    void ack_onlyClearsCallersOwnNotifications() {
        groupService.addItem(group.getCode(), queuedItem("Precious Ring", QUEUER));
        groupService.boughtItem(group.getCode(), 0);
        String id = groupService.notificationsFor(group, QUEUER).get(0).getId();

        // A different user cannot ack away the queuer's notification.
        groupService.ackNotifications(group.getCode(), OTHER, List.of(id));

        assertThat(groupService.notificationsFor(group, QUEUER)).hasSize(1);
    }

    @Test
    void boughtBuildItem_targetsTheBuildsQueuer() {
        QueuedBuild build = new QueuedBuild("My Build", "https://pobb.in/x", PoeVersion.POE1,
                List.of(new Item("Build Wand", "RARE", "Wand")));
        build.setQueuedBy(QUEUER);
        groupService.addBuild(group.getCode(), build);

        groupService.boughtBuildItem(group.getCode(), 0, 0);

        List<PurchaseNotification> forQueuer = groupService.notificationsFor(group, QUEUER);
        assertThat(forQueuer).hasSize(1);
        assertThat(forQueuer.get(0).getTargetScreenName()).isEqualTo(QUEUER);
        assertThat(groupService.notificationsFor(group, BUYER)).isEmpty();
    }

    @Test
    void boughtItem_withNoQueuer_createsNoNotification() {
        groupService.addItem(group.getCode(), queuedItem("Anonymous Ring", null));

        groupService.boughtItem(group.getCode(), 0);

        assertThat(groupService.notificationsFor(group, QUEUER)).isEmpty();
        assertThat(groupService.notificationsFor(group, BUYER)).isEmpty();
    }
}

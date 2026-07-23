package com.poeticketqueue.service;

import com.poeticketqueue.announcement.AnnouncementConfig;
import com.poeticketqueue.announcement.AnnouncementDispatcher;
import com.poeticketqueue.announcement.AnnouncementEvent;
import com.poeticketqueue.announcement.AnnouncementProvider;
import com.poeticketqueue.announcement.AnnouncementType;
import com.poeticketqueue.announcement.CapturingAnnouncementService;
import com.poeticketqueue.announcement.NoOpAnnouncementService;
import com.poeticketqueue.model.Group;
import com.poeticketqueue.model.QueuedBuild;
import com.poeticketqueue.poe.build.PoeVersion;
import com.poeticketqueue.poe.item.Item;
import com.poeticketqueue.util.GroupCodeGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies GroupService fires the RIGHT announcement event on each queue mutation:
 * QUEUED on add-item and build-import, PURCHASED on bought, REJECTED on a real reject
 * (and NOTHING on a plain silent remove/delete).
 */
class GroupServiceAnnouncementTest {

    private CapturingAnnouncementService capturing;
    private GroupService groupService;
    private String code;

    @BeforeEach
    void setUp() {
        capturing = new CapturingAnnouncementService(AnnouncementProvider.DISCORD);
        AnnouncementDispatcher dispatcher = new AnnouncementDispatcher(
                List.of(new NoOpAnnouncementService(), capturing), Runnable::run);
        groupService = new GroupService(new GroupCodeGenerator(), dispatcher);

        AnnouncementConfig config = new AnnouncementConfig();
        config.setProvider(AnnouncementProvider.DISCORD);
        config.setDiscordBotToken("bot-token");
        config.setDiscordChannelId("123");
        Group group = groupService.createGroup("G", "Bob", PoeVersion.POE1, "Standard", config);
        code = group.getCode();
    }

    private Item item(String name, String queuedBy) {
        Item i = new Item(name, "RARE", "Ring");
        i.queuedBy = queuedBy;
        return i;
    }

    @Test
    void addItem_announcesQueued() {
        groupService.addItem(code, item("Precious Ring", "Alice"));

        assertThat(capturing.events).extracting(AnnouncementEvent::type).containsExactly(AnnouncementType.QUEUED);
        assertThat(capturing.events.get(0).itemLabel()).contains("Precious Ring");
        assertThat(capturing.events.get(0).actorScreenName()).isEqualTo("Alice");
    }

    @Test
    void addBuild_announcesQueued() {
        QueuedBuild build = new QueuedBuild("My Build", "https://pobb.in/x", PoeVersion.POE1,
                List.of(new Item("Wand", "RARE", "Wand")));
        build.setQueuedBy("Alice");

        groupService.addBuild(code, build);

        assertThat(capturing.events).extracting(AnnouncementEvent::type).containsExactly(AnnouncementType.QUEUED);
    }

    @Test
    void boughtItem_announcesPurchased() {
        groupService.addItem(code, item("Precious Ring", "Alice"));
        capturing.events.clear();

        groupService.boughtItem(code, 0);

        assertThat(capturing.events).extracting(AnnouncementEvent::type).containsExactly(AnnouncementType.PURCHASED);
    }

    @Test
    void rejectItem_announcesRejected_andRemovesIt() {
        groupService.addItem(code, item("Precious Ring", "Alice"));
        capturing.events.clear();

        groupService.rejectItem(code, 0);

        assertThat(capturing.events).extracting(AnnouncementEvent::type).containsExactly(AnnouncementType.REJECTED);
        assertThat(groupService.findByCode(code).orElseThrow().getItemQueue()).isEmpty();
    }

    @Test
    void removeItem_isSilent_noAnnouncement() {
        groupService.addItem(code, item("Precious Ring", "Alice"));
        capturing.events.clear();

        groupService.removeItem(code, 0);

        assertThat(capturing.events).isEmpty();
        assertThat(groupService.findByCode(code).orElseThrow().getItemQueue()).isEmpty();
    }

    private QueuedBuild buildWith(String... itemNames) {
        List<Item> items = new java.util.ArrayList<>();
        for (String n : itemNames) {
            items.add(new Item(n, "RARE", "Ring"));
        }
        QueuedBuild build = new QueuedBuild("My Build", "https://pobb.in/x", PoeVersion.POE1, items);
        build.setQueuedBy("Alice");
        return build;
    }

    @Test
    void boughtBuildItem_announcesPurchased() {
        groupService.addBuild(code, buildWith("Wand", "Ring"));
        capturing.events.clear();

        groupService.boughtBuildItem(code, 0, 0);

        assertThat(capturing.events).extracting(AnnouncementEvent::type).containsExactly(AnnouncementType.PURCHASED);
    }

    @Test
    void rejectBuildItem_announcesRejected_andRemovesIt() {
        groupService.addBuild(code, buildWith("Wand", "Ring"));
        capturing.events.clear();

        groupService.rejectBuildItem(code, 0, 0);

        assertThat(capturing.events).extracting(AnnouncementEvent::type).containsExactly(AnnouncementType.REJECTED);
        assertThat(groupService.findByCode(code).orElseThrow().getBuildQueue().get(0).getItems()).hasSize(1);
    }

    @Test
    void removeBuildItem_isSilent_noAnnouncement() {
        groupService.addBuild(code, buildWith("Wand", "Ring"));
        capturing.events.clear();

        groupService.removeBuildItem(code, 0, 0);

        assertThat(capturing.events).isEmpty();
        assertThat(groupService.findByCode(code).orElseThrow().getBuildQueue().get(0).getItems()).hasSize(1);
    }
}

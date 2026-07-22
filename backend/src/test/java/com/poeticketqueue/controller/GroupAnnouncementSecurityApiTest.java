package com.poeticketqueue.controller;

import com.poeticketqueue.announcement.AnnouncementDispatcher;
import com.poeticketqueue.announcement.AnnouncementEvent;
import com.poeticketqueue.announcement.AnnouncementType;
import com.poeticketqueue.model.Group;
import com.poeticketqueue.poe.build.PoeVersion;
import com.poeticketqueue.poe.item.Item;
import com.poeticketqueue.service.GroupService;
import com.poeticketqueue.util.SessionAttributes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security + routing guard for the per-group announcement config:
 *  - the Discord bot token supplied at group creation is NEVER serialized back to any
 *    member (not in the create response, not in GET /groups/{code}),
 *  - a real REJECT (?reason=reject) announces a REJECTED event, while a plain silent
 *    delete announces nothing.
 */
@SpringBootTest
@AutoConfigureMockMvc
class GroupAnnouncementSecurityApiTest {

    private static final String SECRET_TOKEN = "super-secret-bot-token-DO-NOT-LEAK";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GroupService groupService;

    @MockBean
    private AnnouncementDispatcher announcementDispatcher;

    private MockHttpSession sessionFor(String screenName) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionAttributes.SCREEN_NAME, screenName);
        return session;
    }

    @Test
    void createGroupWithDiscord_neverSerializesTheBotToken() throws Exception {
        String body = "{"
                + "\"name\":\"Sec Group\",\"screenName\":\"Bob\",\"poeVersion\":\"POE1\",\"league\":\"Standard\","
                + "\"announcementProvider\":\"DISCORD\","
                + "\"discordBotToken\":\"" + SECRET_TOKEN + "\","
                + "\"discordChannelId\":\"123456\"}";

        String created = mockMvc.perform(post("/api/groups")
                        .contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(created).doesNotContain(SECRET_TOKEN);

        // The token also lives, but is never handed back on the shared GET.
        Group group = groupService.findByCode(extractCode(created)).orElseThrow();
        String fetched = mockMvc.perform(get("/api/groups/" + group.getCode()).session(sessionFor("Bob")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(fetched).doesNotContain(SECRET_TOKEN);
    }

    @Test
    void rejectReason_announcesRejected_plainDeleteIsSilent() throws Exception {
        Group group = groupService.createGroup("Route Group", "Bob", PoeVersion.POE1, "Standard");
        Item a = new Item("Reject Ring", "RARE", "Ring");
        a.queuedBy = "Alice";
        Item b = new Item("Delete Ring", "RARE", "Ring");
        b.queuedBy = "Alice";
        groupService.addItem(group.getCode(), a);
        groupService.addItem(group.getCode(), b);

        // Bob is CREATOR -> may trade. A real reject announces REJECTED.
        mockMvc.perform(delete("/api/groups/" + group.getCode() + "/items/0")
                        .param("reason", "reject").session(sessionFor("Bob")))
                .andExpect(status().isOk());
        verify(announcementDispatcher).announce(any(Group.class),
                argThat(e -> e.type() == AnnouncementType.REJECTED));

        // A plain delete announces nothing (no REJECTED event ever fires).
        mockMvc.perform(delete("/api/groups/" + group.getCode() + "/items/0")
                        .session(sessionFor("Bob")))
                .andExpect(status().isOk());
        verify(announcementDispatcher, never()).announce(any(Group.class),
                argThat(e -> e.type() == AnnouncementType.REJECTED
                        && "Delete Ring".equals(e.itemLabel())));
    }

    @Test
    void buildItem_rejectReason_announcesRejected_plainDeleteIsSilent() throws Exception {
        Group group = groupService.createGroup("Build Route Group", "Bob", PoeVersion.POE1, "Standard");
        java.util.List<Item> items = java.util.List.of(
                new Item("Reject Wand", "RARE", "Wand"),
                new Item("Delete Wand", "RARE", "Wand"));
        com.poeticketqueue.model.QueuedBuild build =
                new com.poeticketqueue.model.QueuedBuild("B", "https://pobb.in/x", PoeVersion.POE1, items);
        build.setQueuedBy("Alice");
        groupService.addBuild(group.getCode(), build);

        // A real reject on a build item announces REJECTED.
        mockMvc.perform(delete("/api/groups/" + group.getCode() + "/builds/0/items/0")
                        .param("reason", "reject").session(sessionFor("Bob")))
                .andExpect(status().isOk());
        verify(announcementDispatcher).announce(any(Group.class),
                argThat(e -> e.type() == AnnouncementType.REJECTED));

        // A plain delete on the remaining build item announces nothing.
        mockMvc.perform(delete("/api/groups/" + group.getCode() + "/builds/0/items/0")
                        .session(sessionFor("Bob")))
                .andExpect(status().isOk());
        verify(announcementDispatcher, never()).announce(any(Group.class),
                argThat(e -> e.type() == AnnouncementType.REJECTED
                        && "Delete Wand".equals(e.itemLabel())));
    }

    private static String extractCode(String json) {
        int i = json.indexOf("\"code\":\"");
        int start = i + "\"code\":\"".length();
        int end = json.indexOf('"', start);
        return json.substring(start, end);
    }
}

package com.poeticketqueue.controller;

import com.poeticketqueue.model.Group;
import com.poeticketqueue.poe.build.PoeVersion;
import com.poeticketqueue.poe.item.Item;
import com.poeticketqueue.service.GroupService;
import com.poeticketqueue.util.SessionAttributes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end (controller + serialization) guard for the purchase-notification feature.
 *
 * Locks in the exact regression surface the change touches:
 *  - GET /api/groups/{code} still returns the group's fields at the TOP level
 *    (@JsonUnwrapped) so the existing frontend keeps reading itemQueue/members/buildQueue,
 *  - the raw group-wide purchaseNotifications store is NEVER serialized (@JsonIgnore),
 *  - myPurchaseNotifications is delivered ONLY to the targeted queuer's session (not the buyer),
 *  - ack clears the notification so it is delivered exactly once.
 */
@SpringBootTest
@AutoConfigureMockMvc
class GroupNotificationApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GroupService groupService;

    private MockHttpSession sessionFor(String screenName) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionAttributes.SCREEN_NAME, screenName);
        return session;
    }

    @Test
    void getGroup_keepsFlatShape_andNeverLeaksTheRawNotificationStore() throws Exception {
        // Bob is the CREATOR (a trader); Alice queues an item.
        Group group = groupService.createGroup("Shape Group", "Bob", PoeVersion.POE1, "Standard");
        Item item = new Item("Precious Ring", "RARE", "Ring");
        item.queuedBy = "Alice";
        groupService.addItem(group.getCode(), item);

        mockMvc.perform(get("/api/groups/" + group.getCode()).session(sessionFor("Alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemQueue").isArray())
                .andExpect(jsonPath("$.members").isArray())
                .andExpect(jsonPath("$.buildQueue").isArray())
                .andExpect(jsonPath("$.code").value(group.getCode()))
                .andExpect(jsonPath("$.myPurchaseNotifications").isArray())
                // The @JsonIgnore'd group-wide store must never be serialized.
                .andExpect(jsonPath("$.purchaseNotifications").doesNotExist());
    }

    @Test
    void boughtThenPoll_deliversNotificationToQueuerOnly_thenAckClearsIt() throws Exception {
        Group group = groupService.createGroup("Deliver Group", "Bob", PoeVersion.POE1, "Standard");
        Item item = new Item("Precious Ring", "RARE", "Ring");
        item.queuedBy = "Alice";
        groupService.addItem(group.getCode(), item);
        String code = group.getCode();

        // Bob (trader) marks it bought.
        mockMvc.perform(post("/api/groups/" + code + "/items/0/bought").session(sessionFor("Bob")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemQueue").isEmpty());

        // Alice (the queuer) receives exactly one notification on her poll.
        mockMvc.perform(get("/api/groups/" + code).session(sessionFor("Alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myPurchaseNotifications.length()").value(1))
                .andExpect(jsonPath("$.myPurchaseNotifications[0].targetScreenName").value("Alice"));

        // Bob (the buyer) receives none.
        mockMvc.perform(get("/api/groups/" + code).session(sessionFor("Bob")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myPurchaseNotifications").isEmpty());

        // Capture the id, ack it as Alice, and confirm no replay on the next poll.
        String id = groupService.notificationsFor(group, "Alice").get(0).getId();
        mockMvc.perform(post("/api/groups/" + code + "/notifications/ack")
                        .session(sessionFor("Alice"))
                        .contentType("application/json")
                        .content("{\"ids\":[\"" + id + "\"]}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/groups/" + code).session(sessionFor("Alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myPurchaseNotifications").isEmpty());
    }

    @Test
    void boughtEndpoint_requiresTradePermission() throws Exception {
        Group group = groupService.createGroup("Perm Group", "Bob", PoeVersion.POE1, "Standard");
        Item item = new Item("Precious Ring", "RARE", "Ring");
        item.queuedBy = "Alice";
        groupService.addItem(group.getCode(), item);

        // Carol is a plain member with no trade permission -> 403, item stays.
        groupService.joinGroup(group.getCode(), "Carol");
        mockMvc.perform(post("/api/groups/" + group.getCode() + "/items/0/bought").session(sessionFor("Carol")))
                .andExpect(status().isForbidden());
    }

    @Test
    void ackEndpoint_requiresAuthenticatedSession() throws Exception {
        Group group = groupService.createGroup("Ack Group", "Bob", PoeVersion.POE1, "Standard");
        mockMvc.perform(post("/api/groups/" + group.getCode() + "/notifications/ack")
                        .contentType("application/json")
                        .content("{\"ids\":[]}"))
                .andExpect(status().isUnauthorized());
    }
}

package com.poeticketqueue.controller;

import com.poeticketqueue.model.Group;
import com.poeticketqueue.model.GroupRole;
import com.poeticketqueue.service.GroupService;
import org.apache.commons.lang3.StringUtils;
import com.poeticketqueue.util.SessionAttributes;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.function.Supplier;

@RestController
@RequestMapping("/api/groups")
public class JoinGroupApiController {

    private final GroupService groupService;

    public JoinGroupApiController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping("/{groupCode}/join")
    public ResponseEntity<Group> joinGroup(@PathVariable String groupCode, @RequestBody JoinGroupRequest request, HttpSession session) {
        return groupService.findByCode(groupCode).map(group -> {
            boolean duplicate = group.getMembers().stream()
                    .anyMatch(m -> m.getScreenName().equalsIgnoreCase(request.screenName()));
            if (duplicate) {
                return ResponseEntity.status(409).<Group>build();
            }
            groupService.joinGroup(groupCode, request.screenName());
            session.setAttribute(SessionAttributes.SCREEN_NAME, request.screenName());
            session.setAttribute(SessionAttributes.GROUP_CODE, groupCode);
            if (StringUtils.isNotBlank(request.poeSessionId())) {
                session.setAttribute(SessionAttributes.POE_SESSION_ID, request.poeSessionId());
            }
            return ResponseEntity.ok(group);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{groupCode}/leave")
    public ResponseEntity<Void> leaveGroup(@PathVariable String groupCode, HttpSession session) {
        String screenName = (String) session.getAttribute(SessionAttributes.SCREEN_NAME);
        if (screenName != null) {
            groupService.removeMember(groupCode, screenName);
        }
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{groupCode}/members/{targetScreenName}/role")
    public ResponseEntity<Group> setMemberRole(
            @PathVariable String groupCode,
            @PathVariable String targetScreenName,
            @RequestBody SetRoleRequest request,
            HttpSession session) {
        String requesterScreenName = (String) session.getAttribute(SessionAttributes.SCREEN_NAME);
        if (requesterScreenName == null) {
            return ResponseEntity.status(401).build();
        }
        if (request.role() == GroupRole.CREATOR) {
            return ResponseEntity.status(400).build();
        }
        if (requesterScreenName.equals(targetScreenName)) {
            return ResponseEntity.status(400).build();
        }

        return groupService.findByCode(groupCode).map(group -> requireCreator(group, requesterScreenName, () ->
                groupService.setMemberRole(groupCode, targetScreenName, request.role())
                        .map(ResponseEntity::ok)
                        .orElse(ResponseEntity.notFound().build())
        )).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{groupCode}/members/{targetScreenName}/transfer-creator")
    public ResponseEntity<Group> transferCreator(
            @PathVariable String groupCode,
            @PathVariable String targetScreenName,
            HttpSession session) {
        String requesterScreenName = (String) session.getAttribute(SessionAttributes.SCREEN_NAME);
        if (requesterScreenName == null) {
            return ResponseEntity.status(401).build();
        }
        if (requesterScreenName.equals(targetScreenName)) {
            return ResponseEntity.status(400).build();
        }

        return groupService.findByCode(groupCode).map(group -> requireCreator(group, requesterScreenName, () ->
                groupService.transferCreator(groupCode, requesterScreenName, targetScreenName)
                        .map(ResponseEntity::ok)
                        .orElse(ResponseEntity.notFound().build())
        )).orElse(ResponseEntity.notFound().build());
    }

    private static ResponseEntity<Group> requireCreator(Group group, String requesterScreenName, Supplier<ResponseEntity<Group>> action) {
        boolean isCreator = group.getMembers().stream()
                .anyMatch(m -> requesterScreenName.equals(m.getScreenName()) && m.getRole() == GroupRole.CREATOR);
        if (!isCreator) {
            return ResponseEntity.status(403).build();
        }
        return action.get();
    }

    @PutMapping("/{groupCode}/poe-session")
    public ResponseEntity<Void> updatePoeSession(
            @PathVariable String groupCode,
            @RequestBody UpdatePoeSessionRequest request,
            HttpSession session) {
        if (session.getAttribute(SessionAttributes.SCREEN_NAME) == null) {
            return ResponseEntity.status(401).build();
        }
        if (StringUtils.isNotBlank(request.poeSessionId())) {
            session.setAttribute(SessionAttributes.POE_SESSION_ID, request.poeSessionId());
        } else {
            session.removeAttribute(SessionAttributes.POE_SESSION_ID);
        }
        return ResponseEntity.ok().build();
    }

    record JoinGroupRequest(String screenName, String poeSessionId) {}
    record SetRoleRequest(GroupRole role) {}
    record UpdatePoeSessionRequest(String poeSessionId) {}
}

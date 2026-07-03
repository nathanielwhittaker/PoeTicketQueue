package com.poeticketqueue.controller;

import com.poeticketqueue.model.Group;
import com.poeticketqueue.poe.build.PoeVersion;
import org.apache.commons.lang3.StringUtils;
import com.poeticketqueue.service.GroupService;
import com.poeticketqueue.util.SessionAttributes;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/groups")
public class CreateGroupApiController {

    private final GroupService groupService;

    public CreateGroupApiController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping
    public ResponseEntity<Group> createGroup(@RequestBody CreateGroupRequest request, HttpSession session) {
        Group group = groupService.createGroup(request.name(), request.screenName(), request.poeVersion(), request.league());
        session.setAttribute(SessionAttributes.SCREEN_NAME, request.screenName());
        session.setAttribute(SessionAttributes.GROUP_CODE, group.getCode());
        if (StringUtils.isNotBlank(request.poeSessionId())) {
            session.setAttribute(SessionAttributes.POE_SESSION_ID, request.poeSessionId());
        }
        return ResponseEntity.ok(group);
    }

    record CreateGroupRequest(String name, String screenName, PoeVersion poeVersion, String poeSessionId, String league) {}
}

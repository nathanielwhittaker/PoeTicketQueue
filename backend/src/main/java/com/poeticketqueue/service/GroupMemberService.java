package com.poeticketqueue.service;

import com.poeticketqueue.model.Group;
import com.poeticketqueue.model.GroupRole;
import org.springframework.stereotype.Service;

@Service
public class GroupMemberService {

    public boolean canTrade(Group group, String screenName) {
        return group.getMembers().stream()
                .anyMatch(m -> screenName.equals(m.getScreenName()) &&
                        (m.getRole() == GroupRole.CREATOR || m.getRole() == GroupRole.TRADER));
    }
}

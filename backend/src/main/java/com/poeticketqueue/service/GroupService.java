package com.poeticketqueue.service;

import com.poeticketqueue.model.Group;
import com.poeticketqueue.model.GroupRole;
import com.poeticketqueue.model.Member;
import com.poeticketqueue.poe.build.PoeVersion;
import com.poeticketqueue.poe.item.Item;
import com.poeticketqueue.util.GroupCodeGenerator;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GroupService {

    private final Map<String, Group> activeGroups = new ConcurrentHashMap<>();
    private final GroupCodeGenerator codeGenerator;

    public GroupService(GroupCodeGenerator codeGenerator) {
        this.codeGenerator = codeGenerator;
    }

    public Group createGroup(String groupName, String creatorScreenName, PoeVersion poeVersion, String league) {
        String groupCode = generateUniqueCode();
        Group group = new Group(groupName, groupCode, poeVersion, league);
        group.getMembers().add(new Member(creatorScreenName, GroupRole.CREATOR));
        activeGroups.put(groupCode, group);
        return group;
    }

    public Optional<Group> joinGroup(String groupCode, String screenName) {
        return findByCode(groupCode).map(group -> {
            group.getMembers().add(new Member(screenName, GroupRole.MEMBER));
            return group;
        });
    }

    public Optional<Group> removeMember(String groupCode, String screenName) {
        return findByCode(groupCode).map(group -> {
            boolean wasCreator = group.getMembers().stream()
                    .anyMatch(m -> screenName.equals(m.getScreenName()) && m.getRole() == GroupRole.CREATOR);
            group.getMembers().removeIf(m -> screenName.equals(m.getScreenName()));
            if (group.getMembers().isEmpty()) {
                activeGroups.remove(groupCode);
            } else if (wasCreator) {
                group.getMembers().get(0).setRole(GroupRole.CREATOR);
            }
            return group;
        });
    }

    public Optional<Group> setMemberRole(String groupCode, String targetScreenName, GroupRole newRole) {
        return findByCode(groupCode).map(group -> {
            group.getMembers().stream()
                    .filter(m -> targetScreenName.equals(m.getScreenName()))
                    .findFirst()
                    .ifPresent(m -> m.setRole(newRole));
            return group;
        });
    }

    public Optional<Group> transferCreator(String groupCode, String requesterScreenName, String targetScreenName) {
        return findByCode(groupCode).map(group -> {
            group.getMembers().stream()
                    .filter(m -> requesterScreenName.equals(m.getScreenName()))
                    .findFirst()
                    .ifPresent(m -> m.setRole(GroupRole.MEMBER));
            group.getMembers().stream()
                    .filter(m -> targetScreenName.equals(m.getScreenName()))
                    .findFirst()
                    .ifPresent(m -> m.setRole(GroupRole.CREATOR));
            return group;
        });
    }

    public Optional<Group> addItem(String groupCode, Item item) {
        return findByCode(groupCode).map(group -> {
            group.getItemQueue().add(item);
            return group;
        });
    }

    public Optional<Group> removeItem(String groupCode, int index) {
        return findByCode(groupCode).map(group -> {
            List<Item> queue = group.getItemQueue();
            if (index >= 0 && index < queue.size()) {
                queue.remove(index);
            }
            return group;
        });
    }

    public Optional<Group> findByCode(String groupCode) {
        return Optional.ofNullable(activeGroups.get(groupCode));
    }

    private String generateUniqueCode() {
        String groupCode;
        do {
            groupCode = codeGenerator.generate();
        } while (activeGroups.containsKey(groupCode));
        return groupCode;
    }
}

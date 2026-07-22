package com.poeticketqueue.service;

import com.poeticketqueue.model.Group;
import com.poeticketqueue.model.GroupRole;
import com.poeticketqueue.model.Member;
import com.poeticketqueue.model.PurchaseNotification;
import com.poeticketqueue.model.QueuedBuild;
import com.poeticketqueue.poe.build.PoeVersion;
import com.poeticketqueue.poe.item.Item;
import com.poeticketqueue.util.GroupCodeGenerator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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

    public Optional<Group> boughtItem(String groupCode, int index) {
        return findByCode(groupCode).map(group -> {
            List<Item> queue = group.getItemQueue();
            if (index >= 0 && index < queue.size()) {
                Item item = queue.remove(index);
                if (StringUtils.isNotBlank(item.getQueuedBy())) {
                    group.getPurchaseNotifications().add(new PurchaseNotification(item.getQueuedBy(), itemLabel(item)));
                }
            }
            return group;
        });
    }

    public Optional<Group> addBuild(String groupCode, QueuedBuild build) {
        return findByCode(groupCode).map(group -> {
            group.getBuildQueue().add(build);
            return group;
        });
    }

    public Optional<Group> removeBuildItem(String groupCode, int buildIndex, int itemIndex) {
        return findByCode(groupCode).map(group -> {
            List<QueuedBuild> builds = group.getBuildQueue();
            if (buildIndex >= 0 && buildIndex < builds.size()) {
                List<Item> items = builds.get(buildIndex).getItems();
                if (itemIndex >= 0 && itemIndex < items.size()) {
                    items.remove(itemIndex);
                    if (items.isEmpty()) {
                        builds.remove(buildIndex);
                    }
                }
            }
            return group;
        });
    }

    public Optional<Group> boughtBuildItem(String groupCode, int buildIndex, int itemIndex) {
        return findByCode(groupCode).map(group -> {
            List<QueuedBuild> builds = group.getBuildQueue();
            if (buildIndex >= 0 && buildIndex < builds.size()) {
                QueuedBuild build = builds.get(buildIndex);
                String queuedBy = build.getQueuedBy();
                List<Item> items = build.getItems();
                if (itemIndex >= 0 && itemIndex < items.size()) {
                    Item item = items.remove(itemIndex);
                    if (items.isEmpty()) {
                        builds.remove(buildIndex);
                    }
                    if (StringUtils.isNotBlank(queuedBy)) {
                        group.getPurchaseNotifications().add(new PurchaseNotification(queuedBy, itemLabel(item)));
                    }
                }
            }
            return group;
        });
    }

    public Optional<Group> removeBuild(String groupCode, int buildIndex) {
        return findByCode(groupCode).map(group -> {
            List<QueuedBuild> builds = group.getBuildQueue();
            if (buildIndex >= 0 && buildIndex < builds.size()) {
                builds.remove(buildIndex);
            }
            return group;
        });
    }

    public Optional<Group> findByCode(String groupCode) {
        return Optional.ofNullable(activeGroups.get(groupCode));
    }

    public List<PurchaseNotification> notificationsFor(Group group, String screenName) {
        if (StringUtils.isBlank(screenName)) {
            return new ArrayList<>();
        }
        return new ArrayList<>(group.getPurchaseNotifications().stream()
                .filter(n -> n.getTargetScreenName() != null && n.getTargetScreenName().equals(screenName))
                .toList());
    }

    public Optional<Group> ackNotifications(String groupCode, String screenName, List<String> ids) {
        return findByCode(groupCode).map(group -> {
            if (screenName != null && ids != null) {
                group.getPurchaseNotifications().removeIf(n -> ids.contains(n.getId()) && screenName.equals(n.getTargetScreenName()));
            }
            return group;
        });
    }

    private static String itemLabel(Item item) {
        if (StringUtils.isNotBlank(item.getName())) {
            return item.getName();
        }
        if (StringUtils.isNotBlank(item.getBaseType())) {
            return item.getBaseType();
        }
        if (StringUtils.isNotBlank(item.getCategory())) {
            return item.getCategory();
        }
        return "Item";
    }

    private String generateUniqueCode() {
        String groupCode;
        do {
            groupCode = codeGenerator.generate();
        } while (activeGroups.containsKey(groupCode));
        return groupCode;
    }
}

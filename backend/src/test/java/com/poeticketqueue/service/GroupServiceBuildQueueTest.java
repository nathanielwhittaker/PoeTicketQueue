package com.poeticketqueue.service;

import com.poeticketqueue.model.Group;
import com.poeticketqueue.model.QueuedBuild;
import com.poeticketqueue.poe.build.PoeVersion;
import com.poeticketqueue.poe.item.Item;
import com.poeticketqueue.util.GroupCodeGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * RED (test-first) contract tests for the in-memory build-queue feature on {@link GroupService}.
 * These are written BEFORE the production code exists ({@code QueuedBuild} and the new
 * {@code GroupService}/{@code Group} build-queue methods), so compile/assertion failure here is
 * expected and correct: they define the contract the implementation must satisfy.
 */
class GroupServiceBuildQueueTest {

    private GroupService groupService;

    @BeforeEach
    void setUp() {
        // Real collaborator — no need to mock a trivial code generator.
        groupService = new GroupService(new GroupCodeGenerator());
    }

    /** Creates a group via the real service and returns its generated code. */
    private String newGroupCode() {
        Group group = groupService.createGroup("Test Group", "creator", PoeVersion.POE1, "Standard");
        return group.getCode();
    }

    private Item item(String name) {
        return new Item(name, "RARE", "Ring", List.of());
    }

    private QueuedBuild build(String name, List<Item> items) {
        return new QueuedBuild(name, "https://pobb.in/example", PoeVersion.POE1, new ArrayList<>(items));
    }

    @Test
    void addBuild_addsToBuildQueue_andDoesNotTouchItemQueue() {
        String code = newGroupCode();
        QueuedBuild build = build("Righteous Fire", List.of(item("Test Ring")));

        Optional<Group> result = groupService.addBuild(code, build);

        assertThat(result).isPresent();
        Group group = result.get();
        assertThat(group.getBuildQueue()).containsExactly(build);
        assertThat(group.getItemQueue()).isEmpty();
    }

    @Test
    void freshlyCreatedGroup_hasEmptyNonNullBuildQueue() {
        Group group = groupService.createGroup("Fresh", "creator", PoeVersion.POE2, "Standard");

        assertThat(group.getBuildQueue()).isNotNull();
        assertThat(group.getBuildQueue()).isEmpty();
    }

    @Test
    void removeBuildItem_removesOnlyTargetedItem_whenBuildHasMultiple() {
        String code = newGroupCode();
        Item keepA = item("Ring A");
        Item removeMe = item("Ring B");
        Item keepC = item("Ring C");
        QueuedBuild multi = build("Multi", List.of(keepA, removeMe, keepC));
        QueuedBuild other = build("Other", List.of(item("Other Ring")));
        groupService.addBuild(code, multi);
        groupService.addBuild(code, other);

        Optional<Group> result = groupService.removeBuildItem(code, 0, 1);

        assertThat(result).isPresent();
        Group group = result.get();
        assertThat(group.getBuildQueue()).hasSize(2);
        assertThat(group.getBuildQueue().get(0).getItems()).containsExactly(keepA, keepC);
        assertThat(group.getBuildQueue().get(1)).isSameAs(other);
        assertThat(group.getBuildQueue().get(1).getItems()).hasSize(1);
    }

    @Test
    void removeBuildItem_onLastRemainingItem_autoRemovesTheWholeBuild() {
        String code = newGroupCode();
        QueuedBuild solo = build("Solo", List.of(item("Only Ring")));
        groupService.addBuild(code, solo);

        Optional<Group> result = groupService.removeBuildItem(code, 0, 0);

        assertThat(result).isPresent();
        assertThat(result.get().getBuildQueue()).isEmpty();
    }

    @Test
    void removeBuildItem_emptyingFirstBuild_removesOnlyIt_andLeavesSecondIntactAtCorrectPosition() {
        String code = newGroupCode();
        QueuedBuild first = build("First", List.of(item("First Ring")));
        Item secondItem = item("Second Ring");
        QueuedBuild second = build("Second", List.of(secondItem));
        groupService.addBuild(code, first);
        groupService.addBuild(code, second);

        Optional<Group> result = groupService.removeBuildItem(code, 0, 0);

        assertThat(result).isPresent();
        Group group = result.get();
        assertThat(group.getBuildQueue()).hasSize(1);
        assertThat(group.getBuildQueue().get(0)).isSameAs(second);
        assertThat(group.getBuildQueue().get(0).getItems()).containsExactly(secondItem);
    }

    @Test
    void removeBuildItem_withOutOfRangeIndices_isSafeNoOp() {
        String code = newGroupCode();
        QueuedBuild build = build("Build", List.of(item("Ring 1"), item("Ring 2")));
        groupService.addBuild(code, build);

        assertThatCode(() -> {
            Optional<Group> badBuildIndex = groupService.removeBuildItem(code, 5, 0);
            assertThat(badBuildIndex).isPresent();
            assertThat(badBuildIndex.get().getBuildQueue()).hasSize(1);
            assertThat(badBuildIndex.get().getBuildQueue().get(0).getItems()).hasSize(2);

            Optional<Group> badItemIndex = groupService.removeBuildItem(code, 0, 9);
            assertThat(badItemIndex).isPresent();
            assertThat(badItemIndex.get().getBuildQueue()).hasSize(1);
            assertThat(badItemIndex.get().getBuildQueue().get(0).getItems()).hasSize(2);
        }).doesNotThrowAnyException();
    }

    @Test
    void removeBuild_removesEntireBuildRegardlessOfItemCount_andOutOfRangeIsNoOp() {
        String code = newGroupCode();
        QueuedBuild big = build("Big", List.of(item("R1"), item("R2"), item("R3")));
        QueuedBuild keep = build("Keep", List.of(item("K1")));
        groupService.addBuild(code, big);
        groupService.addBuild(code, keep);

        Optional<Group> removed = groupService.removeBuild(code, 0);

        assertThat(removed).isPresent();
        assertThat(removed.get().getBuildQueue()).hasSize(1);
        assertThat(removed.get().getBuildQueue().get(0)).isSameAs(keep);

        Optional<Group> outOfRange = groupService.removeBuild(code, 42);
        assertThat(outOfRange).isPresent();
        assertThat(outOfRange.get().getBuildQueue()).hasSize(1);
        assertThat(outOfRange.get().getBuildQueue().get(0)).isSameAs(keep);
    }

    @Test
    void buildOperations_onNonExistentGroupCode_returnEmptyOptional() {
        String missing = "ZZZZZZ";
        QueuedBuild build = build("Build", List.of(item("Ring")));

        assertThat(groupService.addBuild(missing, build)).isEmpty();
        assertThat(groupService.removeBuildItem(missing, 0, 0)).isEmpty();
        assertThat(groupService.removeBuild(missing, 0)).isEmpty();
    }
}

package com.example.tasktracker.domain;

import com.example.tasktracker.domain.enums.BugSeverity;
import com.example.tasktracker.domain.enums.IssuePriority;
import com.example.tasktracker.domain.enums.IssueStatus;
import com.example.tasktracker.domain.enums.IssueType;
import com.example.tasktracker.exception.IllegalStatusTransitionException;
import com.example.tasktracker.exception.InvalidIssueException;
import com.example.tasktracker.exception.IssueStatusAlreadyAssignedException;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IssueTest {
    private final Project project = project();

    @Test
    void eachSubclassReportsItsType() {
        assertThat(new Epic("Epic", null, null).getType()).isEqualTo(IssueType.EPIC);
        assertThat(new Story("Story", null, null, 5).getType()).isEqualTo(IssueType.STORY);
        assertThat(new Task("Task", null, null).getType()).isEqualTo(IssueType.TASK);
        assertThat(new Bug("Bug", null, null, BugSeverity.MAJOR, null).getType()).isEqualTo(IssueType.BUG);
    }

    @Test
    void newIssueIsTodoWithMediumPriorityByDefault() {
        Task task = new Task("Task", null, null);

        assertThat(task.getStatus()).isEqualTo(IssueStatus.TODO);
        assertThat(task.getPriority()).isEqualTo(IssuePriority.MEDIUM);
    }

    @Test
    void explicitPriorityIsKept() {
        assertThat(new Task("Task", null, IssuePriority.CRITICAL).getPriority()).isEqualTo(IssuePriority.CRITICAL);
    }

    @Test
    void changeStatusSetsNewStatus() {
        Task task = new Task("Task", null, null);

        task.changeStatus(IssueStatus.IN_PROGRESS);

        assertThat(task.getStatus()).isEqualTo(IssueStatus.IN_PROGRESS);
    }

    @Test
    void workflowAllowsOnlyDefinedTransitions() {
        Map<IssueStatus, Set<IssueStatus>> allowed = Map.of(
                IssueStatus.TODO, Set.of(IssueStatus.IN_PROGRESS),
                IssueStatus.IN_PROGRESS, Set.of(IssueStatus.TODO, IssueStatus.IN_REVIEW),
                IssueStatus.IN_REVIEW, Set.of(IssueStatus.IN_PROGRESS, IssueStatus.DONE),
                IssueStatus.DONE, Set.of(IssueStatus.IN_PROGRESS));

        for (IssueStatus from : IssueStatus.values()) {
            for (IssueStatus to : IssueStatus.values()) {
                if (from == to) {
                    continue;
                }
                Task task = taskIn(from);
                if (allowed.get(from).contains(to)) {
                    task.changeStatus(to);
                    assertThat(task.getStatus()).isEqualTo(to);
                } else {
                    assertThatThrownBy(() -> task.changeStatus(to))
                            .isInstanceOf(IllegalStatusTransitionException.class)
                            .hasMessage("Cannot change status from " + from + " to " + to);
                    assertThat(task.getStatus()).isEqualTo(from);
                }
            }
        }
    }

    @Test
    void changeStatusToSameStatusThrows() {
        Task task = new Task("Task", null, null);

        assertThatThrownBy(() -> task.changeStatus(IssueStatus.TODO))
                .isInstanceOf(IssueStatusAlreadyAssignedException.class)
                .hasMessage("Issue already has status TODO");
    }

    @Test
    void storyTaskAndBugCanBeMovedIntoEpicOfSameProject() {
        Epic epic = issue(new Epic("Epic", null, null), project);

        for (Issue child : new Issue[]{
                issue(new Story("Story", null, null, null), project),
                issue(new Task("Task", null, null), project),
                issue(new Bug("Bug", null, null, BugSeverity.MINOR, null), project)}) {
            child.moveTo(epic);
            assertThat(child.getParent()).isSameAs(epic);
        }
    }

    @Test
    void movingToNullDetachesFromEpic() {
        Epic epic = issue(new Epic("Epic", null, null), project);
        Task task = issue(new Task("Task", null, null), project);
        task.moveTo(epic);

        task.moveTo(null);

        assertThat(task.getParent()).isNull();
    }

    @Test
    void epicCannotHaveParent() {
        Epic epic = issue(new Epic("Epic", null, null), project);
        Epic otherEpic = issue(new Epic("Other", null, null), project);

        assertThatThrownBy(() -> epic.moveTo(otherEpic))
                .isInstanceOf(InvalidIssueException.class)
                .hasMessage("EPIC cannot have a parent");
    }

    @Test
    void onlyEpicCanBeParent() {
        Task task = issue(new Task("Task", null, null), project);
        Story story = issue(new Story("Story", null, null, null), project);

        assertThatThrownBy(() -> task.moveTo(story))
                .isInstanceOf(InvalidIssueException.class)
                .hasMessage("Only EPIC can be a parent");
    }

    @Test
    void parentMustBelongToSameProject() {
        Epic foreignEpic = issue(new Epic("Epic", null, null), project());
        Task task = issue(new Task("Task", null, null), project);

        assertThatThrownBy(() -> task.moveTo(foreignEpic))
                .isInstanceOf(InvalidIssueException.class)
                .hasMessage("Parent must belong to the same project");
    }

    @Test
    void issueCannotBeItsOwnParent() {
        Task task = issue(new Task("Task", null, null), project);

        assertThatThrownBy(() -> task.moveTo(task))
                .isInstanceOf(InvalidIssueException.class)
                .hasMessage("Issue cannot be its own parent");
    }

    private static Task taskIn(IssueStatus status) {
        Task task = new Task("Task", null, null);
        switch (status) {
            case TODO -> {
            }
            case IN_PROGRESS -> task.changeStatus(IssueStatus.IN_PROGRESS);
            case IN_REVIEW -> {
                task.changeStatus(IssueStatus.IN_PROGRESS);
                task.changeStatus(IssueStatus.IN_REVIEW);
            }
            case DONE -> {
                task.changeStatus(IssueStatus.IN_PROGRESS);
                task.changeStatus(IssueStatus.IN_REVIEW);
                task.changeStatus(IssueStatus.DONE);
            }
        }
        return task;
    }

    private static <T extends Issue> T issue(T issue, Project project) {
        issue.setId(UUID.randomUUID());
        issue.setProject(project);
        return issue;
    }

    private static Project project() {
        Project project = new Project("Some Project", null);
        project.setId(UUID.randomUUID());
        return project;
    }
}

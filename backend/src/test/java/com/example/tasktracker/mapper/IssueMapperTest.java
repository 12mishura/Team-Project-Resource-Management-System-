package com.example.tasktracker.mapper;

import com.example.tasktracker.domain.Bug;
import com.example.tasktracker.domain.Epic;
import com.example.tasktracker.domain.Issue;
import com.example.tasktracker.domain.Organization;
import com.example.tasktracker.domain.OrganizationMember;
import com.example.tasktracker.domain.Project;
import com.example.tasktracker.domain.ProjectMember;
import com.example.tasktracker.domain.Story;
import com.example.tasktracker.domain.Task;
import com.example.tasktracker.domain.User;
import com.example.tasktracker.domain.enums.BugSeverity;
import com.example.tasktracker.domain.enums.IssuePriority;
import com.example.tasktracker.domain.enums.IssueType;
import com.example.tasktracker.domain.enums.OrganizationRole;
import com.example.tasktracker.domain.enums.ProjectMemberRole;
import com.example.tasktracker.dto.request.CreateIssueRequest;
import com.example.tasktracker.dto.request.UpdateIssueRequest;
import com.example.tasktracker.dto.response.IssueResponse;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class IssueMapperTest {
    private final IssueMapper mapper = new IssueMapper();

    @Test
    void toEntityCreatesSubclassForEachType() {
        assertThat(mapper.toEntity(create(IssueType.EPIC, null, null, null))).isInstanceOf(Epic.class);
        assertThat(mapper.toEntity(create(IssueType.TASK, null, null, null))).isInstanceOf(Task.class);

        Story story = (Story) mapper.toEntity(create(IssueType.STORY, null, null, 8));
        assertThat(story.getStoryPoints()).isEqualTo(8);

        Bug bug = (Bug) mapper.toEntity(create(IssueType.BUG, BugSeverity.BLOCKER, "Click save", null));
        assertThat(bug.getSeverity()).isEqualTo(BugSeverity.BLOCKER);
        assertThat(bug.getStepsToReproduce()).isEqualTo("Click save");
        assertThat(bug.getTitle()).isEqualTo("Title");
        assertThat(bug.getPriority()).isEqualTo(IssuePriority.HIGH);
    }

    @Test
    void toResponseFillsCommonAndBugFields() {
        Bug bug = prepare(new Bug("Crash", "On save", IssuePriority.CRITICAL, BugSeverity.MAJOR, "Steps"));
        ProjectMember assignee = assignee("bob");
        bug.setAssignee(assignee);
        Epic epic = prepare(new Epic("Epic", null, null));
        bug.moveTo(epic);

        IssueResponse response = mapper.toResponse(bug);

        assertThat(response.type()).isEqualTo(IssueType.BUG);
        assertThat(response.projectId()).isEqualTo(bug.getProject().getId());
        assertThat(response.reporterUsername()).isEqualTo("alice");
        assertThat(response.assigneeId()).isEqualTo(assignee.getId());
        assertThat(response.assigneeUsername()).isEqualTo("bob");
        assertThat(response.parentId()).isEqualTo(epic.getId());
        assertThat(response.severity()).isEqualTo(BugSeverity.MAJOR);
        assertThat(response.stepsToReproduce()).isEqualTo("Steps");
        assertThat(response.storyPoints()).isNull();
    }

    @Test
    void toResponseLeavesForeignTypeFieldsEmpty() {
        IssueResponse story = mapper.toResponse(prepare(new Story("Story", null, null, 3)));
        IssueResponse task = mapper.toResponse(prepare(new Task("Task", null, null)));

        assertThat(story.storyPoints()).isEqualTo(3);
        assertThat(story.severity()).isNull();
        assertThat(task.storyPoints()).isNull();
        assertThat(task.severity()).isNull();
        assertThat(task.assigneeId()).isNull();
        assertThat(task.parentId()).isNull();
    }

    @Test
    void toUpdateChangesOnlyProvidedAndApplicableFields() {
        Story story = prepare(new Story("Story", "Old", IssuePriority.LOW, 1));
        Bug bug = prepare(new Bug("Bug", null, null, BugSeverity.MINOR, null));

        mapper.toUpdate(new UpdateIssueRequest(null, "New", null, null, null, 13), story);
        mapper.toUpdate(new UpdateIssueRequest("Renamed bug", null, IssuePriority.HIGH, BugSeverity.BLOCKER, "Steps", null), bug);

        assertThat(story.getTitle()).isEqualTo("Story");
        assertThat(story.getDescription()).isEqualTo("New");
        assertThat(story.getPriority()).isEqualTo(IssuePriority.LOW);
        assertThat(story.getStoryPoints()).isEqualTo(13);
        assertThat(bug.getTitle()).isEqualTo("Renamed bug");
        assertThat(bug.getPriority()).isEqualTo(IssuePriority.HIGH);
        assertThat(bug.getSeverity()).isEqualTo(BugSeverity.BLOCKER);
        assertThat(bug.getStepsToReproduce()).isEqualTo("Steps");
    }

    private static final Project PROJECT = project();

    private static <T extends Issue> T prepare(T issue) {
        issue.setId(UUID.randomUUID());
        issue.setProject(PROJECT);
        User reporter = new User("alice@mail.com", "alice", "hash");
        reporter.setId(UUID.randomUUID());
        issue.setReporter(reporter);
        return issue;
    }

    private static ProjectMember assignee(String username) {
        User user = new User(username + "@mail.com", username, "hash");
        OrganizationMember organizationMember = new OrganizationMember(OrganizationRole.USER);
        organizationMember.setUser(user);
        ProjectMember member = new ProjectMember(ProjectMemberRole.USER);
        member.setId(UUID.randomUUID());
        member.setOrganizationMember(organizationMember);
        return member;
    }

    private static Project project() {
        Organization organization = new Organization("Some Organization", null, "abcd-2345");
        organization.setId(UUID.randomUUID());
        Project project = new Project("Some Project", null);
        project.setId(UUID.randomUUID());
        project.setOrganization(organization);
        return project;
    }

    private static CreateIssueRequest create(IssueType type, BugSeverity severity, String steps, Integer points) {
        return new CreateIssueRequest(type, "Title", null, IssuePriority.HIGH, severity, steps, points, null);
    }
}

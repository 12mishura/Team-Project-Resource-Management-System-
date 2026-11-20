package com.example.tasktracker.security;

import com.example.tasktracker.domain.Issue;
import com.example.tasktracker.domain.OrganizationMember;
import com.example.tasktracker.domain.ProjectMember;
import com.example.tasktracker.domain.Task;
import com.example.tasktracker.domain.User;
import com.example.tasktracker.domain.enums.IssueType;
import com.example.tasktracker.domain.enums.OrganizationRole;
import com.example.tasktracker.domain.enums.ProjectMemberRole;
import com.example.tasktracker.exception.AccessDeniedToOrganizationException;
import com.example.tasktracker.exception.AccessDeniedToProjectException;
import com.example.tasktracker.exception.IllegalIssueOperationException;
import com.example.tasktracker.repository.OrganizationMemberRepository;
import com.example.tasktracker.repository.ProjectMemberRepository;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProjectPermissionCheckerTest {
    private static final UUID ORGANIZATION_ID = UUID.randomUUID();
    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final String EMAIL = "actor@mail.com";

    private OrganizationMemberRepository organizationMemberRepository;
    private ProjectMemberRepository projectMemberRepository;
    private ProjectPermissionChecker checker;

    @BeforeEach
    void setUp() {
        organizationMemberRepository = mock(OrganizationMemberRepository.class);
        projectMemberRepository = mock(ProjectMemberRepository.class);
        checker = new ProjectPermissionChecker(organizationMemberRepository, projectMemberRepository);
    }

    @Test
    void organizationOwnerAndAdminActAsProjectAdminWithoutMembership() {
        for (OrganizationRole role : new OrganizationRole[]{OrganizationRole.OWNER, OrganizationRole.ADMIN}) {
            stub(organizationMember(role), null);

            ProjectActor actor = checker.requireProjectActor(ORGANIZATION_ID, PROJECT_ID, EMAIL);

            assertThat(actor.role()).isEqualTo(ProjectMemberRole.ADMIN);
            assertThat(actor.projectMember()).isNull();
        }
    }

    @Test
    void organizationAdminKeepsProjectMembershipForSelfAssignment() {
        OrganizationMember organizationMember = organizationMember(OrganizationRole.ADMIN);
        ProjectMember projectMember = projectMember(organizationMember, ProjectMemberRole.USER);
        stub(organizationMember, projectMember);

        ProjectActor actor = checker.requireProjectActor(ORGANIZATION_ID, PROJECT_ID, EMAIL);

        assertThat(actor.role()).isEqualTo(ProjectMemberRole.ADMIN);
        assertThat(actor.projectMember()).isSameAs(projectMember);
    }

    @Test
    void organizationUserGetsProjectRole() {
        for (ProjectMemberRole role : ProjectMemberRole.values()) {
            OrganizationMember organizationMember = organizationMember(OrganizationRole.USER);
            stub(organizationMember, projectMember(organizationMember, role));

            assertThat(checker.requireProjectActor(ORGANIZATION_ID, PROJECT_ID, EMAIL).role()).isEqualTo(role);
        }
    }

    @Test
    void organizationUserOutsideProjectIsDenied() {
        stub(organizationMember(OrganizationRole.USER), null);

        assertThatThrownBy(() -> checker.requireProjectActor(ORGANIZATION_ID, PROJECT_ID, EMAIL))
                .isInstanceOf(AccessDeniedToProjectException.class)
                .hasMessage("Access denied to project " + PROJECT_ID);
    }

    @Test
    void nonMemberOfOrganizationIsDenied() {
        when(organizationMemberRepository.findByOrganizationIdAndUserEmail(ORGANIZATION_ID, EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> checker.requireProjectActor(ORGANIZATION_ID, PROJECT_ID, EMAIL))
                .isInstanceOf(AccessDeniedToOrganizationException.class);
    }

    @Test
    void onlyExtendedOrAdminCreateEpics() {
        assertAllowed(() -> checker.checkCanCreate(actor(ProjectMemberRole.ADMIN), IssueType.EPIC));
        assertAllowed(() -> checker.checkCanCreate(actor(ProjectMemberRole.EXTENDED), IssueType.EPIC));
        assertDenied(() -> checker.checkCanCreate(actor(ProjectMemberRole.USER), IssueType.EPIC),
                "Only project ADMIN or EXTENDED can create epics");
    }

    @Test
    void everyProjectRoleCreatesStoriesTasksAndBugs() {
        for (ProjectMemberRole role : ProjectMemberRole.values()) {
            for (IssueType type : new IssueType[]{IssueType.STORY, IssueType.TASK, IssueType.BUG}) {
                assertAllowed(() -> checker.checkCanCreate(actor(role), type));
            }
        }
    }

    @Test
    void editIsAllowedForExtendedAdminAndReporter() {
        ProjectActor user = actor(ProjectMemberRole.USER);

        assertAllowed(() -> checker.checkCanEdit(actor(ProjectMemberRole.ADMIN), issueBy(otherUser())));
        assertAllowed(() -> checker.checkCanEdit(actor(ProjectMemberRole.EXTENDED), issueBy(otherUser())));
        assertAllowed(() -> checker.checkCanEdit(user, issueBy(user)));
        assertDenied(() -> checker.checkCanEdit(user, issueBy(otherUser())),
                "Only the reporter or project ADMIN or EXTENDED can edit this issue");
    }

    @Test
    void statusIsChangedByExtendedAdminReporterOrAssignee() {
        ProjectActor user = actor(ProjectMemberRole.USER);
        Issue assignedToUser = issueBy(otherUser());
        assignedToUser.setAssignee(user.projectMember());

        assertAllowed(() -> checker.checkCanChangeStatus(actor(ProjectMemberRole.EXTENDED), issueBy(otherUser())));
        assertAllowed(() -> checker.checkCanChangeStatus(actor(ProjectMemberRole.ADMIN), issueBy(otherUser())));
        assertAllowed(() -> checker.checkCanChangeStatus(user, issueBy(user)));
        assertAllowed(() -> checker.checkCanChangeStatus(user, assignedToUser));
        assertDenied(() -> checker.checkCanChangeStatus(user, issueBy(otherUser())),
                "Only the reporter, the assignee or project ADMIN or EXTENDED can change status");
    }

    @Test
    void extendedAndAdminAssignAnyone() {
        ProjectMember someone = projectMember(organizationMember(OrganizationRole.USER), ProjectMemberRole.USER);

        assertAllowed(() -> checker.checkCanAssign(actor(ProjectMemberRole.EXTENDED), issueBy(otherUser()), someone));
        assertAllowed(() -> checker.checkCanAssign(actor(ProjectMemberRole.ADMIN), issueBy(otherUser()), null));
    }

    @Test
    void userTakesFreeIssueAndReleasesOwnButNothingElse() {
        ProjectActor user = actor(ProjectMemberRole.USER);
        ProjectMember someone = projectMember(organizationMember(OrganizationRole.USER), ProjectMemberRole.USER);
        Issue free = issueBy(otherUser());
        Issue mine = issueBy(otherUser());
        mine.setAssignee(user.projectMember());
        Issue taken = issueBy(otherUser());
        taken.setAssignee(someone);

        assertAllowed(() -> checker.checkCanAssign(user, free, user.projectMember()));
        assertAllowed(() -> checker.checkCanAssign(user, mine, null));
        assertAllowed(() -> checker.checkCanAssign(user, mine, user.projectMember()));
        assertDenied(() -> checker.checkCanAssign(user, free, someone),
                "Project USER can only take a free issue or release their own");
        assertDenied(() -> checker.checkCanAssign(user, taken, user.projectMember()),
                "Project USER can only take a free issue or release their own");
        assertDenied(() -> checker.checkCanAssign(user, taken, null),
                "Project USER can only take a free issue or release their own");
    }

    @Test
    void onlyExtendedOrAdminChangeEpic() {
        assertAllowed(() -> checker.checkCanChangeParent(actor(ProjectMemberRole.ADMIN)));
        assertAllowed(() -> checker.checkCanChangeParent(actor(ProjectMemberRole.EXTENDED)));
        assertDenied(() -> checker.checkCanChangeParent(actor(ProjectMemberRole.USER)),
                "Only project ADMIN or EXTENDED can change the epic of an issue");
    }

    @Test
    void deleteIsAllowedForAdminAndReporterOnly() {
        ProjectActor extended = actor(ProjectMemberRole.EXTENDED);

        assertAllowed(() -> checker.checkCanDelete(actor(ProjectMemberRole.ADMIN), issueBy(otherUser())));
        assertAllowed(() -> checker.checkCanDelete(extended, issueBy(extended)));
        assertDenied(() -> checker.checkCanDelete(extended, issueBy(otherUser())),
                "Only the reporter or project ADMIN can delete this issue");
        assertDenied(() -> checker.checkCanDelete(actor(ProjectMemberRole.USER), issueBy(otherUser())),
                "Only the reporter or project ADMIN can delete this issue");
    }

    private void stub(OrganizationMember organizationMember, ProjectMember projectMember) {
        when(organizationMemberRepository.findByOrganizationIdAndUserEmail(ORGANIZATION_ID, EMAIL))
                .thenReturn(Optional.of(organizationMember));
        when(projectMemberRepository.findByProjectIdAndOrganizationMemberId(PROJECT_ID, organizationMember.getId()))
                .thenReturn(Optional.ofNullable(projectMember));
    }

    private static ProjectActor actor(ProjectMemberRole role) {
        OrganizationMember organizationMember = organizationMember(OrganizationRole.USER);
        return new ProjectActor(organizationMember, projectMember(organizationMember, role), role);
    }

    private static OrganizationMember organizationMember(OrganizationRole role) {
        OrganizationMember member = new OrganizationMember(role);
        member.setId(UUID.randomUUID());
        member.setUser(otherUser());
        return member;
    }

    private static ProjectMember projectMember(OrganizationMember organizationMember, ProjectMemberRole role) {
        ProjectMember member = new ProjectMember(role);
        member.setId(UUID.randomUUID());
        member.setOrganizationMember(organizationMember);
        return member;
    }

    private static User otherUser() {
        User user = new User(UUID.randomUUID() + "@mail.com", "user", "hash");
        user.setId(UUID.randomUUID());
        return user;
    }

    private static Issue issueBy(ProjectActor reporter) {
        return issueBy(reporter.organizationMember().getUser());
    }

    private static Issue issueBy(User reporter) {
        Task task = new Task("Task", null, null);
        task.setId(UUID.randomUUID());
        task.setReporter(reporter);
        return task;
    }

    private static void assertAllowed(ThrowingCallable call) {
        assertThatCode(call).doesNotThrowAnyException();
    }

    private static void assertDenied(ThrowingCallable call, String message) {
        assertThatThrownBy(call).isInstanceOf(IllegalIssueOperationException.class).hasMessage(message);
    }
}

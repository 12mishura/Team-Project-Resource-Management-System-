package com.example.tasktracker.controller;

import com.example.tasktracker.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IssueControllerTest extends AbstractIntegrationTest {
    private String ownerToken;
    private String orgAdminToken;
    private String projectAdminToken;
    private String extendedToken;
    private String userToken;
    private String secondUserToken;
    private String strangerToken;
    private String outsiderToken;

    private String organizationId;
    private String projectId;
    private String otherProjectId;
    private String foreignOrganizationId;
    private String foreignProjectId;

    private String userMemberId;
    private String userProjectMemberId;
    private String secondUserProjectMemberId;
    private String extendedProjectMemberId;
    private String otherProjectMemberId;

    @BeforeEach
    void setUp() throws Exception {
        ownerToken = registerAndLogin("owner");
        orgAdminToken = registerAndLogin("orgadmin");
        projectAdminToken = registerAndLogin("padmin");
        extendedToken = registerAndLogin("extended");
        userToken = registerAndLogin("member");
        secondUserToken = registerAndLogin("member2");
        strangerToken = registerAndLogin("stranger");
        outsiderToken = registerAndLogin("outsider");

        organizationId = createOrganization(ownerToken, "MyOrganization");
        String inviteCode = getInviteCode(ownerToken, organizationId);
        String orgAdminMemberId = joinAndGetMemberId(orgAdminToken, inviteCode);
        changeRole(ownerToken, organizationId, orgAdminMemberId, "ADMIN").andExpect(status().isOk());
        String projectAdminMemberId = joinAndGetMemberId(projectAdminToken, inviteCode);
        String extendedMemberId = joinAndGetMemberId(extendedToken, inviteCode);
        userMemberId = joinAndGetMemberId(userToken, inviteCode);
        String secondUserMemberId = joinAndGetMemberId(secondUserToken, inviteCode);
        joinAndGetMemberId(strangerToken, inviteCode);

        projectId = createProject(organizationId, ownerToken, "Backend Rewrite");
        otherProjectId = createProject(organizationId, ownerToken, "Frontend Rewrite");
        addToProject(projectId, projectAdminMemberId, "ADMIN");
        extendedProjectMemberId = addToProject(projectId, extendedMemberId, "EXTENDED");
        userProjectMemberId = addToProject(projectId, userMemberId, "USER");
        secondUserProjectMemberId = addToProject(projectId, secondUserMemberId, "USER");
        otherProjectMemberId = addToProject(otherProjectId, userMemberId, "USER");

        foreignOrganizationId = createOrganization(outsiderToken, "ForeignOrganization");
        foreignProjectId = createProject(foreignOrganizationId, outsiderToken, "Foreign Project");
    }

    @Nested
    class Create {

        @Test
        void projectUserCreatesTaskWithDefaults() throws Exception {
            create(userToken, issue("TASK", "Write tests", ""))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(notNullValue()))
                    .andExpect(jsonPath("$.projectId").value(projectId))
                    .andExpect(jsonPath("$.type").value("TASK"))
                    .andExpect(jsonPath("$.title").value("Write tests"))
                    .andExpect(jsonPath("$.status").value("TODO"))
                    .andExpect(jsonPath("$.priority").value("MEDIUM"))
                    .andExpect(jsonPath("$.reporterUsername").value("member"))
                    .andExpect(jsonPath("$.assigneeId").value(nullValue()))
                    .andExpect(jsonPath("$.parentId").value(nullValue()))
                    .andExpect(jsonPath("$.version").value(0))
                    .andExpect(jsonPath("$.createdAt").value(notNullValue()));
        }

        @Test
        void storyKeepsStoryPointsAndBugKeepsSeverity() throws Exception {
            create(userToken, issue("STORY", "Login page", ",\"storyPoints\":5,\"priority\":\"HIGH\""))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.storyPoints").value(5))
                    .andExpect(jsonPath("$.priority").value("HIGH"))
                    .andExpect(jsonPath("$.severity").value(nullValue()));
            create(userToken, issue("BUG", "Crash on save", ",\"severity\":\"BLOCKER\",\"stepsToReproduce\":\"Click save\""))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.severity").value("BLOCKER"))
                    .andExpect(jsonPath("$.stepsToReproduce").value("Click save"))
                    .andExpect(jsonPath("$.storyPoints").value(nullValue()));
        }

        @Test
        void bugWithoutSeverityIsRejected() throws Exception {
            create(userToken, issue("BUG", "Crash on save", ""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("severity is required for BUG"));
        }

        @Test
        void fieldsOfAnotherTypeAreRejected() throws Exception {
            create(userToken, issue("TASK", "Write tests", ",\"storyPoints\":3"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("storyPoints is only allowed for STORY"));
            create(userToken, issue("STORY", "Login page", ",\"severity\":\"MINOR\""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("severity and stepsToReproduce are only allowed for BUG"));
            assertThat(issueCount()).isZero();
        }

        @Test
        void invalidRequestsAreRejected() throws Exception {
            create(userToken, issue("TASK", "  ", "")).andExpect(status().isBadRequest());
            create(userToken, "{\"title\":\"No type here\"}").andExpect(status().isBadRequest());
            create(userToken, issue("FEATURE", "Unknown type", "")).andExpect(status().isBadRequest());
            create(userToken, issue("STORY", "Too many points", ",\"storyPoints\":101"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        }

        @Test
        void onlyExtendedOrAdminCreateEpics() throws Exception {
            create(userToken, issue("EPIC", "Big epic", ""))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail").value("Only project ADMIN or EXTENDED can create epics"));
            create(extendedToken, issue("EPIC", "Big epic", "")).andExpect(status().isCreated());
            create(projectAdminToken, issue("EPIC", "Second epic", "")).andExpect(status().isCreated());
        }

        @Test
        void organizationOwnerAndAdminWorkWithoutProjectMembership() throws Exception {
            create(ownerToken, issue("EPIC", "Owner epic", "")).andExpect(status().isCreated());
            create(orgAdminToken, issue("TASK", "Admin task", "")).andExpect(status().isCreated());
        }

        @Test
        void organizationMemberOutsideProjectIsDenied() throws Exception {
            create(strangerToken, issue("TASK", "Sneaky task", ""))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail").value("Access denied to project " + projectId));
        }

        @Test
        void outsiderAndAnonymousAreDenied() throws Exception {
            create(outsiderToken, issue("TASK", "Sneaky task", ""))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail").value("Access denied to organization " + organizationId));
            mockMvc.perform(post(issuesPath(projectId)).contentType(MediaType.APPLICATION_JSON).content(issue("TASK", "No token", "")))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void unknownOrForeignProjectReturnsNotFound() throws Exception {
            mockMvc.perform(withToken(post(issuesPath(UUID.randomUUID().toString())), ownerToken)
                            .contentType(MediaType.APPLICATION_JSON).content(issue("TASK", "Ghost task", "")))
                    .andExpect(status().isNotFound());
            mockMvc.perform(withToken(post(issuesPath(foreignProjectId)), ownerToken)
                            .contentType(MediaType.APPLICATION_JSON).content(issue("TASK", "Ghost task", "")))
                    .andExpect(status().isNotFound());
        }

        @Test
        void extendedAndAdminCreateIssueInsideEpic() throws Exception {
            String epicId = createId(extendedToken, issue("EPIC", "Big epic", ""));

            create(extendedToken, issue("STORY", "Child story", ",\"parentId\":\"" + epicId + "\""))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.parentId").value(epicId));
            create(orgAdminToken, issue("TASK", "Admin child task", ",\"parentId\":\"" + epicId + "\""))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.parentId").value(epicId));
        }

        @Test
        void userCannotCreateIssueInsideEpic() throws Exception {
            String epicId = createId(extendedToken, issue("EPIC", "Big epic", ""));

            create(userToken, issue("STORY", "Child story", ",\"parentId\":\"" + epicId + "\""))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail").value("Only project ADMIN or EXTENDED can change the epic of an issue"));
            create(userToken, issue("STORY", "Loose story", "")).andExpect(status().isCreated());
            list(userToken, "?parentId=" + epicId).andExpect(jsonPath("$.content", hasSize(0)));
        }

        @Test
        void invalidParentOnCreateIsRejected() throws Exception {
            String taskId = createId(userToken, issue("TASK", "Not an epic", ""));
            String foreignEpicId = createId(ownerToken, otherProjectId, issue("EPIC", "Other project epic", ""));

            create(extendedToken, issue("STORY", "Child story", ",\"parentId\":\"" + taskId + "\""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("Only EPIC can be a parent"));
            create(extendedToken, issue("STORY", "Child story", ",\"parentId\":\"" + foreignEpicId + "\""))
                    .andExpect(status().isNotFound());
            create(extendedToken, issue("EPIC", "Nested epic", ",\"parentId\":\"" + createId(extendedToken, issue("EPIC", "Root epic", "")) + "\""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("EPIC cannot have a parent"));
        }

        @Test
        void archivedProjectIsReadOnly() throws Exception {
            String issueId = createId(userToken, issue("TASK", "Old task", ""));
            archiveProject();

            create(userToken, issue("TASK", "New task", ""))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail").value("Project " + projectId + " is archived and read-only"));
            update(ownerToken, issueId, "{\"title\":\"Renamed task\"}").andExpect(status().isConflict());
            changeStatus(ownerToken, issueId, "DONE").andExpect(status().isConflict());
            assign(ownerToken, issueId, userProjectMemberId).andExpect(status().isConflict());
            remove(ownerToken, issueId).andExpect(status().isConflict());
            getOne(userToken, issueId).andExpect(status().isOk());
            list(userToken, "").andExpect(jsonPath("$.content", hasSize(1)));
        }

        @Test
        void extendedMemberAssignsAnyProjectMemberOnCreate() throws Exception {
            create(extendedToken, issue("TASK", "Prepare release", ",\"assigneeId\":\"%s\"".formatted(userProjectMemberId)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.assigneeId").value(userProjectMemberId))
                    .andExpect(jsonPath("$.assigneeUsername").value("member"));
        }

        @Test
        void projectUserCanOnlyAssignThemselvesOnCreate() throws Exception {
            create(userToken, issue("TASK", "Take it myself", ",\"assigneeId\":\"%s\"".formatted(userProjectMemberId)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.assigneeId").value(userProjectMemberId));
            create(userToken, issue("TASK", "Give it away", ",\"assigneeId\":\"%s\"".formatted(secondUserProjectMemberId)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail").value("Project USER can only take a free issue or release their own"));
            assertThat(issueCount()).isEqualTo(1);
        }

        @Test
        void assigneeFromAnotherProjectIsRejectedOnCreate() throws Exception {
            create(ownerToken, issue("TASK", "Wrong project", ",\"assigneeId\":\"%s\"".formatted(otherProjectMemberId)))
                    .andExpect(status().isNotFound());
            assertThat(issueCount()).isZero();
        }
    }

    @Nested
    class Read {

        @Test
        void anyProjectMemberSeesIssue() throws Exception {
            String issueId = createId(extendedToken, issue("TASK", "Shared task", ""));

            getOne(userToken, issueId)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(issueId))
                    .andExpect(jsonPath("$.reporterUsername").value("extended"));
            getOne(orgAdminToken, issueId).andExpect(status().isOk());
        }

        @Test
        void strangerAndOutsiderCannotRead() throws Exception {
            String issueId = createId(userToken, issue("TASK", "Private task", ""));

            getOne(strangerToken, issueId).andExpect(status().isForbidden());
            getOne(outsiderToken, issueId).andExpect(status().isForbidden());
            list(strangerToken, "").andExpect(status().isForbidden());
        }

        @Test
        void issueOfAnotherProjectIsNotReachableThroughThisProject() throws Exception {
            String otherIssueId = createId(ownerToken, otherProjectId, issue("TASK", "Other project task", ""));

            getOne(ownerToken, otherIssueId)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value("Issue with ID " + otherIssueId + " not found"));
            getOne(ownerToken, UUID.randomUUID().toString()).andExpect(status().isNotFound());
        }

        @Test
        void listContainsOnlyThisProjectInCreationOrder() throws Exception {
            createId(userToken, issue("TASK", "First task", ""));
            createId(userToken, issue("STORY", "Second story", ""));
            createId(ownerToken, otherProjectId, issue("TASK", "Other project task", ""));

            list(userToken, "")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].title").value(contains("First task", "Second story")))
                    .andExpect(jsonPath("$.totalElements").value(2));
        }

        @Test
        void listFiltersCombine() throws Exception {
            String epicId = createId(extendedToken, issue("EPIC", "Big epic", ""));
            String bugId = createId(extendedToken, issue("BUG", "Crash", ",\"severity\":\"MAJOR\",\"parentId\":\"" + epicId + "\""));
            createId(extendedToken, issue("TASK", "Task in epic", ",\"parentId\":\"" + epicId + "\""));
            String freeTaskId = createId(userToken, issue("TASK", "Free task", ""));
            assign(extendedToken, bugId, userProjectMemberId).andExpect(status().isOk());
            changeStatus(userToken, freeTaskId, "IN_PROGRESS").andExpect(status().isOk());
            changeStatus(userToken, freeTaskId, "IN_REVIEW").andExpect(status().isOk());
            changeStatus(userToken, freeTaskId, "DONE").andExpect(status().isOk());

            list(userToken, "?type=BUG").andExpect(jsonPath("$.content[*].title").value(contains("Crash")));
            list(userToken, "?type=EPIC").andExpect(jsonPath("$.content[*].title").value(contains("Big epic")));
            list(userToken, "?status=DONE").andExpect(jsonPath("$.content[*].title").value(contains("Free task")));
            list(userToken, "?assigneeId=" + userProjectMemberId).andExpect(jsonPath("$.content[*].title").value(contains("Crash")));
            list(userToken, "?parentId=" + epicId)
                    .andExpect(jsonPath("$.content[*].title").value(containsInAnyOrder("Crash", "Task in epic")));
            list(userToken, "?parentId=" + epicId + "&type=TASK").andExpect(jsonPath("$.content[*].title").value(contains("Task in epic")));
            list(userToken, "?type=BUG&status=DONE").andExpect(jsonPath("$.content", hasSize(0)));
        }

        @Test
        void listIsPaginatedAndRejectsUnknownFilterValues() throws Exception {
            createId(userToken, issue("TASK", "Task one", ""));
            createId(userToken, issue("TASK", "Task two", ""));
            createId(userToken, issue("TASK", "Task three", ""));

            list(userToken, "?page=1&size=2")
                    .andExpect(jsonPath("$.content[*].title").value(contains("Task three")))
                    .andExpect(jsonPath("$.totalElements").value(3))
                    .andExpect(jsonPath("$.totalPages").value(2));
            list(userToken, "?type=FEATURE").andExpect(status().isBadRequest());
            list(userToken, "?assigneeId=not-a-uuid").andExpect(status().isBadRequest());
        }
    }

    @Nested
    class Update {

        @Test
        void reporterEditsOwnIssue() throws Exception {
            String issueId = createId(userToken, issue("STORY", "Login page", ",\"storyPoints\":3"));

            update(userToken, issueId, "{\"title\":\"Login page v2\",\"storyPoints\":8,\"priority\":\"LOW\"}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Login page v2"))
                    .andExpect(jsonPath("$.storyPoints").value(8))
                    .andExpect(jsonPath("$.priority").value("LOW"))
                    .andExpect(jsonPath("$.version").value(1));
        }

        @Test
        void userCannotEditSomeoneElsesIssue() throws Exception {
            String issueId = createId(secondUserToken, issue("TASK", "Their task", ""));

            update(userToken, issueId, "{\"title\":\"Hijacked task\"}")
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail").value("Only the reporter or project ADMIN or EXTENDED can edit this issue"));
        }

        @Test
        void extendedAndOrganizationAdminEditAnyIssue() throws Exception {
            String issueId = createId(userToken, issue("TASK", "Their task", ""));

            update(extendedToken, issueId, "{\"description\":\"By extended\"}").andExpect(status().isOk());
            update(orgAdminToken, issueId, "{\"description\":\"By org admin\"}")
                    .andExpect(jsonPath("$.description").value("By org admin"));
        }

        @Test
        void fieldsOfAnotherTypeCannotBeUpdated() throws Exception {
            String taskId = createId(userToken, issue("TASK", "Plain task", ""));

            update(userToken, taskId, "{\"severity\":\"MAJOR\"}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("severity and stepsToReproduce are only allowed for BUG"));
            update(userToken, taskId, "{\"title\":\"ab\"}").andExpect(status().isBadRequest());
        }
    }

    @Nested
    class Status {

        @Test
        void assigneeMovesIssueThroughWorkflow() throws Exception {
            String issueId = createId(secondUserToken, issue("TASK", "Assigned task", ""));
            assign(extendedToken, issueId, userProjectMemberId).andExpect(status().isOk());

            changeStatus(userToken, issueId, "IN_PROGRESS").andExpect(jsonPath("$.status").value("IN_PROGRESS"));
            changeStatus(userToken, issueId, "IN_REVIEW").andExpect(jsonPath("$.status").value("IN_REVIEW"));
            changeStatus(userToken, issueId, "DONE").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DONE"));
        }

        @Test
        void unrelatedUserCannotChangeStatus() throws Exception {
            String issueId = createId(secondUserToken, issue("TASK", "Their task", ""));

            changeStatus(userToken, issueId, "IN_PROGRESS")
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail").value("Only the reporter, the assignee or project ADMIN or EXTENDED can change status"));
            changeStatus(extendedToken, issueId, "IN_PROGRESS").andExpect(status().isOk());
        }

        @Test
        void skippingWorkflowStepsIsRejected() throws Exception {
            String issueId = createId(userToken, issue("TASK", "My task", ""));

            changeStatus(userToken, issueId, "DONE")
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail").value("Cannot change status from TODO to DONE"));
            changeStatus(userToken, issueId, "IN_REVIEW")
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail").value("Cannot change status from TODO to IN_REVIEW"));
            changeStatus(ownerToken, issueId, "DONE").andExpect(status().isConflict());
            getOne(userToken, issueId).andExpect(jsonPath("$.status").value("TODO"));
        }

        @Test
        void issueCanGoBackAndBeReopened() throws Exception {
            String issueId = createId(userToken, issue("TASK", "My task", ""));

            changeStatus(userToken, issueId, "IN_PROGRESS").andExpect(status().isOk());
            changeStatus(userToken, issueId, "TODO").andExpect(jsonPath("$.status").value("TODO"));
            changeStatus(userToken, issueId, "IN_PROGRESS").andExpect(status().isOk());
            changeStatus(userToken, issueId, "IN_REVIEW").andExpect(status().isOk());
            changeStatus(userToken, issueId, "IN_PROGRESS").andExpect(jsonPath("$.status").value("IN_PROGRESS"));
            changeStatus(userToken, issueId, "IN_REVIEW").andExpect(status().isOk());
            changeStatus(userToken, issueId, "DONE").andExpect(status().isOk());
            changeStatus(userToken, issueId, "TODO")
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail").value("Cannot change status from DONE to TODO"));
            changeStatus(userToken, issueId, "IN_PROGRESS").andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        }

        @Test
        void sameOrUnknownStatusIsRejected() throws Exception {
            String issueId = createId(userToken, issue("TASK", "My task", ""));

            changeStatus(userToken, issueId, "TODO")
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail").value("Issue already has status TODO"));
            changeStatus(userToken, issueId, "CLOSED").andExpect(status().isBadRequest());
        }
    }

    @Nested
    class Assign {

        @Test
        void extendedAssignsAnyProjectMember() throws Exception {
            String issueId = createId(userToken, issue("TASK", "Some task", ""));

            assign(extendedToken, issueId, secondUserProjectMemberId)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.assigneeId").value(secondUserProjectMemberId))
                    .andExpect(jsonPath("$.assigneeUsername").value("member2"));
            assign(orgAdminToken, issueId, null).andExpect(jsonPath("$.assigneeId").value(nullValue()));
        }

        @Test
        void userTakesFreeIssueAndReleasesIt() throws Exception {
            String issueId = createId(secondUserToken, issue("TASK", "Free task", ""));

            assign(userToken, issueId, userProjectMemberId)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.assigneeUsername").value("member"));
            assign(userToken, issueId, null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.assigneeId").value(nullValue()));
        }

        @Test
        void userCannotAssignOthersOrTakeTakenIssue() throws Exception {
            String freeId = createId(userToken, issue("TASK", "Free task", ""));
            String takenId = createId(userToken, issue("TASK", "Taken task", ""));
            assign(extendedToken, takenId, secondUserProjectMemberId).andExpect(status().isOk());

            assign(userToken, freeId, secondUserProjectMemberId).andExpect(status().isForbidden());
            assign(userToken, takenId, userProjectMemberId).andExpect(status().isForbidden());
            assign(userToken, takenId, null)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail").value("Project USER can only take a free issue or release their own"));
        }

        @Test
        void emptyOrNullAssigneeBodyIsRejectedAndChangesNothing() throws Exception {
            String issueId = createId(userToken, issue("TASK", "Assigned task", ""));
            assign(extendedToken, issueId, userProjectMemberId).andExpect(status().isOk());

            patchRaw(extendedToken, issueId, "/assignee", "{}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("projectMemberId: must not be null"));
            patchRaw(extendedToken, issueId, "/assignee", "{\"projectMemberId\":null}").andExpect(status().isBadRequest());

            getOne(userToken, issueId).andExpect(jsonPath("$.assigneeId").value(userProjectMemberId));
        }

        @Test
        void unassignEndpointFollowsSameRules() throws Exception {
            String takenId = createId(userToken, issue("TASK", "Taken task", ""));
            assign(extendedToken, takenId, secondUserProjectMemberId).andExpect(status().isOk());

            mockMvc.perform(withToken(delete(issuesPath(projectId) + "/" + takenId + "/assignee"), userToken))
                    .andExpect(status().isForbidden());
            mockMvc.perform(withToken(delete(issuesPath(projectId) + "/" + takenId + "/assignee"), secondUserToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(takenId))
                    .andExpect(jsonPath("$.assigneeId").value(nullValue()));
        }

        @Test
        void assigneeMustBeMemberOfThisProject() throws Exception {
            String issueId = createId(userToken, issue("TASK", "Some task", ""));

            assign(ownerToken, issueId, otherProjectMemberId)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value("Project member with ID " + otherProjectMemberId + " not found"));
            assign(ownerToken, issueId, userMemberId).andExpect(status().isNotFound());
            assign(ownerToken, issueId, UUID.randomUUID().toString()).andExpect(status().isNotFound());
        }

        @Test
        void anyProjectMemberAssignsFreeIssueToSelf() throws Exception {
            String issueId = createId(extendedToken, issue("TASK", "Free task", ""));

            assignToMe(userToken, issueId)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.assigneeId").value(userProjectMemberId))
                    .andExpect(jsonPath("$.assigneeUsername").value("member"));
        }

        @Test
        void userCannotTakeIssueAssignedToSomeoneElseButExtendedCan() throws Exception {
            String issueId = createId(extendedToken, issue("TASK", "Taken task", ""));
            assign(extendedToken, issueId, secondUserProjectMemberId).andExpect(status().isOk());

            assignToMe(userToken, issueId)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail").value("Project USER can only take a free issue or release their own"));
            assignToMe(extendedToken, issueId)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.assigneeId").value(extendedProjectMemberId));
        }

        @Test
        void organizationOwnerMustJoinProjectBeforeAssigningSelf() throws Exception {
            String issueId = createId(extendedToken, issue("TASK", "Owner task", ""));

            assignToMe(ownerToken, issueId)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail").value("Add yourself to the project before assigning issues to yourself"));

            String ownerMemberId = read(mockMvc.perform(withToken(get("/organizations"), ownerToken)).andReturn(), "$.content[0].memberId");
            String ownerProjectMemberId = addToProject(projectId, ownerMemberId, "ADMIN");
            assignToMe(ownerToken, issueId)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.assigneeId").value(ownerProjectMemberId))
                    .andExpect(jsonPath("$.assigneeUsername").value("owner"));
        }
    }

    @Nested
    class Parent {

        @Test
        void extendedMovesIssueIntoEpicAndOut() throws Exception {
            String epicId = createId(extendedToken, issue("EPIC", "Big epic", ""));
            String taskId = createId(userToken, issue("TASK", "Loose task", ""));

            changeParent(extendedToken, taskId, epicId)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.parentId").value(epicId));
            changeParent(extendedToken, taskId, null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.parentId").value(nullValue()));
        }

        @Test
        void emptyOrNullParentBodyIsRejectedAndChangesNothing() throws Exception {
            String epicId = createId(extendedToken, issue("EPIC", "Big epic", ""));
            String taskId = createId(extendedToken, issue("TASK", "Child task", ",\"parentId\":\"" + epicId + "\""));

            patchRaw(extendedToken, taskId, "/parent", "{}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("parentId: must not be null"));
            patchRaw(extendedToken, taskId, "/parent", "{\"parentId\":null}").andExpect(status().isBadRequest());

            getOne(userToken, taskId).andExpect(jsonPath("$.parentId").value(epicId));
        }

        @Test
        void userCannotDetachFromEpic() throws Exception {
            String epicId = createId(extendedToken, issue("EPIC", "Big epic", ""));
            String taskId = createId(extendedToken, issue("TASK", "Child task", ",\"parentId\":\"" + epicId + "\""));

            changeParent(userToken, taskId, null).andExpect(status().isForbidden());
            getOne(userToken, taskId).andExpect(jsonPath("$.parentId").value(epicId));
        }

        @Test
        void userCannotChangeEpic() throws Exception {
            String epicId = createId(extendedToken, issue("EPIC", "Big epic", ""));
            String taskId = createId(userToken, issue("TASK", "My task", ""));

            changeParent(userToken, taskId, epicId)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail").value("Only project ADMIN or EXTENDED can change the epic of an issue"));
        }

        @Test
        void hierarchyRulesAreEnforced() throws Exception {
            String epicId = createId(extendedToken, issue("EPIC", "Big epic", ""));
            String otherEpicId = createId(extendedToken, issue("EPIC", "Other epic", ""));
            String storyId = createId(userToken, issue("STORY", "A story", ""));
            String taskId = createId(userToken, issue("TASK", "A task", ""));
            String foreignEpicId = createId(ownerToken, otherProjectId, issue("EPIC", "Foreign epic", ""));

            changeParent(extendedToken, epicId, otherEpicId)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("EPIC cannot have a parent"));
            changeParent(extendedToken, taskId, storyId)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("Only EPIC can be a parent"));
            changeParent(extendedToken, taskId, foreignEpicId).andExpect(status().isNotFound());
        }
    }

    @Nested
    class Delete {

        @Test
        void reporterDeletesOwnIssue() throws Exception {
            String issueId = createId(userToken, issue("TASK", "My task", ""));

            remove(userToken, issueId).andExpect(status().isNoContent());
            getOne(userToken, issueId).andExpect(status().isNotFound());
        }

        @Test
        void userAndExtendedCannotDeleteOthersIssues() throws Exception {
            String issueId = createId(secondUserToken, issue("TASK", "Their task", ""));

            remove(userToken, issueId).andExpect(status().isForbidden());
            remove(extendedToken, issueId)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail").value("Only the reporter or project ADMIN can delete this issue"));
            assertThat(issueCount()).isEqualTo(1);
        }

        @Test
        void projectAdminAndOrganizationOwnerDeleteAnyIssue() throws Exception {
            String first = createId(userToken, issue("TASK", "First task", ""));
            String second = createId(userToken, issue("TASK", "Second task", ""));

            remove(projectAdminToken, first).andExpect(status().isNoContent());
            remove(ownerToken, second).andExpect(status().isNoContent());
            assertThat(issueCount()).isZero();
        }

        @Test
        void deletingEpicKeepsChildrenWithoutParent() throws Exception {
            String epicId = createId(extendedToken, issue("EPIC", "Big epic", ""));
            String childId = createId(extendedToken, issue("TASK", "Child task", ",\"parentId\":\"" + epicId + "\""));

            remove(projectAdminToken, epicId).andExpect(status().isNoContent());

            getOne(userToken, childId)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.parentId").value(nullValue()));
        }
    }

    @Nested
    class Cascades {

        @Test
        void removingProjectMemberUnassignsTheirIssues() throws Exception {
            String issueId = createId(secondUserToken, issue("TASK", "Assigned task", ""));
            assign(extendedToken, issueId, userProjectMemberId).andExpect(status().isOk());

            mockMvc.perform(withToken(delete("/organizations/{o}/projects/{p}/members/{m}", organizationId, projectId, userProjectMemberId), ownerToken))
                    .andExpect(status().isNoContent());

            getOne(ownerToken, issueId)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.assigneeId").value(nullValue()));
        }

        @Test
        void leavingOrganizationKeepsReportedIssuesAndUnassigns() throws Exception {
            String reported = createId(userToken, issue("TASK", "Reported task", ""));
            assign(userToken, reported, userProjectMemberId).andExpect(status().isOk());

            leave(userToken, organizationId).andExpect(status().isNoContent());

            getOne(ownerToken, reported)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.reporterUsername").value("member"))
                    .andExpect(jsonPath("$.assigneeId").value(nullValue()));
        }

        @Test
        void deletingProjectDeletesItsIssues() throws Exception {
            createId(userToken, issue("TASK", "Doomed task", ""));
            createId(ownerToken, otherProjectId, issue("TASK", "Survivor task", ""));

            mockMvc.perform(withToken(delete("/organizations/{o}/projects/{p}", organizationId, projectId), ownerToken))
                    .andExpect(status().isNoContent());

            assertThat(issueCount()).isEqualTo(1);
        }

        @Test
        void deletingOrganizationWithIssuesAndMembersSucceeds() throws Exception {
            String epicId = createId(extendedToken, issue("EPIC", "Big epic", ""));
            String taskId = createId(extendedToken, issue("TASK", "Child task", ",\"parentId\":\"" + epicId + "\""));
            assign(extendedToken, taskId, userProjectMemberId).andExpect(status().isOk());

            mockMvc.perform(withToken(delete("/organizations/{id}", organizationId), ownerToken))
                    .andExpect(status().isNoContent());

            assertThat(issueCount()).isZero();
        }
    }

    @Nested
    class DatabaseGuards {

        @Test
        void databaseRejectsInconsistentIssues() {
            String reporter = jdbcTemplate.queryForObject("SELECT id::text FROM users WHERE username = 'member'", String.class);

            assertThat(insertRaw("BUG", reporter, "NULL", "NULL")).isFalse();
            assertThat(insertRaw("TASK", reporter, "'MAJOR'", "NULL")).isFalse();
            assertThat(insertRaw("TASK", reporter, "NULL", "5")).isFalse();
            assertThat(insertRaw("STORY", reporter, "NULL", "5")).isTrue();
        }

        private boolean insertRaw(String type, String reporter, String severity, String storyPoints) {
            try {
                jdbcTemplate.update("""
                        INSERT INTO issues (id, type, project_id, reporter_id, title, status, priority, severity, story_points,
                                            version, created_at, updated_at)
                        VALUES (gen_random_uuid(), ?, ?::uuid, ?::uuid, 'Raw issue', 'TODO', 'MEDIUM', %s, %s, 0, now(), now())
                        """.formatted(severity, storyPoints), type, projectId, reporter);
                return true;
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                return false;
            }
        }
    }

    private String createProject(String orgId, String token, String title) throws Exception {
        return read(mockMvc.perform(withToken(post("/organizations/{id}/projects", orgId), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"%s\"}".formatted(title)))
                .andExpect(status().isCreated())
                .andReturn(), "$.id");
    }

    private String addToProject(String targetProjectId, String organizationMemberId, String role) throws Exception {
        return read(mockMvc.perform(withToken(post("/organizations/{o}/projects/{p}/members", organizationId, targetProjectId), ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"organizationMemberId\":\"%s\",\"role\":\"%s\"}".formatted(organizationMemberId, role)))
                .andExpect(status().isCreated())
                .andReturn(), "$.id");
    }

    private void archiveProject() throws Exception {
        mockMvc.perform(withToken(patch("/organizations/{o}/projects/{p}/status", organizationId, projectId), ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ARCHIVED\"}"))
                .andExpect(status().isOk());
    }

    private String issuesPath(String targetProjectId) {
        return "/organizations/" + organizationId + "/projects/" + targetProjectId + "/issues";
    }

    private ResultActions create(String token, String body) throws Exception {
        return create(token, projectId, body);
    }

    private ResultActions create(String token, String targetProjectId, String body) throws Exception {
        return mockMvc.perform(withToken(post(issuesPath(targetProjectId)), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String createId(String token, String body) throws Exception {
        return createId(token, projectId, body);
    }

    private String createId(String token, String targetProjectId, String body) throws Exception {
        return read(create(token, targetProjectId, body).andExpect(status().isCreated()).andReturn(), "$.id");
    }

    private ResultActions list(String token, String query) throws Exception {
        return mockMvc.perform(withToken(get(issuesPath(projectId) + query), token));
    }

    private ResultActions getOne(String token, String issueId) throws Exception {
        return mockMvc.perform(withToken(get(issuesPath(projectId) + "/" + issueId), token));
    }

    private ResultActions update(String token, String issueId, String body) throws Exception {
        return mockMvc.perform(withToken(patch(issuesPath(projectId) + "/" + issueId), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions changeStatus(String token, String issueId, String status) throws Exception {
        return mockMvc.perform(withToken(patch(issuesPath(projectId) + "/" + issueId + "/status"), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"%s\"}".formatted(status)));
    }

    private ResultActions assign(String token, String issueId, String projectMemberId) throws Exception {
        if (projectMemberId == null) {
            return mockMvc.perform(withToken(delete(issuesPath(projectId) + "/" + issueId + "/assignee"), token));
        }
        return patchRaw(token, issueId, "/assignee", "{\"projectMemberId\":\"%s\"}".formatted(projectMemberId));
    }

    private ResultActions assignToMe(String token, String issueId) throws Exception {
        return mockMvc.perform(withToken(patch(issuesPath(projectId) + "/" + issueId + "/assignee/me"), token));
    }

    private ResultActions changeParent(String token, String issueId, String parentId) throws Exception {
        if (parentId == null) {
            return mockMvc.perform(withToken(delete(issuesPath(projectId) + "/" + issueId + "/parent"), token));
        }
        return patchRaw(token, issueId, "/parent", "{\"parentId\":\"%s\"}".formatted(parentId));
    }

    private ResultActions patchRaw(String token, String issueId, String suffix, String body) throws Exception {
        return mockMvc.perform(withToken(patch(issuesPath(projectId) + "/" + issueId + suffix), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions remove(String token, String issueId) throws Exception {
        return mockMvc.perform(withToken(delete(issuesPath(projectId) + "/" + issueId), token));
    }

    private Integer issueCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM issues", Integer.class);
    }

    private static String issue(String type, String title, String extra) {
        return "{\"type\":\"%s\",\"title\":\"%s\"%s}".formatted(type, title, extra);
    }
}

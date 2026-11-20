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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IssueCommentAndHistoryTest extends AbstractIntegrationTest {
    private String ownerToken;
    private String projectAdminToken;
    private String extendedToken;
    private String userToken;
    private String secondUserToken;
    private String strangerToken;
    private String outsiderToken;

    private String organizationId;
    private String projectId;
    private String otherProjectId;
    private String userProjectMemberId;
    private String issueId;

    @BeforeEach
    void setUp() throws Exception {
        ownerToken = registerAndLogin("owner");
        projectAdminToken = registerAndLogin("padmin");
        extendedToken = registerAndLogin("extended");
        userToken = registerAndLogin("member");
        secondUserToken = registerAndLogin("member2");
        strangerToken = registerAndLogin("stranger");
        outsiderToken = registerAndLogin("outsider");

        organizationId = createOrganization(ownerToken, "MyOrganization");
        String code = getInviteCode(ownerToken, organizationId);
        String projectAdminMember = joinAndGetMemberId(projectAdminToken, code);
        String extendedMember = joinAndGetMemberId(extendedToken, code);
        String userMember = joinAndGetMemberId(userToken, code);
        String secondUserMember = joinAndGetMemberId(secondUserToken, code);
        joinAndGetMemberId(strangerToken, code);

        projectId = createProject("Backend Rewrite");
        otherProjectId = createProject("Frontend Rewrite");
        addToProject(projectId, projectAdminMember, "ADMIN");
        addToProject(projectId, extendedMember, "EXTENDED");
        userProjectMemberId = addToProject(projectId, userMember, "USER");
        addToProject(projectId, secondUserMember, "USER");

        issueId = createIssue(userToken, projectId, "{\"type\":\"STORY\",\"title\":\"Login page\",\"storyPoints\":3}");
    }

    @Nested
    class Comments {

        @Test
        void projectMemberAddsComment() throws Exception {
            comment(secondUserToken, issueId, "Looks good")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(notNullValue()))
                    .andExpect(jsonPath("$.issueId").value(issueId))
                    .andExpect(jsonPath("$.authorUsername").value("member2"))
                    .andExpect(jsonPath("$.text").value("Looks good"))
                    .andExpect(jsonPath("$.createdAt").value(notNullValue()))
                    .andExpect(jsonPath("$.editedAt").value(nullValue()));
            comment(ownerToken, issueId, "Owner without project membership").andExpect(status().isCreated());
        }

        @Test
        void strangerAndOutsiderCannotCommentOrRead() throws Exception {
            comment(strangerToken, issueId, "Hi").andExpect(status().isForbidden());
            comment(outsiderToken, issueId, "Hi").andExpect(status().isForbidden());
            comments(strangerToken, issueId).andExpect(status().isForbidden());
        }

        @Test
        void invalidCommentIsRejected() throws Exception {
            comment(userToken, issueId, "   ").andExpect(status().isBadRequest());
            comment(userToken, issueId, "a".repeat(5001)).andExpect(status().isBadRequest());
            commentRaw(userToken, issueId, "{}").andExpect(status().isBadRequest());
        }

        @Test
        void commentIsTrimmedAndListedInOrderWithPagination() throws Exception {
            comment(userToken, issueId, "  First  ").andExpect(jsonPath("$.text").value("First"));
            comment(extendedToken, issueId, "Second").andExpect(status().isCreated());
            comment(userToken, issueId, "Third").andExpect(status().isCreated());

            comments(secondUserToken, issueId)
                    .andExpect(jsonPath("$.content[*].text").value(contains("First", "Second", "Third")))
                    .andExpect(jsonPath("$.totalElements").value(3));
            mockMvc.perform(withToken(get(commentsPath(issueId) + "?page=1&size=2"), userToken))
                    .andExpect(jsonPath("$.content[*].text").value(contains("Third")));
        }

        @Test
        void unknownOrForeignIssueReturnsNotFound() throws Exception {
            String foreignIssue = createIssue(ownerToken, otherProjectId, "{\"type\":\"TASK\",\"title\":\"Other task\"}");

            comment(userToken, UUID.randomUUID().toString(), "Hi").andExpect(status().isNotFound());
            comment(ownerToken, foreignIssue, "Hi").andExpect(status().isNotFound());
        }

        @Test
        void onlyAuthorEditsComment() throws Exception {
            String commentId = read(comment(userToken, issueId, "Original").andReturn(), "$.id");

            edit(userToken, issueId, commentId, "Edited")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.text").value("Edited"))
                    .andExpect(jsonPath("$.editedAt").value(notNullValue()));
            edit(projectAdminToken, issueId, commentId, "By admin")
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail").value("Only the author can edit this comment"));
            edit(ownerToken, issueId, commentId, "By owner").andExpect(status().isForbidden());
            edit(userToken, issueId, commentId, " ").andExpect(status().isBadRequest());
        }

        @Test
        void authorAndProjectAdminDeleteButOthersCannot() throws Exception {
            String first = read(comment(userToken, issueId, "First").andReturn(), "$.id");
            String second = read(comment(userToken, issueId, "Second").andReturn(), "$.id");
            String third = read(comment(userToken, issueId, "Third").andReturn(), "$.id");

            deleteComment(extendedToken, issueId, first)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail").value("Only the author or project ADMIN can delete this comment"));
            deleteComment(secondUserToken, issueId, first).andExpect(status().isForbidden());
            deleteComment(userToken, issueId, first).andExpect(status().isNoContent());
            deleteComment(projectAdminToken, issueId, second).andExpect(status().isNoContent());
            deleteComment(ownerToken, issueId, third).andExpect(status().isNoContent());
            deleteComment(ownerToken, issueId, third)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value("Comment with ID " + third + " not found"));
        }

        @Test
        void commentOfAnotherIssueIsNotReachable() throws Exception {
            String otherIssue = createIssue(userToken, projectId, "{\"type\":\"TASK\",\"title\":\"Other issue\"}");
            String commentId = read(comment(userToken, otherIssue, "Elsewhere").andReturn(), "$.id");

            edit(userToken, issueId, commentId, "Hijack").andExpect(status().isNotFound());
            deleteComment(userToken, issueId, commentId).andExpect(status().isNotFound());
        }

        @Test
        void archivedProjectCommentsAreReadOnly() throws Exception {
            String commentId = read(comment(userToken, issueId, "Before archive").andReturn(), "$.id");
            archiveProject();

            comment(userToken, issueId, "After archive").andExpect(status().isConflict());
            edit(userToken, issueId, commentId, "Edited").andExpect(status().isConflict());
            deleteComment(userToken, issueId, commentId).andExpect(status().isConflict());
            comments(userToken, issueId).andExpect(jsonPath("$.content", hasSize(1)));
        }

        @Test
        void commentsSurviveAuthorLeavingAndDisappearWithIssue() throws Exception {
            comment(userToken, issueId, "Bye").andExpect(status().isCreated());
            leave(userToken, organizationId).andExpect(status().isNoContent());

            comments(ownerToken, issueId).andExpect(jsonPath("$.content[0].authorUsername").value("member"));

            mockMvc.perform(withToken(delete(issuePath(issueId)), ownerToken)).andExpect(status().isNoContent());
            assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM issue_comments", Integer.class)).isZero();
        }
    }

    @Nested
    class History {

        @Test
        void creationIsRecorded() throws Exception {
            history(userToken, issueId)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].type").value("CREATED"))
                    .andExpect(jsonPath("$.content[0].newValue").value("STORY"))
                    .andExpect(jsonPath("$.content[0].actorUsername").value("member"));
        }

        @Test
        void onlyChangedFieldsAreRecorded() throws Exception {
            mockMvc.perform(withToken(patch(issuePath(issueId)), userToken).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Login page v2\",\"storyPoints\":8,\"description\":null}"))
                    .andExpect(status().isOk());

            history(userToken, issueId)
                    .andExpect(jsonPath("$.content", hasSize(3)))
                    .andExpect(jsonPath("$.content[1].type").value("FIELD_CHANGED"))
                    .andExpect(jsonPath("$.content[1].field").value("title"))
                    .andExpect(jsonPath("$.content[1].oldValue").value("Login page"))
                    .andExpect(jsonPath("$.content[1].newValue").value("Login page v2"))
                    .andExpect(jsonPath("$.content[2].field").value("storyPoints"))
                    .andExpect(jsonPath("$.content[2].oldValue").value("3"))
                    .andExpect(jsonPath("$.content[2].newValue").value("8"));
        }

        @Test
        void noOpUpdateRecordsNothing() throws Exception {
            mockMvc.perform(withToken(patch(issuePath(issueId)), userToken).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Login page\"}"))
                    .andExpect(status().isOk());

            history(userToken, issueId).andExpect(jsonPath("$.content", hasSize(1)));
        }

        @Test
        void statusAssigneeAndParentChangesAreRecorded() throws Exception {
            String epicId = createIssue(extendedToken, projectId, "{\"type\":\"EPIC\",\"title\":\"Big epic\"}");
            patchJson(extendedToken, issueId, "/status", "{\"status\":\"IN_PROGRESS\"}").andExpect(status().isOk());
            patchJson(extendedToken, issueId, "/assignee", "{\"projectMemberId\":\"" + userProjectMemberId + "\"}").andExpect(status().isOk());
            patchJson(extendedToken, issueId, "/parent", "{\"parentId\":\"" + epicId + "\"}").andExpect(status().isOk());
            mockMvc.perform(withToken(delete(issuePath(issueId) + "/assignee"), userToken)).andExpect(status().isOk());
            mockMvc.perform(withToken(delete(issuePath(issueId) + "/parent"), extendedToken)).andExpect(status().isOk());

            history(userToken, issueId)
                    .andExpect(jsonPath("$.content[*].type").value(contains(
                            "CREATED", "STATUS_CHANGED", "ASSIGNEE_CHANGED", "PARENT_CHANGED", "ASSIGNEE_CHANGED", "PARENT_CHANGED")))
                    .andExpect(jsonPath("$.content[1].oldValue").value("TODO"))
                    .andExpect(jsonPath("$.content[1].newValue").value("IN_PROGRESS"))
                    .andExpect(jsonPath("$.content[1].actorUsername").value("extended"))
                    .andExpect(jsonPath("$.content[2].oldValue").value(nullValue()))
                    .andExpect(jsonPath("$.content[2].newValue").value("member"))
                    .andExpect(jsonPath("$.content[3].newValue").value(epicId))
                    .andExpect(jsonPath("$.content[4].oldValue").value("member"))
                    .andExpect(jsonPath("$.content[4].newValue").value(nullValue()))
                    .andExpect(jsonPath("$.content[4].actorUsername").value("member"))
                    .andExpect(jsonPath("$.content[5].oldValue").value(epicId));
        }

        @Test
        void rejectedOrNoOpOperationsLeaveNoTrace() throws Exception {
            patchJson(secondUserToken, issueId, "/status", "{\"status\":\"IN_PROGRESS\"}").andExpect(status().isForbidden());
            patchJson(userToken, issueId, "/status", "{\"status\":\"DONE\"}").andExpect(status().isConflict());
            mockMvc.perform(withToken(delete(issuePath(issueId) + "/parent"), extendedToken)).andExpect(status().isOk());
            mockMvc.perform(withToken(delete(issuePath(issueId) + "/assignee"), extendedToken)).andExpect(status().isOk());

            history(userToken, issueId).andExpect(jsonPath("$.content", hasSize(1)));
        }

        @Test
        void historyIsVisibleOnlyToProjectViewers() throws Exception {
            history(strangerToken, issueId).andExpect(status().isForbidden());
            history(outsiderToken, issueId).andExpect(status().isForbidden());
            history(ownerToken, issueId).andExpect(status().isOk());
            history(userToken, UUID.randomUUID().toString()).andExpect(status().isNotFound());
        }

        @Test
        void historyIsDeletedWithIssue() throws Exception {
            patchJson(userToken, issueId, "/status", "{\"status\":\"IN_PROGRESS\"}").andExpect(status().isOk());

            mockMvc.perform(withToken(delete(issuePath(issueId)), userToken)).andExpect(status().isNoContent());

            assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM issue_events", Integer.class)).isZero();
        }
    }

    private String createProject(String title) throws Exception {
        return read(mockMvc.perform(withToken(post("/organizations/{o}/projects", organizationId), ownerToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"%s\"}".formatted(title)))
                .andExpect(status().isCreated()).andReturn(), "$.id");
    }

    private String addToProject(String targetProjectId, String memberId, String role) throws Exception {
        return read(mockMvc.perform(withToken(post("/organizations/{o}/projects/{p}/members", organizationId, targetProjectId), ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"organizationMemberId\":\"%s\",\"role\":\"%s\"}".formatted(memberId, role)))
                .andExpect(status().isCreated()).andReturn(), "$.id");
    }

    private String createIssue(String token, String targetProjectId, String body) throws Exception {
        return read(mockMvc.perform(withToken(post("/organizations/{o}/projects/{p}/issues", organizationId, targetProjectId), token)
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn(), "$.id");
    }

    private void archiveProject() throws Exception {
        mockMvc.perform(withToken(patch("/organizations/{o}/projects/{p}/status", organizationId, projectId), ownerToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ARCHIVED\"}"))
                .andExpect(status().isOk());
    }

    private String issuePath(String id) {
        return "/organizations/" + organizationId + "/projects/" + projectId + "/issues/" + id;
    }

    private String commentsPath(String id) {
        return issuePath(id) + "/comments";
    }

    private ResultActions comment(String token, String id, String text) throws Exception {
        return commentRaw(token, id, "{\"text\":\"%s\"}".formatted(text));
    }

    private ResultActions commentRaw(String token, String id, String body) throws Exception {
        return mockMvc.perform(withToken(post(commentsPath(id)), token).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions comments(String token, String id) throws Exception {
        return mockMvc.perform(withToken(get(commentsPath(id)), token));
    }

    private ResultActions edit(String token, String id, String commentId, String text) throws Exception {
        return mockMvc.perform(withToken(patch(commentsPath(id) + "/" + commentId), token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"%s\"}".formatted(text)));
    }

    private ResultActions deleteComment(String token, String id, String commentId) throws Exception {
        return mockMvc.perform(withToken(delete(commentsPath(id) + "/" + commentId), token));
    }

    private ResultActions history(String token, String id) throws Exception {
        return mockMvc.perform(withToken(get(issuePath(id) + "/history"), token));
    }

    private ResultActions patchJson(String token, String id, String suffix, String body) throws Exception {
        return mockMvc.perform(withToken(patch(issuePath(id) + suffix), token).contentType(MediaType.APPLICATION_JSON).content(body));
    }
}

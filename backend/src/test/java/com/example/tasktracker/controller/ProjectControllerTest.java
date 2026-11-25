package com.example.tasktracker.controller;

import com.example.tasktracker.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProjectControllerTest extends AbstractIntegrationTest {
    private String ownerToken;
    private String adminToken;
    private String userToken;
    private String outsiderToken;
    private String organizationId;

    @BeforeEach
    void setUp() throws Exception {
        ownerToken = registerAndLogin("owner");
        adminToken = registerAndLogin("admin");
        userToken = registerAndLogin("member");
        outsiderToken = registerAndLogin("outsider");

        organizationId = createOrganization(ownerToken, "MyOrganization");
        String inviteCode = getInviteCode(ownerToken, organizationId);
        String adminMemberId = joinAndGetMemberId(adminToken, inviteCode);
        changeRole(ownerToken, organizationId, adminMemberId, "ADMIN").andExpect(status().isOk());
        joinAndGetMemberId(userToken, inviteCode);
    }

    @Test
    void ownerCreatesProject() throws Exception {
        create(ownerToken, """
                {"title":"Backend Rewrite","description":"Move to new stack"}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(notNullValue()))
                .andExpect(jsonPath("$.organizationId").value(organizationId))
                .andExpect(jsonPath("$.title").value("Backend Rewrite"))
                .andExpect(jsonPath("$.description").value("Move to new stack"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").value(notNullValue()));
    }

    @Test
    void adminCreatesProject() throws Exception {
        create(adminToken, titleBody("Admin Project")).andExpect(status().isCreated());
    }

    @Test
    void regularMemberCannotCreateProject() throws Exception {
        create(userToken, titleBody("User Project"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Only OWNER or ADMIN can perform this operation"));
    }

    @Test
    void outsiderCannotCreateProject() throws Exception {
        create(outsiderToken, titleBody("Hijacked Project"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Access denied to organization " + organizationId));
    }

    @Test
    void createWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/organizations/{id}/projects", organizationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(titleBody("No Token Project")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createInUnknownOrganizationReturnsNotFound() throws Exception {
        mockMvc.perform(withToken(post("/organizations/{id}/projects", UUID.randomUUID()), ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(titleBody("Ghost Project")))
                .andExpect(status().isNotFound());
    }

    @Test
    void createWithDuplicateTitleReturnsConflict() throws Exception {
        create(ownerToken, titleBody("Backend Rewrite")).andExpect(status().isCreated());

        create(adminToken, titleBody("BACKEND REWRITE"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Project with title BACKEND REWRITE already exists"));
    }

    @Test
    void createWithInvalidTitleReturnsBadRequest() throws Exception {
        create(ownerToken, titleBody("ab")).andExpect(status().isBadRequest());
        create(ownerToken, "{}").andExpect(status().isBadRequest());
        create(ownerToken, titleBody("          ")).andExpect(status().isBadRequest());
    }

    @Test
    void createWithTooLongDescriptionReturnsBadRequest() throws Exception {
        create(ownerToken, """
                {"title":"Backend Rewrite","description":"%s"}
                """.formatted("a".repeat(401)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void membersListProjectsAndCanFilterByStatus() throws Exception {
        String first = createAndGetId(ownerToken, "First Project");
        createAndGetId(ownerToken, "Second Project");
        changeStatus(ownerToken, first, "FINISHED").andExpect(status().isOk());

        list(userToken, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].title").value(contains("First Project", "Second Project")));
        list(userToken, "?status=FINISHED")
                .andExpect(jsonPath("$.content[*].title").value(contains("First Project")));
        list(userToken, "?status=ARCHIVED")
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void listIsPaginatedWithDefaultPageSize() throws Exception {
        createAndGetId(ownerToken, "First Project");

        list(userToken, "")
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void listReturnsRequestedPage() throws Exception {
        createAndGetId(ownerToken, "First Project");
        createAndGetId(ownerToken, "Second Project");
        createAndGetId(ownerToken, "Third Project");

        list(userToken, "?page=1&size=2")
                .andExpect(jsonPath("$.content[*].title").value(contains("Third Project")))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void pageSizeIsCappedAt100() throws Exception {
        list(userToken, "?size=1000").andExpect(jsonPath("$.size").value(100));
    }

    @Test
    void pageBeyondLastReturnsEmptyContent() throws Exception {
        createAndGetId(ownerToken, "First Project");

        list(userToken, "?page=5")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void negativePageIsTreatedAsFirstPage() throws Exception {
        createAndGetId(ownerToken, "First Project");

        list(userToken, "?page=-3")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void clientSortIsIgnoredAndCreationOrderKept() throws Exception {
        createAndGetId(ownerToken, "Beta Project");
        createAndGetId(ownerToken, "Alpha Project");

        list(userToken, "?sort=title,asc")
                .andExpect(jsonPath("$.content[*].title").value(contains("Beta Project", "Alpha Project")));
        list(userToken, "?sort=unknownField")
                .andExpect(status().isOk());
    }

    @Test
    void listWithUnknownStatusReturnsBadRequest() throws Exception {
        list(ownerToken, "?status=DELETED")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void outsiderCannotListProjects() throws Exception {
        list(outsiderToken, "").andExpect(status().isForbidden());
    }

    @Test
    void memberGetsProjectById() throws Exception {
        String projectId = createAndGetId(ownerToken, "Backend Rewrite");

        mockMvc.perform(withToken(get("/organizations/{o}/projects/{p}", organizationId, projectId), userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(projectId));
    }

    @Test
    void getUnknownProjectReturnsNotFound() throws Exception {
        UUID unknown = UUID.randomUUID();

        mockMvc.perform(withToken(get("/organizations/{o}/projects/{p}", organizationId, unknown), ownerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Project with ID " + unknown + " not found"));
    }

    @Test
    void projectOfAnotherOrganizationIsNotReachable() throws Exception {
        String otherOrganization = createOrganization(outsiderToken, "OtherOrganization");
        String foreignProject = mockMvc.perform(withToken(post("/organizations/{id}/projects", otherOrganization), outsiderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(titleBody("Foreign Project")))
                .andReturn().getResponse().getContentAsString().replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(withToken(get("/organizations/{o}/projects/{p}", organizationId, foreignProject), ownerToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(withToken(delete("/organizations/{o}/projects/{p}", organizationId, foreignProject), ownerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidProjectIdReturnsBadRequest() throws Exception {
        mockMvc.perform(withToken(get("/organizations/{o}/projects/not-a-uuid", organizationId), ownerToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminUpdatesProject() throws Exception {
        String projectId = createAndGetId(ownerToken, "Backend Rewrite");

        update(adminToken, projectId, """
                {"title":"Frontend Rewrite","description":"Changed"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Frontend Rewrite"))
                .andExpect(jsonPath("$.description").value("Changed"));
    }

    @Test
    void regularMemberCannotUpdateProject() throws Exception {
        String projectId = createAndGetId(ownerToken, "Backend Rewrite");

        update(userToken, projectId, titleBody("Hacked Project")).andExpect(status().isForbidden());
    }

    @Test
    void updateToTakenTitleReturnsConflict() throws Exception {
        createAndGetId(ownerToken, "Backend Rewrite");
        String projectId = createAndGetId(ownerToken, "Frontend Rewrite");

        update(ownerToken, projectId, titleBody("backend rewrite")).andExpect(status().isConflict());
    }

    @Test
    void updateWithShortTitleReturnsBadRequest() throws Exception {
        String projectId = createAndGetId(ownerToken, "Backend Rewrite");

        update(ownerToken, projectId, titleBody("ab")).andExpect(status().isBadRequest());
    }

    @Test
    void ownerChangesStatus() throws Exception {
        String projectId = createAndGetId(ownerToken, "Backend Rewrite");

        changeStatus(ownerToken, projectId, "ARCHIVED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }

    @Test
    void sameStatusReturnsConflict() throws Exception {
        String projectId = createAndGetId(ownerToken, "Backend Rewrite");

        changeStatus(ownerToken, projectId, "ACTIVE")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Project already has status ACTIVE"));
    }

    @Test
    void unknownOrMissingStatusReturnsBadRequest() throws Exception {
        String projectId = createAndGetId(ownerToken, "Backend Rewrite");

        changeStatus(ownerToken, projectId, "DELETED").andExpect(status().isBadRequest());
        mockMvc.perform(withToken(patch("/organizations/{o}/projects/{p}/status", organizationId, projectId), ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void regularMemberCannotChangeStatus() throws Exception {
        String projectId = createAndGetId(ownerToken, "Backend Rewrite");

        changeStatus(userToken, projectId, "FINISHED").andExpect(status().isForbidden());
    }

    @Test
    void ownerDeletesProject() throws Exception {
        String projectId = createAndGetId(ownerToken, "Backend Rewrite");

        mockMvc.perform(withToken(delete("/organizations/{o}/projects/{p}", organizationId, projectId), ownerToken))
                .andExpect(status().isNoContent());
        mockMvc.perform(withToken(get("/organizations/{o}/projects/{p}", organizationId, projectId), ownerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminCannotDeleteProject() throws Exception {
        String projectId = createAndGetId(ownerToken, "Backend Rewrite");

        mockMvc.perform(withToken(delete("/organizations/{o}/projects/{p}", organizationId, projectId), adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Only OWNER can perform this operation"));
    }

    private ResultActions create(String token, String body) throws Exception {
        return mockMvc.perform(withToken(post("/organizations/{id}/projects", organizationId), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String createAndGetId(String token, String title) throws Exception {
        return read(create(token, titleBody(title)).andExpect(status().isCreated()).andReturn(), "$.id");
    }

    private ResultActions list(String token, String query) throws Exception {
        return mockMvc.perform(withToken(get("/organizations/" + organizationId + "/projects" + query), token));
    }

    private ResultActions update(String token, String projectId, String body) throws Exception {
        return mockMvc.perform(withToken(patch("/organizations/{o}/projects/{p}", organizationId, projectId), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions changeStatus(String token, String projectId, String status) throws Exception {
        return mockMvc.perform(withToken(patch("/organizations/{o}/projects/{p}/status", organizationId, projectId), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"status":"%s"}
                        """.formatted(status)));
    }

    private static String titleBody(String title) {
        return """
                {"title":"%s"}
                """.formatted(title);
    }
}

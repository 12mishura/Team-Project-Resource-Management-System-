package com.example.tasktracker.controller;

import com.example.tasktracker.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
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

class ProjectMemberControllerTest extends AbstractIntegrationTest {
    private String ownerToken;
    private String adminToken;
    private String userToken;
    private String outsiderToken;

    private String organizationId;
    private String projectId;
    private String otherProjectId;
    private String adminMemberId;
    private String userMemberId;
    private String foreignOrganizationId;
    private String foreignProjectId;
    private String foreignMemberId;

    @BeforeEach
    void setUp() throws Exception {
        ownerToken = registerAndLogin("owner");
        adminToken = registerAndLogin("admin");
        userToken = registerAndLogin("member");
        outsiderToken = registerAndLogin("outsider");

        organizationId = createOrganization(ownerToken, "MyOrganization");
        String inviteCode = getInviteCode(ownerToken, organizationId);
        adminMemberId = joinAndGetMemberId(adminToken, inviteCode);
        changeRole(ownerToken, organizationId, adminMemberId, "ADMIN").andExpect(status().isOk());
        userMemberId = joinAndGetMemberId(userToken, inviteCode);
        projectId = createProject(ownerToken, organizationId, "Backend Rewrite");
        otherProjectId = createProject(ownerToken, organizationId, "Frontend Rewrite");

        foreignOrganizationId = createOrganization(outsiderToken, "ForeignOrganization");
        foreignProjectId = createProject(outsiderToken, foreignOrganizationId, "Foreign Project");
        foreignMemberId = read(getMembers(outsiderToken, foreignOrganizationId).andReturn(), "$.content[0].id");
    }

    @Test
    void ownerAddsOrganizationMemberToProject() throws Exception {
        add(ownerToken, projectId, userMemberId, "EXTENDED")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(notNullValue()))
                .andExpect(jsonPath("$.projectId").value(projectId))
                .andExpect(jsonPath("$.organizationMemberId").value(userMemberId))
                .andExpect(jsonPath("$.email").value("member@mail.com"))
                .andExpect(jsonPath("$.username").value("member"))
                .andExpect(jsonPath("$.role").value("EXTENDED"))
                .andExpect(jsonPath("$.createdAt").value(notNullValue()));
    }

    @Test
    void adminAddsMember() throws Exception {
        add(adminToken, projectId, userMemberId, "USER").andExpect(status().isCreated());
    }

    @Test
    void regularMemberCannotAddMembers() throws Exception {
        add(userToken, projectId, adminMemberId, "USER")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Only organization OWNER or ADMIN or project ADMIN can manage this project"));
    }

    @Test
    void outsiderCannotAddMembers() throws Exception {
        add(outsiderToken, projectId, userMemberId, "USER")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Access denied to organization " + organizationId));
    }

    @Test
    void addWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/organizations/{o}/projects/{p}/members", organizationId, projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addBody(userMemberId, "USER")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void memberOfAnotherOrganizationCannotBeAdded() throws Exception {
        add(ownerToken, projectId, foreignMemberId, "USER")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Member with ID " + foreignMemberId + " not found"));
        assertThat(projectMemberCount()).isZero();
    }

    @Test
    void projectOfAnotherOrganizationIsNotReachable() throws Exception {
        add(ownerToken, foreignProjectId, userMemberId, "USER").andExpect(status().isNotFound());
        list(ownerToken, foreignProjectId).andExpect(status().isNotFound());
    }

    @Test
    void unknownProjectReturnsNotFound() throws Exception {
        UUID unknown = UUID.randomUUID();

        add(ownerToken, unknown.toString(), userMemberId, "USER")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Project with ID " + unknown + " not found"));
    }

    @Test
    void unknownOrganizationMemberReturnsNotFound() throws Exception {
        add(ownerToken, projectId, UUID.randomUUID().toString(), "USER").andExpect(status().isNotFound());
    }

    @Test
    void unknownOrganizationReturnsNotFound() throws Exception {
        mockMvc.perform(withToken(post("/organizations/{o}/projects/{p}/members", UUID.randomUUID(), projectId), ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addBody(userMemberId, "USER")))
                .andExpect(status().isNotFound());
    }

    @Test
    void addingSameMemberTwiceReturnsConflict() throws Exception {
        add(ownerToken, projectId, userMemberId, "USER").andExpect(status().isCreated());

        add(adminToken, projectId, userMemberId, "ADMIN")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Member is already in project " + projectId));
    }

    @Test
    void sameMemberCanBeInDifferentProjects() throws Exception {
        add(ownerToken, projectId, userMemberId, "USER").andExpect(status().isCreated());
        add(ownerToken, otherProjectId, userMemberId, "ADMIN").andExpect(status().isCreated());
    }

    @Test
    void invalidAddRequestReturnsBadRequest() throws Exception {
        add(ownerToken, projectId, userMemberId, "OWNER").andExpect(status().isBadRequest());
        mockMvc.perform(withToken(post("/organizations/{o}/projects/{p}/members", organizationId, projectId), ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void concurrentAddsCreateExactlyOneMembership() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(6);
        List<Callable<Integer>> calls = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            calls.add(() -> add(ownerToken, projectId, userMemberId, "USER").andReturn().getResponse().getStatus());
        }
        List<Integer> statuses = new ArrayList<>();
        for (Future<Integer> future : pool.invokeAll(calls)) {
            statuses.add(future.get());
        }
        pool.shutdown();

        assertThat(statuses).containsOnlyOnce(201).containsOnly(201, 409);
        assertThat(projectMemberCount()).isEqualTo(1);
    }

    @Test
    void anyOrganizationMemberSeesProjectMembersInJoinOrder() throws Exception {
        add(ownerToken, projectId, adminMemberId, "ADMIN").andExpect(status().isCreated());
        add(ownerToken, projectId, userMemberId, "USER").andExpect(status().isCreated());
        add(ownerToken, otherProjectId, userMemberId, "USER").andExpect(status().isCreated());

        list(userToken, projectId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].username").value(contains("admin", "member")))
                .andExpect(jsonPath("$.content[*].role").value(contains("ADMIN", "USER")))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void membersListIsPaginated() throws Exception {
        add(ownerToken, projectId, adminMemberId, "ADMIN").andExpect(status().isCreated());
        add(ownerToken, projectId, userMemberId, "USER").andExpect(status().isCreated());

        mockMvc.perform(withToken(get("/organizations/{o}/projects/{p}/members?page=1&size=1", organizationId, projectId), ownerToken))
                .andExpect(jsonPath("$.content[*].username").value(contains("member")))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void outsiderCannotListProjectMembers() throws Exception {
        list(outsiderToken, projectId).andExpect(status().isForbidden());
    }

    @Test
    void listOfDeletedProjectReturnsNotFound() throws Exception {
        add(ownerToken, projectId, userMemberId, "USER").andExpect(status().isCreated());
        mockMvc.perform(withToken(delete("/organizations/{o}/projects/{p}", organizationId, projectId), ownerToken))
                .andExpect(status().isNoContent());

        list(ownerToken, projectId).andExpect(status().isNotFound());
        assertThat(projectMemberCount()).isZero();
    }

    @Test
    void adminChangesRoleInProject() throws Exception {
        String projectMemberId = addAndGetId(projectId, userMemberId, "USER");

        update(adminToken, projectId, projectMemberId, "EXTENDED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(projectMemberId))
                .andExpect(jsonPath("$.role").value("EXTENDED"));
    }

    @Test
    void assigningSameRoleReturnsConflict() throws Exception {
        String projectMemberId = addAndGetId(projectId, userMemberId, "USER");

        update(ownerToken, projectId, projectMemberId, "USER")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Project member already has role USER"));
    }

    @Test
    void regularMemberCannotChangeRoles() throws Exception {
        String projectMemberId = addAndGetId(projectId, userMemberId, "USER");

        update(userToken, projectId, projectMemberId, "ADMIN").andExpect(status().isForbidden());
    }

    @Test
    void memberOfAnotherProjectCannotBeChangedThroughThisProject() throws Exception {
        String otherProjectMemberId = addAndGetId(otherProjectId, userMemberId, "USER");

        update(ownerToken, projectId, otherProjectMemberId, "ADMIN")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Project member with ID " + otherProjectMemberId + " not found"));
    }

    @Test
    void invalidRoleOnUpdateReturnsBadRequest() throws Exception {
        String projectMemberId = addAndGetId(projectId, userMemberId, "USER");

        update(ownerToken, projectId, projectMemberId, "OWNER").andExpect(status().isBadRequest());
        mockMvc.perform(withToken(patch("/organizations/{o}/projects/{p}/members/{m}", organizationId, projectId, projectMemberId), ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ownerRemovesMemberFromProject() throws Exception {
        String projectMemberId = addAndGetId(projectId, userMemberId, "USER");

        remove(ownerToken, projectId, projectMemberId).andExpect(status().isNoContent());

        list(ownerToken, projectId).andExpect(jsonPath("$.content", hasSize(0)));
        assertThat(projectMemberCount()).isZero();
    }

    @Test
    void removedMemberCanBeAddedAgain() throws Exception {
        String projectMemberId = addAndGetId(projectId, userMemberId, "USER");
        remove(adminToken, projectId, projectMemberId).andExpect(status().isNoContent());

        add(ownerToken, projectId, userMemberId, "ADMIN").andExpect(status().isCreated());
    }

    @Test
    void regularMemberCannotRemoveMembers() throws Exception {
        String projectMemberId = addAndGetId(projectId, adminMemberId, "ADMIN");

        remove(userToken, projectId, projectMemberId).andExpect(status().isForbidden());
        assertThat(projectMemberCount()).isEqualTo(1);
    }

    @Test
    void removingUnknownOrForeignProjectMemberReturnsNotFound() throws Exception {
        String otherProjectMemberId = addAndGetId(otherProjectId, userMemberId, "USER");

        remove(ownerToken, projectId, UUID.randomUUID().toString()).andExpect(status().isNotFound());
        remove(ownerToken, projectId, otherProjectMemberId).andExpect(status().isNotFound());
        assertThat(projectMemberCount()).isEqualTo(1);
    }

    @Test
    void leavingOrganizationRemovesProjectMembership() throws Exception {
        addAndGetId(projectId, userMemberId, "USER");

        leave(userToken, organizationId).andExpect(status().isNoContent());

        list(ownerToken, projectId).andExpect(jsonPath("$.content", hasSize(0)));
    }

    private String createProject(String token, String orgId, String title) throws Exception {
        return read(mockMvc.perform(withToken(post("/organizations/{id}/projects", orgId), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"%s"}
                                """.formatted(title)))
                .andExpect(status().isCreated())
                .andReturn(), "$.id");
    }

    private ResultActions add(String token, String targetProjectId, String organizationMemberId, String role) throws Exception {
        return mockMvc.perform(withToken(post("/organizations/{o}/projects/{p}/members", organizationId, targetProjectId), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(addBody(organizationMemberId, role)));
    }

    private String addAndGetId(String targetProjectId, String organizationMemberId, String role) throws Exception {
        return read(add(ownerToken, targetProjectId, organizationMemberId, role)
                .andExpect(status().isCreated())
                .andReturn(), "$.id");
    }

    private ResultActions list(String token, String targetProjectId) throws Exception {
        return mockMvc.perform(withToken(get("/organizations/{o}/projects/{p}/members", organizationId, targetProjectId), token));
    }

    private ResultActions update(String token, String targetProjectId, String projectMemberId, String role) throws Exception {
        return mockMvc.perform(withToken(patch("/organizations/{o}/projects/{p}/members/{m}", organizationId, targetProjectId, projectMemberId), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"role":"%s"}
                        """.formatted(role)));
    }

    private ResultActions remove(String token, String targetProjectId, String projectMemberId) throws Exception {
        return mockMvc.perform(withToken(delete("/organizations/{o}/projects/{p}/members/{m}", organizationId, targetProjectId, projectMemberId), token));
    }

    private Integer projectMemberCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM project_members", Integer.class);
    }

    private static String addBody(String organizationMemberId, String role) {
        return """
                {"organizationMemberId":"%s","role":"%s"}
                """.formatted(organizationMemberId, role);
    }
}

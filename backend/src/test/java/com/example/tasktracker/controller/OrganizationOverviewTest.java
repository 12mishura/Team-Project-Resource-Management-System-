package com.example.tasktracker.controller;

import com.example.tasktracker.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrganizationOverviewTest extends AbstractIntegrationTest {
    private String ownerToken;
    private String adminToken;
    private String devToken;
    private String outsiderToken;
    private String organizationId;
    private String mainProjectId;
    private String sideProjectId;

    @BeforeEach
    void setUp() throws Exception {
        ownerToken = registerAndLogin("owner");
        adminToken = registerAndLogin("admin");
        devToken = registerAndLogin("dev");
        outsiderToken = registerAndLogin("outsider");

        organizationId = createOrganization(ownerToken, "Overview Org");
        String inviteCode = getInviteCode(ownerToken, organizationId);
        String adminMemberId = joinAndGetMemberId(adminToken, inviteCode);
        changeRole(ownerToken, organizationId, adminMemberId, "ADMIN").andExpect(status().isOk());
        String devMemberId = joinAndGetMemberId(devToken, inviteCode);

        mainProjectId = createProject("Main project");
        sideProjectId = createProject("Side project");
        String devProjectMemberId = addToProject(mainProjectId, devMemberId);

        createIssue(mainProjectId, "{\"type\":\"TASK\",\"title\":\"Open task\",\"assigneeId\":\"%s\"}".formatted(devProjectMemberId));
        createIssue(mainProjectId, "{\"type\":\"BUG\",\"title\":\"Open bug\",\"severity\":\"MAJOR\"}");
        String doneId = createIssue(mainProjectId, "{\"type\":\"TASK\",\"title\":\"Finished task\",\"assigneeId\":\"%s\"}".formatted(devProjectMemberId));
        for (String next : new String[]{"IN_PROGRESS", "IN_REVIEW", "DONE"}) {
            mockMvc.perform(withToken(patch("/organizations/{o}/projects/{p}/issues/{i}/status", organizationId, mainProjectId, doneId), ownerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"%s\"}".formatted(next)))
                    .andExpect(status().isOk());
        }
        createIssue(sideProjectId, "{\"type\":\"TASK\",\"title\":\"Side task\"}");
    }

    @Test
    void ownerSeesStatsOfEveryProject() throws Exception {
        overview(ownerToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationId").value(organizationId))
                .andExpect(jsonPath("$.projectsCount").value(2))
                .andExpect(jsonPath("$.membersCount").value(3))
                .andExpect(jsonPath("$.openIssues").value(3))
                .andExpect(jsonPath("$.assignedToMe").value(0))
                .andExpect(jsonPath("$.projects", hasSize(2)))
                .andExpect(jsonPath("$.projects[0].projectId").value(mainProjectId))
                .andExpect(jsonPath("$.projects[0].totalIssues").value(3))
                .andExpect(jsonPath("$.projects[0].openIssues").value(2))
                .andExpect(jsonPath("$.projects[0].openBugs").value(1))
                .andExpect(jsonPath("$.projects[0].issuesByStatus.TODO").value(2))
                .andExpect(jsonPath("$.projects[0].issuesByStatus.IN_PROGRESS").value(0))
                .andExpect(jsonPath("$.projects[0].issuesByStatus.DONE").value(1))
                .andExpect(jsonPath("$.projects[0].membersCount").value(1))
                .andExpect(jsonPath("$.projects[0].members", contains("dev")))
                .andExpect(jsonPath("$.projects[1].projectId").value(sideProjectId))
                .andExpect(jsonPath("$.projects[1].totalIssues").value(1))
                .andExpect(jsonPath("$.projects[1].membersCount").value(0));
    }

    @Test
    void organizationAdminAlsoSeesEveryProject() throws Exception {
        overview(adminToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projects", hasSize(2)));
    }

    @Test
    void projectUserSeesOnlyTheirProjectsAndOwnAssignments() throws Exception {
        overview(devToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectsCount").value(2))
                .andExpect(jsonPath("$.projects", hasSize(1)))
                .andExpect(jsonPath("$.projects[0].projectId").value(mainProjectId))
                .andExpect(jsonPath("$.projects[0].assignedToMe").value(1))
                .andExpect(jsonPath("$.assignedToMe").value(1))
                .andExpect(jsonPath("$.openIssues").value(2));
    }

    @Test
    void outsiderIsForbidden() throws Exception {
        overview(outsiderToken).andExpect(status().isForbidden());
    }

    private ResultActions overview(String token) throws Exception {
        return mockMvc.perform(withToken(get("/organizations/{id}/overview", organizationId), token));
    }

    private String createProject(String title) throws Exception {
        return read(mockMvc.perform(withToken(post("/organizations/{id}/projects", organizationId), ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"%s\"}".formatted(title)))
                .andExpect(status().isCreated())
                .andReturn(), "$.id");
    }

    private String addToProject(String projectId, String organizationMemberId) throws Exception {
        return read(mockMvc.perform(withToken(post("/organizations/{o}/projects/{p}/members", organizationId, projectId), ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"organizationMemberId\":\"%s\",\"role\":\"USER\"}".formatted(organizationMemberId)))
                .andExpect(status().isCreated())
                .andReturn(), "$.id");
    }

    private String createIssue(String projectId, String body) throws Exception {
        return read(mockMvc.perform(withToken(post("/organizations/{o}/projects/{p}/issues", organizationId, projectId), ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn(), "$.id");
    }
}

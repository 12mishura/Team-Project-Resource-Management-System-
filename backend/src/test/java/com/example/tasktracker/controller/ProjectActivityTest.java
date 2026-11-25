package com.example.tasktracker.controller;

import com.example.tasktracker.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProjectActivityTest extends AbstractIntegrationTest {
    private String ownerToken;
    private String memberToken;
    private String strangerToken;
    private String outsiderToken;
    private String organizationId;
    private String projectId;
    private String otherProjectId;
    private String issueId;

    @BeforeEach
    void setUp() throws Exception {
        ownerToken = registerAndLogin("owner");
        memberToken = registerAndLogin("member");
        strangerToken = registerAndLogin("stranger");
        outsiderToken = registerAndLogin("outsider");

        organizationId = createOrganization(ownerToken, "Activity Org");
        String inviteCode = getInviteCode(ownerToken, organizationId);
        String memberId = joinAndGetMemberId(memberToken, inviteCode);
        joinAndGetMemberId(strangerToken, inviteCode);

        projectId = createProject("Activity project");
        otherProjectId = createProject("Other project");
        addToProject(projectId, memberId);

        issueId = createIssue(projectId, "Tracked task");
        mockMvc.perform(withToken(patch("/organizations/{o}/projects/{p}/issues/{i}/status", organizationId, projectId, issueId), ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk());
        createIssue(otherProjectId, "Elsewhere");
    }

    @Test
    void projectMemberSeesNewestEventsOfThisProjectOnly() throws Exception {
        activity(memberToken, projectId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].type").value("STATUS_CHANGED"))
                .andExpect(jsonPath("$.content[0].issueId").value(issueId))
                .andExpect(jsonPath("$.content[0].issueTitle").value("Tracked task"))
                .andExpect(jsonPath("$.content[0].issueType").value("TASK"))
                .andExpect(jsonPath("$.content[0].oldValue").value("TODO"))
                .andExpect(jsonPath("$.content[0].newValue").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.content[0].actorUsername").value("owner"))
                .andExpect(jsonPath("$.content[1].type").value("CREATED"));
    }

    @Test
    void organizationMemberOutsideTheProjectIsForbidden() throws Exception {
        activity(strangerToken, projectId).andExpect(status().isForbidden());
    }

    @Test
    void outsiderIsForbidden() throws Exception {
        activity(outsiderToken, projectId).andExpect(status().isForbidden());
    }

    @Test
    void pageSizeIsRespected() throws Exception {
        mockMvc.perform(withToken(get("/organizations/{o}/projects/{p}/activity", organizationId, projectId), ownerToken).param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    private ResultActions activity(String token, String targetProjectId) throws Exception {
        return mockMvc.perform(withToken(get("/organizations/{o}/projects/{p}/activity", organizationId, targetProjectId), token));
    }

    private String createProject(String title) throws Exception {
        return read(mockMvc.perform(withToken(post("/organizations/{id}/projects", organizationId), ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"%s\"}".formatted(title)))
                .andExpect(status().isCreated())
                .andReturn(), "$.id");
    }

    private void addToProject(String targetProjectId, String organizationMemberId) throws Exception {
        mockMvc.perform(withToken(post("/organizations/{o}/projects/{p}/members", organizationId, targetProjectId), ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"organizationMemberId\":\"%s\",\"role\":\"USER\"}".formatted(organizationMemberId)))
                .andExpect(status().isCreated());
    }

    private String createIssue(String targetProjectId, String title) throws Exception {
        return read(mockMvc.perform(withToken(post("/organizations/{o}/projects/{p}/issues", organizationId, targetProjectId), ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"TASK\",\"title\":\"%s\"}".formatted(title)))
                .andExpect(status().isCreated())
                .andReturn(), "$.id");
    }
}

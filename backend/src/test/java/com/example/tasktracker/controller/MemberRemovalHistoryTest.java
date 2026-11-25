package com.example.tasktracker.controller;

import com.example.tasktracker.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MemberRemovalHistoryTest extends AbstractIntegrationTest {
    private String ownerToken;
    private String devToken;
    private String organizationId;
    private String devMemberId;
    private String projectId;
    private String devProjectMemberId;
    private String issueId;

    @BeforeEach
    void setUp() throws Exception {
        ownerToken = registerAndLogin("owner");
        devToken = registerAndLogin("dev");
        organizationId = createOrganization(ownerToken, "Removal Org");
        devMemberId = joinAndGetMemberId(devToken, getInviteCode(ownerToken, organizationId));

        projectId = read(mockMvc.perform(withToken(post("/organizations/{id}/projects", organizationId), ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Removal project\"}"))
                .andExpect(status().isCreated())
                .andReturn(), "$.id");
        devProjectMemberId = read(mockMvc.perform(withToken(post("/organizations/{o}/projects/{p}/members", organizationId, projectId), ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"organizationMemberId\":\"%s\",\"role\":\"USER\"}".formatted(devMemberId)))
                .andExpect(status().isCreated())
                .andReturn(), "$.id");
        issueId = read(mockMvc.perform(withToken(post("/organizations/{o}/projects/{p}/issues", organizationId, projectId), ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"TASK\",\"title\":\"Assigned work\",\"assigneeId\":\"%s\"}".formatted(devProjectMemberId)))
                .andExpect(status().isCreated())
                .andReturn(), "$.id");
    }

    @Test
    void removingAProjectMemberUnassignsTheirIssuesInHistory() throws Exception {
        mockMvc.perform(withToken(delete("/organizations/{o}/projects/{p}/members/{m}", organizationId, projectId, devProjectMemberId), ownerToken))
                .andExpect(status().isNoContent());

        expectReleasedBy("owner");
    }

    @Test
    void leavingTheOrganizationUnassignsIssuesInHistory() throws Exception {
        leave(devToken, organizationId).andExpect(status().isNoContent());

        expectReleasedBy("dev");
    }

    @Test
    void removingAnOrganizationMemberUnassignsIssuesInHistory() throws Exception {
        removeMember(ownerToken, organizationId, devMemberId).andExpect(status().isNoContent());

        expectReleasedBy("owner");
    }

    private void expectReleasedBy(String actor) throws Exception {
        mockMvc.perform(withToken(get("/organizations/{o}/projects/{p}/issues/{i}", organizationId, projectId, issueId), ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigneeId").doesNotExist());
        mockMvc.perform(withToken(get("/organizations/{o}/projects/{p}/issues/{i}/history", organizationId, projectId, issueId), ownerToken))
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[2].type").value("ASSIGNEE_CHANGED"))
                .andExpect(jsonPath("$.content[2].oldValue").value("dev"))
                .andExpect(jsonPath("$.content[2].newValue").value(nullValue()))
                .andExpect(jsonPath("$.content[2].actorUsername").value(actor));
    }
}

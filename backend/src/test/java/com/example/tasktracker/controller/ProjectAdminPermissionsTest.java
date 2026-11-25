package com.example.tasktracker.controller;

import com.example.tasktracker.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProjectAdminPermissionsTest extends AbstractIntegrationTest {
    private static final String DENIED = "Only organization OWNER or ADMIN or project ADMIN can manage this project";

    private String ownerToken;
    private String projectAdminToken;
    private String extendedToken;
    private String userToken;
    private String organizationId;
    private String projectId;
    private String otherProjectId;
    private String projectAdminMembership;
    private String extendedMembership;
    private String userMembership;
    private String newcomerMemberId;

    @BeforeEach
    void setUp() throws Exception {
        ownerToken = registerAndLogin("owner");
        projectAdminToken = registerAndLogin("padmin");
        extendedToken = registerAndLogin("extended");
        userToken = registerAndLogin("member");
        String newcomerToken = registerAndLogin("newcomer");

        organizationId = createOrganization(ownerToken, "Delegated Org");
        String inviteCode = getInviteCode(ownerToken, organizationId);
        String projectAdminMemberId = joinAndGetMemberId(projectAdminToken, inviteCode);
        String extendedMemberId = joinAndGetMemberId(extendedToken, inviteCode);
        String userMemberId = joinAndGetMemberId(userToken, inviteCode);
        newcomerMemberId = joinAndGetMemberId(newcomerToken, inviteCode);

        projectId = createProject("Delegated project");
        otherProjectId = createProject("Other project");
        projectAdminMembership = read(addMember(ownerToken, projectId, projectAdminMemberId, "ADMIN").andExpect(status().isCreated()).andReturn(), "$.id");
        extendedMembership = read(addMember(ownerToken, projectId, extendedMemberId, "EXTENDED").andExpect(status().isCreated()).andReturn(), "$.id");
        userMembership = read(addMember(ownerToken, projectId, userMemberId, "USER").andExpect(status().isCreated()).andReturn(), "$.id");
    }

    @Test
    void projectAdminManagesMembers() throws Exception {
        String added = read(addMember(projectAdminToken, projectId, newcomerMemberId, "USER")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("newcomer"))
                .andReturn(), "$.id");
        changeProjectRole(projectAdminToken, projectId, userMembership, "EXTENDED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("EXTENDED"));
        removeProjectMember(projectAdminToken, projectId, added).andExpect(status().isNoContent());
    }

    @Test
    void projectAdminUpdatesDetailsAndStatus() throws Exception {
        mockMvc.perform(withToken(patch("/organizations/{o}/projects/{p}", organizationId, projectId), projectAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Renamed by admin\",\"description\":\"Delegated\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Renamed by admin"))
                .andExpect(jsonPath("$.description").value("Delegated"));
        mockMvc.perform(withToken(patch("/organizations/{o}/projects/{p}/status", organizationId, projectId), projectAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"FINISHED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINISHED"));
    }

    @Test
    void projectAdminCannotDeleteProject() throws Exception {
        mockMvc.perform(withToken(delete("/organizations/{o}/projects/{p}", organizationId, projectId), projectAdminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void projectAdminCannotChangeOwnMembership() throws Exception {
        changeProjectRole(projectAdminToken, projectId, projectAdminMembership, "USER")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Project ADMIN cannot change their own membership"));
        removeProjectMember(projectAdminToken, projectId, projectAdminMembership)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Project ADMIN cannot change their own membership"));
    }

    @Test
    void organizationOwnerCanStillChangeProjectAdmin() throws Exception {
        changeProjectRole(ownerToken, projectId, projectAdminMembership, "USER")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("USER"));
        addMember(projectAdminToken, projectId, newcomerMemberId, "USER")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value(DENIED));
    }

    @Test
    void extendedAndUserCannotManageProject() throws Exception {
        addMember(extendedToken, projectId, newcomerMemberId, "USER").andExpect(status().isForbidden()).andExpect(jsonPath("$.detail").value(DENIED));
        changeProjectRole(userToken, projectId, extendedMembership, "ADMIN").andExpect(status().isForbidden());
        removeProjectMember(extendedToken, projectId, userMembership).andExpect(status().isForbidden());
        mockMvc.perform(withToken(patch("/organizations/{o}/projects/{p}/status", organizationId, projectId), userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ARCHIVED\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void projectAdminHasNoPowerOverOtherProjects() throws Exception {
        addMember(projectAdminToken, otherProjectId, newcomerMemberId, "USER")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value(DENIED));
        mockMvc.perform(withToken(patch("/organizations/{o}/projects/{p}", organizationId, otherProjectId), projectAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Not mine\"}"))
                .andExpect(status().isForbidden());
    }

    private String createProject(String title) throws Exception {
        return read(mockMvc.perform(withToken(post("/organizations/{id}/projects", organizationId), ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"%s\"}".formatted(title)))
                .andExpect(status().isCreated())
                .andReturn(), "$.id");
    }

    private ResultActions addMember(String token, String targetProjectId, String organizationMemberId, String role) throws Exception {
        return mockMvc.perform(withToken(post("/organizations/{o}/projects/{p}/members", organizationId, targetProjectId), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"organizationMemberId\":\"%s\",\"role\":\"%s\"}".formatted(organizationMemberId, role)));
    }

    private ResultActions changeProjectRole(String token, String targetProjectId, String projectMemberId, String role) throws Exception {
        return mockMvc.perform(withToken(patch("/organizations/{o}/projects/{p}/members/{m}", organizationId, targetProjectId, projectMemberId), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"%s\"}".formatted(role)));
    }

    private ResultActions removeProjectMember(String token, String targetProjectId, String projectMemberId) throws Exception {
        return mockMvc.perform(withToken(delete("/organizations/{o}/projects/{p}/members/{m}", organizationId, targetProjectId, projectMemberId), token));
    }
}

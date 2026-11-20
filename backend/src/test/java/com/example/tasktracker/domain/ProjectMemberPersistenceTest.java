package com.example.tasktracker.domain;

import com.example.tasktracker.AbstractIntegrationTest;
import com.example.tasktracker.domain.enums.ProjectMemberRole;
import com.example.tasktracker.dto.response.ProjectMemberResponse;
import com.example.tasktracker.mapper.ProjectMemberMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProjectMemberPersistenceTest extends AbstractIntegrationTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private ProjectMemberMapper projectMemberMapper;

    private String ownerToken;
    private String userToken;
    private String organizationId;
    private String projectId;
    private String ownerMemberId;
    private String userMemberId;

    @BeforeEach
    void setUp() throws Exception {
        ownerToken = registerAndLogin("owner");
        userToken = registerAndLogin("member");
        organizationId = createOrganization(ownerToken, "MyOrganization");
        userMemberId = joinAndGetMemberId(userToken, getInviteCode(ownerToken, organizationId));
        ownerMemberId = read(getMembers(ownerToken, organizationId).andReturn(), "$.content[0].id");
        projectId = read(mockMvc.perform(withToken(post("/organizations/{id}/projects", organizationId), ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Backend Rewrite"}
                                """))
                .andExpect(status().isCreated())
                .andReturn(), "$.id");
    }

    @Test
    void entityIsMappedAndReadableThroughMapper() {
        UUID id = insertProjectMember(userMemberId, "EXTENDED");

        ProjectMemberResponse response = transactionTemplate.execute(status ->
                projectMemberMapper.toResponse(entityManager.find(ProjectMember.class, id)));

        assertThat(response.projectId()).hasToString(projectId);
        assertThat(response.organizationMemberId()).hasToString(userMemberId);
        assertThat(response.email()).isEqualTo("member@mail.com");
        assertThat(response.username()).isEqualTo("member");
        assertThat(response.role()).isEqualTo(ProjectMemberRole.EXTENDED);
        assertThat(response.createdAt()).isNotNull();
    }

    @Test
    void persistingThroughJpaFillsTimestamps() {
        UUID id = transactionTemplate.execute(status -> {
            ProjectMember member = new ProjectMember(ProjectMemberRole.USER);
            member.setProject(entityManager.getReference(Project.class, UUID.fromString(projectId)));
            member.setOrganizationMember(entityManager.getReference(OrganizationMember.class, UUID.fromString(userMemberId)));
            entityManager.persist(member);
            entityManager.flush();
            return member.getId();
        });

        assertThat(jdbcTemplate.queryForObject(
                "SELECT role FROM project_members WHERE id = ?", String.class, id)).isEqualTo("USER");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT created_at IS NOT NULL AND updated_at IS NOT NULL FROM project_members WHERE id = ?", Boolean.class, id)).isTrue();
    }

    @Test
    void sameOrganizationMemberCannotJoinProjectTwice() {
        insertProjectMember(userMemberId, "USER");

        assertThatThrownBy(() -> insertProjectMember(userMemberId, "ADMIN"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void unknownRoleIsRejectedByDatabase() {
        assertThatThrownBy(() -> insertProjectMember(userMemberId, "OWNER"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletingProjectRemovesItsMembers() throws Exception {
        insertProjectMember(ownerMemberId, "ADMIN");
        insertProjectMember(userMemberId, "USER");

        mockMvc.perform(withToken(delete("/organizations/{o}/projects/{p}", organizationId, projectId), ownerToken))
                .andExpect(status().isNoContent());

        assertThat(projectMemberCount()).isZero();
    }

    @Test
    void removingOrganizationMemberRemovesTheirProjectMemberships() throws Exception {
        insertProjectMember(ownerMemberId, "ADMIN");
        insertProjectMember(userMemberId, "USER");

        removeMember(ownerToken, organizationId, userMemberId).andExpect(status().isNoContent());

        assertThat(projectMemberCount()).isEqualTo(1);
    }

    @Test
    void leavingOrganizationRemovesProjectMemberships() throws Exception {
        insertProjectMember(userMemberId, "USER");

        leave(userToken, organizationId).andExpect(status().isNoContent());

        assertThat(projectMemberCount()).isZero();
    }

    @Test
    void deletingOrganizationWithProjectMembersSucceeds() throws Exception {
        insertProjectMember(ownerMemberId, "ADMIN");
        insertProjectMember(userMemberId, "USER");

        mockMvc.perform(withToken(delete("/organizations/{id}", organizationId), ownerToken))
                .andExpect(status().isNoContent());

        assertThat(projectMemberCount()).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM projects", Integer.class)).isZero();
    }

    private UUID insertProjectMember(String organizationMemberId, String role) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO project_members (id, project_id, organization_member_id, role, created_at, updated_at)
                VALUES (?, ?::uuid, ?::uuid, ?, now(), now())
                """, id, projectId, organizationMemberId, role);
        return id;
    }

    private Integer projectMemberCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM project_members", Integer.class);
    }
}

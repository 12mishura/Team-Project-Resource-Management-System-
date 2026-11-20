package com.example.tasktracker.mapper;

import com.example.tasktracker.domain.OrganizationMember;
import com.example.tasktracker.domain.Project;
import com.example.tasktracker.domain.ProjectMember;
import com.example.tasktracker.domain.User;
import com.example.tasktracker.domain.enums.OrganizationRole;
import com.example.tasktracker.domain.enums.ProjectMemberRole;
import com.example.tasktracker.dto.response.ProjectMemberResponse;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectMemberMapperTest {
    private final ProjectMemberMapper mapper = new ProjectMemberMapper();

    @Test
    void toResponseTakesUserDataFromOrganizationMember() {
        User user = new User("alice@mail.com", "alice", "hash");
        OrganizationMember organizationMember = new OrganizationMember(OrganizationRole.USER);
        organizationMember.setId(UUID.randomUUID());
        organizationMember.setUser(user);
        Project project = new Project("Backend Rewrite", null);
        project.setId(UUID.randomUUID());
        ProjectMember member = new ProjectMember(ProjectMemberRole.EXTENDED);
        member.setId(UUID.randomUUID());
        member.setProject(project);
        member.setOrganizationMember(organizationMember);

        ProjectMemberResponse response = mapper.toResponse(member);

        assertThat(response.id()).isEqualTo(member.getId());
        assertThat(response.projectId()).isEqualTo(project.getId());
        assertThat(response.organizationMemberId()).isEqualTo(organizationMember.getId());
        assertThat(response.email()).isEqualTo("alice@mail.com");
        assertThat(response.username()).isEqualTo("alice");
        assertThat(response.role()).isEqualTo(ProjectMemberRole.EXTENDED);
    }
}

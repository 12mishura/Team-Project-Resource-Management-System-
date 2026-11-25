package com.example.tasktracker.domain;

import com.example.tasktracker.domain.enums.ProjectMemberRole;
import com.example.tasktracker.exception.ProjectMemberRoleAlreadyAssignedException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectMemberTest {

    @Test
    void changeRoleSetsNewRole() {
        ProjectMember member = new ProjectMember(ProjectMemberRole.USER);

        member.changeRole(ProjectMemberRole.EXTENDED);

        assertThat(member.getRole()).isEqualTo(ProjectMemberRole.EXTENDED);
    }

    @Test
    void changeRoleToSameRoleThrows() {
        ProjectMember member = new ProjectMember(ProjectMemberRole.ADMIN);

        assertThatThrownBy(() -> member.changeRole(ProjectMemberRole.ADMIN))
                .isInstanceOf(ProjectMemberRoleAlreadyAssignedException.class)
                .hasMessage("Project member already has role ADMIN");
        assertThat(member.getRole()).isEqualTo(ProjectMemberRole.ADMIN);
    }
}

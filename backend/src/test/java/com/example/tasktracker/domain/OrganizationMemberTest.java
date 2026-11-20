package com.example.tasktracker.domain;

import com.example.tasktracker.domain.enums.OrganizationRole;
import com.example.tasktracker.exception.MemberRoleAlreadyAssignedException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrganizationMemberTest {

    @Test
    void changeRoleSetsNewRole() {
        OrganizationMember member = new OrganizationMember(OrganizationRole.USER);

        member.changeRole(OrganizationRole.ADMIN);

        assertThat(member.getRole()).isEqualTo(OrganizationRole.ADMIN);
    }

    @Test
    void changeRoleToSameRoleThrows() {
        OrganizationMember member = new OrganizationMember(OrganizationRole.ADMIN);

        assertThatThrownBy(() -> member.changeRole(OrganizationRole.ADMIN))
                .isInstanceOf(MemberRoleAlreadyAssignedException.class)
                .hasMessage("Member already has role ADMIN");
        assertThat(member.getRole()).isEqualTo(OrganizationRole.ADMIN);
    }
}

package com.example.tasktracker.security;

import com.example.tasktracker.domain.OrganizationMember;
import com.example.tasktracker.domain.enums.OrganizationRole;
import com.example.tasktracker.exception.AccessDeniedToOrganizationException;
import com.example.tasktracker.exception.IllegalMemberOperationException;
import com.example.tasktracker.exception.OrganizationNotFoundException;
import com.example.tasktracker.repository.OrganizationMemberRepository;
import com.example.tasktracker.repository.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static com.example.tasktracker.domain.enums.OrganizationRole.ADMIN;
import static com.example.tasktracker.domain.enums.OrganizationRole.OWNER;
import static com.example.tasktracker.domain.enums.OrganizationRole.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrganizationPermissionCheckerTest {
    private static final UUID ORGANIZATION_ID = UUID.randomUUID();
    private static final String EMAIL = "user@mail.com";

    private OrganizationRepository organizationRepository;
    private OrganizationMemberRepository organizationMemberRepository;
    private OrganizationPermissionChecker checker;

    @BeforeEach
    void setUp() {
        organizationRepository = mock(OrganizationRepository.class);
        organizationMemberRepository = mock(OrganizationMemberRepository.class);
        checker = new OrganizationPermissionChecker(organizationRepository, organizationMemberRepository);
    }

    @Test
    void requireMemberThrowsNotFoundWhenOrganizationMissing() {
        when(organizationRepository.existsById(ORGANIZATION_ID)).thenReturn(false);

        assertThatThrownBy(() -> checker.requireMember(ORGANIZATION_ID, EMAIL))
                .isInstanceOf(OrganizationNotFoundException.class);
    }

    @Test
    void requireMemberThrowsAccessDeniedForNonMember() {
        when(organizationRepository.existsById(ORGANIZATION_ID)).thenReturn(true);
        when(organizationMemberRepository.findByOrganizationIdAndUserEmail(ORGANIZATION_ID, EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> checker.requireMember(ORGANIZATION_ID, EMAIL))
                .isInstanceOf(AccessDeniedToOrganizationException.class);
    }

    @Test
    void requireMemberReturnsMember() {
        OrganizationMember member = member(USER);
        stubMember(member);

        assertThat(checker.requireMember(ORGANIZATION_ID, EMAIL)).isSameAs(member);
    }

    @Test
    void requireRoleAcceptsAllowedRole() {
        stubMember(member(ADMIN));

        assertThatCode(() -> checker.requireRole(ORGANIZATION_ID, EMAIL, OWNER, ADMIN)).doesNotThrowAnyException();
    }

    @Test
    void requireRoleRejectsOtherRole() {
        stubMember(member(USER));

        assertForbidden(() -> checker.requireRole(ORGANIZATION_ID, EMAIL, OWNER, ADMIN),
                "Only OWNER or ADMIN can perform this operation");
    }

    @Test
    void requireRoleRejectsNonMemberWithAccessDenied() {
        when(organizationRepository.existsById(ORGANIZATION_ID)).thenReturn(true);
        when(organizationMemberRepository.findByOrganizationIdAndUserEmail(ORGANIZATION_ID, EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> checker.requireRole(ORGANIZATION_ID, EMAIL, OWNER))
                .isInstanceOf(AccessDeniedToOrganizationException.class);
    }

    @Test
    void requireRoleMessageListsSingleRole() {
        stubMember(member(ADMIN));

        assertForbidden(() -> checker.requireRole(ORGANIZATION_ID, EMAIL, OWNER), "Only OWNER can perform this operation");
    }

    @Test
    void ownerCanChangeAdminAndUserRoles() {
        OrganizationMember owner = member(OWNER);

        assertThatCode(() -> checker.checkCanChangeRole(owner, member(USER), ADMIN)).doesNotThrowAnyException();
        assertThatCode(() -> checker.checkCanChangeRole(owner, member(ADMIN), USER)).doesNotThrowAnyException();
    }

    @Test
    void adminCanChangeUserRole() {
        assertThatCode(() -> checker.checkCanChangeRole(member(ADMIN), member(USER), ADMIN)).doesNotThrowAnyException();
    }

    @Test
    void userCannotChangeRoles() {
        assertForbidden(() -> checker.checkCanChangeRole(member(USER), member(USER), ADMIN),
                "Only owner or admin can change roles");
    }

    @Test
    void nobodyCanChangeOwnRole() {
        OrganizationMember admin = member(ADMIN);

        assertForbidden(() -> checker.checkCanChangeRole(admin, admin, USER), "Cannot change your own role");
    }

    @Test
    void ownerRoleCannotBeGrantedOrTaken() {
        assertForbidden(() -> checker.checkCanChangeRole(member(OWNER), member(USER), OWNER),
                "Owner role can only be changed via ownership transfer");
        assertForbidden(() -> checker.checkCanChangeRole(member(ADMIN), member(OWNER), USER),
                "Owner role can only be changed via ownership transfer");
    }

    @Test
    void adminCannotChangeAnotherAdmin() {
        assertForbidden(() -> checker.checkCanChangeRole(member(ADMIN), member(ADMIN), USER),
                "Admin cannot change role of another admin");
    }

    @Test
    void ownerCanRemoveAdminAndUser() {
        assertThatCode(() -> checker.checkCanRemove(member(OWNER), member(ADMIN))).doesNotThrowAnyException();
        assertThatCode(() -> checker.checkCanRemove(member(OWNER), member(USER))).doesNotThrowAnyException();
    }

    @Test
    void adminCanRemoveUser() {
        assertThatCode(() -> checker.checkCanRemove(member(ADMIN), member(USER))).doesNotThrowAnyException();
    }

    @Test
    void userCannotRemoveMembers() {
        assertForbidden(() -> checker.checkCanRemove(member(USER), member(USER)), "Only owner or admin can remove members");
    }

    @Test
    void ownerCannotBeRemoved() {
        assertForbidden(() -> checker.checkCanRemove(member(ADMIN), member(OWNER)), "Owner cannot be removed");
    }

    @Test
    void adminCannotRemoveAnotherAdmin() {
        assertForbidden(() -> checker.checkCanRemove(member(ADMIN), member(ADMIN)), "Admin cannot remove another admin");
    }

    @Test
    void adminAndUserCanLeave() {
        assertThatCode(() -> checker.checkCanLeave(member(ADMIN))).doesNotThrowAnyException();
        assertThatCode(() -> checker.checkCanLeave(member(USER))).doesNotThrowAnyException();
    }

    @Test
    void ownerCannotLeave() {
        assertForbidden(() -> checker.checkCanLeave(member(OWNER)),
                "Owner cannot leave organization, transfer ownership or delete organization");
    }

    @Test
    void ownerCanTransferOwnershipToAnotherMember() {
        assertThatCode(() -> checker.checkCanTransferOwnership(member(OWNER), UUID.randomUUID())).doesNotThrowAnyException();
    }

    @Test
    void onlyOwnerCanTransferOwnership() {
        assertForbidden(() -> checker.checkCanTransferOwnership(member(ADMIN), UUID.randomUUID()),
                "Only OWNER can transfer ownership");
        assertForbidden(() -> checker.checkCanTransferOwnership(member(USER), UUID.randomUUID()),
                "Only OWNER can transfer ownership");
    }

    @Test
    void ownerCannotTransferOwnershipToSelf() {
        OrganizationMember owner = member(OWNER);

        assertForbidden(() -> checker.checkCanTransferOwnership(owner, owner.getId()),
                "Cannot transfer ownership to yourself");
    }

    private void stubMember(OrganizationMember member) {
        when(organizationRepository.existsById(ORGANIZATION_ID)).thenReturn(true);
        when(organizationMemberRepository.findByOrganizationIdAndUserEmail(ORGANIZATION_ID, EMAIL)).thenReturn(Optional.of(member));
    }

    private void assertForbidden(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, String message) {
        assertThatThrownBy(call)
                .isInstanceOf(IllegalMemberOperationException.class)
                .hasMessage(message);
    }

    private static OrganizationMember member(OrganizationRole role) {
        OrganizationMember member = new OrganizationMember(role);
        member.setId(UUID.randomUUID());
        return member;
    }
}

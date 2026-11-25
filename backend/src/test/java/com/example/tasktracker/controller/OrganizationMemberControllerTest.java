package com.example.tasktracker.controller;

import com.example.tasktracker.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrganizationMemberControllerTest extends AbstractIntegrationTest {
    private String ownerToken;
    private String adminToken;
    private String secondAdminToken;
    private String userToken;
    private String secondUserToken;
    private String outsiderToken;

    private String organizationId;
    private String inviteCode;
    private String ownerMemberId;
    private String adminMemberId;
    private String secondAdminMemberId;
    private String userMemberId;
    private String secondUserMemberId;

    @BeforeEach
    void setUp() throws Exception {
        ownerToken = registerAndLogin("owner");
        adminToken = registerAndLogin("admin");
        secondAdminToken = registerAndLogin("admin2");
        userToken = registerAndLogin("member");
        secondUserToken = registerAndLogin("member2");
        outsiderToken = registerAndLogin("outsider");

        organizationId = createOrganization(ownerToken, "MyOrganization");
        inviteCode = getInviteCode(ownerToken, organizationId);
        ownerMemberId = read(getMembers(ownerToken, organizationId).andReturn(), "$.content[0].id");

        adminMemberId = joinAndGetMemberId(adminToken, inviteCode);
        secondAdminMemberId = joinAndGetMemberId(secondAdminToken, inviteCode);
        userMemberId = joinAndGetMemberId(userToken, inviteCode);
        secondUserMemberId = joinAndGetMemberId(secondUserToken, inviteCode);

        changeRole(ownerToken, organizationId, adminMemberId, "ADMIN").andExpect(status().isOk());
        changeRole(ownerToken, organizationId, secondAdminMemberId, "ADMIN").andExpect(status().isOk());
    }

    @Test
    void joinCreatesMemberWithUserRole() throws Exception {
        String token = registerAndLogin("newcomer");

        join(token, inviteCode)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("newcomer@mail.com"))
                .andExpect(jsonPath("$.username").value("newcomer"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void joinTwiceReturnsConflict() throws Exception {
        join(userToken, inviteCode)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("User is already member of organization " + organizationId));
    }

    @Test
    void ownerJoiningOwnOrganizationReturnsConflict() throws Exception {
        join(ownerToken, inviteCode).andExpect(status().isConflict());
    }

    @Test
    void joinWithUnknownCodeReturnsNotFound() throws Exception {
        join(outsiderToken, "zzzz-zzzz")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Organization with invite code zzzz-zzzz not exists"));
    }

    @Test
    void joinAcceptsCodeInUpperCase() throws Exception {
        join(outsiderToken, inviteCode.toUpperCase()).andExpect(status().isCreated());
    }

    @Test
    void joinAcceptsCodeWithSurroundingSpaces() throws Exception {
        join(outsiderToken, "  " + inviteCode + "  ").andExpect(status().isCreated());
    }

    @Test
    void joinWithTooLongCodeReturnsBadRequest() throws Exception {
        join(outsiderToken, "a".repeat(10)).andExpect(status().isBadRequest());
    }

    @Test
    void joinWithBlankCodeReturnsBadRequest() throws Exception {
        join(outsiderToken, " ").andExpect(status().isBadRequest());
    }

    @Test
    void joinWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/organizations/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"inviteCode":"%s"}
                                """.formatted(inviteCode)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void membersListIsOrderedByJoinDate() throws Exception {
        getMembers(userToken, organizationId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.content[*].username").value(contains("owner", "admin", "admin2", "member", "member2")))
                .andExpect(jsonPath("$.content[*].role").value(contains("OWNER", "ADMIN", "ADMIN", "USER", "USER")));
    }

    @Test
    void membersListIsForbiddenForOutsider() throws Exception {
        getMembers(outsiderToken, organizationId).andExpect(status().isForbidden());
    }

    @Test
    void membersListOfUnknownOrganizationReturnsNotFound() throws Exception {
        getMembers(ownerToken, UUID.randomUUID().toString()).andExpect(status().isNotFound());
    }

    @Test
    void ownerCanPromoteUserToAdmin() throws Exception {
        changeRole(ownerToken, organizationId, userMemberId, "ADMIN")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userMemberId))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void ownerCanDemoteAdminToUser() throws Exception {
        changeRole(ownerToken, organizationId, adminMemberId, "USER")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void adminCanPromoteUserToAdmin() throws Exception {
        changeRole(adminToken, organizationId, userMemberId, "ADMIN")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void assigningSameRoleReturnsConflict() throws Exception {
        changeRole(ownerToken, organizationId, adminMemberId, "ADMIN")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Member already has role ADMIN"));
    }

    @Test
    void regularMemberCannotChangeRoles() throws Exception {
        changeRole(userToken, organizationId, secondUserMemberId, "ADMIN")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Only owner or admin can change roles"));
    }

    @Test
    void adminCannotChangeRoleOfAnotherAdmin() throws Exception {
        changeRole(adminToken, organizationId, secondAdminMemberId, "USER")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Admin cannot change role of another admin"));
    }

    @Test
    void adminCannotChangeOwnRole() throws Exception {
        changeRole(adminToken, organizationId, adminMemberId, "USER")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Cannot change your own role"));
    }

    @Test
    void ownerCannotChangeOwnRole() throws Exception {
        changeRole(ownerToken, organizationId, ownerMemberId, "ADMIN")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Cannot change your own role"));
    }

    @Test
    void ownerRoleCannotBeGranted() throws Exception {
        changeRole(ownerToken, organizationId, userMemberId, "OWNER")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Owner role can only be changed via ownership transfer"));
    }

    @Test
    void ownerRoleCannotBeChangedByAdmin() throws Exception {
        changeRole(adminToken, organizationId, ownerMemberId, "USER")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Owner role can only be changed via ownership transfer"));
    }

    @Test
    void outsiderCannotChangeRoles() throws Exception {
        changeRole(outsiderToken, organizationId, userMemberId, "ADMIN")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Access denied to organization " + organizationId));
    }

    @Test
    void unknownRoleReturnsBadRequest() throws Exception {
        changeRole(ownerToken, organizationId, userMemberId, "BOSS").andExpect(status().isBadRequest());
    }

    @Test
    void missingRoleReturnsBadRequest() throws Exception {
        mockMvc.perform(withToken(patch("/organizations/{organizationId}/members/{memberId}", organizationId, userMemberId), ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changingRoleOfUnknownMemberReturnsNotFound() throws Exception {
        changeRole(ownerToken, organizationId, UUID.randomUUID().toString(), "ADMIN")
                .andExpect(status().isNotFound());
    }

    @Test
    void changingRoleOfMemberFromAnotherOrganizationReturnsNotFound() throws Exception {
        String foreignMemberId = memberIdInAnotherOrganization();

        changeRole(ownerToken, organizationId, foreignMemberId, "ADMIN").andExpect(status().isNotFound());
    }

    @Test
    void ownerCanRemoveAdmin() throws Exception {
        removeMember(ownerToken, organizationId, adminMemberId).andExpect(status().isNoContent());

        getMembers(ownerToken, organizationId)
                .andExpect(jsonPath("$.content[*].id").value(not(hasItem(adminMemberId))));
    }

    @Test
    void ownerCanRemoveUser() throws Exception {
        removeMember(ownerToken, organizationId, userMemberId).andExpect(status().isNoContent());
    }

    @Test
    void adminCanRemoveUser() throws Exception {
        removeMember(adminToken, organizationId, userMemberId).andExpect(status().isNoContent());
    }

    @Test
    void adminCannotRemoveAnotherAdmin() throws Exception {
        removeMember(adminToken, organizationId, secondAdminMemberId)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Admin cannot remove another admin"));
    }

    @Test
    void adminCannotRemoveOwner() throws Exception {
        removeMember(adminToken, organizationId, ownerMemberId)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Owner cannot be removed"));
    }

    @Test
    void regularMemberCannotRemoveOthers() throws Exception {
        removeMember(userToken, organizationId, secondUserMemberId)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Only owner or admin can remove members"));
    }

    @Test
    void memberCanRemoveSelfById() throws Exception {
        removeMember(userToken, organizationId, userMemberId).andExpect(status().isNoContent());

        getMembers(userToken, organizationId).andExpect(status().isForbidden());
    }

    @Test
    void ownerCannotRemoveSelfById() throws Exception {
        removeMember(ownerToken, organizationId, ownerMemberId)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Owner cannot leave organization, transfer ownership or delete organization"));
    }

    @Test
    void outsiderCannotRemoveMembers() throws Exception {
        removeMember(outsiderToken, organizationId, userMemberId).andExpect(status().isForbidden());
    }

    @Test
    void removingUnknownMemberReturnsNotFound() throws Exception {
        removeMember(ownerToken, organizationId, UUID.randomUUID().toString()).andExpect(status().isNotFound());
    }

    @Test
    void removingMemberFromAnotherOrganizationReturnsNotFound() throws Exception {
        String foreignMemberId = memberIdInAnotherOrganization();

        removeMember(ownerToken, organizationId, foreignMemberId).andExpect(status().isNotFound());
    }

    @Test
    void userCanLeaveOrganization() throws Exception {
        leave(userToken, organizationId).andExpect(status().isNoContent());

        getMembers(userToken, organizationId).andExpect(status().isForbidden());
        getMembers(ownerToken, organizationId).andExpect(jsonPath("$.content", hasSize(4)));
    }

    @Test
    void adminCanLeaveOrganization() throws Exception {
        leave(adminToken, organizationId).andExpect(status().isNoContent());
    }

    @Test
    void ownerCannotLeaveOrganization() throws Exception {
        leave(ownerToken, organizationId)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Owner cannot leave organization, transfer ownership or delete organization"));
    }

    @Test
    void outsiderCannotLeaveOrganization() throws Exception {
        leave(outsiderToken, organizationId).andExpect(status().isForbidden());
    }

    @Test
    void memberCanRejoinAfterLeaving() throws Exception {
        leave(userToken, organizationId).andExpect(status().isNoContent());

        join(userToken, inviteCode)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void ownerTransfersOwnershipToRegularMember() throws Exception {
        transfer(ownerToken, organizationId, userMemberId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userMemberId))
                .andExpect(jsonPath("$.role").value("OWNER"));

        getMembers(userToken, organizationId)
                .andExpect(jsonPath("$.content[?(@.id == '" + ownerMemberId + "')].role").value(contains("ADMIN")))
                .andExpect(jsonPath("$.content[?(@.id == '" + userMemberId + "')].role").value(contains("OWNER")));
        assertThat(ownerCount()).isEqualTo(1);
    }

    @Test
    void ownerTransfersOwnershipToAdmin() throws Exception {
        transfer(ownerToken, organizationId, adminMemberId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("OWNER"));
        assertThat(ownerCount()).isEqualTo(1);
    }

    @Test
    void formerOwnerCanLeaveAfterTransferAndNewOwnerGetsOwnerRights() throws Exception {
        transfer(ownerToken, organizationId, userMemberId).andExpect(status().isOk());

        leave(ownerToken, organizationId).andExpect(status().isNoContent());
        leave(userToken, organizationId).andExpect(status().isForbidden());
        mockMvc.perform(withToken(delete("/organizations/{id}", organizationId), userToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void newOwnerCanTransferBack() throws Exception {
        transfer(ownerToken, organizationId, userMemberId).andExpect(status().isOk());

        transfer(userToken, organizationId, ownerMemberId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ownerMemberId));
        assertThat(ownerCount()).isEqualTo(1);
    }

    @Test
    void formerOwnerCannotTransferAgain() throws Exception {
        transfer(ownerToken, organizationId, userMemberId).andExpect(status().isOk());

        transfer(ownerToken, organizationId, secondUserMemberId)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Only OWNER can transfer ownership"));
    }

    @Test
    void adminCannotTransferOwnership() throws Exception {
        transfer(adminToken, organizationId, userMemberId)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Only OWNER can transfer ownership"));
        assertThat(ownerCount()).isEqualTo(1);
    }

    @Test
    void regularMemberCannotTransferOwnership() throws Exception {
        transfer(userToken, organizationId, userMemberId).andExpect(status().isForbidden());
    }

    @Test
    void outsiderCannotTransferOwnership() throws Exception {
        transfer(outsiderToken, organizationId, userMemberId)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Access denied to organization " + organizationId));
    }

    @Test
    void transferToSelfIsRejected() throws Exception {
        transfer(ownerToken, organizationId, ownerMemberId)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Cannot transfer ownership to yourself"));
    }

    @Test
    void transferToUnknownMemberReturnsNotFound() throws Exception {
        transfer(ownerToken, organizationId, UUID.randomUUID().toString()).andExpect(status().isNotFound());
        assertThat(ownerCount()).isEqualTo(1);
    }

    @Test
    void transferToMemberOfAnotherOrganizationReturnsNotFound() throws Exception {
        transfer(ownerToken, organizationId, memberIdInAnotherOrganization()).andExpect(status().isNotFound());
    }

    @Test
    void transferInUnknownOrganizationReturnsNotFound() throws Exception {
        transfer(ownerToken, UUID.randomUUID().toString(), userMemberId).andExpect(status().isNotFound());
    }

    @Test
    void transferWithoutMemberIdReturnsBadRequest() throws Exception {
        mockMvc.perform(withToken(post("/organizations/{id}/transfer-ownership", organizationId), ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void concurrentTransfersLeaveExactlyOneOwner() throws Exception {
        String[] targets = {userMemberId, secondUserMemberId, adminMemberId, secondAdminMemberId};
        ExecutorService pool = Executors.newFixedThreadPool(targets.length);
        List<Callable<Integer>> calls = new ArrayList<>();
        for (String target : targets) {
            calls.add(() -> transfer(ownerToken, organizationId, target).andReturn().getResponse().getStatus());
        }
        List<Integer> statuses = new ArrayList<>();
        for (Future<Integer> future : pool.invokeAll(calls)) {
            statuses.add(future.get());
        }
        pool.shutdown();

        assertThat(statuses).containsOnlyOnce(200);
        assertThat(ownerCount()).isEqualTo(1);
    }

    @Test
    void databaseRejectsSecondOwner() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE organization_members SET role = 'OWNER' WHERE id = ?::uuid", userMemberId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void membersListIsPaginated() throws Exception {
        mockMvc.perform(withToken(get("/organizations/{id}/members?page=1&size=2", organizationId), ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].username").value(contains("admin2", "member")))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    private ResultActions transfer(String token, String orgId, String memberId) throws Exception {
        return mockMvc.perform(withToken(post("/organizations/{id}/transfer-ownership", orgId), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"memberId":"%s"}
                        """.formatted(memberId)));
    }

    private Integer ownerCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM organization_members WHERE organization_id = ?::uuid AND role = 'OWNER'",
                Integer.class, organizationId);
    }

    private String memberIdInAnotherOrganization() throws Exception {
        String anotherOrganizationId = createOrganization(outsiderToken, "AnotherOrganization");
        return read(getMembers(outsiderToken, anotherOrganizationId).andReturn(), "$.content[0].id");
    }
}

package com.example.tasktracker.controller;

import com.example.tasktracker.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrganizationControllerTest extends AbstractIntegrationTest {
    private static final String TITLE = "MyOrganization";

    private String ownerToken;
    private String adminToken;
    private String userToken;
    private String outsiderToken;
    private String organizationId;

    @BeforeEach
    void setUp() throws Exception {
        ownerToken = registerAndLogin("owner");
        adminToken = registerAndLogin("admin");
        userToken = registerAndLogin("member");
        outsiderToken = registerAndLogin("outsider");

        organizationId = createOrganization(ownerToken, TITLE);
        String inviteCode = getInviteCode(ownerToken, organizationId);
        String adminMemberId = joinAndGetMemberId(adminToken, inviteCode);
        changeRole(ownerToken, organizationId, adminMemberId, "ADMIN").andExpect(status().isOk());
        joinAndGetMemberId(userToken, inviteCode);
    }

    @Test
    void createReturnsOrganizationAndMakesCreatorOwner() throws Exception {
        String token = registerAndLogin("creator");

        String id = createOrganization(token, "AnotherOrganization");

        mockMvc.perform(withToken(get("/organizations/{id}", id), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("AnotherOrganization"))
                .andExpect(jsonPath("$.createdAt").value(notNullValue()))
                .andExpect(jsonPath("$.updatedAt").value(notNullValue()))
                .andExpect(jsonPath("$.inviteCode").doesNotExist());
        getMembers(token, id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].username").value("creator"))
                .andExpect(jsonPath("$.content[0].role").value("OWNER"));
    }

    @Test
    void createWithDescriptionOf400CharsSucceeds() throws Exception {
        create(ownerToken, "LongDescriptionOrg", "a".repeat(400))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("a".repeat(400)));
    }

    @Test
    void createWithDescriptionOver400CharsReturnsBadRequest() throws Exception {
        create(ownerToken, "LongDescriptionOrg", "a".repeat(401)).andExpect(status().isBadRequest());
    }

    @Test
    void createWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/organizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"AnotherOrganization"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createWithDuplicateTitleReturnsConflict() throws Exception {
        create(outsiderToken, TITLE, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Organization with title MyOrganization already exists"));
    }

    @Test
    void createWithShortTitleReturnsBadRequest() throws Exception {
        create(ownerToken, "Short", null).andExpect(status().isBadRequest());
    }

    @Test
    void createWithBlankTitleReturnsBadRequest() throws Exception {
        create(ownerToken, "  ", null).andExpect(status().isBadRequest());
    }

    @Test
    void getByOwnerReturnsOrganization() throws Exception {
        mockMvc.perform(withToken(get("/organizations/{id}", organizationId), ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(organizationId))
                .andExpect(jsonPath("$.title").value(TITLE));
    }

    @Test
    void getByRegularMemberReturnsOrganization() throws Exception {
        mockMvc.perform(withToken(get("/organizations/{id}", organizationId), userToken))
                .andExpect(status().isOk());
    }

    @Test
    void getByOutsiderReturnsForbidden() throws Exception {
        mockMvc.perform(withToken(get("/organizations/{id}", organizationId), outsiderToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUnknownOrganizationReturnsNotFound() throws Exception {
        mockMvc.perform(withToken(get("/organizations/{id}", UUID.randomUUID()), ownerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void getWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/organizations/{id}", organizationId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void inviteCodeIsAvailableToOwner() throws Exception {
        mockMvc.perform(withToken(get("/organizations/{id}/invite-code", organizationId), ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inviteCode").value(matchesPattern("^[a-z0-9]{4}-[a-z0-9]{4}$")));
    }

    @Test
    void inviteCodeIsAvailableToAdmin() throws Exception {
        mockMvc.perform(withToken(get("/organizations/{id}/invite-code", organizationId), adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void inviteCodeIsForbiddenForRegularMember() throws Exception {
        mockMvc.perform(withToken(get("/organizations/{id}/invite-code", organizationId), userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void inviteCodeIsForbiddenForOutsider() throws Exception {
        mockMvc.perform(withToken(get("/organizations/{id}/invite-code", organizationId), outsiderToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateByOwnerChangesFields() throws Exception {
        update(ownerToken, organizationId, """
                {"title":"RenamedOrganization","description":"New description"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("RenamedOrganization"))
                .andExpect(jsonPath("$.description").value("New description"));
    }

    @Test
    void updateWithOnlyDescriptionKeepsTitle() throws Exception {
        update(ownerToken, organizationId, """
                {"description":"Only description"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(TITLE))
                .andExpect(jsonPath("$.description").value("Only description"));
    }

    @Test
    void updateByAdminSucceeds() throws Exception {
        update(adminToken, organizationId, """
                {"description":"Changed by admin"}
                """)
                .andExpect(status().isOk());
    }

    @Test
    void updateByRegularMemberReturnsForbidden() throws Exception {
        update(userToken, organizationId, """
                {"description":"Hacked"}
                """)
                .andExpect(status().isForbidden());
    }

    @Test
    void updateByOutsiderReturnsForbidden() throws Exception {
        update(outsiderToken, organizationId, """
                {"description":"Hacked"}
                """)
                .andExpect(status().isForbidden());
    }

    @Test
    void updateUnknownOrganizationReturnsNotFound() throws Exception {
        update(ownerToken, UUID.randomUUID().toString(), """
                {"description":"Nothing"}
                """)
                .andExpect(status().isNotFound());
    }

    @Test
    void updateToTakenTitleReturnsConflict() throws Exception {
        createOrganization(outsiderToken, "TakenOrganization");

        update(ownerToken, organizationId, """
                {"title":"TakenOrganization"}
                """)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Organization with title TakenOrganization already exists"));
    }

    @Test
    void updateWithSameTitleSucceeds() throws Exception {
        update(ownerToken, organizationId, """
                {"title":"%s"}
                """.formatted(TITLE))
                .andExpect(status().isOk());
    }

    @Test
    void updateWithShortTitleReturnsBadRequest() throws Exception {
        update(ownerToken, organizationId, """
                {"title":"Short"}
                """)
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateWithWhitespaceOnlyTitleReturnsBadRequest() throws Exception {
        update(ownerToken, organizationId, """
                {"title":"          "}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(startsWith("title:")));

        mockMvc.perform(withToken(get("/organizations/{id}", organizationId), ownerToken))
                .andExpect(jsonPath("$.title").value(TITLE));
    }

    @Test
    void createTrimsTitle() throws Exception {
        create(ownerToken, "   AnotherOrganization   ", null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("AnotherOrganization"));
    }

    @Test
    void createWithTitleDifferingOnlyByCaseReturnsConflict() throws Exception {
        create(outsiderToken, "MYORGANIZATION", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Organization with title MYORGANIZATION already exists"));
    }

    @Test
    void createWithTitleDuplicatingAfterTrimReturnsConflict() throws Exception {
        create(outsiderToken, "  " + TITLE + "  ", null).andExpect(status().isConflict());
    }

    @Test
    void updateChangingOnlyCaseOfOwnTitleSucceeds() throws Exception {
        update(ownerToken, organizationId, """
                {"title":"MYORGANIZATION"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("MYORGANIZATION"));
    }

    @Test
    void updateToTakenTitleWithDifferentCaseReturnsConflict() throws Exception {
        createOrganization(outsiderToken, "TakenOrganization");

        update(ownerToken, organizationId, """
                {"title":"takenorganization"}
                """)
                .andExpect(status().isConflict());
    }

    @Test
    void deleteByOwnerRemovesOrganizationAndMembers() throws Exception {
        mockMvc.perform(withToken(delete("/organizations/{id}", organizationId), ownerToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(withToken(get("/organizations/{id}", organizationId), ownerToken))
                .andExpect(status().isNotFound());
        Integer members = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM organization_members WHERE organization_id = ?::uuid", Integer.class, organizationId);
        assertThat(members).isZero();
    }

    @Test
    void deleteByAdminReturnsForbidden() throws Exception {
        mockMvc.perform(withToken(delete("/organizations/{id}", organizationId), adminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteByRegularMemberReturnsForbidden() throws Exception {
        mockMvc.perform(withToken(delete("/organizations/{id}", organizationId), userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteByOutsiderReturnsForbidden() throws Exception {
        mockMvc.perform(withToken(delete("/organizations/{id}", organizationId), outsiderToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteUnknownOrganizationReturnsNotFound() throws Exception {
        mockMvc.perform(withToken(delete("/organizations/{id}", UUID.randomUUID()), ownerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void myOrganizationsShowOwnRoleInEachOrganization() throws Exception {
        myOrganizations(ownerToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(organizationId))
                .andExpect(jsonPath("$.content[0].title").value(TITLE))
                .andExpect(jsonPath("$.content[0].role").value("OWNER"))
                .andExpect(jsonPath("$.content[0].memberId").value(notNullValue()))
                .andExpect(jsonPath("$.content[0].joinedAt").value(notNullValue()));
        myOrganizations(adminToken).andExpect(jsonPath("$.content[0].role").value("ADMIN"));
        myOrganizations(userToken).andExpect(jsonPath("$.content[0].role").value("USER"));
    }

    @Test
    void myOrganizationsAreOrderedByJoinDateAndContainOnlyOwnMemberships() throws Exception {
        createOrganization(outsiderToken, "OutsiderOrganization");
        join(outsiderToken, getInviteCode(ownerToken, organizationId)).andExpect(status().isCreated());

        myOrganizations(outsiderToken)
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].title").value(contains("OutsiderOrganization", TITLE)))
                .andExpect(jsonPath("$.content[*].role").value(contains("OWNER", "USER")));
        myOrganizations(ownerToken).andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void myOrganizationsMemberIdMatchesMembersList() throws Exception {
        String memberId = read(myOrganizations(userToken).andReturn(), "$.content[0].memberId");

        getMembers(ownerToken, organizationId)
                .andExpect(jsonPath("$.content[?(@.username == 'member')].id").value(contains(memberId)));
    }

    @Test
    void newUserHasNoOrganizations() throws Exception {
        myOrganizations(registerAndLogin("newcomer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void myOrganizationsDoNotExposeInviteCode() throws Exception {
        myOrganizations(ownerToken).andExpect(jsonPath("$.content[0].inviteCode").doesNotExist());
    }

    @Test
    void myOrganizationsReflectRoleChange() throws Exception {
        String memberId = read(myOrganizations(userToken).andReturn(), "$.content[0].memberId");
        changeRole(ownerToken, organizationId, memberId, "ADMIN").andExpect(status().isOk());

        myOrganizations(userToken).andExpect(jsonPath("$.content[0].role").value("ADMIN"));
    }

    @Test
    void organizationDisappearsAfterLeaving() throws Exception {
        leave(userToken, organizationId).andExpect(status().isNoContent());

        myOrganizations(userToken).andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void organizationDisappearsAfterDeletion() throws Exception {
        mockMvc.perform(withToken(delete("/organizations/{id}", organizationId), ownerToken))
                .andExpect(status().isNoContent());

        myOrganizations(ownerToken).andExpect(jsonPath("$.content", hasSize(0)));
        myOrganizations(userToken).andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void myOrganizationsArePaginated() throws Exception {
        createOrganization(ownerToken, "SecondOrganization");
        createOrganization(ownerToken, "ThirdOrganization");

        mockMvc.perform(withToken(get("/organizations?page=0&size=2"), ownerToken))
                .andExpect(jsonPath("$.content[*].title").value(contains(TITLE, "SecondOrganization")))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void myOrganizationsWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/organizations")).andExpect(status().isUnauthorized());
    }

    private ResultActions myOrganizations(String token) throws Exception {
        return mockMvc.perform(withToken(get("/organizations"), token));
    }

    private ResultActions create(String token, String title, String description) throws Exception {
        String body = description == null
                ? """
                  {"title":"%s"}
                  """.formatted(title)
                : """
                  {"title":"%s","description":"%s"}
                  """.formatted(title, description);
        return mockMvc.perform(withToken(post("/organizations"), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions update(String token, String id, String body) throws Exception {
        return mockMvc.perform(withToken(patch("/organizations/{id}", id), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}

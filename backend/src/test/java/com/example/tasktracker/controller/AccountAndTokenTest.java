package com.example.tasktracker.controller;

import com.example.tasktracker.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AccountAndTokenTest extends AbstractIntegrationTest {
    private static final String EMAIL = "alice@mail.com";

    private String accessToken;
    private String refreshToken;

    @BeforeEach
    void setUp() throws Exception {
        register(EMAIL, "alice", PASSWORD).andExpect(status().isCreated());
        MvcResult result = login(EMAIL, PASSWORD).andExpect(status().isOk()).andReturn();
        accessToken = read(result, "$.accessToken");
        refreshToken = read(result, "$.refreshToken");
    }

    @Nested
    class RefreshTokens {

        @Test
        void loginReturnsBothTokens() throws Exception {
            login(EMAIL, PASSWORD)
                    .andExpect(jsonPath("$.accessToken").value(notNullValue()))
                    .andExpect(jsonPath("$.refreshToken").value(matchesPattern("^[A-Za-z0-9_-]{43}$")))
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.expiresIn").value(3600))
                    .andExpect(jsonPath("$.refreshExpiresIn").value(1209600));
        }

        @Test
        void refreshTokenIsStoredOnlyAsHash() {
            List<String> hashes = jdbcTemplate.queryForList("SELECT token_hash FROM refresh_tokens", String.class);

            assertThat(hashes).hasSize(1).doesNotContain(refreshToken);
            assertThat(hashes.getFirst()).matches("^[0-9a-f]{64}$");
        }

        @Test
        void refreshIssuesWorkingNewPair() throws Exception {
            MvcResult result = refresh(refreshToken)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.refreshToken").value(not(refreshToken)))
                    .andReturn();

            mockMvc.perform(withToken(get("/auth/me"), read(result, "$.accessToken")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(EMAIL));
            refresh(read(result, "$.refreshToken")).andExpect(status().isOk());
        }

        @Test
        void reusingRotatedTokenRevokesWholeSession() throws Exception {
            String rotated = read(refresh(refreshToken).andExpect(status().isOk()).andReturn(), "$.refreshToken");

            refresh(refreshToken)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.detail").value("Invalid or expired refresh token"));
            refresh(rotated).andExpect(status().isUnauthorized());
        }

        @Test
        void unknownBlankOrExpiredTokenIsRejected() throws Exception {
            refresh("not-a-real-token").andExpect(status().isUnauthorized());
            mockMvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest());

            jdbcTemplate.update("UPDATE refresh_tokens SET expires_at = now() - interval '1 minute'");
            refresh(refreshToken).andExpect(status().isUnauthorized());
        }

        @Test
        void logoutRevokesRefreshTokenAndIsIdempotent() throws Exception {
            logout(refreshToken).andExpect(status().isNoContent());
            logout(refreshToken).andExpect(status().isNoContent());
            logout("never-issued").andExpect(status().isNoContent());

            refresh(refreshToken).andExpect(status().isUnauthorized());
        }

        @Test
        void concurrentRefreshWithSameTokenSucceedsOnce() throws Exception {
            ExecutorService pool = Executors.newFixedThreadPool(5);
            List<Callable<Integer>> calls = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                calls.add(() -> refresh(refreshToken).andReturn().getResponse().getStatus());
            }
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> future : pool.invokeAll(calls)) {
                statuses.add(future.get());
            }
            pool.shutdown();

            assertThat(statuses).containsOnlyOnce(200).containsOnly(200, 401);
        }
    }

    @Nested
    class Profile {

        @Test
        void usernameCanBeChanged() throws Exception {
            updateProfile("{\"username\":\"  alice2  \"}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("alice2"))
                    .andExpect(jsonPath("$.email").value(EMAIL));
            mockMvc.perform(withToken(get("/auth/me"), accessToken)).andExpect(jsonPath("$.username").value("alice2"));
        }

        @Test
        void changingOnlyCaseOfOwnUsernameIsAllowed() throws Exception {
            updateProfile("{\"username\":\"ALICE\"}").andExpect(status().isOk()).andExpect(jsonPath("$.username").value("ALICE"));
        }

        @Test
        void takenOrInvalidUsernameIsRejected() throws Exception {
            register("bob@mail.com", "bob", PASSWORD).andExpect(status().isCreated());

            updateProfile("{\"username\":\"BOB\"}")
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail").value("User with username 'BOB' already exists"));
            updateProfile("{\"username\":\"ab\"}").andExpect(status().isBadRequest());
            updateProfile("{}").andExpect(status().isBadRequest());
            mockMvc.perform(patch("/users/me").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"nobody\"}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void wrongCurrentOrSameOrWeakNewPasswordIsRejected() throws Exception {
            changePassword("wrongPassword1", "newPassword123")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("Current password is incorrect"));
            changePassword(PASSWORD, PASSWORD)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("New password must be different from the current one"));
            changePassword(PASSWORD, "short").andExpect(status().isBadRequest());
            changePassword(PASSWORD, "ж".repeat(37)).andExpect(status().isBadRequest());

            login(EMAIL, PASSWORD).andExpect(status().isOk());
        }

        @Test
        void changingPasswordInvalidatesAllExistingTokens() throws Exception {
            changePassword(PASSWORD, "newPassword123").andExpect(status().isNoContent());

            mockMvc.perform(withToken(get("/auth/me"), accessToken)).andExpect(status().isUnauthorized());
            refresh(refreshToken).andExpect(status().isUnauthorized());
            login(EMAIL, PASSWORD).andExpect(status().isUnauthorized());

            String freshToken = read(login(EMAIL, "newPassword123").andExpect(status().isOk()).andReturn(), "$.accessToken");
            mockMvc.perform(withToken(get("/auth/me"), freshToken)).andExpect(status().isOk());
        }
    }

    @Nested
    class InviteCode {
        private String organizationId;
        private String inviteCode;

        @BeforeEach
        void createOrganization() throws Exception {
            organizationId = AccountAndTokenTest.this.createOrganization(accessToken, "MyOrganization");
            inviteCode = getInviteCode(accessToken, organizationId);
        }

        @Test
        void ownerRegeneratesCodeAndOldOneStopsWorking() throws Exception {
            String newCode = read(regenerate(accessToken)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.inviteCode").value(matchesPattern("^[a-z0-9]{4}-[a-z0-9]{4}$")))
                    .andReturn(), "$.inviteCode");
            assertThat(newCode).isNotEqualTo(inviteCode);

            String joiner = registerAndLogin("joiner");
            join(joiner, inviteCode).andExpect(status().isNotFound());
            join(joiner, newCode).andExpect(status().isCreated());
            assertThat(getInviteCode(accessToken, organizationId)).isEqualTo(newCode);
        }

        @Test
        void adminCanRegenerateButUserAndOutsiderCannot() throws Exception {
            String adminToken = registerAndLogin("admin");
            String adminMemberId = joinAndGetMemberId(adminToken, inviteCode);
            changeRole(accessToken, organizationId, adminMemberId, "ADMIN").andExpect(status().isOk());
            String userToken = registerAndLogin("member");
            joinAndGetMemberId(userToken, inviteCode);

            regenerate(adminToken).andExpect(status().isOk());
            regenerate(userToken).andExpect(status().isForbidden());
            regenerate(registerAndLogin("outsider")).andExpect(status().isForbidden());
        }

        private ResultActions regenerate(String token) throws Exception {
            return mockMvc.perform(withToken(post("/organizations/{id}/invite-code/regenerate", organizationId), token));
        }
    }

    private ResultActions refresh(String token) throws Exception {
        return mockMvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"%s\"}".formatted(token)));
    }

    private ResultActions logout(String token) throws Exception {
        return mockMvc.perform(post("/auth/logout").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"%s\"}".formatted(token)));
    }

    private ResultActions updateProfile(String body) throws Exception {
        return mockMvc.perform(withToken(patch("/users/me"), accessToken).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions changePassword(String current, String next) throws Exception {
        return mockMvc.perform(withToken(put("/users/me/password"), accessToken).contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"%s\",\"newPassword\":\"%s\"}".formatted(current, next)));
    }
}

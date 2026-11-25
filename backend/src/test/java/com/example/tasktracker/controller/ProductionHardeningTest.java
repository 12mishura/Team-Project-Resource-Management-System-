package com.example.tasktracker.controller;

import com.example.tasktracker.AbstractIntegrationTest;
import com.example.tasktracker.service.RefreshTokenCleanup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "app.rate-limit.enabled=true",
        "app.rate-limit.login-per-minute=3",
        "app.rate-limit.register-per-hour=2"
})
class ProductionHardeningTest extends AbstractIntegrationTest {
    @Autowired
    private RefreshTokenCleanup refreshTokenCleanup;

    @Test
    void repeatedLoginsFromOneAddressAreThrottled() throws Exception {
        for(int attempt = 0; attempt < 3; attempt++){
            login("nobody@mail.com", "wrong-password").andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@mail.com\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.startsWith("Too many attempts. Try again in")));

        mockMvc.perform(post("/auth/login")
                        .with(request -> {
                            request.setRemoteAddr("10.0.0.2");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@mail.com\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registrationsAreLimitedPerAddress() throws Exception {
        register("first@mail.com", "first", PASSWORD).andExpect(status().isCreated());
        register("second@mail.com", "second", PASSWORD).andExpect(status().isCreated());

        register("third@mail.com", "third", PASSWORD).andExpect(status().isTooManyRequests());
    }

    @Test
    void healthIsPublicAndRevealsNoDetails() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    void otherActuatorEndpointsStayClosed() throws Exception {
        mockMvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
    }

    @Test
    void cleanupRemovesOnlyExpiredRefreshTokens() {
        UUID userId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO users (id, email, username, password_hash, token_version, created_at, updated_at) VALUES (?, ?, ?, ?, 0, now(), now())",
                userId, "cleanup@mail.com", "cleanup", "hash");
        Instant now = Instant.now();
        insertToken(userId, "expired", now.minus(1, ChronoUnit.HOURS));
        insertToken(userId, "active", now.plus(1, ChronoUnit.DAYS));

        assertThat(refreshTokenCleanup.deleteExpired()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForList("SELECT token_hash FROM refresh_tokens", String.class)).containsExactly("active");
    }

    private void insertToken(UUID userId, String hash, Instant expiresAt) {
        jdbcTemplate.update("INSERT INTO refresh_tokens (id, user_id, token_hash, expires_at, created_at) VALUES (?, ?, ?, ?, now())",
                UUID.randomUUID(), userId, hash, Timestamp.from(expiresAt));
    }
}

package com.example.tasktracker.controller;

import com.example.tasktracker.AbstractIntegrationTest;
import com.example.tasktracker.security.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest extends AbstractIntegrationTest {

    @Autowired
    private JwtProperties jwtProperties;

    @Test
    void registerReturnsCreatedUser() throws Exception {
        register("alice@mail.com", "alice", PASSWORD)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(notNullValue()))
                .andExpect(jsonPath("$.email").value("alice@mail.com"))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.createdAt").value(notNullValue()))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void registerNormalizesEmail() throws Exception {
        register("Alice@Mail.COM", "alice", PASSWORD)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("alice@mail.com"));
    }

    @Test
    void registerTrimsEmailAndUsername() throws Exception {
        register("  Alice@Mail.com  ", "  alice  ", PASSWORD)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("alice@mail.com"))
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    void registerWithUsernameDifferingOnlyByCaseReturnsConflict() throws Exception {
        register("alice@mail.com", "alice", PASSWORD).andExpect(status().isCreated());

        register("other@mail.com", "ALICE", PASSWORD)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("User with username 'ALICE' already exists"));
    }

    @Test
    void loginTrimsEmail() throws Exception {
        register("alice@mail.com", "alice", PASSWORD).andExpect(status().isCreated());

        login("  alice@mail.com  ", PASSWORD).andExpect(status().isOk());
    }

    @Test
    void meAcceptsBearerSchemeInAnyCase() throws Exception {
        String token = registerAndLogin("alice");

        mockMvc.perform(get("/auth/me").header(HttpHeaders.AUTHORIZATION, "bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/auth/me").header(HttpHeaders.AUTHORIZATION, "BEARER " + token))
                .andExpect(status().isOk());
    }

    @Test
    void registerWithDuplicateEmailReturnsConflict() throws Exception {
        register("alice@mail.com", "alice", PASSWORD).andExpect(status().isCreated());

        register("ALICE@mail.com", "alice2", PASSWORD)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("User with email 'alice@mail.com' already exists"));
    }

    @Test
    void registerWithDuplicateUsernameReturnsConflict() throws Exception {
        register("alice@mail.com", "alice", PASSWORD).andExpect(status().isCreated());

        register("other@mail.com", "alice", PASSWORD)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("User with username 'alice' already exists"));
    }

    @Test
    void registerWithInvalidEmailReturnsBadRequest() throws Exception {
        register("not-an-email", "alice", PASSWORD).andExpect(status().isBadRequest());
    }

    @Test
    void registerWithShortPasswordReturnsBadRequest() throws Exception {
        register("alice@mail.com", "alice", "short").andExpect(status().isBadRequest());
    }

    @Test
    void registerWithPasswordOver72BytesReturnsBadRequest() throws Exception {
        register("alice@mail.com", "alice", "ж".repeat(37))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("password: must not exceed 72 bytes in UTF-8"));
    }

    @Test
    void registerWithMultibytePasswordOfExactly72BytesSucceeds() throws Exception {
        String password = "ж".repeat(36);

        register("alice@mail.com", "alice", password).andExpect(status().isCreated());
        login("alice@mail.com", password).andExpect(status().isOk());
    }

    @Test
    void loginWithPasswordOver72BytesReturnsUnauthorized() throws Exception {
        register("alice@mail.com", "alice", PASSWORD).andExpect(status().isCreated());

        login("alice@mail.com", "ж".repeat(40)).andExpect(status().isUnauthorized());
    }

    @Test
    void registerWithShortUsernameReturnsBadRequest() throws Exception {
        register("alice@mail.com", "al", PASSWORD).andExpect(status().isBadRequest());
    }

    @Test
    void loginReturnsBearerToken() throws Exception {
        register("alice@mail.com", "alice", PASSWORD).andExpect(status().isCreated());

        login("alice@mail.com", PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(notNullValue()))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600));
    }

    @Test
    void loginIgnoresEmailCase() throws Exception {
        register("alice@mail.com", "alice", PASSWORD).andExpect(status().isCreated());

        login("ALICE@Mail.com", PASSWORD).andExpect(status().isOk());
    }

    @Test
    void loginWithWrongPasswordReturnsUnauthorized() throws Exception {
        register("alice@mail.com", "alice", PASSWORD).andExpect(status().isCreated());

        login("alice@mail.com", "wrongPassword")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));
    }

    @Test
    void loginWithUnknownEmailReturnsUnauthorized() throws Exception {
        login("nobody@mail.com", PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));
    }

    @Test
    void meReturnsCurrentUser() throws Exception {
        String token = registerAndLogin("alice");

        mockMvc.perform(withToken(get("/auth/me"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@mail.com"))
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    void meWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Authentication required"));
    }

    @Test
    void meWithMalformedTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(withToken(get("/auth/me"), "abc.def.ghi"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meWithTokenSignedByAnotherKeyReturnsUnauthorized() throws Exception {
        register("alice@mail.com", "alice", PASSWORD).andExpect(status().isCreated());
        String foreignToken = Jwts.builder()
                .subject("alice@mail.com")
                .expiration(Date.from(Instant.now().plus(Duration.ofHours(1))))
                .signWith(Jwts.SIG.HS384.key().build())
                .compact();

        mockMvc.perform(withToken(get("/auth/me"), foreignToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meWithValidlySignedTokenWithoutVersionReturnsUnauthorized() throws Exception {
        register("alice@mail.com", "alice", PASSWORD).andExpect(status().isCreated());
        String tokenWithoutVersion = Jwts.builder()
                .subject("alice@mail.com")
                .expiration(Date.from(Instant.now().plus(Duration.ofHours(1))))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.secret())))
                .compact();

        mockMvc.perform(withToken(get("/auth/me"), tokenWithoutVersion))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meWithExpiredTokenReturnsUnauthorized() throws Exception {
        register("alice@mail.com", "alice", PASSWORD).andExpect(status().isCreated());
        Instant past = Instant.now().minus(Duration.ofHours(2));
        String expiredToken = Jwts.builder()
                .subject("alice@mail.com")
                .issuedAt(Date.from(past))
                .expiration(Date.from(past.plus(Duration.ofHours(1))))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.secret())))
                .compact();

        mockMvc.perform(withToken(get("/auth/me"), expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meWithTokenOfDeletedUserReturnsUnauthorized() throws Exception {
        String token = registerAndLogin("alice");
        jdbcTemplate.update("DELETE FROM users WHERE email = ?", "alice@mail.com");

        mockMvc.perform(withToken(get("/auth/me"), token))
                .andExpect(status().isUnauthorized());
    }
}

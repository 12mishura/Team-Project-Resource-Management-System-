package com.example.tasktracker.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenTest {
    private final Instant now = Instant.parse("2026-01-01T10:00:00Z");

    @Test
    void tokenExpiresExactlyAtExpirationTime() {
        RefreshToken token = new RefreshToken(null, "hash", now);

        assertThat(token.isExpired(now.minusSeconds(1))).isFalse();
        assertThat(token.isExpired(now)).isTrue();
        assertThat(token.isExpired(now.plusSeconds(1))).isTrue();
    }

    @Test
    void revokeKeepsFirstRevocationTime() {
        RefreshToken token = new RefreshToken(null, "hash", now.plusSeconds(60));

        assertThat(token.isRevoked()).isFalse();
        token.revoke(now);
        token.revoke(now.plusSeconds(30));

        assertThat(token.isRevoked()).isTrue();
        assertThat(token.getRevokedAt()).isEqualTo(now);
    }
}

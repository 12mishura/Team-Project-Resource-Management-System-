package com.example.tasktracker.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {
    private static final String TOKEN_VERSION_CLAIM = "tv";

    private final SecretKey signingKey;
    private final JwtProperties properties;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()));
    }

    public String generateToken(UserDetails userDetails) {
        Instant now = Instant.now();
        int tokenVersion = userDetails instanceof AuthenticatedUser user ? user.tokenVersion() : 0;
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim(TOKEN_VERSION_CLAIM, tokenVersion)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.expiration())))
                .signWith(signingKey)
                .compact();
    }

    public long getExpirationSeconds() {
        return properties.expiration().toSeconds();
    }

    public long getRefreshExpirationSeconds() {
        return properties.refreshExpiration().toSeconds();
    }

    public Optional<TokenClaims> parse(String token) {
        try {
            Claims claims = parseClaims(token);
            if (claims.getSubject() == null) {
                return Optional.empty();
            }
            return Optional.of(new TokenClaims(claims.getSubject(), claims.get(TOKEN_VERSION_CLAIM, Integer.class)));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

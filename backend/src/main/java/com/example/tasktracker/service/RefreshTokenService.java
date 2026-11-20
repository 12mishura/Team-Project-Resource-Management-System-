package com.example.tasktracker.service;

import com.example.tasktracker.domain.RefreshToken;
import com.example.tasktracker.domain.User;
import com.example.tasktracker.exception.InvalidRefreshTokenException;
import com.example.tasktracker.repository.RefreshTokenRepository;
import com.example.tasktracker.security.JwtProperties;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
@AllArgsConstructor
public class RefreshTokenService {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    @Transactional
    public String issue(User user){
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        refreshTokenRepository.save(new RefreshToken(user, hash(rawToken), Instant.now().plus(jwtProperties.refreshExpiration())));
        return rawToken;
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public User consume(String rawToken){
        Instant now = Instant.now();
        RefreshToken token = refreshTokenRepository.findForUpdate(hash(rawToken))
                .orElseThrow(InvalidRefreshTokenException::new);
        if(token.isRevoked()){
            refreshTokenRepository.revokeAllForUser(token.getUser().getId(), now);
            throw new InvalidRefreshTokenException();
        }
        if(token.isExpired(now)){
            throw new InvalidRefreshTokenException();
        }
        token.revoke(now);
        return token.getUser();
    }

    @Transactional
    public void revoke(String rawToken){
        refreshTokenRepository.findForUpdate(hash(rawToken)).ifPresent(token -> token.revoke(Instant.now()));
    }

    @Transactional
    public void revokeAll(UUID userId){
        refreshTokenRepository.revokeAllForUser(userId, Instant.now());
    }

    private static String hash(String rawToken){
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}

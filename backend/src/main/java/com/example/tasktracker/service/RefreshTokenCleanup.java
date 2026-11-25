package com.example.tasktracker.service;

import com.example.tasktracker.repository.RefreshTokenRepository;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Component
@AllArgsConstructor
public class RefreshTokenCleanup {
    private final RefreshTokenRepository refreshTokenRepository;
    private final Clock clock;

    @Scheduled(cron = "${app.refresh-token-cleanup-cron:0 30 3 * * *}")
    @Transactional
    public int deleteExpired(){
        return refreshTokenRepository.deleteExpiredBefore(Instant.now(clock));
    }
}

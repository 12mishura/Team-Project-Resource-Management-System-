package com.example.tasktracker.dto.response;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String username,
        Instant createdAt
) {
}

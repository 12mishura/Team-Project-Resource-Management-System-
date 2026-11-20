package com.example.tasktracker.dto.response;

import java.time.Instant;
import java.util.UUID;

public record OrganizationResponse(
        UUID id,
        String title,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
}

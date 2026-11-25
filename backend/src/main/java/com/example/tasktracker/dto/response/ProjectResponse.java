package com.example.tasktracker.dto.response;

import com.example.tasktracker.domain.enums.ProjectStatus;

import java.time.Instant;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        UUID organizationId,
        String title,
        String description,
        ProjectStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}

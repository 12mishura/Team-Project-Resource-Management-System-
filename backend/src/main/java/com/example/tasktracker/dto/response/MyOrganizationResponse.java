package com.example.tasktracker.dto.response;

import com.example.tasktracker.domain.enums.OrganizationRole;

import java.time.Instant;
import java.util.UUID;

public record MyOrganizationResponse(
        UUID id,
        String title,
        String description,
        OrganizationRole role,
        UUID memberId,
        Instant joinedAt
) {
}

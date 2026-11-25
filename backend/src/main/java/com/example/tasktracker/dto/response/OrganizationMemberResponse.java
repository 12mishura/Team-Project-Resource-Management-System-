package com.example.tasktracker.dto.response;

import com.example.tasktracker.domain.enums.OrganizationRole;

import java.time.Instant;
import java.util.UUID;

public record OrganizationMemberResponse(
        UUID id,
        String email,
        String username,
        OrganizationRole role,
        int weeklyCapacityMinutes,
        Instant createdAt
) {
}

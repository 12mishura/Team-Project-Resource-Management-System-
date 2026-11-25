package com.example.tasktracker.dto.response;

import com.example.tasktracker.domain.enums.ProjectMemberRole;

import java.time.Instant;
import java.util.UUID;

public record ProjectMemberResponse(
        UUID id,
        UUID projectId,
        UUID organizationMemberId,
        String email,
        String username,
        ProjectMemberRole role,
        Instant createdAt
) {
}

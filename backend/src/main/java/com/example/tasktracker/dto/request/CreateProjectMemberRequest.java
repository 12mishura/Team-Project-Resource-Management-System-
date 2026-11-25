package com.example.tasktracker.dto.request;

import com.example.tasktracker.domain.enums.ProjectMemberRole;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateProjectMemberRequest(
        @NotNull UUID organizationMemberId,
        @NotNull ProjectMemberRole role
) {
}

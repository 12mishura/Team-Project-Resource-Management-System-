package com.example.tasktracker.dto.request;

import com.example.tasktracker.domain.enums.ProjectMemberRole;
import jakarta.validation.constraints.NotNull;

public record UpdateProjectMemberRequest(
        @NotNull ProjectMemberRole role
) {
}

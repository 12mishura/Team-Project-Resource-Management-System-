package com.example.backend.dto;

import com.example.backend.entity.enums.ProjectRole;
import jakarta.validation.constraints.NotNull;

public record ProjectMemberRequestDTO(
        @NotNull Integer userId,
        @NotNull Integer projectId,
        @NotNull ProjectRole projectRole
) {}
package com.example.tasktracker.dto.request;

import com.example.tasktracker.domain.enums.ProjectStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeProjectStatusRequest(
        @NotNull ProjectStatus status
) {
}

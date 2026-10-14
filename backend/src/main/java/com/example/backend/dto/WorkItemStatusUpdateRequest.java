package com.example.backend.dto;

import com.example.backend.entity.enums.WorkItemStatus;
import jakarta.validation.constraints.NotNull;

public record WorkItemStatusUpdateRequest(
        @NotNull(message = "Status is required")
        WorkItemStatus status
) {
}
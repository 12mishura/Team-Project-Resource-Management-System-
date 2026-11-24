package com.example.tasktracker.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateOrganizationRequest(
        @Size(min = 3, max = 50) String title,
        @Size(max = 400) String description
) {
    public UpdateOrganizationRequest {
        title = title == null ? null : title.strip();
    }
}

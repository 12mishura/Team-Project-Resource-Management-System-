package com.example.tasktracker.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateOrganizationRequest(
        @NotBlank @Size(min = 8, max = 50) String title,
        @Size(max = 400) String description
) {
    public CreateOrganizationRequest {
        title = title == null ? null : title.strip();
    }
}

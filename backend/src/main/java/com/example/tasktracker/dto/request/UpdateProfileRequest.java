package com.example.tasktracker.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @Size(min = 3, max = 50) String username
) {
    public UpdateProfileRequest {
        username = username == null ? null : username.strip();
    }
}

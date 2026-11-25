package com.example.tasktracker.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
        @NotBlank @Size(min = 3, max = 50) String title,
        @Size(max = 400) String description
) {
    public CreateProjectRequest{
        title = title == null ? null : title.strip();
    }
}

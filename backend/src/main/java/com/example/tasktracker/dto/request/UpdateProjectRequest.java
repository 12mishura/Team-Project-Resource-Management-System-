package com.example.tasktracker.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateProjectRequest(
        @Size(min = 8, max = 50) String title,
        @Size(max = 400) String description
) {
    public UpdateProjectRequest{
        title = title == null ? null : title.strip();
    }
}

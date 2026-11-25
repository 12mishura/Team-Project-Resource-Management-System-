package com.example.tasktracker.dto.response;

import java.util.UUID;

public record ProjectMinutesResponse(
        UUID projectId,
        String projectTitle,
        long minutes
) {
}

package com.example.tasktracker.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record WorkLogResponse(
        UUID id,
        UUID issueId,
        UUID authorId,
        String authorUsername,
        LocalDate workDate,
        int minutes,
        String comment,
        Instant createdAt,
        Instant updatedAt
) {
}

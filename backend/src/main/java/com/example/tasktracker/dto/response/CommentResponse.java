package com.example.tasktracker.dto.response;

import java.time.Instant;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        UUID issueId,
        UUID authorId,
        String authorUsername,
        String text,
        Instant createdAt,
        Instant editedAt
) {
}

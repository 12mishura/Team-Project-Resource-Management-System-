package com.example.tasktracker.dto.response;

import com.example.tasktracker.domain.enums.IssueEventType;

import java.time.Instant;
import java.util.UUID;

public record IssueEventResponse(
        UUID id,
        IssueEventType type,
        String field,
        String oldValue,
        String newValue,
        UUID actorId,
        String actorUsername,
        Instant createdAt
) {
}

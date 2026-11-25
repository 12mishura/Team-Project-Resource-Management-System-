package com.example.tasktracker.dto.response;

import com.example.tasktracker.domain.enums.IssueEventType;
import com.example.tasktracker.domain.enums.IssueType;

import java.time.Instant;
import java.util.UUID;

public record ProjectActivityResponse(
        UUID id,
        UUID issueId,
        String issueTitle,
        IssueType issueType,
        IssueEventType type,
        String field,
        String oldValue,
        String newValue,
        UUID actorId,
        String actorUsername,
        Instant createdAt
) {
}

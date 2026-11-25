package com.example.tasktracker.dto.response;

import com.example.tasktracker.domain.enums.BugSeverity;
import com.example.tasktracker.domain.enums.IssuePriority;
import com.example.tasktracker.domain.enums.IssueStatus;
import com.example.tasktracker.domain.enums.IssueType;

import java.time.Instant;
import java.util.UUID;

public record IssueResponse(
        UUID id,
        UUID organizationId,
        UUID projectId,
        IssueType type,
        String title,
        String description,
        IssueStatus status,
        IssuePriority priority,
        UUID reporterId,
        String reporterUsername,
        UUID assigneeId,
        String assigneeUsername,
        UUID parentId,
        BugSeverity severity,
        String stepsToReproduce,
        Integer storyPoints,
        Integer estimateMinutes,
        long spentMinutes,
        Long remainingMinutes,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {
}

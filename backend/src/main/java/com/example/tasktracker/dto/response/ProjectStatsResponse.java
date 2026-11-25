package com.example.tasktracker.dto.response;

import com.example.tasktracker.domain.enums.IssueStatus;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ProjectStatsResponse(
        UUID projectId,
        Map<IssueStatus, Long> issuesByStatus,
        long totalIssues,
        long openIssues,
        long openBugs,
        long assignedToMe,
        long membersCount,
        List<String> members
) {
}

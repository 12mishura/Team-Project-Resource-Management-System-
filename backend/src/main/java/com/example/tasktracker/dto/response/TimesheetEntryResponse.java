package com.example.tasktracker.dto.response;

import com.example.tasktracker.domain.enums.IssueStatus;
import com.example.tasktracker.domain.enums.IssueType;

import java.time.LocalDate;
import java.util.UUID;

public record TimesheetEntryResponse(
        UUID id,
        UUID organizationId,
        String organizationTitle,
        UUID projectId,
        String projectTitle,
        UUID issueId,
        String issueTitle,
        IssueType issueType,
        IssueStatus issueStatus,
        LocalDate workDate,
        int minutes,
        String comment
) {
}

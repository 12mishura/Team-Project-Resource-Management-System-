package com.example.tasktracker.dto.request;

import com.example.tasktracker.domain.enums.IssueStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeIssueStatusRequest(
        @NotNull IssueStatus status
) {
}

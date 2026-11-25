package com.example.tasktracker.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignIssueRequest(
        @NotNull UUID projectMemberId
) {
}

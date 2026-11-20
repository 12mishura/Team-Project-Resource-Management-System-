package com.example.tasktracker.dto.request;

import com.example.tasktracker.domain.enums.BugSeverity;
import com.example.tasktracker.domain.enums.IssuePriority;
import com.example.tasktracker.domain.enums.IssueType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateIssueRequest(
        @NotNull IssueType type,
        @NotBlank @Size(min = 3, max = 200) String title,
        @Size(max = 5000) String description,
        IssuePriority priority,
        BugSeverity severity,
        @Size(max = 5000) String stepsToReproduce,
        @Min(0) @Max(100) Integer storyPoints,
        UUID parentId
) {
    public CreateIssueRequest {
        title = title == null ? null : title.strip();
    }
}

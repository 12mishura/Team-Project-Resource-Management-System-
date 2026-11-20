package com.example.tasktracker.dto.request;

import com.example.tasktracker.domain.enums.BugSeverity;
import com.example.tasktracker.domain.enums.IssuePriority;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateIssueRequest(
        @Size(min = 3, max = 200) String title,
        @Size(max = 5000) String description,
        IssuePriority priority,
        BugSeverity severity,
        @Size(max = 5000) String stepsToReproduce,
        @Min(0) @Max(100) Integer storyPoints
) {
    public UpdateIssueRequest {
        title = title == null ? null : title.strip();
    }
}

package com.example.tasktracker.dto.request;

import com.example.tasktracker.domain.enums.IssueSortField;
import com.example.tasktracker.domain.enums.IssueStatus;
import com.example.tasktracker.domain.enums.IssueType;
import org.springframework.data.domain.Sort;

import java.util.UUID;

public record IssueFilter(
        IssueType type,
        IssueStatus status,
        UUID assigneeId,
        UUID parentId,
        String q,
        IssueSortField sortBy,
        Sort.Direction direction
) {
    public IssueFilter {
        q = q == null || q.isBlank() ? null : q.strip();
        sortBy = sortBy == null ? IssueSortField.CREATED_AT : sortBy;
        direction = direction == null ? Sort.Direction.ASC : direction;
    }
}

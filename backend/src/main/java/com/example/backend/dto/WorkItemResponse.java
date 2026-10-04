package com.example.backend.dto;

import com.example.backend.entity.enums.WorkItemPriority;
import com.example.backend.entity.enums.WorkItemStatus;
import com.example.backend.entity.enums.WorkItemType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record WorkItemResponse(
        Long id,
        String title,
        String description,
        WorkItemType type,
        WorkItemStatus status,
        WorkItemPriority priority,
        Integer estimatedHours,
        LocalDate dueDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long projectId,
        Long sprintId,
        Long parentId,
        Long taskCreatorId,
        Long taskAssigneeId
) {
}

package com.example.backend.DTO;

import com.example.backend.Entity.Enum.WorkItemPriority;
import com.example.backend.Entity.Enum.WorkItemStatus;
import com.example.backend.Entity.Enum.WorkItemType;

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

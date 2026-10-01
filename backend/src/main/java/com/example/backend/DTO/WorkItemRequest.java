package com.example.backend.DTO;

import com.example.backend.Entity.Enum.WorkItemPriority;
import com.example.backend.Entity.Enum.WorkItemType;

import java.time.LocalDate;

public record WorkItemRequest(
        String title,
        String description,
        WorkItemType type,
        WorkItemPriority priority,
        Integer estimatedHours,
        LocalDate dueDate,
        Long parentId
) {
}

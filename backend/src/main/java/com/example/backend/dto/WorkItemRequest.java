package com.example.backend.dto;

import com.example.backend.entity.enums.WorkItemPriority;
import com.example.backend.entity.enums.WorkItemType;

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

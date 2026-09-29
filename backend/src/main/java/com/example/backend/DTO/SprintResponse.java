package com.example.backend.DTO;

import com.example.backend.Entity.Enum.SprintPlan;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record SprintResponse(
        Long id,
        String name,
        String goal,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        SprintPlan status,
        Long projectId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
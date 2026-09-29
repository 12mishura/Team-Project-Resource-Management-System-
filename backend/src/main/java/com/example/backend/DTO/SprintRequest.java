package com.example.backend.DTO;

import com.example.backend.Entity.Enum.SprintPlan;
import java.time.LocalDate;

public record SprintRequest(
        String name,
        String goal,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        SprintPlan status,
        Long projectId
) {
}
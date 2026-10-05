package com.example.backend.dto;

import com.example.backend.entity.enums.SprintPlan;
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
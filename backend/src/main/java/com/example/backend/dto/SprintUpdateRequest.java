package com.example.backend.dto;

import java.time.LocalDate;

public record SprintUpdateRequest(
        String name,
        String goal,
        String description,
        LocalDate startDate,
        LocalDate endDate
) {
}
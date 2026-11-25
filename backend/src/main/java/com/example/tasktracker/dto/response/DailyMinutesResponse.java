package com.example.tasktracker.dto.response;

import java.time.LocalDate;

public record DailyMinutesResponse(
        LocalDate date,
        long minutes
) {
}

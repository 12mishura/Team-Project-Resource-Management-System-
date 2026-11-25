package com.example.tasktracker.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateWorkLogRequest(
        @NotNull @Min(1) @Max(1440) Integer minutes,
        @NotNull LocalDate workDate,
        @Size(max = 500) String comment
) {
    public CreateWorkLogRequest {
        comment = comment == null || comment.isBlank() ? null : comment.strip();
    }
}

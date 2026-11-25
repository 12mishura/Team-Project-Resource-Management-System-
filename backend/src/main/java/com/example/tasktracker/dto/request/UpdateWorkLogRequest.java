package com.example.tasktracker.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateWorkLogRequest(
        @Min(1) @Max(1440) Integer minutes,
        LocalDate workDate,
        @Size(max = 500) String comment
) {
    public UpdateWorkLogRequest {
        comment = comment == null ? null : comment.strip();
    }
}

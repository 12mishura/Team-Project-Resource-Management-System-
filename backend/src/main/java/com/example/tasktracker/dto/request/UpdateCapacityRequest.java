package com.example.tasktracker.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateCapacityRequest(
        @NotNull @Min(0) @Max(10080) Integer weeklyCapacityMinutes
) {
}

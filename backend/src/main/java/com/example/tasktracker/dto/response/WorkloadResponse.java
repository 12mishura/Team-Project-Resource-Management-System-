package com.example.tasktracker.dto.response;

import java.time.LocalDate;
import java.util.List;

public record WorkloadResponse(
        LocalDate from,
        LocalDate to,
        int workingDays,
        long loggedMinutes,
        long capacityMinutes,
        long remainingEstimateMinutes,
        List<MemberWorkloadResponse> members
) {
}

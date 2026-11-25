package com.example.tasktracker.dto.response;

import java.util.List;
import java.util.UUID;

public record MemberWorkloadResponse(
        UUID organizationMemberId,
        UUID userId,
        String username,
        String role,
        int weeklyCapacityMinutes,
        long capacityMinutes,
        long loggedMinutes,
        long openIssues,
        long unestimatedIssues,
        long remainingEstimateMinutes,
        boolean overloaded,
        List<DailyMinutesResponse> days,
        List<ProjectMinutesResponse> projects
) {
}

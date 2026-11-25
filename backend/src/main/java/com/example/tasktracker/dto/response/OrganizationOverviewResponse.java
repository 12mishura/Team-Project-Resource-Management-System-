package com.example.tasktracker.dto.response;

import java.util.List;
import java.util.UUID;

public record OrganizationOverviewResponse(
        UUID organizationId,
        long projectsCount,
        long membersCount,
        long openIssues,
        long assignedToMe,
        List<ProjectStatsResponse> projects
) {
}

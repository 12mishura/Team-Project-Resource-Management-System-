package com.example.tasktracker.dto.request;

import com.example.tasktracker.domain.enums.OrganizationRole;
import jakarta.validation.constraints.NotNull;

public record UpdateOrganizationMemberRequest(
        @NotNull OrganizationRole role
        ) {
}

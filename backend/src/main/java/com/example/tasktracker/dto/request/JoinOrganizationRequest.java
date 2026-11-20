package com.example.tasktracker.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record JoinOrganizationRequest(
        @NotBlank @Size(max = 9) String inviteCode
) {
    public JoinOrganizationRequest {
        inviteCode = inviteCode == null ? null : inviteCode.strip().toLowerCase(Locale.ROOT);
    }
}

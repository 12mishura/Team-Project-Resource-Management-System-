package com.example.tasktracker.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TransferOwnershipRequest(
        @NotNull UUID memberId
) {
}

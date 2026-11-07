package com.example.backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProjectResponse(UUID id , String name , String description , LocalDate startDate , LocalDate endDate , LocalDateTime createdAt , LocalDateTime updatedAt , Integer organizationId) {
}

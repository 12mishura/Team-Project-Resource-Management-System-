package com.example.backend.DTO;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProjectResponse(Long id , String name , String description , LocalDate startDate , LocalDate endDate , LocalDateTime created_at , LocalDateTime updated_at , Long organization_id) {
}

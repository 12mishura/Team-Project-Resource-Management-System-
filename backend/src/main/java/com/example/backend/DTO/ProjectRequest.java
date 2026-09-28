package com.example.backend.DTO;

import java.time.LocalDate;

public record ProjectRequest(String name , String description , LocalDate startDate , LocalDate endDate) {
}

package com.example.backend.Mapper;

import com.example.backend.DTO.SprintRequest;
import com.example.backend.DTO.SprintResponse;
import com.example.backend.Entity.Enum.SprintPlan;
import com.example.backend.Entity.Sprint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SprintMapperTest {

    private SprintMapper sprintMapper;

    @BeforeEach
    void setUp() {
        sprintMapper = new SprintMapper();
    }

    @Test
    void shouldMapRequestToEntity() {
        LocalDate startDate = LocalDate.of(2026, 10, 1);
        LocalDate endDate = LocalDate.of(2026, 10, 15);

        SprintRequest request = new SprintRequest(
                "Sprint 1",
                "MVP Release",
                "Complete base domain models",
                startDate,
                endDate,
                SprintPlan.PLANNED,
                1L
        );

        Sprint sprint = sprintMapper.toEntity(request);

        assertNotNull(sprint);
        assertEquals("Sprint 1", sprint.getName());
        assertEquals("MVP Release", sprint.getGoal());
        assertEquals("Complete base domain models", sprint.getDescription());
        assertEquals(startDate, sprint.getStartDate());
        assertEquals(endDate, sprint.getEndDate());
        assertEquals(SprintPlan.PLANNED, sprint.getStatus());
        assertEquals(1L, sprint.getProjectId());
    }

    @Test
    void shouldMapEntityToResponse() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate startDate = LocalDate.of(2026, 10, 1);
        LocalDate endDate = LocalDate.of(2026, 10, 15);

        Sprint sprint = Sprint.builder()
                .id(10L)
                .name("Sprint 1")
                .goal("MVP Release")
                .description("Complete base domain models")
                .startDate(startDate)
                .endDate(endDate)
                .status(SprintPlan.ACTIVE)
                .projectId(1L)
                .createdAt(now)
                .updatedAt(now)
                .build();

        SprintResponse response = sprintMapper.toResponse(sprint);

        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals("Sprint 1", response.name());
        assertEquals("MVP Release", response.goal());
        assertEquals(1L, response.projectId());
        assertEquals(SprintPlan.ACTIVE, response.status());
        assertEquals(now, response.createdAt());
    }
}
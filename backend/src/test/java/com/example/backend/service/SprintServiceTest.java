package com.example.backend.service;

import com.example.backend.dto.SprintRequest;
import com.example.backend.dto.SprintResponse;
import com.example.backend.dto.SprintUpdateRequest;
import com.example.backend.entity.Sprint;
import com.example.backend.entity.enums.SprintPlan;
import com.example.backend.mapper.SprintMapper;
import com.example.backend.repository.SprintRepository;
import com.example.backend.service.impl.SprintServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SprintServiceTest {

    @Mock
    private SprintRepository sprintRepository;

    @Spy
    private SprintMapper sprintMapper;

    @InjectMocks
    private SprintServiceImpl sprintService;

    private Sprint plannedSprint;
    private final Long projectId = 1L;
    private final Long sprintId = 10L;

    @BeforeEach
    void setUp() {
        plannedSprint = Sprint.builder()
                .id(sprintId)
                .name("Sprint 1")
                .projectId(projectId)
                .status(SprintPlan.PLANNED)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusWeeks(2))
                .build();
    }

    @Test
    void createSprint_shouldThrowException_whenEndDateBeforeStartDate() {
        SprintRequest request = new SprintRequest(
                "Invalid Sprint",
                "Goal",
                "Desc",
                LocalDate.now().plusDays(5),
                LocalDate.now(),
                SprintPlan.PLANNED,
                projectId
        );

        assertThrows(IllegalArgumentException.class, () -> sprintService.createSprint(projectId, request));
    }

    @Test
    void startSprint_shouldChangeStatusToActive() {
        when(sprintRepository.findByIdAndProjectId(sprintId, projectId))
                .thenReturn(Optional.of(plannedSprint));
        when(sprintRepository.existsByProjectIdAndStatus(projectId, SprintPlan.ACTIVE))
                .thenReturn(false);

        SprintResponse response = sprintService.startSprint(projectId, sprintId);

        assertEquals(SprintPlan.ACTIVE, response.status());
        assertEquals(SprintPlan.ACTIVE, plannedSprint.getStatus());
    }

    @Test
    void startSprint_shouldThrowException_whenActiveSprintAlreadyExists() {
        when(sprintRepository.findByIdAndProjectId(sprintId, projectId))
                .thenReturn(Optional.of(plannedSprint));
        when(sprintRepository.existsByProjectIdAndStatus(projectId, SprintPlan.ACTIVE))
                .thenReturn(true);

        assertThrows(IllegalStateException.class, () -> sprintService.startSprint(projectId, sprintId));
    }

    @Test
    void completeSprint_shouldChangeStatusToCompleted() {
        Sprint activeSprint = Sprint.builder()
                .id(sprintId)
                .projectId(projectId)
                .status(SprintPlan.ACTIVE)
                .build();

        when(sprintRepository.findByIdAndProjectId(sprintId, projectId))
                .thenReturn(Optional.of(activeSprint));

        SprintResponse response = sprintService.completeSprint(projectId, sprintId);

        assertEquals(SprintPlan.COMPLETED, response.status());
    }

    @Test
    void completeSprint_shouldThrowException_whenSprintIsNotActive() {
        when(sprintRepository.findByIdAndProjectId(sprintId, projectId))
                .thenReturn(Optional.of(plannedSprint)); // Status is PLANNED

        assertThrows(IllegalStateException.class, () -> sprintService.completeSprint(projectId, sprintId));
    }

    @Test
    void getSprintById_shouldThrowException_whenSprintBelongsToAnotherProject() {
        when(sprintRepository.findByIdAndProjectId(sprintId, projectId))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> sprintService.getSprintById(projectId, sprintId));
    }
}
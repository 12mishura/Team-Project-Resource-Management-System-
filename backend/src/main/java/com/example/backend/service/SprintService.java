package com.example.backend.service;

import com.example.backend.dto.SprintRequest;
import com.example.backend.dto.SprintResponse;
import com.example.backend.dto.SprintUpdateRequest;

import java.util.List;

public interface SprintService {

    SprintResponse createSprint(Long projectId, SprintRequest request);

    List<SprintResponse> getSprintsByProject(Long projectId);

    SprintResponse getSprintById(Long projectId, Long sprintId);

    SprintResponse updateSprint(Long projectId, Long sprintId, SprintUpdateRequest request);

    void deleteSprint(Long projectId, Long sprintId);

    SprintResponse startSprint(Long projectId, Long sprintId);

    SprintResponse completeSprint(Long projectId, Long sprintId);
}
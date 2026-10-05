package com.example.backend.service.impl;

import com.example.backend.dto.SprintRequest;
import com.example.backend.dto.SprintResponse;
import com.example.backend.dto.SprintUpdateRequest;
import com.example.backend.entity.Sprint;
import com.example.backend.entity.enums.SprintPlan;
import com.example.backend.mapper.SprintMapper;
import com.example.backend.repository.SprintRepository;
import com.example.backend.service.SprintService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SprintServiceImpl implements SprintService {

    private final SprintRepository sprintRepository;
    private final SprintMapper sprintMapper;

    @Override
    public SprintResponse createSprint(Long projectId, SprintRequest request) {
        validateDates(request.startDate(), request.endDate());

        Sprint sprint = sprintMapper.toEntity(request);
        sprint.setProjectId(projectId);
        sprint.setStatus(SprintPlan.PLANNED);

        Sprint savedSprint = sprintRepository.save(sprint);
        return sprintMapper.toResponse(savedSprint);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SprintResponse> getSprintsByProject(Long projectId) {
        return sprintRepository.findAllByProjectId(projectId)
                .stream()
                .map(sprintMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SprintResponse getSprintById(Long projectId, Long sprintId) {
        Sprint sprint = findSprintOrThrow(projectId, sprintId);
        return sprintMapper.toResponse(sprint);
    }

    @Override
    public SprintResponse updateSprint(Long projectId, Long sprintId, SprintUpdateRequest request) {
        Sprint sprint = findSprintOrThrow(projectId, sprintId);

        LocalDate startDate = request.startDate() != null ? request.startDate() : sprint.getStartDate();
        LocalDate endDate = request.endDate() != null ? request.endDate() : sprint.getEndDate();
        validateDates(startDate, endDate);

        if (request.name() != null) {
            sprint.setName(request.name());
        }
        if (request.goal() != null) {
            sprint.setGoal(request.goal());
        }
        if (request.description() != null) {
            sprint.setDescription(request.description());
        }
        sprint.setStartDate(startDate);
        sprint.setEndDate(endDate);

        return sprintMapper.toResponse(sprint);
    }

    @Override
    public void deleteSprint(Long projectId, Long sprintId) {
        Sprint sprint = findSprintOrThrow(projectId, sprintId);
        if (sprint.getStatus() == SprintPlan.ACTIVE) {
            throw new IllegalStateException("Cannot delete an ACTIVE sprint. Complete or cancel it first.");
        }
        sprintRepository.delete(sprint);
    }

    @Override
    public SprintResponse startSprint(Long projectId, Long sprintId) {
        Sprint sprint = findSprintOrThrow(projectId, sprintId);

        if (sprint.getStatus() != SprintPlan.PLANNED) {
            throw new IllegalStateException("Only PLANNED sprints can be started. Current status: " + sprint.getStatus());
        }

        if (sprintRepository.existsByProjectIdAndStatus(projectId, SprintPlan.ACTIVE)) {
            throw new IllegalStateException("Cannot start sprint: project already has an ACTIVE sprint.");
        }

        sprint.setStatus(SprintPlan.ACTIVE);
        return sprintMapper.toResponse(sprint);
    }

    @Override
    public SprintResponse completeSprint(Long projectId, Long sprintId) {
        Sprint sprint = findSprintOrThrow(projectId, sprintId);

        if (sprint.getStatus() != SprintPlan.ACTIVE) {
            throw new IllegalStateException("Only ACTIVE sprints can be completed. Current status: " + sprint.getStatus());
        }

        sprint.setStatus(SprintPlan.COMPLETED);
        return sprintMapper.toResponse(sprint);
    }

    private Sprint findSprintOrThrow(Long projectId, Long sprintId) {
        return sprintRepository.findByIdAndProjectId(sprintId, projectId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Sprint with id " + sprintId + " not found for project " + projectId));
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && !endDate.isAfter(startDate)) {
            throw new IllegalArgumentException("Sprint endDate must be after startDate.");
        }
    }
}
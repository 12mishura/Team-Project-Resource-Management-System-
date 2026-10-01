package com.example.backend.Mapper;

import com.example.backend.DTO.SprintRequest;
import com.example.backend.DTO.SprintResponse;
import com.example.backend.Entity.Enum.SprintPlan;
import com.example.backend.Entity.Sprint;
import org.springframework.stereotype.Component;

@Component
public class SprintMapper {

    public Sprint toEntity(SprintRequest request) {
        if (request == null) {
            return null;
        }

        return Sprint.builder()
                .name(request.name())
                .goal(request.goal())
                .description(request.description())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .status(request.status() != null ? request.status() : SprintPlan.PLANNED)
                .projectId(request.projectId())
                .build();
    }

    public SprintResponse toResponse(Sprint sprint) {
        if (sprint == null) {
            return null;
        }

        return new SprintResponse(
                sprint.getId(),
                sprint.getName(),
                sprint.getGoal(),
                sprint.getDescription(),
                sprint.getStartDate(),
                sprint.getEndDate(),
                sprint.getStatus(),
                sprint.getProjectId(),
                sprint.getCreatedAt(),
                sprint.getUpdatedAt()
        );
    }
}
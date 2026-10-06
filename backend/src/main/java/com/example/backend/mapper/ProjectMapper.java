package com.example.backend.mapper;

import com.example.backend.dto.ProjectRequest;
import com.example.backend.dto.ProjectResponse;
import com.example.backend.entity.Organization;
import com.example.backend.entity.Project;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProjectMapper {
    public ProjectResponse toResponse(Project project){
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getStartDate(),
                project.getEndDate(),
                project.getCreatedAt(),
                project.getUpdatedAt(),
                project.getOrganization().getId()
        );
    }

    public List<ProjectResponse> toList(List<Project> projects){
        return projects.stream().map(this::toResponse).toList();
    }

    public Project toEntity(ProjectRequest request , Organization organization){
        if (request == null) {
            return null;
        }

        Project project = new Project();

        project.setName(request.name());
        project.setDescription(request.description());
        project.setStartDate(request.startDate());
        project.setEndDate(request.endDate());
        project.setOrganization(organization);

        return project;
    }
}

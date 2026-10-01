package com.example.backend.Mapper;

import com.example.backend.DTO.ProjectRequest;
import com.example.backend.DTO.ProjectResponse;
import com.example.backend.Entity.Project;
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
                project.getCreated_at(),
                project.getUpdated_at(),
                project.getOrganization().getId()
        );
    }

    public List<ProjectResponse> toResponseList(List<Project> projects){
        return projects.stream().map(this :: toResponse).toList();
    }

    public Project toEntity(ProjectRequest request){
        Project project = new Project();

        project.setName(request.name());
        project.setDescription(request.description());
        project.setStartDate(request.startDate());
        project.setEndDate(request.endDate());

        return project;
    }
}

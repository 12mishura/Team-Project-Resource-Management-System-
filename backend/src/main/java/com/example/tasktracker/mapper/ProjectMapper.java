package com.example.tasktracker.mapper;

import com.example.tasktracker.domain.Project;
import com.example.tasktracker.dto.request.CreateProjectRequest;
import com.example.tasktracker.dto.request.UpdateProjectRequest;
import com.example.tasktracker.dto.response.ProjectResponse;
import org.springframework.stereotype.Component;


@Component
public class ProjectMapper {
    public ProjectResponse toResponse(Project project){
        return new ProjectResponse(
                project.getId(),
                project.getOrganization().getId(),
                project.getTitle(),
                project.getDescription(),
                project.getStatus(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }

    public Project toEntity(CreateProjectRequest request){
        return new Project(request.title(), request.description());
    }

    public Project toUpdate(UpdateProjectRequest request, Project project){
        if(request.title() != null){
            project.setTitle(request.title());
        }
        if(request.description() != null){
            project.setDescription(request.description());
        }
        return project;
    }
}

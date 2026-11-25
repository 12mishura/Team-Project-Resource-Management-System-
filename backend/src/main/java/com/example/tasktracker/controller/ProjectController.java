package com.example.tasktracker.controller;

import com.example.tasktracker.domain.enums.ProjectStatus;
import com.example.tasktracker.dto.request.ChangeProjectStatusRequest;
import com.example.tasktracker.dto.request.CreateProjectRequest;
import com.example.tasktracker.dto.request.UpdateProjectRequest;
import com.example.tasktracker.dto.response.PageResponse;
import com.example.tasktracker.dto.response.ProjectResponse;
import com.example.tasktracker.service.ProjectService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/organizations/{organizationId}/projects")
@AllArgsConstructor
public class ProjectController {
    private final ProjectService projectService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse createProject(@PathVariable UUID organizationId, @Valid @RequestBody CreateProjectRequest request){
        return projectService.createProject(organizationId, request);
    }

    @GetMapping
    public PageResponse<ProjectResponse> getProjects(@PathVariable UUID organizationId,
                                                    @RequestParam(required = false) ProjectStatus status,
                                                    Pageable pageable){
        return projectService.getProjects(organizationId, status, pageable);
    }

    @GetMapping("/{projectId}")
    public ProjectResponse getProject(@PathVariable UUID organizationId, @PathVariable UUID projectId){
        return projectService.getProject(organizationId, projectId);
    }

    @PatchMapping("/{projectId}")
    public ProjectResponse updateProject(@PathVariable UUID organizationId,
                                         @PathVariable UUID projectId,
                                         @Valid @RequestBody UpdateProjectRequest request,
                                         @AuthenticationPrincipal UserDetails userDetails){
        return projectService.updateProject(organizationId, projectId, request, userDetails.getUsername());
    }

    @PatchMapping("/{projectId}/status")
    public ProjectResponse changeStatus(@PathVariable UUID organizationId,
                                        @PathVariable UUID projectId,
                                        @Valid @RequestBody ChangeProjectStatusRequest request,
                                        @AuthenticationPrincipal UserDetails userDetails){
        return projectService.changeStatus(organizationId, projectId, request, userDetails.getUsername());
    }

    @DeleteMapping("/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProject(@PathVariable UUID organizationId, @PathVariable UUID projectId){
        projectService.deleteProject(organizationId, projectId);
    }
}

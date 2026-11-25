package com.example.tasktracker.service;

import com.example.tasktracker.aop.OrganizationAccess;
import com.example.tasktracker.aop.OrganizationId;
import com.example.tasktracker.domain.Project;
import com.example.tasktracker.domain.enums.OrganizationRole;
import com.example.tasktracker.domain.enums.ProjectStatus;
import com.example.tasktracker.dto.request.ChangeProjectStatusRequest;
import com.example.tasktracker.dto.request.CreateProjectRequest;
import com.example.tasktracker.dto.request.UpdateProjectRequest;
import com.example.tasktracker.dto.response.PageResponse;
import com.example.tasktracker.dto.response.ProjectResponse;
import com.example.tasktracker.exception.DuplicateProjectTitleException;
import com.example.tasktracker.exception.ProjectNotFoundException;
import com.example.tasktracker.mapper.ProjectMapper;
import com.example.tasktracker.repository.OrganizationRepository;
import com.example.tasktracker.repository.ProjectRepository;
import com.example.tasktracker.security.ProjectPermissionChecker;
import com.example.tasktracker.util.PageRequests;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@AllArgsConstructor
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;
    private final OrganizationRepository organizationRepository;
    private final ProjectPermissionChecker permissionChecker;

    @OrganizationAccess(roles = {OrganizationRole.OWNER, OrganizationRole.ADMIN})
    @Transactional
    public ProjectResponse createProject(@OrganizationId UUID organizationId, CreateProjectRequest request){
        if(projectRepository.existsByOrganizationIdAndTitleIgnoreCase(organizationId, request.title())){
            throw new DuplicateProjectTitleException(request.title());
        }
        Project project = projectMapper.toEntity(request);
        project.setOrganization(organizationRepository.getReferenceById(organizationId));
        projectRepository.saveAndFlush(project);
        return projectMapper.toResponse(project);
    }

    @OrganizationAccess
    @Transactional(readOnly = true)
    public ProjectResponse getProject(@OrganizationId UUID organizationId, UUID projectId){
        return projectMapper.toResponse(findProject(organizationId, projectId));
    }

    @OrganizationAccess
    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> getProjects(@OrganizationId UUID organizationId, ProjectStatus status, Pageable pageable){
        Pageable sorted = PageRequests.byCreation(pageable);
        Page<Project> projects = status == null
                ? projectRepository.findAllByOrganizationId(organizationId, sorted)
                : projectRepository.findAllByOrganizationIdAndStatus(organizationId, status, sorted);
        return PageResponse.from(projects, projectMapper::toResponse);
    }

    @OrganizationAccess
    @Transactional
    public ProjectResponse updateProject(@OrganizationId UUID organizationId, UUID projectId, UpdateProjectRequest request, String email){
        Project project = findProject(organizationId, projectId);
        permissionChecker.requireProjectAdmin(organizationId, projectId, email);
        if(request.title() != null && !request.title().equalsIgnoreCase(project.getTitle())
                && projectRepository.existsByOrganizationIdAndTitleIgnoreCase(organizationId, request.title())){
            throw new DuplicateProjectTitleException(request.title());
        }
        projectMapper.toUpdate(request, project);
        projectRepository.flush();
        return projectMapper.toResponse(project);
    }

    @OrganizationAccess
    @Transactional
    public ProjectResponse changeStatus(@OrganizationId UUID organizationId, UUID projectId, ChangeProjectStatusRequest request,
                                        String email){
        Project project = findProject(organizationId, projectId);
        permissionChecker.requireProjectAdmin(organizationId, projectId, email);
        project.changeStatus(request.status());
        projectRepository.flush();
        return projectMapper.toResponse(project);
    }

    @OrganizationAccess(roles = OrganizationRole.OWNER)
    @Transactional
    public void deleteProject(@OrganizationId UUID organizationId, UUID projectId){
        projectRepository.delete(findProject(organizationId, projectId));
    }

    private Project findProject(UUID organizationId, UUID projectId){
        return projectRepository.findByIdAndOrganizationId(projectId, organizationId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
    }
}

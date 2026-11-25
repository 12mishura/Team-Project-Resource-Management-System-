package com.example.tasktracker.service;

import com.example.tasktracker.aop.OrganizationAccess;
import com.example.tasktracker.aop.OrganizationId;
import com.example.tasktracker.domain.OrganizationMember;
import com.example.tasktracker.domain.Project;
import com.example.tasktracker.domain.ProjectMember;
import com.example.tasktracker.dto.request.CreateProjectMemberRequest;
import com.example.tasktracker.dto.request.UpdateProjectMemberRequest;
import com.example.tasktracker.dto.response.PageResponse;
import com.example.tasktracker.dto.response.ProjectMemberResponse;
import com.example.tasktracker.exception.MemberNotFoundException;
import com.example.tasktracker.exception.ProjectMemberAlreadyExistsException;
import com.example.tasktracker.exception.ProjectMemberNotFoundException;
import com.example.tasktracker.exception.ProjectNotFoundException;
import com.example.tasktracker.mapper.ProjectMemberMapper;
import com.example.tasktracker.repository.OrganizationMemberRepository;
import com.example.tasktracker.repository.ProjectMemberRepository;
import com.example.tasktracker.repository.ProjectRepository;
import com.example.tasktracker.security.ProjectActor;
import com.example.tasktracker.security.ProjectPermissionChecker;
import com.example.tasktracker.util.PageRequests;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@AllArgsConstructor
public class ProjectMemberService {
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectMemberMapper projectMemberMapper;
    private final ProjectRepository projectRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final ProjectPermissionChecker permissionChecker;
    private final AssignmentReleaser assignmentReleaser;

    @OrganizationAccess
    @Transactional
    public ProjectMemberResponse addMember(@OrganizationId UUID organizationId, UUID projectId, CreateProjectMemberRequest request,
                                           String email){
        Project project = findProject(organizationId, projectId);
        permissionChecker.requireProjectAdmin(organizationId, projectId, email);
        OrganizationMember organizationMember = organizationMemberRepository
                .findByIdAndOrganizationId(request.organizationMemberId(), organizationId)
                .orElseThrow(() -> new MemberNotFoundException(request.organizationMemberId()));

        if(projectMemberRepository.existsByProjectIdAndOrganizationMemberId(projectId, organizationMember.getId())){
            throw new ProjectMemberAlreadyExistsException(projectId);
        }
        ProjectMember member = new ProjectMember(request.role());
        member.setProject(project);
        member.setOrganizationMember(organizationMember);
        projectMemberRepository.saveAndFlush(member);
        return projectMemberMapper.toResponse(member);
    }

    @OrganizationAccess
    @Transactional(readOnly = true)
    public PageResponse<ProjectMemberResponse> getMembers(@OrganizationId UUID organizationId, UUID projectId, Pageable pageable){
        findProject(organizationId, projectId);
        return PageResponse.from(
                projectMemberRepository.findAllByProjectId(projectId, PageRequests.byCreation(pageable)),
                projectMemberMapper::toResponse);
    }

    @OrganizationAccess
    @Transactional
    public ProjectMemberResponse updateRole(@OrganizationId UUID organizationId, UUID projectId, UUID projectMemberId,
                                            UpdateProjectMemberRequest request, String email){
        ProjectMember member = findMember(organizationId, projectId, projectMemberId);
        ProjectActor actor = permissionChecker.requireProjectAdmin(organizationId, projectId, email);
        permissionChecker.checkCanChangeMembership(actor, member);
        member.changeRole(request.role());
        projectMemberRepository.flush();
        return projectMemberMapper.toResponse(member);
    }

    @OrganizationAccess
    @Transactional
    public void removeMember(@OrganizationId UUID organizationId, UUID projectId, UUID projectMemberId, String email){
        ProjectMember member = findMember(organizationId, projectId, projectMemberId);
        ProjectActor actor = permissionChecker.requireProjectAdmin(organizationId, projectId, email);
        permissionChecker.checkCanChangeMembership(actor, member);
        assignmentReleaser.releaseProjectMember(member, actor.organizationMember().getUser());
        projectMemberRepository.delete(member);
    }

    private Project findProject(UUID organizationId, UUID projectId){
        return projectRepository.findByIdAndOrganizationId(projectId, organizationId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
    }

    private ProjectMember findMember(UUID organizationId, UUID projectId, UUID projectMemberId){
        findProject(organizationId, projectId);
        return projectMemberRepository.findByIdAndProjectId(projectMemberId, projectId)
                .orElseThrow(() -> new ProjectMemberNotFoundException(projectMemberId));
    }
}

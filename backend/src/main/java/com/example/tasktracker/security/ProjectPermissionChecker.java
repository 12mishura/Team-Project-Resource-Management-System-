package com.example.tasktracker.security;

import com.example.tasktracker.domain.Issue;
import com.example.tasktracker.domain.IssueComment;
import com.example.tasktracker.domain.OrganizationMember;
import com.example.tasktracker.domain.ProjectMember;
import com.example.tasktracker.domain.enums.IssueType;
import com.example.tasktracker.domain.enums.OrganizationRole;
import com.example.tasktracker.domain.enums.ProjectMemberRole;
import com.example.tasktracker.exception.AccessDeniedToOrganizationException;
import com.example.tasktracker.exception.AccessDeniedToProjectException;
import com.example.tasktracker.exception.IllegalIssueOperationException;
import com.example.tasktracker.repository.OrganizationMemberRepository;
import com.example.tasktracker.repository.ProjectMemberRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@AllArgsConstructor
public class ProjectPermissionChecker {
    private final OrganizationMemberRepository organizationMemberRepository;
    private final ProjectMemberRepository projectMemberRepository;

    public ProjectActor requireProjectActor(UUID organizationId, UUID projectId, String email){
        OrganizationMember organizationMember = organizationMemberRepository.findByOrganizationIdAndUserEmail(organizationId, email)
                .orElseThrow(() -> new AccessDeniedToOrganizationException(organizationId));
        ProjectMember projectMember = projectMemberRepository
                .findByProjectIdAndOrganizationMemberId(projectId, organizationMember.getId())
                .orElse(null);

        if(organizationMember.getRole() == OrganizationRole.OWNER || organizationMember.getRole() == OrganizationRole.ADMIN){
            return new ProjectActor(organizationMember, projectMember, ProjectMemberRole.ADMIN);
        }
        if(projectMember == null){
            throw new AccessDeniedToProjectException(projectId);
        }
        return new ProjectActor(organizationMember, projectMember, projectMember.getRole());
    }

    public void checkCanCreate(ProjectActor actor, IssueType type){
        if(type == IssueType.EPIC && !actor.isAtLeast(ProjectMemberRole.EXTENDED)){
            throw new IllegalIssueOperationException("Only project ADMIN or EXTENDED can create epics");
        }
    }

    public void checkCanEdit(ProjectActor actor, Issue issue){
        if(!actor.isAtLeast(ProjectMemberRole.EXTENDED) && !actor.isReporterOf(issue)){
            throw new IllegalIssueOperationException("Only the reporter or project ADMIN or EXTENDED can edit this issue");
        }
    }

    public void checkCanChangeStatus(ProjectActor actor, Issue issue){
        if(!actor.isAtLeast(ProjectMemberRole.EXTENDED) && !actor.isReporterOf(issue) && !actor.isAssigneeOf(issue)){
            throw new IllegalIssueOperationException("Only the reporter, the assignee or project ADMIN or EXTENDED can change status");
        }
    }

    public void checkCanAssign(ProjectActor actor, Issue issue, ProjectMember newAssignee){
        if(actor.isAtLeast(ProjectMemberRole.EXTENDED)){
            return;
        }
        boolean takesFreeOrOwnIssue = actor.is(newAssignee) && (issue.getAssignee() == null || actor.isAssigneeOf(issue));
        boolean releasesOwnIssue = newAssignee == null && actor.isAssigneeOf(issue);
        if(!takesFreeOrOwnIssue && !releasesOwnIssue){
            throw new IllegalIssueOperationException("Project USER can only take a free issue or release their own");
        }
    }

    public void checkCanChangeParent(ProjectActor actor){
        if(!actor.isAtLeast(ProjectMemberRole.EXTENDED)){
            throw new IllegalIssueOperationException("Only project ADMIN or EXTENDED can change the epic of an issue");
        }
    }

    public void checkCanEditComment(ProjectActor actor, IssueComment comment){
        if(!comment.getAuthor().getId().equals(actor.userId())){
            throw new IllegalIssueOperationException("Only the author can edit this comment");
        }
    }

    public void checkCanDeleteComment(ProjectActor actor, IssueComment comment){
        if(!actor.isAtLeast(ProjectMemberRole.ADMIN) && !comment.getAuthor().getId().equals(actor.userId())){
            throw new IllegalIssueOperationException("Only the author or project ADMIN can delete this comment");
        }
    }

    public void checkCanDelete(ProjectActor actor, Issue issue){
        if(!actor.isAtLeast(ProjectMemberRole.ADMIN) && !actor.isReporterOf(issue)){
            throw new IllegalIssueOperationException("Only the reporter or project ADMIN can delete this issue");
        }
    }
}

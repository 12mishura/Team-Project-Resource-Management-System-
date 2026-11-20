package com.example.tasktracker.service;

import com.example.tasktracker.aop.OrganizationAccess;
import com.example.tasktracker.aop.OrganizationId;
import com.example.tasktracker.domain.Issue;
import com.example.tasktracker.domain.IssueComment;
import com.example.tasktracker.domain.Project;
import com.example.tasktracker.domain.enums.ProjectStatus;
import com.example.tasktracker.dto.request.CreateCommentRequest;
import com.example.tasktracker.dto.request.UpdateCommentRequest;
import com.example.tasktracker.dto.response.CommentResponse;
import com.example.tasktracker.dto.response.PageResponse;
import com.example.tasktracker.exception.CommentNotFoundException;
import com.example.tasktracker.exception.IssueNotFoundException;
import com.example.tasktracker.exception.ProjectArchivedException;
import com.example.tasktracker.exception.ProjectNotFoundException;
import com.example.tasktracker.mapper.CommentMapper;
import com.example.tasktracker.repository.IssueCommentRepository;
import com.example.tasktracker.repository.IssueRepository;
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
public class IssueCommentService {
    private final IssueCommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final IssueRepository issueRepository;
    private final ProjectRepository projectRepository;
    private final ProjectPermissionChecker permissionChecker;

    @OrganizationAccess
    @Transactional
    public CommentResponse addComment(@OrganizationId UUID organizationId, UUID projectId, UUID issueId,
                                      CreateCommentRequest request, String email){
        Project project = findProject(organizationId, projectId);
        ProjectActor actor = permissionChecker.requireProjectActor(organizationId, projectId, email);
        requireNotArchived(project);
        Issue issue = findIssue(projectId, issueId);

        IssueComment comment = new IssueComment(request.text());
        comment.setIssue(issue);
        comment.setAuthor(actor.organizationMember().getUser());
        commentRepository.saveAndFlush(comment);
        return commentMapper.toResponse(comment);
    }

    @OrganizationAccess
    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> getComments(@OrganizationId UUID organizationId, UUID projectId, UUID issueId,
                                                     Pageable pageable, String email){
        findProject(organizationId, projectId);
        permissionChecker.requireProjectActor(organizationId, projectId, email);
        findIssue(projectId, issueId);
        return PageResponse.from(
                commentRepository.findAllByIssueId(issueId, PageRequests.byCreation(pageable)),
                commentMapper::toResponse);
    }

    @OrganizationAccess
    @Transactional
    public CommentResponse editComment(@OrganizationId UUID organizationId, UUID projectId, UUID issueId, UUID commentId,
                                       UpdateCommentRequest request, String email){
        Project project = findProject(organizationId, projectId);
        ProjectActor actor = permissionChecker.requireProjectActor(organizationId, projectId, email);
        requireNotArchived(project);
        IssueComment comment = findComment(projectId, issueId, commentId);
        permissionChecker.checkCanEditComment(actor, comment);

        comment.edit(request.text());
        commentRepository.flush();
        return commentMapper.toResponse(comment);
    }

    @OrganizationAccess
    @Transactional
    public void deleteComment(@OrganizationId UUID organizationId, UUID projectId, UUID issueId, UUID commentId, String email){
        Project project = findProject(organizationId, projectId);
        ProjectActor actor = permissionChecker.requireProjectActor(organizationId, projectId, email);
        requireNotArchived(project);
        IssueComment comment = findComment(projectId, issueId, commentId);
        permissionChecker.checkCanDeleteComment(actor, comment);

        commentRepository.delete(comment);
    }

    private Project findProject(UUID organizationId, UUID projectId){
        return projectRepository.findByIdAndOrganizationId(projectId, organizationId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
    }

    private Issue findIssue(UUID projectId, UUID issueId){
        return issueRepository.findByIdAndProjectId(issueId, projectId)
                .orElseThrow(() -> new IssueNotFoundException(issueId));
    }

    private IssueComment findComment(UUID projectId, UUID issueId, UUID commentId){
        findIssue(projectId, issueId);
        return commentRepository.findByIdAndIssueId(commentId, issueId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));
    }

    private void requireNotArchived(Project project){
        if(project.getStatus() == ProjectStatus.ARCHIVED){
            throw new ProjectArchivedException(project.getId());
        }
    }
}

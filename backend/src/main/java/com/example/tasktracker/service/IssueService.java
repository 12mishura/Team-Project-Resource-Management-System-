package com.example.tasktracker.service;

import com.example.tasktracker.aop.OrganizationAccess;
import com.example.tasktracker.aop.OrganizationId;
import com.example.tasktracker.domain.Issue;
import com.example.tasktracker.domain.Project;
import com.example.tasktracker.domain.ProjectMember;
import com.example.tasktracker.domain.enums.BugSeverity;
import com.example.tasktracker.domain.enums.IssueEventType;
import com.example.tasktracker.domain.enums.IssueStatus;
import com.example.tasktracker.domain.enums.IssueType;
import com.example.tasktracker.domain.enums.ProjectStatus;
import com.example.tasktracker.dto.request.AssignIssueRequest;
import com.example.tasktracker.dto.request.ChangeIssueParentRequest;
import com.example.tasktracker.dto.request.ChangeIssueStatusRequest;
import com.example.tasktracker.dto.request.CreateIssueRequest;
import com.example.tasktracker.dto.request.IssueFilter;
import com.example.tasktracker.dto.request.UpdateIssueRequest;
import com.example.tasktracker.dto.response.IssueEventResponse;
import com.example.tasktracker.dto.response.IssueResponse;
import com.example.tasktracker.dto.response.PageResponse;
import com.example.tasktracker.dto.response.ProjectActivityResponse;
import com.example.tasktracker.exception.IllegalIssueOperationException;
import com.example.tasktracker.exception.InvalidIssueException;
import com.example.tasktracker.exception.IssueNotFoundException;
import com.example.tasktracker.exception.ProjectArchivedException;
import com.example.tasktracker.exception.ProjectMemberNotFoundException;
import com.example.tasktracker.exception.ProjectNotFoundException;
import com.example.tasktracker.mapper.IssueEventMapper;
import com.example.tasktracker.mapper.IssueMapper;
import com.example.tasktracker.repository.IssueEventRepository;
import com.example.tasktracker.repository.IssueRepository;
import com.example.tasktracker.repository.ProjectMemberRepository;
import com.example.tasktracker.repository.ProjectRepository;
import com.example.tasktracker.security.ProjectActor;
import com.example.tasktracker.security.ProjectPermissionChecker;
import com.example.tasktracker.util.PageRequests;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static com.example.tasktracker.repository.IssueSpecifications.assignedTo;
import static com.example.tasktracker.repository.IssueSpecifications.assignedToUser;
import static com.example.tasktracker.repository.IssueSpecifications.hasStatus;
import static com.example.tasktracker.repository.IssueSpecifications.hasType;
import static com.example.tasktracker.repository.IssueSpecifications.inProject;
import static com.example.tasktracker.repository.IssueSpecifications.orderedBy;
import static com.example.tasktracker.repository.IssueSpecifications.titleContains;
import static com.example.tasktracker.repository.IssueSpecifications.withParent;

@Service
@AllArgsConstructor
public class IssueService {
    private final IssueRepository issueRepository;
    private final IssueMapper issueMapper;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectPermissionChecker permissionChecker;
    private final IssueHistoryRecorder historyRecorder;
    private final IssueEventRepository eventRepository;
    private final IssueEventMapper eventMapper;

    @OrganizationAccess
    @Transactional
    public IssueResponse createIssue(@OrganizationId UUID organizationId, UUID projectId, CreateIssueRequest request, String email){
        Project project = findProject(organizationId, projectId);
        ProjectActor actor = permissionChecker.requireProjectActor(organizationId, projectId, email);
        requireNotArchived(project);
        permissionChecker.checkCanCreate(actor, request.type());
        if(request.parentId() != null){
            permissionChecker.checkCanChangeParent(actor);
        }
        validateTypeSpecificFields(request.type(), request.severity(), request.stepsToReproduce(), request.storyPoints(), request.estimateMinutes());
        if(request.type() == IssueType.BUG && request.severity() == null){
            throw new InvalidIssueException("severity is required for BUG");
        }

        Issue issue = issueMapper.toEntity(request);
        issue.setProject(project);
        issue.setReporter(actor.organizationMember().getUser());
        if(request.parentId() != null){
            issue.moveTo(findIssue(projectId, request.parentId()));
        }
        if(request.assigneeId() != null){
            ProjectMember assignee = projectMemberRepository.findByIdAndProjectId(request.assigneeId(), projectId)
                    .orElseThrow(() -> new ProjectMemberNotFoundException(request.assigneeId()));
            permissionChecker.checkCanAssign(actor, issue, assignee);
            issue.setAssignee(assignee);
        }

        issueRepository.saveAndFlush(issue);
        historyRecorder.recordCreated(issue, actor.organizationMember().getUser());
        historyRecorder.recordChange(issue, actor.organizationMember().getUser(), IssueEventType.ASSIGNEE_CHANGED,
                "assignee", null, assigneeName(issue));
        return issueMapper.toResponse(issue);
    }

    @OrganizationAccess
    @Transactional(readOnly = true)
    public PageResponse<IssueEventResponse> getHistory(@OrganizationId UUID organizationId, UUID projectId, UUID issueId,
                                                       Pageable pageable, String email){
        findProject(organizationId, projectId);
        permissionChecker.requireProjectActor(organizationId, projectId, email);
        findIssue(projectId, issueId);
        return PageResponse.from(
                eventRepository.findAllByIssueId(issueId, PageRequests.byCreation(pageable)),
                eventMapper::toResponse);
    }

    @OrganizationAccess
    @Transactional(readOnly = true)
    public PageResponse<ProjectActivityResponse> getProjectActivity(@OrganizationId UUID organizationId, UUID projectId,
                                                                    Pageable pageable, String email){
        findProject(organizationId, projectId);
        permissionChecker.requireProjectActor(organizationId, projectId, email);
        return PageResponse.from(
                eventRepository.findAllByIssueProjectId(projectId, PageRequests.newestFirst(pageable)),
                eventMapper::toActivityResponse);
    }

    @OrganizationAccess
    @Transactional(readOnly = true)
    public PageResponse<IssueResponse> getIssues(@OrganizationId UUID organizationId, UUID projectId, IssueFilter filter,
                                                 Pageable pageable, String email){
        findProject(organizationId, projectId);
        permissionChecker.requireProjectActor(organizationId, projectId, email);
        return search(inProject(projectId), filter, pageable);
    }

    @Transactional(readOnly = true)
    public PageResponse<IssueResponse> getMyIssues(String email, IssueFilter filter, Pageable pageable){
        return search(assignedToUser(email), filter, pageable);
    }

    private PageResponse<IssueResponse> search(Specification<Issue> scope, IssueFilter filter, Pageable pageable){
        Specification<Issue> specification = scope;
        if(filter.type() != null){
            specification = specification.and(hasType(filter.type()));
        }
        if(filter.status() != null){
            specification = specification.and(hasStatus(filter.status()));
        }
        if(filter.assigneeId() != null){
            specification = specification.and(assignedTo(filter.assigneeId()));
        }
        if(filter.parentId() != null){
            specification = specification.and(withParent(filter.parentId()));
        }
        if(filter.q() != null){
            specification = specification.and(titleContains(filter.q()));
        }
        specification = specification.and(orderedBy(filter.sortBy(), filter.direction()));
        return PageResponse.from(
                issueRepository.findAll(specification, PageRequest.of(pageable.getPageNumber(), pageable.getPageSize())),
                issueMapper::toResponse);
    }

    @OrganizationAccess
    @Transactional(readOnly = true)
    public IssueResponse getIssue(@OrganizationId UUID organizationId, UUID projectId, UUID issueId, String email){
        findProject(organizationId, projectId);
        permissionChecker.requireProjectActor(organizationId, projectId, email);
        return issueMapper.toResponse(findIssue(projectId, issueId));
    }

    @OrganizationAccess
    @Transactional
    public IssueResponse updateIssue(@OrganizationId UUID organizationId, UUID projectId, UUID issueId,
                                     UpdateIssueRequest request, String email){
        Project project = findProject(organizationId, projectId);
        ProjectActor actor = permissionChecker.requireProjectActor(organizationId, projectId, email);
        requireNotArchived(project);
        Issue issue = findIssue(projectId, issueId);
        permissionChecker.checkCanEdit(actor, issue);
        validateTypeSpecificFields(issue.getType(), request.severity(), request.stepsToReproduce(), request.storyPoints(), request.estimateMinutes());

        Map<String, String> before = historyRecorder.snapshot(issue);
        issueMapper.toUpdate(request, issue);
        issueRepository.flush();
        historyRecorder.recordFieldChanges(issue, actor.organizationMember().getUser(), before, historyRecorder.snapshot(issue));
        return issueMapper.toResponse(issue);
    }

    @OrganizationAccess
    @Transactional
    public IssueResponse changeStatus(@OrganizationId UUID organizationId, UUID projectId, UUID issueId,
                                      ChangeIssueStatusRequest request, String email){
        Project project = findProject(organizationId, projectId);
        ProjectActor actor = permissionChecker.requireProjectActor(organizationId, projectId, email);
        requireNotArchived(project);
        Issue issue = findIssue(projectId, issueId);
        permissionChecker.checkCanChangeStatus(actor, issue);

        IssueStatus oldStatus = issue.getStatus();
        issue.changeStatus(request.status());
        issueRepository.flush();
        historyRecorder.recordChange(issue, actor.organizationMember().getUser(), IssueEventType.STATUS_CHANGED,
                "status", oldStatus, issue.getStatus());
        return issueMapper.toResponse(issue);
    }

    @OrganizationAccess
    @Transactional
    public IssueResponse assignIssue(@OrganizationId UUID organizationId, UUID projectId, UUID issueId,
                                     AssignIssueRequest request, String email){
        Project project = findProject(organizationId, projectId);
        ProjectActor actor = permissionChecker.requireProjectActor(organizationId, projectId, email);
        requireNotArchived(project);
        Issue issue = findIssue(projectId, issueId);
        ProjectMember newAssignee = projectMemberRepository.findByIdAndProjectId(request.projectMemberId(), projectId)
                .orElseThrow(() -> new ProjectMemberNotFoundException(request.projectMemberId()));
        permissionChecker.checkCanAssign(actor, issue, newAssignee);

        String oldAssignee = assigneeName(issue);
        issue.setAssignee(newAssignee);
        issueRepository.flush();
        historyRecorder.recordChange(issue, actor.organizationMember().getUser(), IssueEventType.ASSIGNEE_CHANGED,
                "assignee", oldAssignee, assigneeName(issue));
        return issueMapper.toResponse(issue);
    }

    @OrganizationAccess
    @Transactional
    public IssueResponse assignToMe(@OrganizationId UUID organizationId, UUID projectId, UUID issueId, String email){
        Project project = findProject(organizationId, projectId);
        ProjectActor actor = permissionChecker.requireProjectActor(organizationId, projectId, email);
        requireNotArchived(project);
        Issue issue = findIssue(projectId, issueId);

        String oldAssignee = assigneeName(issue);
        issue.setAssignee(selfAsAssignee(actor, issue));
        issueRepository.flush();
        historyRecorder.recordChange(issue, actor.organizationMember().getUser(), IssueEventType.ASSIGNEE_CHANGED,
                "assignee", oldAssignee, assigneeName(issue));
        return issueMapper.toResponse(issue);
    }

    @OrganizationAccess
    @Transactional
    public IssueResponse unassignIssue(@OrganizationId UUID organizationId, UUID projectId, UUID issueId, String email){
        Project project = findProject(organizationId, projectId);
        ProjectActor actor = permissionChecker.requireProjectActor(organizationId, projectId, email);
        requireNotArchived(project);
        Issue issue = findIssue(projectId, issueId);
        permissionChecker.checkCanAssign(actor, issue, null);

        String oldAssignee = assigneeName(issue);
        issue.setAssignee(null);
        issueRepository.flush();
        historyRecorder.recordChange(issue, actor.organizationMember().getUser(), IssueEventType.ASSIGNEE_CHANGED,
                "assignee", oldAssignee, null);
        return issueMapper.toResponse(issue);
    }

    @OrganizationAccess
    @Transactional
    public IssueResponse changeParent(@OrganizationId UUID organizationId, UUID projectId, UUID issueId,
                                      ChangeIssueParentRequest request, String email){
        Project project = findProject(organizationId, projectId);
        ProjectActor actor = permissionChecker.requireProjectActor(organizationId, projectId, email);
        requireNotArchived(project);
        Issue issue = findIssue(projectId, issueId);
        permissionChecker.checkCanChangeParent(actor);

        UUID oldParent = parentId(issue);
        issue.moveTo(findIssue(projectId, request.parentId()));
        issueRepository.flush();
        historyRecorder.recordChange(issue, actor.organizationMember().getUser(), IssueEventType.PARENT_CHANGED,
                "parent", oldParent, parentId(issue));
        return issueMapper.toResponse(issue);
    }

    @OrganizationAccess
    @Transactional
    public IssueResponse detachFromEpic(@OrganizationId UUID organizationId, UUID projectId, UUID issueId, String email){
        Project project = findProject(organizationId, projectId);
        ProjectActor actor = permissionChecker.requireProjectActor(organizationId, projectId, email);
        requireNotArchived(project);
        Issue issue = findIssue(projectId, issueId);
        permissionChecker.checkCanChangeParent(actor);

        UUID oldParent = parentId(issue);
        issue.moveTo(null);
        issueRepository.flush();
        historyRecorder.recordChange(issue, actor.organizationMember().getUser(), IssueEventType.PARENT_CHANGED,
                "parent", oldParent, null);
        return issueMapper.toResponse(issue);
    }

    @OrganizationAccess
    @Transactional
    public void deleteIssue(@OrganizationId UUID organizationId, UUID projectId, UUID issueId, String email){
        Project project = findProject(organizationId, projectId);
        ProjectActor actor = permissionChecker.requireProjectActor(organizationId, projectId, email);
        requireNotArchived(project);
        Issue issue = findIssue(projectId, issueId);
        permissionChecker.checkCanDelete(actor, issue);

        if(issue.getType() == IssueType.EPIC){
            for(Issue child : issueRepository.findAllByParentId(issue.getId())){
                child.moveTo(null);
                historyRecorder.recordChange(child, actor.organizationMember().getUser(), IssueEventType.PARENT_CHANGED,
                        "parent", issue.getId(), null);
            }
        }
        issueRepository.delete(issue);
    }

    private ProjectMember selfAsAssignee(ProjectActor actor, Issue issue){
        if(actor.projectMember() == null){
            throw new IllegalIssueOperationException("Add yourself to the project before assigning issues to yourself");
        }
        permissionChecker.checkCanAssign(actor, issue, actor.projectMember());
        return actor.projectMember();
    }

    private static String assigneeName(Issue issue){
        return issue.getAssignee() == null ? null : issue.getAssignee().getOrganizationMember().getUser().getUsername();
    }

    private static UUID parentId(Issue issue){
        return issue.getParent() == null ? null : issue.getParent().getId();
    }

    private Project findProject(UUID organizationId, UUID projectId){
        return projectRepository.findByIdAndOrganizationId(projectId, organizationId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
    }

    private Issue findIssue(UUID projectId, UUID issueId){
        return issueRepository.findByIdAndProjectId(issueId, projectId)
                .orElseThrow(() -> new IssueNotFoundException(issueId));
    }

    private void requireNotArchived(Project project){
        if(project.getStatus() == ProjectStatus.ARCHIVED){
            throw new ProjectArchivedException(project.getId());
        }
    }

    private void validateTypeSpecificFields(IssueType type, BugSeverity severity, String stepsToReproduce, Integer storyPoints,
                                            Integer estimateMinutes){
        if(type == IssueType.EPIC && estimateMinutes != null){
            throw new InvalidIssueException("EPIC estimate is calculated from its issues");
        }
        if(type != IssueType.BUG && (severity != null || stepsToReproduce != null)){
            throw new InvalidIssueException("severity and stepsToReproduce are only allowed for BUG");
        }
        if(type != IssueType.STORY && storyPoints != null){
            throw new InvalidIssueException("storyPoints is only allowed for STORY");
        }
    }
}

package com.example.tasktracker.service;

import com.example.tasktracker.aop.OrganizationAccess;
import com.example.tasktracker.aop.OrganizationId;
import com.example.tasktracker.domain.Issue;
import com.example.tasktracker.domain.Project;
import com.example.tasktracker.domain.WorkLog;
import com.example.tasktracker.domain.enums.IssueType;
import com.example.tasktracker.domain.enums.ProjectStatus;
import com.example.tasktracker.dto.request.CreateWorkLogRequest;
import com.example.tasktracker.dto.request.UpdateWorkLogRequest;
import com.example.tasktracker.dto.response.PageResponse;
import com.example.tasktracker.dto.response.TimesheetEntryResponse;
import com.example.tasktracker.dto.response.WorkLogResponse;
import com.example.tasktracker.exception.InvalidWorkLogException;
import com.example.tasktracker.exception.IssueNotFoundException;
import com.example.tasktracker.exception.ProjectArchivedException;
import com.example.tasktracker.exception.ProjectNotFoundException;
import com.example.tasktracker.exception.WorkLogNotFoundException;
import com.example.tasktracker.mapper.WorkLogMapper;
import com.example.tasktracker.repository.IssueRepository;
import com.example.tasktracker.repository.ProjectRepository;
import com.example.tasktracker.repository.WorkLogRepository;
import com.example.tasktracker.security.ProjectActor;
import com.example.tasktracker.security.ProjectPermissionChecker;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class WorkLogService {
    public static final int MAX_PERIOD_DAYS = 92;
    private static final int MINUTES_PER_DAY = 1440;
    private static final UUID NO_WORK_LOG = new UUID(0, 0);
    private static final Sort NEWEST_WORK_FIRST = Sort.by(Sort.Direction.DESC, "workDate", "createdAt", "id");

    private final WorkLogRepository workLogRepository;
    private final WorkLogMapper workLogMapper;
    private final IssueRepository issueRepository;
    private final ProjectRepository projectRepository;
    private final ProjectPermissionChecker permissionChecker;
    private final IssueHistoryRecorder historyRecorder;

    @OrganizationAccess
    @Transactional
    public WorkLogResponse addWorkLog(@OrganizationId UUID organizationId, UUID projectId, UUID issueId,
                                      CreateWorkLogRequest request, String email){
        Project project = findProject(organizationId, projectId);
        ProjectActor actor = permissionChecker.requireProjectActor(organizationId, projectId, email);
        requireNotArchived(project);
        permissionChecker.checkCanLogWork(actor);
        Issue issue = findIssue(projectId, issueId);
        if(issue.getType() == IssueType.EPIC){
            throw new InvalidWorkLogException("Log time on the issues of an epic, not on the epic itself");
        }
        requireNotInFuture(request.workDate());
        requireDayLimit(actor.userId(), request.workDate(), request.minutes(), NO_WORK_LOG);

        WorkLog workLog = new WorkLog(request.workDate(), request.minutes(), request.comment());
        workLog.setIssue(issue);
        workLog.setAuthor(actor.organizationMember().getUser());
        workLogRepository.saveAndFlush(workLog);
        historyRecorder.recordWorkLogged(issue, actor.organizationMember().getUser(), request.minutes());
        return workLogMapper.toResponse(workLog);
    }

    @OrganizationAccess
    @Transactional(readOnly = true)
    public PageResponse<WorkLogResponse> getWorkLogs(@OrganizationId UUID organizationId, UUID projectId, UUID issueId,
                                                     Pageable pageable, String email){
        findProject(organizationId, projectId);
        permissionChecker.requireProjectActor(organizationId, projectId, email);
        findIssue(projectId, issueId);
        Pageable page = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), NEWEST_WORK_FIRST);
        return PageResponse.from(workLogRepository.findAllByIssueId(issueId, page), workLogMapper::toResponse);
    }

    @OrganizationAccess
    @Transactional
    public WorkLogResponse updateWorkLog(@OrganizationId UUID organizationId, UUID projectId, UUID issueId, UUID workLogId,
                                         UpdateWorkLogRequest request, String email){
        Project project = findProject(organizationId, projectId);
        ProjectActor actor = permissionChecker.requireProjectActor(organizationId, projectId, email);
        requireNotArchived(project);
        WorkLog workLog = findWorkLog(projectId, issueId, workLogId);
        permissionChecker.checkCanChangeWorkLog(actor, workLog);

        LocalDate workDate = request.workDate() == null ? workLog.getWorkDate() : request.workDate();
        int minutes = request.minutes() == null ? workLog.getMinutes() : request.minutes();
        requireNotInFuture(workDate);
        requireDayLimit(workLog.getAuthor().getId(), workDate, minutes, workLog.getId());

        workLog.setWorkDate(workDate);
        workLog.setMinutes(minutes);
        if(request.comment() != null){
            workLog.setComment(request.comment().isEmpty() ? null : request.comment());
        }
        workLogRepository.flush();
        return workLogMapper.toResponse(workLog);
    }

    @OrganizationAccess
    @Transactional
    public void deleteWorkLog(@OrganizationId UUID organizationId, UUID projectId, UUID issueId, UUID workLogId, String email){
        Project project = findProject(organizationId, projectId);
        ProjectActor actor = permissionChecker.requireProjectActor(organizationId, projectId, email);
        requireNotArchived(project);
        WorkLog workLog = findWorkLog(projectId, issueId, workLogId);
        permissionChecker.checkCanChangeWorkLog(actor, workLog);

        workLogRepository.delete(workLog);
    }

    @Transactional(readOnly = true)
    public List<TimesheetEntryResponse> getTimesheet(String email, LocalDate from, LocalDate to){
        requireValidPeriod(from, to);
        return workLogRepository.findTimesheet(email, from, to).stream()
                .map(workLogMapper::toTimesheetEntry)
                .toList();
    }

    public static void requireValidPeriod(LocalDate from, LocalDate to){
        if(from.isAfter(to)){
            throw new InvalidWorkLogException("from must not be after to");
        }
        if(ChronoUnit.DAYS.between(from, to) >= MAX_PERIOD_DAYS){
            throw new InvalidWorkLogException("Period must not be longer than " + MAX_PERIOD_DAYS + " days");
        }
    }

    private void requireNotInFuture(LocalDate workDate){
        if(workDate.isAfter(LocalDate.now(ZoneOffset.UTC).plusDays(1))){
            throw new InvalidWorkLogException("Work date cannot be in the future");
        }
    }

    private void requireDayLimit(UUID authorId, LocalDate workDate, int minutes, UUID excludedId){
        long alreadyLogged = workLogRepository.sumForDay(authorId, workDate, excludedId);
        if(alreadyLogged + minutes > MINUTES_PER_DAY){
            throw new InvalidWorkLogException("Cannot log more than 24 hours for one day");
        }
    }

    private Project findProject(UUID organizationId, UUID projectId){
        return projectRepository.findByIdAndOrganizationId(projectId, organizationId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
    }

    private Issue findIssue(UUID projectId, UUID issueId){
        return issueRepository.findByIdAndProjectId(issueId, projectId)
                .orElseThrow(() -> new IssueNotFoundException(issueId));
    }

    private WorkLog findWorkLog(UUID projectId, UUID issueId, UUID workLogId){
        findIssue(projectId, issueId);
        return workLogRepository.findByIdAndIssueId(workLogId, issueId)
                .orElseThrow(() -> new WorkLogNotFoundException(workLogId));
    }

    private void requireNotArchived(Project project){
        if(project.getStatus() == ProjectStatus.ARCHIVED){
            throw new ProjectArchivedException(project.getId());
        }
    }
}

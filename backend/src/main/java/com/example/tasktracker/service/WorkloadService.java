package com.example.tasktracker.service;

import com.example.tasktracker.aop.OrganizationAccess;
import com.example.tasktracker.aop.OrganizationId;
import com.example.tasktracker.domain.Issue;
import com.example.tasktracker.domain.OrganizationMember;
import com.example.tasktracker.domain.Project;
import com.example.tasktracker.domain.enums.IssueStatus;
import com.example.tasktracker.domain.enums.IssueType;
import com.example.tasktracker.domain.enums.OrganizationRole;
import com.example.tasktracker.domain.enums.ProjectStatus;
import com.example.tasktracker.dto.request.UpdateCapacityRequest;
import com.example.tasktracker.dto.response.DailyMinutesResponse;
import com.example.tasktracker.dto.response.MemberWorkloadResponse;
import com.example.tasktracker.dto.response.OrganizationMemberResponse;
import com.example.tasktracker.dto.response.ProjectMinutesResponse;
import com.example.tasktracker.dto.response.WorkloadResponse;
import com.example.tasktracker.exception.MemberNotFoundException;
import com.example.tasktracker.exception.ProjectNotFoundException;
import com.example.tasktracker.mapper.OrganizationMemberMapper;
import com.example.tasktracker.repository.IssueRepository;
import com.example.tasktracker.repository.OrganizationMemberRepository;
import com.example.tasktracker.repository.ProjectMemberRepository;
import com.example.tasktracker.repository.ProjectRepository;
import com.example.tasktracker.repository.WorkLogRepository;
import com.example.tasktracker.repository.projection.WorkLogTotal;
import com.example.tasktracker.security.ProjectPermissionChecker;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class WorkloadService {
    private static final int WORKING_DAYS_PER_WEEK = 5;

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final OrganizationMemberMapper organizationMemberMapper;
    private final IssueRepository issueRepository;
    private final WorkLogRepository workLogRepository;
    private final ProjectPermissionChecker permissionChecker;

    @OrganizationAccess
    @Transactional(readOnly = true)
    public WorkloadResponse getProjectWorkload(@OrganizationId UUID organizationId, UUID projectId,
                                               LocalDate from, LocalDate to, String email){
        Project project = projectRepository.findByIdAndOrganizationId(projectId, organizationId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
        permissionChecker.requireProjectAdmin(organizationId, projectId, email);
        WorkLogService.requireValidPeriod(from, to);

        List<Person> people = projectMemberRepository.findAllWithUsersByProjectId(projectId).stream()
                .map(member -> Person.of(member.getOrganizationMember(), member.getRole().name()))
                .toList();
        return build(from, to, people,
                workLogRepository.totalsByProject(projectId, from, to),
                issueRepository.findOpenAssignedByProject(projectId, IssueStatus.DONE),
                Map.of(project.getId(), project.getTitle()));
    }

    @OrganizationAccess(roles = {OrganizationRole.OWNER, OrganizationRole.ADMIN})
    @Transactional(readOnly = true)
    public WorkloadResponse getOrganizationWorkload(@OrganizationId UUID organizationId, LocalDate from, LocalDate to){
        WorkLogService.requireValidPeriod(from, to);

        List<Person> people = organizationMemberRepository.findAllWithUsersByOrganizationId(organizationId).stream()
                .map(member -> Person.of(member, member.getRole().name()))
                .toList();
        Map<UUID, String> projectTitles = projectRepository.findAllByOrganizationId(organizationId, Sort.by("createdAt", "id")).stream()
                .collect(Collectors.toMap(Project::getId, Project::getTitle));
        return build(from, to, people,
                workLogRepository.totalsByOrganization(organizationId, from, to),
                issueRepository.findOpenAssignedByOrganization(organizationId, IssueStatus.DONE, ProjectStatus.ARCHIVED),
                projectTitles);
    }

    @OrganizationAccess(roles = {OrganizationRole.OWNER, OrganizationRole.ADMIN})
    @Transactional
    public OrganizationMemberResponse updateCapacity(@OrganizationId UUID organizationId, UUID memberId, UpdateCapacityRequest request){
        OrganizationMember member = organizationMemberRepository.findByIdAndOrganizationId(memberId, organizationId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));
        member.setWeeklyCapacityMinutes(request.weeklyCapacityMinutes());
        organizationMemberRepository.flush();
        return organizationMemberMapper.toResponse(member);
    }

    private static WorkloadResponse build(LocalDate from, LocalDate to, List<Person> people, List<WorkLogTotal> totals,
                                          List<Issue> openIssues, Map<UUID, String> projectTitles){
        int workingDays = workingDays(from, to);
        Map<UUID, List<WorkLogTotal>> totalsByUser = totals.stream().collect(Collectors.groupingBy(WorkLogTotal::getUserId));
        Map<UUID, List<Issue>> issuesByMember = openIssues.stream()
                .filter(issue -> issue.getType() != IssueType.EPIC)
                .collect(Collectors.groupingBy(issue -> issue.getAssignee().getOrganizationMember().getId()));

        List<MemberWorkloadResponse> members = people.stream()
                .map(person -> memberWorkload(person, from, to, workingDays,
                        totalsByUser.getOrDefault(person.userId(), List.of()),
                        issuesByMember.getOrDefault(person.organizationMemberId(), List.of()),
                        projectTitles))
                .sorted(Comparator.comparing(MemberWorkloadResponse::loggedMinutes).reversed()
                        .thenComparing(MemberWorkloadResponse::username, String.CASE_INSENSITIVE_ORDER))
                .toList();

        return new WorkloadResponse(
                from,
                to,
                workingDays,
                members.stream().mapToLong(MemberWorkloadResponse::loggedMinutes).sum(),
                members.stream().mapToLong(MemberWorkloadResponse::capacityMinutes).sum(),
                members.stream().mapToLong(MemberWorkloadResponse::remainingEstimateMinutes).sum(),
                members
        );
    }

    private static MemberWorkloadResponse memberWorkload(Person person, LocalDate from, LocalDate to, int workingDays,
                                                         List<WorkLogTotal> totals, List<Issue> issues,
                                                         Map<UUID, String> projectTitles){
        Map<LocalDate, Long> byDay = totals.stream()
                .collect(Collectors.groupingBy(WorkLogTotal::getWorkDate, Collectors.summingLong(WorkLogTotal::getTotal)));
        List<DailyMinutesResponse> days = new ArrayList<>();
        for(LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)){
            days.add(new DailyMinutesResponse(day, byDay.getOrDefault(day, 0L)));
        }

        Map<UUID, Long> byProject = totals.stream()
                .collect(Collectors.groupingBy(WorkLogTotal::getProjectId, Collectors.summingLong(WorkLogTotal::getTotal)));
        List<ProjectMinutesResponse> projects = byProject.entrySet().stream()
                .map(entry -> new ProjectMinutesResponse(entry.getKey(), projectTitles.get(entry.getKey()), entry.getValue()))
                .sorted(Comparator.comparing(ProjectMinutesResponse::minutes).reversed())
                .toList();

        long logged = byDay.values().stream().mapToLong(Long::longValue).sum();
        long remaining = issues.stream().map(Issue::getRemainingMinutes).filter(value -> value != null).mapToLong(Long::longValue).sum();
        long unestimated = issues.stream().filter(issue -> issue.getEstimateMinutes() == null).count();
        long capacity = (long) person.weeklyCapacityMinutes() * workingDays / WORKING_DAYS_PER_WEEK;

        return new MemberWorkloadResponse(
                person.organizationMemberId(),
                person.userId(),
                person.username(),
                person.role(),
                person.weeklyCapacityMinutes(),
                capacity,
                logged,
                issues.size(),
                unestimated,
                remaining,
                remaining > person.weeklyCapacityMinutes(),
                days,
                projects
        );
    }

    private static int workingDays(LocalDate from, LocalDate to){
        int count = 0;
        for(LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)){
            if(day.getDayOfWeek() != DayOfWeek.SATURDAY && day.getDayOfWeek() != DayOfWeek.SUNDAY){
                count++;
            }
        }
        return count;
    }

    private record Person(UUID organizationMemberId, UUID userId, String username, String role, int weeklyCapacityMinutes) {
        static Person of(OrganizationMember member, String role){
            return new Person(member.getId(), member.getUser().getId(), member.getUser().getUsername(), role,
                    member.getWeeklyCapacityMinutes());
        }
    }
}

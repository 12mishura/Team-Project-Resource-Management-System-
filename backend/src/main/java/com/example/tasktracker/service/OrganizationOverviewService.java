package com.example.tasktracker.service;

import com.example.tasktracker.aop.OrganizationAccess;
import com.example.tasktracker.aop.OrganizationId;
import com.example.tasktracker.domain.OrganizationMember;
import com.example.tasktracker.domain.Project;
import com.example.tasktracker.domain.enums.IssueStatus;
import com.example.tasktracker.domain.enums.OrganizationRole;
import com.example.tasktracker.dto.response.OrganizationOverviewResponse;
import com.example.tasktracker.dto.response.ProjectStatsResponse;
import com.example.tasktracker.repository.IssueRepository;
import com.example.tasktracker.repository.OrganizationMemberRepository;
import com.example.tasktracker.repository.ProjectMemberRepository;
import com.example.tasktracker.repository.ProjectRepository;
import com.example.tasktracker.repository.projection.ProjectCount;
import com.example.tasktracker.repository.projection.ProjectMemberName;
import com.example.tasktracker.repository.projection.ProjectStatusCount;
import com.example.tasktracker.security.OrganizationPermissionChecker;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class OrganizationOverviewService {
    private static final int MEMBER_PREVIEW_LIMIT = 5;

    private final OrganizationPermissionChecker permissionChecker;
    private final ProjectRepository projectRepository;
    private final IssueRepository issueRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final OrganizationMemberRepository organizationMemberRepository;

    @OrganizationAccess
    @Transactional(readOnly = true)
    public OrganizationOverviewResponse getOverview(@OrganizationId UUID organizationId, String email){
        OrganizationMember actor = permissionChecker.requireMember(organizationId, email);
        boolean privileged = actor.getRole() != OrganizationRole.USER;

        List<Project> projects = projectRepository.findAllByOrganizationId(organizationId, Sort.by("createdAt", "id"));
        Map<UUID, List<ProjectMemberName>> members = projectMemberRepository.findMemberNamesByOrganizationId(organizationId).stream()
                .collect(Collectors.groupingBy(ProjectMemberName::getProjectId));
        Map<UUID, Map<IssueStatus, Long>> statuses = issueRepository.countByProjectAndStatus(organizationId).stream()
                .collect(Collectors.groupingBy(ProjectStatusCount::getProjectId,
                        Collectors.toMap(ProjectStatusCount::getStatus, ProjectStatusCount::getTotal)));
        Map<UUID, Long> openBugs = totals(issueRepository.countOpenBugsByProject(organizationId, IssueStatus.DONE));
        Map<UUID, Long> assigned = totals(issueRepository.countOpenAssignedByProject(organizationId, email, IssueStatus.DONE));

        List<ProjectStatsResponse> stats = projects.stream()
                .filter(project -> privileged || isMember(members.get(project.getId()), email))
                .map(project -> projectStats(project.getId(), statuses.get(project.getId()), members.get(project.getId()),
                        openBugs.getOrDefault(project.getId(), 0L), assigned.getOrDefault(project.getId(), 0L)))
                .toList();

        return new OrganizationOverviewResponse(
                organizationId,
                projects.size(),
                organizationMemberRepository.countByOrganizationId(organizationId),
                stats.stream().mapToLong(ProjectStatsResponse::openIssues).sum(),
                stats.stream().mapToLong(ProjectStatsResponse::assignedToMe).sum(),
                stats
        );
    }

    private static ProjectStatsResponse projectStats(UUID projectId, Map<IssueStatus, Long> counts, List<ProjectMemberName> members,
                                                     long openBugs, long assignedToMe){
        Map<IssueStatus, Long> byStatus = new EnumMap<>(IssueStatus.class);
        for(IssueStatus status : IssueStatus.values()){
            byStatus.put(status, counts == null ? 0L : counts.getOrDefault(status, 0L));
        }
        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        List<ProjectMemberName> projectMembers = members == null ? List.of() : members;
        return new ProjectStatsResponse(
                projectId,
                byStatus,
                total,
                total - byStatus.get(IssueStatus.DONE),
                openBugs,
                assignedToMe,
                projectMembers.size(),
                projectMembers.stream().limit(MEMBER_PREVIEW_LIMIT).map(ProjectMemberName::getUsername).toList()
        );
    }

    private static boolean isMember(List<ProjectMemberName> members, String email){
        return members != null && members.stream().anyMatch(member -> member.getEmail().equalsIgnoreCase(email));
    }

    private static Map<UUID, Long> totals(List<ProjectCount> counts){
        return counts.stream().collect(Collectors.toMap(ProjectCount::getProjectId, ProjectCount::getTotal));
    }
}

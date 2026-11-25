package com.example.tasktracker.security;

import com.example.tasktracker.domain.Issue;
import com.example.tasktracker.domain.OrganizationMember;
import com.example.tasktracker.domain.ProjectMember;
import com.example.tasktracker.domain.enums.ProjectMemberRole;

import java.util.UUID;

public record ProjectActor(
        OrganizationMember organizationMember,
        ProjectMember projectMember,
        ProjectMemberRole role
) {
    public UUID userId(){
        return organizationMember.getUser().getId();
    }

    public boolean isAtLeast(ProjectMemberRole required){
        return rank(role) >= rank(required);
    }

    public boolean isReporterOf(Issue issue){
        return issue.getReporter().getId().equals(userId());
    }

    public boolean isAssigneeOf(Issue issue){
        return projectMember != null && issue.getAssignee() != null
                && issue.getAssignee().getId().equals(projectMember.getId());
    }

    public boolean is(ProjectMember member){
        return projectMember != null && member != null && member.getId().equals(projectMember.getId());
    }

    private static int rank(ProjectMemberRole role){
        return switch (role) {
            case ADMIN -> 3;
            case EXTENDED -> 2;
            case USER -> 1;
        };
    }
}

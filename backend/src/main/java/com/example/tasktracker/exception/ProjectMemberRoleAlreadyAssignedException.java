package com.example.tasktracker.exception;

import com.example.tasktracker.domain.enums.ProjectMemberRole;

public class ProjectMemberRoleAlreadyAssignedException extends RuntimeException {
    public ProjectMemberRoleAlreadyAssignedException(ProjectMemberRole role) {
        super("Project member already has role " + role);
    }
}

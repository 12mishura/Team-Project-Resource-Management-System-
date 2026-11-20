package com.example.tasktracker.exception;

import com.example.tasktracker.domain.enums.ProjectStatus;

public class ProjectStatusAlreadyAssignedException extends RuntimeException {
    public ProjectStatusAlreadyAssignedException(ProjectStatus status) {
        super("Project already has status " + status);
    }
}

package com.example.tasktracker.exception;

import java.util.UUID;

public class ProjectMemberAlreadyExistsException extends RuntimeException {
    public ProjectMemberAlreadyExistsException(UUID projectId) {
        super("Member is already in project " + projectId);
    }
}

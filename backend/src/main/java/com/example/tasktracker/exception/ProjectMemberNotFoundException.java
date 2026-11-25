package com.example.tasktracker.exception;

import java.util.UUID;

public class ProjectMemberNotFoundException extends RuntimeException {
    public ProjectMemberNotFoundException(UUID id) {
        super("Project member with ID " + id + " not found");
    }
}

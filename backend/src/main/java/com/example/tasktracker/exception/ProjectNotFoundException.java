package com.example.tasktracker.exception;

import java.util.UUID;

public class ProjectNotFoundException extends RuntimeException {
    public ProjectNotFoundException(UUID id) {
        super("Project with ID " + id + " not found");
    }
}

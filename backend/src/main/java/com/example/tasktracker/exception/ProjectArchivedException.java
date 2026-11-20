package com.example.tasktracker.exception;

import java.util.UUID;

public class ProjectArchivedException extends RuntimeException {
    public ProjectArchivedException(UUID projectId) {
        super("Project " + projectId + " is archived and read-only");
    }
}

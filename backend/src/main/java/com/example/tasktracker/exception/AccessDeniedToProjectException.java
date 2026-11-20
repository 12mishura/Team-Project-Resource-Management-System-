package com.example.tasktracker.exception;

import java.util.UUID;

public class AccessDeniedToProjectException extends RuntimeException {
    public AccessDeniedToProjectException(UUID projectId) {
        super("Access denied to project " + projectId);
    }
}

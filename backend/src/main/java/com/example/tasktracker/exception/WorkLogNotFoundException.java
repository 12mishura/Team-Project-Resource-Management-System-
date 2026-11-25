package com.example.tasktracker.exception;

import java.util.UUID;

public class WorkLogNotFoundException extends RuntimeException {
    public WorkLogNotFoundException(UUID id) {
        super("Work log with ID " + id + " not found");
    }
}

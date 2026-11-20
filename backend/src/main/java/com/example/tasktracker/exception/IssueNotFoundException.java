package com.example.tasktracker.exception;

import java.util.UUID;

public class IssueNotFoundException extends RuntimeException {
    public IssueNotFoundException(UUID id) {
        super("Issue with ID " + id + " not found");
    }
}

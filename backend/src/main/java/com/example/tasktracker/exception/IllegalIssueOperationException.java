package com.example.tasktracker.exception;

public class IllegalIssueOperationException extends RuntimeException {
    public IllegalIssueOperationException(String message) {
        super(message);
    }
}

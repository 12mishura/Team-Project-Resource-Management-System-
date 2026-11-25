package com.example.tasktracker.exception;

public class InvalidIssueException extends RuntimeException {
    public InvalidIssueException(String message) {
        super(message);
    }
}

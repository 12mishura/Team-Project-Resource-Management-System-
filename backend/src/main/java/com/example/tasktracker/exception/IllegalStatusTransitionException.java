package com.example.tasktracker.exception;

import com.example.tasktracker.domain.enums.IssueStatus;

public class IllegalStatusTransitionException extends RuntimeException {
    public IllegalStatusTransitionException(IssueStatus from, IssueStatus to) {
        super("Cannot change status from " + from + " to " + to);
    }
}

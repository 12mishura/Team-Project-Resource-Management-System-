package com.example.tasktracker.exception;

import com.example.tasktracker.domain.enums.IssueStatus;

public class IssueStatusAlreadyAssignedException extends RuntimeException {
    public IssueStatusAlreadyAssignedException(IssueStatus status) {
        super("Issue already has status " + status);
    }
}

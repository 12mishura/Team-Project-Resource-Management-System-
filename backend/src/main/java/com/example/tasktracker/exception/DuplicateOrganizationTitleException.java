package com.example.tasktracker.exception;

public class DuplicateOrganizationTitleException extends RuntimeException {
    public DuplicateOrganizationTitleException(String title) {
        super("Organization with title " + title + " already exists");
    }
}

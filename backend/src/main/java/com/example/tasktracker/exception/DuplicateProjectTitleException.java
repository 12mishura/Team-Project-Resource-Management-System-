package com.example.tasktracker.exception;

public class DuplicateProjectTitleException extends RuntimeException {
    public DuplicateProjectTitleException(String title) {
        super("Project with title " + title + " already exists");
    }
}

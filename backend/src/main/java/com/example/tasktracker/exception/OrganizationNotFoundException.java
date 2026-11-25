package com.example.tasktracker.exception;

import java.util.UUID;

public class OrganizationNotFoundException extends RuntimeException {
    public OrganizationNotFoundException(UUID id) {
        super("Organization with ID " + id + " not found");
    }
}

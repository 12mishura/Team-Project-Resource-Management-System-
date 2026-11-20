package com.example.tasktracker.exception;

import java.util.UUID;

public class AccessDeniedToOrganizationException extends RuntimeException {
    public AccessDeniedToOrganizationException(UUID organizationId) {
        super("Access denied to organization " + organizationId);
    }
}

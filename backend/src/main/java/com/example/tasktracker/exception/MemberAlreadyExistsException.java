package com.example.tasktracker.exception;

import java.util.UUID;

public class MemberAlreadyExistsException extends RuntimeException {
    public MemberAlreadyExistsException(UUID id) {
        super("User is already member of organization " + id);
    }
}

package com.example.tasktracker.exception;

import java.util.UUID;

public class MemberNotFoundException extends RuntimeException {
    public MemberNotFoundException(UUID id) {
        super("Member with ID " + id + " not found");
    }
}

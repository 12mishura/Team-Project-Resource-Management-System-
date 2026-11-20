package com.example.tasktracker.exception;

public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String field, String value) {
        super("User with " + field + " '" + value + "' already exists");
    }
}

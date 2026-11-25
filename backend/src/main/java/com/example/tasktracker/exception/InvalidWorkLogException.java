package com.example.tasktracker.exception;

public class InvalidWorkLogException extends RuntimeException {
    public InvalidWorkLogException(String message) {
        super(message);
    }
}

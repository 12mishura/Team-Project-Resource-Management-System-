package com.example.tasktracker.exception;

import java.util.UUID;

public class CommentNotFoundException extends RuntimeException {
    public CommentNotFoundException(UUID id) {
        super("Comment with ID " + id + " not found");
    }
}

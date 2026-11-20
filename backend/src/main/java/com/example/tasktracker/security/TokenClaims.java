package com.example.tasktracker.security;

public record TokenClaims(
        String subject,
        Integer tokenVersion
) {
}

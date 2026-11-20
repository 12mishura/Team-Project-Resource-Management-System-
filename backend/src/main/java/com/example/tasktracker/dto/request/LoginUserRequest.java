package com.example.tasktracker.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Locale;

public record LoginUserRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {
    public LoginUserRequest {
        email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }
}

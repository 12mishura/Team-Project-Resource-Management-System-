package com.example.tasktracker.dto.request;

import com.example.tasktracker.validator.MaxBytes;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record RegisterUserRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 3, max = 50) String username,
        @NotBlank @Size(min = 8) @MaxBytes(72) String password
) {
    public RegisterUserRequest {
        email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
        username = username == null ? null : username.strip();
    }
}

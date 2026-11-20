package com.example.tasktracker.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        @NotBlank
        @Size(min = 44, message = "must be at least 32 bytes encoded in Base64, set the JWT_SECRET environment variable")
        @Pattern(regexp = "^[A-Za-z0-9+/]+={0,2}$", message = "must be Base64-encoded, set the JWT_SECRET environment variable")
        String secret,
        @NotNull Duration expiration,
        @NotNull Duration refreshExpiration
) {
}

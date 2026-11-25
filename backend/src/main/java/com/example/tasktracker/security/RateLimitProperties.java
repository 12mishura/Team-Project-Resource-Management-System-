package com.example.tasktracker.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        boolean enabled,
        int loginPerMinute,
        int refreshPerMinute,
        int registerPerHour
) {
}

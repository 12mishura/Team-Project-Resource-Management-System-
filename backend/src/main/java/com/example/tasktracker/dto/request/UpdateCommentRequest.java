package com.example.tasktracker.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCommentRequest(
        @NotBlank @Size(max = 5000) String text
) {
    public UpdateCommentRequest {
        text = text == null ? null : text.strip();
    }
}

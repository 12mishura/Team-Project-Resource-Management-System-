package com.example.backend.dto;

import com.example.backend.entity.enums.NotificationReferenceType;
import com.example.backend.entity.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationCreateRequest {

    @NotNull
    private UUID userId;

    @NotBlank
    @Size(max = 150)
    private String title;

    @NotBlank
    private String message;

    @NotNull
    private NotificationType type;

    private UUID referenceId;

    private NotificationReferenceType referenceType;
}

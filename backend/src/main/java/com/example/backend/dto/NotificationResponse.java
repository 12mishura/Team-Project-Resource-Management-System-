package com.example.backend.DTO;

import com.example.backend.Entity.Enum.NotificationReferenceType;
import com.example.backend.Entity.Enum.NotificationType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private UUID id;

    private UUID userId;

    private String title;

    private String message;

    private NotificationType type;

    private boolean read;

    private UUID referenceId;

    private NotificationReferenceType referenceType;

    private LocalDateTime createdAt;

    private LocalDateTime readAt;
}
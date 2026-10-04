package com.example.backend.dto;

import com.example.backend.entity.enums.NotificationReferenceType;
import com.example.backend.entity.enums.NotificationType;
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
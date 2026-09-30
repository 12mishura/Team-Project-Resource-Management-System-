package com.example.backend.DTO;

import com.example.backend.Entity.Enum.NotificationReferenceType;
import com.example.backend.Entity.Enum.NotificationType;
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

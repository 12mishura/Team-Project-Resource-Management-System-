package com.example.backend.entity;

import com.example.backend.entity.enums.NotificationReferenceType;
import com.example.backend.entity.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "notifications",
        indexes = {
                @Index(name = "idx_notification_user", columnList = "user_id"),
                @Index(name = "idx_notification_read", columnList = "is_read"),
                @Index(name = "idx_notification_created", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * User who receives the notification.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Short notification title.
     *
     * Example:
     * "Task assigned to you"
     */
    @Column(nullable = false, length = 150)
    private String title;

    /**
     * Full notification message.
     *
     * Example:
     * "You have been assigned the task 'Implement login'."
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    /**
     * Type of notification.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    /**
     * Whether the user has opened/read the notification.
     */
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean read = false;

    /**
     * Optional reference to the object that caused the notification.
     *
     * Example:
     * WORK_ITEM -> workItemId
     * PROJECT    -> projectId
     * SPRINT     -> sprintId
     */
    @Column(name = "reference_id")
    private UUID referenceId;

    /**
     * What entity referenceId points to.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", length = 50)
    private NotificationReferenceType referenceType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public void markAsRead() {
        this.read = true;
        this.readAt = LocalDateTime.now();
    }
}
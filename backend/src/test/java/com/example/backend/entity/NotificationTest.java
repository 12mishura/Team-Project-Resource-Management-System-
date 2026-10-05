package com.example.backend.entity;

import com.example.backend.entity.enums.NotificationReferenceType;
import com.example.backend.entity.enums.NotificationType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NotificationTest {

    @Test
    void shouldCreateUnreadNotification() {

        Notification notification = Notification.builder()
                .title("Task assigned")
                .message("You have a new task")
                .type(NotificationType.TASK_ASSIGNED)
                .build();

        assertFalse(notification.isRead());
    }

    @Test
    void shouldMarkNotificationAsRead() {

        Notification notification = Notification.builder()
                .title("Task assigned")
                .message("You have a new task")
                .type(NotificationType.TASK_ASSIGNED)
                .build();

        assertFalse(notification.isRead());
        assertNull(notification.getReadAt());

        notification.markAsRead();

        assertTrue(notification.isRead());
        assertNotNull(notification.getReadAt());
    }

    @Test
    void shouldSetCreatedAtOnCreate() {

        Notification notification = Notification.builder()
                .title("Test")
                .message("Test message")
                .type(NotificationType.SYSTEM)
                .build();

        assertNull(notification.getCreatedAt());

        notification.onCreate();

        assertNotNull(notification.getCreatedAt());
    }

    @Test
    void shouldAssociateNotificationWithUser() {

        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .username("john")
                .email("john@example.com")
                .build();

        Notification notification = Notification.builder()
                .user(user)
                .title("Test")
                .message("Test message")
                .type(NotificationType.SYSTEM)
                .build();

        assertNotNull(notification.getUser());
        assertEquals(userId, notification.getUser().getId());
    }

    @Test
    void shouldStoreReference() {

        UUID workItemId = UUID.randomUUID();

        Notification notification = Notification.builder()
                .title("Task updated")
                .message("Task status changed")
                .type(NotificationType.TASK_STATUS_CHANGED)
                .referenceId(workItemId)
                .referenceType(NotificationReferenceType.WORK_ITEM)
                .build();

        assertEquals(workItemId, notification.getReferenceId());

        assertEquals(
                NotificationReferenceType.WORK_ITEM,
                notification.getReferenceType()
        );
    }
}

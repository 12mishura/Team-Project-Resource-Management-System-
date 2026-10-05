package com.example.backend.mapper;

import com.example.backend.dto.NotificationCreateRequest;
import com.example.backend.dto.NotificationResponse;
import com.example.backend.entity.Notification;
import com.example.backend.entity.User;

import com.example.backend.entity.enums.NotificationReferenceType;
import com.example.backend.entity.enums.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NotificationMapperTest {

    private NotificationMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(NotificationMapper.class);
    }

    @Test
    void shouldMapNotificationToResponse() {

        UUID notificationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID workItemId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .username("john")
                .email("john@example.com")
                .build();

        Notification notification = Notification.builder()
                .id(notificationId)
                .user(user)
                .title("Task assigned")
                .message("You have been assigned a task")
                .type(NotificationType.TASK_ASSIGNED)
                .read(false)
                .referenceId(workItemId)
                .referenceType(NotificationReferenceType.WORK_ITEM)
                .build();

        NotificationResponse response = mapper.toResponse(notification);

        assertNotNull(response);

        assertEquals(notificationId, response.getId());
        assertEquals(userId, response.getUserId());

        assertEquals("Task assigned", response.getTitle());
        assertEquals(
                "You have been assigned a task",
                response.getMessage()
        );

        assertEquals(
                NotificationType.TASK_ASSIGNED,
                response.getType()
        );

        assertFalse(response.isRead());

        assertEquals(workItemId, response.getReferenceId());

        assertEquals(
                NotificationReferenceType.WORK_ITEM,
                response.getReferenceType()
        );
    }

    @Test
    void shouldMapCreateRequestToEntity() {

        UUID referenceId = UUID.randomUUID();

        NotificationCreateRequest request =
                NotificationCreateRequest.builder()
                        .userId(UUID.randomUUID())
                        .title("Task assigned")
                        .message("You have been assigned a task")
                        .type(NotificationType.TASK_ASSIGNED)
                        .referenceId(referenceId)
                        .referenceType(
                                NotificationReferenceType.WORK_ITEM
                        )
                        .build();

        Notification notification = mapper.toEntity(request);

        assertNotNull(notification);

        assertEquals("Task assigned", notification.getTitle());

        assertEquals(
                "You have been assigned a task",
                notification.getMessage()
        );

        assertEquals(
                NotificationType.TASK_ASSIGNED,
                notification.getType()
        );

        assertFalse(notification.isRead());

        assertEquals(
                referenceId,
                notification.getReferenceId()
        );

        assertEquals(
                NotificationReferenceType.WORK_ITEM,
                notification.getReferenceType()
        );

        // User is intentionally set by the service.
        assertNull(notification.getUser());
    }

    @Test
    void shouldMapReadNotification() {

        User user = User.builder()
                .id(UUID.randomUUID())
                .build();

        Notification notification = Notification.builder()
                .id(UUID.randomUUID())
                .user(user)
                .title("Test")
                .message("Test message")
                .type(NotificationType.SYSTEM)
                .read(true)
                .build();

        NotificationResponse response = mapper.toResponse(notification);

        assertTrue(response.isRead());
    }
}

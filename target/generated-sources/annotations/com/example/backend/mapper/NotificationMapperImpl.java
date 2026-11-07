package com.example.backend.mapper;

import com.example.backend.dto.NotificationCreateRequest;
import com.example.backend.dto.NotificationResponse;
import com.example.backend.entity.Notification;
import com.example.backend.entity.User;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-10-06T23:59:05+0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 26 (Oracle Corporation)"
)
@Component
public class NotificationMapperImpl implements NotificationMapper {

    @Override
    public NotificationResponse toResponse(Notification notification) {
        if ( notification == null ) {
            return null;
        }

        NotificationResponse.NotificationResponseBuilder notificationResponse = NotificationResponse.builder();

        notificationResponse.userId( notificationUserId( notification ) );
        notificationResponse.id( notification.getId() );
        notificationResponse.title( notification.getTitle() );
        notificationResponse.message( notification.getMessage() );
        notificationResponse.type( notification.getType() );
        notificationResponse.read( notification.isRead() );
        notificationResponse.referenceId( notification.getReferenceId() );
        notificationResponse.referenceType( notification.getReferenceType() );
        notificationResponse.createdAt( notification.getCreatedAt() );
        notificationResponse.readAt( notification.getReadAt() );

        return notificationResponse.build();
    }

    @Override
    public Notification toEntity(NotificationCreateRequest request) {
        if ( request == null ) {
            return null;
        }

        Notification.NotificationBuilder notification = Notification.builder();

        notification.title( request.getTitle() );
        notification.message( request.getMessage() );
        notification.type( request.getType() );
        notification.referenceId( request.getReferenceId() );
        notification.referenceType( request.getReferenceType() );

        notification.read( false );

        return notification.build();
    }

    private UUID notificationUserId(Notification notification) {
        User user = notification.getUser();
        if ( user == null ) {
            return null;
        }
        return user.getId();
    }
}

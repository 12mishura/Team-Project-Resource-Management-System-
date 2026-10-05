package com.example.backend.mapper;

import com.example.backend.dto.NotificationCreateRequest;
import com.example.backend.dto.NotificationResponse;
import com.example.backend.entity.Notification;
import com.example.backend.entity.User;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface NotificationMapper {

    @Mapping(
            target = "userId",
            source = "user.id"
    )
    NotificationResponse toResponse(Notification notification);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "read", constant = "false")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "readAt", ignore = true)
    Notification toEntity(NotificationCreateRequest request);

    @AfterMapping
    default void setUser(
            NotificationCreateRequest request,
            @MappingTarget Notification notification,
            @Context User user
    ) {
        notification.setUser(user);
    }
}
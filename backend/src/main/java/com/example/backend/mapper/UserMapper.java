package com.example.backend.mapper;

import com.example.backend.DTO.UserResponse;
import com.example.backend.DTO.UserUpdateRequest;
import com.example.backend.Entity.User;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface UserMapper {

    @Mapping(
            target = "organizationId",
            source = "organization.id"
    )
    UserResponse toResponse(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "notifications", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(
            UserUpdateRequest request,
            @MappingTarget User user
    );
}

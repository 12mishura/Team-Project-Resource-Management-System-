package com.example.backend.mapper;

import com.example.backend.dto.ProjectMemberRequest;
import com.example.backend.dto.ProjectMemberResponse;
import com.example.backend.entity.ProjectMember;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProjectMemberMapper {

    // Entity -> Response DTO
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "project.id", target = "projectId")
    @Mapping(source = "project.name", target = "projectName")
    ProjectMemberResponse toDto(ProjectMember entity);

    List<ProjectMemberResponse> toDtoList(List<ProjectMember> entities);

    // Request DTO -> Entity (user and project are set in the service)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "joinedAt", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "project", ignore = true)
    ProjectMember toEntity(ProjectMemberRequest dto);

    // Update the role of an existing member
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "joinedAt", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "project", ignore = true)
    void updateEntity(ProjectMemberRequest dto, @MappingTarget ProjectMember entity);
}
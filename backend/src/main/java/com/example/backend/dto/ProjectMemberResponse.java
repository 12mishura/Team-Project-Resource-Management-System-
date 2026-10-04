package com.example.backend.dto;

import com.example.backend.entity.enums.ProjectRole;

import java.time.LocalDateTime;

public record ProjectMemberResponse(
        Integer id,
        ProjectRole projectRole,
        LocalDateTime joinedAt,
        Integer userId,
        String username,
        Integer projectId,
        String projectName
) {}
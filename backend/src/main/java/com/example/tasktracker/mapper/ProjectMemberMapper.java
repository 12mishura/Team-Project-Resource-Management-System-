package com.example.tasktracker.mapper;

import com.example.tasktracker.domain.ProjectMember;
import com.example.tasktracker.dto.response.ProjectMemberResponse;
import org.springframework.stereotype.Component;

@Component
public class ProjectMemberMapper {

    public ProjectMemberResponse toResponse(ProjectMember member){
        return new ProjectMemberResponse(
                member.getId(),
                member.getProject().getId(),
                member.getOrganizationMember().getId(),
                member.getOrganizationMember().getUser().getEmail(),
                member.getOrganizationMember().getUser().getUsername(),
                member.getRole(),
                member.getCreatedAt()
        );
    }
}

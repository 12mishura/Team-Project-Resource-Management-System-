package com.example.tasktracker.mapper;

import com.example.tasktracker.domain.OrganizationMember;
import com.example.tasktracker.dto.response.OrganizationMemberResponse;
import org.springframework.stereotype.Component;


@Component
public class OrganizationMemberMapper {

    public OrganizationMemberResponse toResponse(OrganizationMember member){
        return new OrganizationMemberResponse(
                member.getId(),
                member.getUser().getEmail(),
                member.getUser().getUsername(),
                member.getRole(),
                member.getWeeklyCapacityMinutes(),
                member.getCreatedAt()
        );
    }
}

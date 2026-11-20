package com.example.tasktracker.mapper;

import com.example.tasktracker.domain.Organization;
import com.example.tasktracker.domain.OrganizationMember;
import com.example.tasktracker.dto.request.CreateOrganizationRequest;
import com.example.tasktracker.dto.request.UpdateOrganizationRequest;
import com.example.tasktracker.dto.response.MyOrganizationResponse;
import com.example.tasktracker.dto.response.OrganizationResponse;
import org.springframework.stereotype.Component;

@Component
public class OrganizationMapper {
    public OrganizationResponse toResponse(Organization organization){
        return new OrganizationResponse(
                organization.getId(),
                organization.getTitle(),
                organization.getDescription(),
                organization.getCreatedAt(),
                organization.getUpdatedAt()
        );
    }

    public MyOrganizationResponse toMyResponse(OrganizationMember member){
        Organization organization = member.getOrganization();
        return new MyOrganizationResponse(
                organization.getId(),
                organization.getTitle(),
                organization.getDescription(),
                member.getRole(),
                member.getId(),
                member.getCreatedAt()
        );
    }

    public Organization toEntity(CreateOrganizationRequest request, String inviteCode){
        return new Organization(request.title(), request.description(), inviteCode);
    }

    public Organization toUpdate(UpdateOrganizationRequest request, Organization organization){
        if (request.title() != null) {
            organization.setTitle(request.title());
        }
        if (request.description() != null) {
            organization.setDescription(request.description());
        }
        return organization;
    }
}

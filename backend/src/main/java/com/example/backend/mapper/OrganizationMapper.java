package com.example.backend.mapper;

import com.example.backend.dto.OrganizationDto;
import com.example.backend.entity.Organization;
import org.springframework.stereotype.Component;

@Component
public class OrganizationMapper {

    public OrganizationDto toDto(Organization organization) {
        if (organization == null) {
            return null;
        }

        OrganizationDto dto = new OrganizationDto();
        dto.setId(organization.getId());
        dto.setName(organization.getName());
        dto.setDescription(organization.getDescription());
        dto.setCreatedAt(organization.getCreatedAt());
        dto.setUpdatedAt(organization.getUpdatedAt());

        return dto;
    }

    public Organization toEntity(OrganizationDto dto) {
        if (dto == null) {
            return null;
        }

        Organization organization = new Organization();
        organization.setName(dto.getName());
        organization.setDescription(dto.getDescription());

        return organization;
    }
}
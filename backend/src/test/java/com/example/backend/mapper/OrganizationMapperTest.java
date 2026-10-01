package com.example.backend.mapper;

import com.example.backend.dto.OrganizationDto;
import com.example.backend.entity.Organization;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OrganizationMapperTest {

    private final OrganizationMapper mapper = new OrganizationMapper();

    @Test
    void toDtoCopiesOrganizationDetails() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 9, 29, 12, 0);
        LocalDateTime updatedAt = createdAt.plusHours(1);

        Organization organization = new Organization();
        organization.setId(1);
        organization.setName("Student Team");
        organization.setDescription("Our university project team");
        organization.setCreatedAt(createdAt);
        organization.setUpdatedAt(updatedAt);

        OrganizationDto dto = mapper.toDto(organization);

        assertEquals(1, dto.getId());
        assertEquals("Student Team", dto.getName());
        assertEquals("Our university project team", dto.getDescription());
        assertEquals(createdAt, dto.getCreatedAt());
        assertEquals(updatedAt, dto.getUpdatedAt());
    }

    @Test
    void toEntityCreatesOrganizationWithoutGeneratedFields() {
        OrganizationDto dto = new OrganizationDto();
        dto.setId(99);
        dto.setName("Student Team");
        dto.setDescription("Our university project team");
        dto.setCreatedAt(LocalDateTime.of(2026, 1, 1, 12, 0));
        dto.setUpdatedAt(LocalDateTime.of(2026, 1, 2, 12, 0));

        Organization organization = mapper.toEntity(dto);

        assertEquals("Student Team", organization.getName());
        assertEquals("Our university project team", organization.getDescription());
        assertNull(organization.getId());
        assertNull(organization.getCreatedAt());
        assertNull(organization.getUpdatedAt());
    }

    @Test
    void toDtoReturnsNullForNullInput() {
        assertNull(mapper.toDto(null));
    }

    @Test
    void toEntityReturnsNullForNullInput() {
        assertNull(mapper.toEntity(null));
    }
}
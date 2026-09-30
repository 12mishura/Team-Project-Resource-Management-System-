package com.example.backend.mapper;

import com.example.backend.DTO.UserResponse;
import com.example.backend.DTO.UserUpdateRequest;
import com.example.backend.Entity.Enum.OrgRole;
import com.example.backend.Entity.Organization;
import com.example.backend.Entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserMapperTest {

    private UserMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(UserMapper.class);
    }

    @Test
    void shouldMapUserToResponse() {

        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();

        Organization organization = Organization.builder()
                .id(organizationId)
                .name("Test Organization")
                .build();

        User user = User.builder()
                .id(userId)
                .username("john")
                .email("john@example.com")
                .passwordHash("hashed-password")
                .role(OrgRole.MEMBER)
                .organization(organization)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        UserResponse response = mapper.toResponse(user);

        assertNotNull(response);
        assertEquals(userId, response.getId());
        assertEquals("john", response.getUsername());
        assertEquals("john@example.com", response.getEmail());
        assertEquals(OrgRole.MEMBER, response.getRole());
        assertEquals(organizationId, response.getOrganizationId());
    }

    @Test
    void shouldNotExposePasswordHash() {

        User user = User.builder()
                .id(UUID.randomUUID())
                .username("john")
                .email("john@example.com")
                .passwordHash("very-secret-password")
                .role(OrgRole.MEMBER)
                .build();

        UserResponse response = mapper.toResponse(user);

        assertNotNull(response);

        // UserResponse has no passwordHash field.
        // This test documents the security requirement.
        assertEquals("john", response.getUsername());
    }

    @Test
    void shouldUpdateUserFields() {

        User user = User.builder()
                .id(UUID.randomUUID())
                .username("old-name")
                .email("old@example.com")
                .passwordHash("existing-hash")
                .role(OrgRole.MEMBER)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        UserUpdateRequest request = UserUpdateRequest.builder()
                .username("new-name")
                .email("new@example.com")
                .role(OrgRole.ADMIN)
                .build();

        mapper.updateEntity(request, user);

        assertEquals("new-name", user.getUsername());
        assertEquals("new@example.com", user.getEmail());
        assertEquals(OrgRole.ADMIN, user.getRole());

        // Should not be modified by mapper
        assertEquals("existing-hash", user.getPasswordHash());
    }

    @Test
    void shouldIgnoreIdWhenUpdating() {

        UUID originalId = UUID.randomUUID();

        User user = User.builder()
                .id(originalId)
                .username("john")
                .email("john@example.com")
                .passwordHash("hash")
                .role(OrgRole.MEMBER)
                .build();

        UserUpdateRequest request = UserUpdateRequest.builder()
                .username("updated")
                .build();

        mapper.updateEntity(request, user);

        assertEquals(originalId, user.getId());
    }
}

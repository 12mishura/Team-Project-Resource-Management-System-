package com.example.backend.mapper;

import com.example.backend.dto.ProjectRequest;
import com.example.backend.dto.ProjectResponse;
import com.example.backend.entity.Organization;
import com.example.backend.entity.Project;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProjectMapperTest {
    private final ProjectMapper mapper = new ProjectMapper();
    private static final LocalDate START_DATE = LocalDate.of(2026, 10, 1);
    private static final LocalDate END_DATE = LocalDate.of(2026, 12, 31);
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 10, 1, 9, 0);
    private static final LocalDateTime UPDATED_AT = LocalDateTime.of(2026, 10, 2, 10, 0);

    @Test
    void toEntity_mapsRequestAndUsesSuppliedOrganization() {
        ProjectRequest request = new ProjectRequest("Resource management", "Team project",
                START_DATE, END_DATE, 7);
        Organization organization = buildOrganization(7);

        Project project = mapper.toEntity(request, organization);

        assertAll(
                () -> assertEquals(request.name(), project.getName()),
                () -> assertEquals(request.description(), project.getDescription()),
                () -> assertEquals(request.startDate(), project.getStartDate()),
                () -> assertEquals(request.endDate(), project.getEndDate()),
                () -> assertSame(organization, project.getOrganization()),
                () -> assertNull(project.getId()),
                () -> assertNull(project.getCreatedAt()),
                () -> assertNull(project.getUpdatedAt())
        );
    }

    @Test
    void toEntity_returnsNullForNullRequest() {
        assertNull(mapper.toEntity(null, buildOrganization(7)));
        assertNull(mapper.toEntity(null, null));
    }

    @Test
    void toEntity_preservesNullOptionalFields() {
        Organization organization = buildOrganization(7);
        Project project = mapper.toEntity(new ProjectRequest("Project", null, null, null, 7), organization);

        assertAll(
                () -> assertEquals("Project", project.getName()),
                () -> assertNull(project.getDescription()),
                () -> assertNull(project.getStartDate()),
                () -> assertNull(project.getEndDate()),
                () -> assertSame(organization, project.getOrganization())
        );
    }

    @Test
    void toEntity_usesSuppliedOrganizationEvenWhenRequestIdDiffers() {
        Organization organization = buildOrganization(8);
        ProjectRequest request = new ProjectRequest("Project", null, null, null, 7);

        Project project = mapper.toEntity(request, organization);

        assertSame(organization, project.getOrganization());
        assertEquals(8, project.getOrganization().getId());
    }

    @Test
    void toEntity_preservesNullOrganization() {
        ProjectRequest request = new ProjectRequest("Project", null, null, null, 7);

        Project project = mapper.toEntity(request, null);

        assertEquals("Project", project.getName());
        assertNull(project.getOrganization());
    }

    @Test
    void toResponse_mapsAllFields() {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Project project = buildProject(id, "Project", 7);

        ProjectResponse response = mapper.toResponse(project);

        assertEquals(new ProjectResponse(id, "Project", "Description", START_DATE, END_DATE,
                CREATED_AT, UPDATED_AT, 7), response);
    }

    @Test
    void toResponse_preservesNullOptionalFields() {
        Project project = new Project();
        project.setName("Project");
        project.setOrganization(buildOrganization(7));

        assertEquals(new ProjectResponse(null, "Project", null, null, null, null, null, 7),
                mapper.toResponse(project));
    }

    @Test
    void toList_mapsEveryProjectInOrder() {
        UUID firstId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID secondId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        Project first = buildProject(firstId, "First", 7);
        Project second = buildProject(secondId, "Second", 8);

        assertEquals(List.of(
                        new ProjectResponse(firstId, "First", "Description", START_DATE, END_DATE,
                                CREATED_AT, UPDATED_AT, 7),
                        new ProjectResponse(secondId, "Second", "Description", START_DATE, END_DATE,
                                CREATED_AT, UPDATED_AT, 8)),
                mapper.toList(List.of(first, second)));
    }

    @Test
    void toList_returnsEmptyListForEmptyInput() {
        assertTrue(mapper.toList(List.of()).isEmpty());
    }

    private Project buildProject(UUID id, String name, Integer organizationId) {
        Project project = new Project();
        project.setId(id);
        project.setName(name);
        project.setDescription("Description");
        project.setStartDate(START_DATE);
        project.setEndDate(END_DATE);
        project.setCreatedAt(CREATED_AT);
        project.setUpdatedAt(UPDATED_AT);
        project.setOrganization(buildOrganization(organizationId));
        return project;
    }

    private Organization buildOrganization(Integer id) {
        Organization organization = new Organization();
        organization.setId(id);
        organization.setName("Organization " + id);
        return organization;
    }
}

package com.example.backend.Mapper;

import com.example.backend.DTO.ProjectRequest;
import com.example.backend.DTO.ProjectResponse;
import com.example.backend.Entity.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProjectMapperTest {

    private ProjectMapper projectMapper;

    @BeforeEach
    void setUp() {
        projectMapper = new ProjectMapper();
    }

    @Test
    void shouldMapProjectToResponse() {

        // Arrange
        Organization organization = new Organization();
        organization.setId(10L);

        Project project = new Project();

        project.setId(1L);
        project.setName("Test Project");
        project.setDescription("Test description");
        project.setStartDate(LocalDate.of(2026, 9, 28));
        project.setEndDate(LocalDate.of(2026, 12, 28));
        project.setCreated_at(
                LocalDateTime.of(2026, 9, 28, 12, 0)
        );
        project.setUpdated_at(
                LocalDateTime.of(2026, 9, 28, 13, 0)
        );
        project.setOrganization(organization);

        // Act
        ProjectResponse response = projectMapper.toResponse(project);

        // Assert
        assertEquals(1L, response.id());
        assertEquals("Test Project", response.name());
        assertEquals(
                "Test description",
                response.description()
        );

        assertEquals(
                LocalDate.of(2026, 9, 28),
                response.startDate()
        );

        assertEquals(
                LocalDate.of(2026, 12, 28),
                response.endDate()
        );

        assertEquals(10L, response.organization_id());
    }

    @Test
    void shouldMapRequestToEntity() {

        // Arrange
        ProjectRequest request = new ProjectRequest(
                "New Project",
                "Project description",
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2027, 1, 1)
        );

        // Act
        Project project = projectMapper.toEntity(request);

        // Assert
        assertEquals("New Project", project.getName());

        assertEquals(
                "Project description",
                project.getDescription()
        );

        assertEquals(
                LocalDate.of(2026, 10, 1),
                project.getStartDate()
        );

        assertEquals(
                LocalDate.of(2027, 1, 1),
                project.getEndDate()
        );
    }

    @Test
    void shouldMapProjectListToResponseList() {

        // Arrange
        Organization organization = new Organization();
        organization.setId(10L);

        Project project1 = new Project();
        project1.setId(1L);
        project1.setName("Project One");
        project1.setOrganization(organization);

        Project project2 = new Project();
        project2.setId(2L);
        project2.setName("Project Two");
        project2.setOrganization(organization);

        List<Project> projects = List.of(
                project1,
                project2
        );

        // Act
        List<ProjectResponse> responses =
                projectMapper.toResponseList(projects);

        // Assert
        assertEquals(2, responses.size());

        assertEquals(
                "Project One",
                responses.get(0).name()
        );

        assertEquals(
                "Project Two",
                responses.get(1).name()
        );
    }
}

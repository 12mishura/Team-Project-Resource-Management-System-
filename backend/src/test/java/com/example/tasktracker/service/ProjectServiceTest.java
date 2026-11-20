package com.example.tasktracker.service;

import com.example.tasktracker.AbstractIntegrationTest;
import com.example.tasktracker.domain.enums.ProjectStatus;
import com.example.tasktracker.dto.request.ChangeProjectStatusRequest;
import com.example.tasktracker.dto.request.CreateProjectRequest;
import com.example.tasktracker.dto.request.UpdateProjectRequest;
import com.example.tasktracker.exception.ProjectNotFoundException;
import com.example.tasktracker.dto.response.ProjectResponse;
import com.example.tasktracker.exception.AccessDeniedToOrganizationException;
import com.example.tasktracker.exception.DuplicateProjectTitleException;
import com.example.tasktracker.exception.IllegalMemberOperationException;
import com.example.tasktracker.exception.OrganizationNotFoundException;
import com.example.tasktracker.exception.ProjectStatusAlreadyAssignedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProjectServiceTest extends AbstractIntegrationTest {
    private static final String OWNER = "owner@mail.com";
    private static final String ADMIN = "admin@mail.com";
    private static final String USER = "member@mail.com";
    private static final String OUTSIDER = "outsider@mail.com";
    private static final Pageable FIRST_PAGE = PageRequest.of(0, 20);

    @Autowired
    private ProjectService projectService;

    private String ownerToken;
    private UUID organizationId;

    @BeforeEach
    void setUp() throws Exception {
        ownerToken = registerAndLogin("owner");
        String adminToken = registerAndLogin("admin");
        String userToken = registerAndLogin("member");
        String outsiderToken = registerAndLogin("outsider");

        organizationId = UUID.fromString(createOrganization(ownerToken, "MyOrganization"));
        String inviteCode = getInviteCode(ownerToken, organizationId.toString());
        String adminMemberId = joinAndGetMemberId(adminToken, inviteCode);
        changeRole(ownerToken, organizationId.toString(), adminMemberId, "ADMIN").andExpect(status().isOk());
        joinAndGetMemberId(userToken, inviteCode);
        createOrganization(outsiderToken, "OutsiderOrganization");
    }

    @Test
    void ownerCreatesActiveProjectInOwnOrganization() {
        ProjectResponse project = as(OWNER, () -> projectService.createProject(organizationId, request("Backend Rewrite")));

        assertThat(project.id()).isNotNull();
        assertThat(project.organizationId()).isEqualTo(organizationId);
        assertThat(project.title()).isEqualTo("Backend Rewrite");
        assertThat(project.status()).isEqualTo(ProjectStatus.ACTIVE);
        assertThat(project.createdAt()).isNotNull();
        assertThat(project.updatedAt()).isNotNull();
    }

    @Test
    void adminCanCreateProject() {
        assertThat(as(ADMIN, () -> projectService.createProject(organizationId, request("Admin Project"))).title())
                .isEqualTo("Admin Project");
    }

    @Test
    void regularMemberCannotCreateProject() {
        assertThatThrownBy(() -> as(USER, () -> projectService.createProject(organizationId, request("User Project"))))
                .isInstanceOf(IllegalMemberOperationException.class)
                .hasMessage("Only OWNER or ADMIN can perform this operation");
        assertThat(projectCount()).isZero();
    }

    @Test
    void outsiderCannotCreateProjectEvenBeingOwnerElsewhere() {
        assertThatThrownBy(() -> as(OUTSIDER, () -> projectService.createProject(organizationId, request("Hijacked Project"))))
                .isInstanceOf(AccessDeniedToOrganizationException.class);
        assertThat(projectCount()).isZero();
    }

    @Test
    void unknownOrganizationReturnsOrganizationNotFound() {
        UUID unknown = UUID.randomUUID();

        assertThatThrownBy(() -> as(OWNER, () -> projectService.createProject(unknown, request("Ghost Project"))))
                .isInstanceOf(OrganizationNotFoundException.class)
                .hasMessage("Organization with ID " + unknown + " not found");
    }

    @Test
    void duplicateTitleInSameOrganizationIsRejectedIgnoringCase() {
        as(OWNER, () -> projectService.createProject(organizationId, request("Backend Rewrite")));

        assertThatThrownBy(() -> as(ADMIN, () -> projectService.createProject(organizationId, request("BACKEND REWRITE"))))
                .isInstanceOf(DuplicateProjectTitleException.class)
                .hasMessage("Project with title BACKEND REWRITE already exists");
    }

    @Test
    void sameTitleIsAllowedInDifferentOrganizations() {
        as(OWNER, () -> projectService.createProject(organizationId, request("Backend Rewrite")));
        UUID otherOrganization = jdbcTemplate.queryForObject(
                "SELECT id FROM organization WHERE title = 'OutsiderOrganization'", UUID.class);

        assertThat(as(OUTSIDER, () -> projectService.createProject(otherOrganization, request("Backend Rewrite"))).organizationId())
                .isEqualTo(otherOrganization);
    }

    @Test
    void titleIsTrimmed() {
        assertThat(as(OWNER, () -> projectService.createProject(organizationId, request("   Backend Rewrite   "))).title())
                .isEqualTo("Backend Rewrite");
    }

    @Test
    void concurrentCreationWithSameTitleCreatesExactlyOneProject() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(6);
        List<Callable<String>> calls = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            String title = i % 2 == 0 ? "Race Project" : "RACE PROJECT";
            calls.add(() -> {
                try {
                    as(OWNER, () -> projectService.createProject(organizationId, request(title)));
                    return "created";
                } catch (DuplicateProjectTitleException | DataIntegrityViolationException e) {
                    return "rejected";
                }
            });
        }
        List<String> results = new ArrayList<>();
        for (Future<String> future : pool.invokeAll(calls)) {
            results.add(future.get());
        }
        pool.shutdown();

        assertThat(results).containsOnlyOnce("created");
        assertThat(projectCount()).isEqualTo(1);
    }

    @Test
    void deletingOrganizationDeletesItsProjects() throws Exception {
        as(OWNER, () -> projectService.createProject(organizationId, request("Backend Rewrite")));

        mockMvc.perform(withToken(delete("/organizations/{id}", organizationId), ownerToken))
                .andExpect(status().isNoContent());

        assertThat(projectCount()).isZero();
    }

    @Test
    void ownerUpdatesTitleAndDescription() {
        ProjectResponse created = as(OWNER, () -> projectService.createProject(organizationId, request("Backend Rewrite")));

        ProjectResponse updated = as(OWNER, () -> projectService.updateProject(organizationId, created.id(),
                new UpdateProjectRequest("Frontend Rewrite", "New description")));

        assertThat(updated.title()).isEqualTo("Frontend Rewrite");
        assertThat(updated.description()).isEqualTo("New description");
        assertThat(updated.updatedAt()).isAfterOrEqualTo(created.updatedAt());
    }

    @Test
    void adminCanUpdateProject() {
        UUID projectId = as(OWNER, () -> projectService.createProject(organizationId, request("Backend Rewrite"))).id();

        assertThat(as(ADMIN, () -> projectService.updateProject(organizationId, projectId, new UpdateProjectRequest(null, "By admin")))
                .description()).isEqualTo("By admin");
    }

    @Test
    void updateWithOnlyDescriptionKeepsTitle() {
        UUID projectId = as(OWNER, () -> projectService.createProject(organizationId, request("Backend Rewrite"))).id();

        ProjectResponse updated = as(OWNER, () -> projectService.updateProject(organizationId, projectId,
                new UpdateProjectRequest(null, "Only description")));

        assertThat(updated.title()).isEqualTo("Backend Rewrite");
    }

    @Test
    void regularMemberCannotUpdateProject() {
        UUID projectId = as(OWNER, () -> projectService.createProject(organizationId, request("Backend Rewrite"))).id();

        assertThatThrownBy(() -> as(USER, () -> projectService.updateProject(organizationId, projectId,
                new UpdateProjectRequest("Hacked Project", null))))
                .isInstanceOf(IllegalMemberOperationException.class);
    }

    @Test
    void outsiderCannotUpdateProject() {
        UUID projectId = as(OWNER, () -> projectService.createProject(organizationId, request("Backend Rewrite"))).id();

        assertThatThrownBy(() -> as(OUTSIDER, () -> projectService.updateProject(organizationId, projectId,
                new UpdateProjectRequest("Hacked Project", null))))
                .isInstanceOf(AccessDeniedToOrganizationException.class);
    }

    @Test
    void updatingUnknownProjectReturnsNotFound() {
        UUID unknown = UUID.randomUUID();

        assertThatThrownBy(() -> as(OWNER, () -> projectService.updateProject(organizationId, unknown,
                new UpdateProjectRequest("Whatever Title", null))))
                .isInstanceOf(ProjectNotFoundException.class)
                .hasMessage("Project with ID " + unknown + " not found");
    }

    @Test
    void projectOfAnotherOrganizationCannotBeUpdatedThroughOwnOrganization() {
        UUID otherOrganization = jdbcTemplate.queryForObject(
                "SELECT id FROM organization WHERE title = 'OutsiderOrganization'", UUID.class);
        UUID foreignProject = as(OUTSIDER, () -> projectService.createProject(otherOrganization, request("Foreign Project"))).id();

        assertThatThrownBy(() -> as(OWNER, () -> projectService.updateProject(organizationId, foreignProject,
                new UpdateProjectRequest("Stolen Project", null))))
                .isInstanceOf(ProjectNotFoundException.class);
    }

    @Test
    void renamingToAnotherProjectsTitleIsRejectedIgnoringCase() {
        as(OWNER, () -> projectService.createProject(organizationId, request("Backend Rewrite")));
        UUID projectId = as(OWNER, () -> projectService.createProject(organizationId, request("Frontend Rewrite"))).id();

        assertThatThrownBy(() -> as(OWNER, () -> projectService.updateProject(organizationId, projectId,
                new UpdateProjectRequest("backend rewrite", null))))
                .isInstanceOf(DuplicateProjectTitleException.class);
    }

    @Test
    void changingOnlyCaseOfOwnTitleIsAllowed() {
        UUID projectId = as(OWNER, () -> projectService.createProject(organizationId, request("Backend Rewrite"))).id();

        assertThat(as(OWNER, () -> projectService.updateProject(organizationId, projectId,
                new UpdateProjectRequest("BACKEND REWRITE", null))).title()).isEqualTo("BACKEND REWRITE");
    }

    private UUID otherOrganizationId() {
        return jdbcTemplate.queryForObject("SELECT id FROM organization WHERE title = 'OutsiderOrganization'", UUID.class);
    }

    private Integer projectCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM projects WHERE organization_id = ?", Integer.class, organizationId);
    }

    @Test
    void regularMemberCanReadProject() {
        UUID projectId = as(OWNER, () -> projectService.createProject(organizationId, request("Backend Rewrite"))).id();

        ProjectResponse project = as(USER, () -> projectService.getProject(organizationId, projectId));

        assertThat(project.id()).isEqualTo(projectId);
        assertThat(project.title()).isEqualTo("Backend Rewrite");
    }

    @Test
    void outsiderCannotReadProject() {
        UUID projectId = as(OWNER, () -> projectService.createProject(organizationId, request("Backend Rewrite"))).id();

        assertThatThrownBy(() -> as(OUTSIDER, () -> projectService.getProject(organizationId, projectId)))
                .isInstanceOf(AccessDeniedToOrganizationException.class);
    }

    @Test
    void readingUnknownProjectReturnsNotFound() {
        assertThatThrownBy(() -> as(OWNER, () -> projectService.getProject(organizationId, UUID.randomUUID())))
                .isInstanceOf(ProjectNotFoundException.class);
    }

    @Test
    void projectOfAnotherOrganizationIsNotVisibleThroughOwnOrganization() {
        UUID otherOrganization = otherOrganizationId();
        UUID foreignProject = as(OUTSIDER, () -> projectService.createProject(otherOrganization, request("Foreign Project"))).id();

        assertThatThrownBy(() -> as(OWNER, () -> projectService.getProject(organizationId, foreignProject)))
                .isInstanceOf(ProjectNotFoundException.class);
    }

    @Test
    void listReturnsOnlyOwnOrganizationProjectsInCreationOrder() {
        as(OWNER, () -> projectService.createProject(organizationId, request("First Project")));
        as(OWNER, () -> projectService.createProject(organizationId, request("Second Project")));
        as(OUTSIDER, () -> projectService.createProject(otherOrganizationId(), request("Foreign Project")));

        List<ProjectResponse> projects = as(USER, () -> projectService.getProjects(organizationId, null, FIRST_PAGE)).content();

        assertThat(projects).extracting(ProjectResponse::title).containsExactly("First Project", "Second Project");
    }

    @Test
    void listCanBeFilteredByStatus() {
        UUID finished = as(OWNER, () -> projectService.createProject(organizationId, request("Finished Project"))).id();
        as(OWNER, () -> projectService.createProject(organizationId, request("Active Project")));
        as(OWNER, () -> projectService.changeStatus(organizationId, finished, new ChangeProjectStatusRequest(ProjectStatus.FINISHED)));

        assertThat(as(USER, () -> projectService.getProjects(organizationId, ProjectStatus.FINISHED, FIRST_PAGE)).content())
                .extracting(ProjectResponse::title).containsExactly("Finished Project");
        assertThat(as(USER, () -> projectService.getProjects(organizationId, ProjectStatus.ACTIVE, FIRST_PAGE)).content())
                .extracting(ProjectResponse::title).containsExactly("Active Project");
        assertThat(as(USER, () -> projectService.getProjects(organizationId, ProjectStatus.ARCHIVED, FIRST_PAGE)).content()).isEmpty();
    }

    @Test
    void outsiderCannotListProjects() {
        assertThatThrownBy(() -> as(OUTSIDER, () -> projectService.getProjects(organizationId, null, FIRST_PAGE)))
                .isInstanceOf(AccessDeniedToOrganizationException.class);
    }

    @Test
    void adminChangesStatus() {
        UUID projectId = as(OWNER, () -> projectService.createProject(organizationId, request("Backend Rewrite"))).id();

        ProjectResponse archived = as(ADMIN, () -> projectService.changeStatus(organizationId, projectId,
                new ChangeProjectStatusRequest(ProjectStatus.ARCHIVED)));

        assertThat(archived.status()).isEqualTo(ProjectStatus.ARCHIVED);
        assertThat(as(OWNER, () -> projectService.getProject(organizationId, projectId)).status()).isEqualTo(ProjectStatus.ARCHIVED);
    }

    @Test
    void settingSameStatusIsRejected() {
        UUID projectId = as(OWNER, () -> projectService.createProject(organizationId, request("Backend Rewrite"))).id();

        assertThatThrownBy(() -> as(OWNER, () -> projectService.changeStatus(organizationId, projectId,
                new ChangeProjectStatusRequest(ProjectStatus.ACTIVE))))
                .isInstanceOf(ProjectStatusAlreadyAssignedException.class)
                .hasMessage("Project already has status ACTIVE");
    }

    @Test
    void regularMemberCannotChangeStatus() {
        UUID projectId = as(OWNER, () -> projectService.createProject(organizationId, request("Backend Rewrite"))).id();

        assertThatThrownBy(() -> as(USER, () -> projectService.changeStatus(organizationId, projectId,
                new ChangeProjectStatusRequest(ProjectStatus.FINISHED))))
                .isInstanceOf(IllegalMemberOperationException.class);
        assertThat(as(OWNER, () -> projectService.getProject(organizationId, projectId)).status()).isEqualTo(ProjectStatus.ACTIVE);
    }

    @Test
    void ownerDeletesProject() {
        UUID projectId = as(OWNER, () -> projectService.createProject(organizationId, request("Backend Rewrite"))).id();

        as(OWNER, () -> {
            projectService.deleteProject(organizationId, projectId);
            return null;
        });

        assertThat(projectCount()).isZero();
    }

    @Test
    void adminCannotDeleteProject() {
        UUID projectId = as(OWNER, () -> projectService.createProject(organizationId, request("Backend Rewrite"))).id();

        assertThatThrownBy(() -> as(ADMIN, () -> {
            projectService.deleteProject(organizationId, projectId);
            return null;
        }))
                .isInstanceOf(IllegalMemberOperationException.class)
                .hasMessage("Only OWNER can perform this operation");
        assertThat(projectCount()).isEqualTo(1);
    }

    @Test
    void deletingProjectOfAnotherOrganizationReturnsNotFound() {
        UUID foreignProject = as(OUTSIDER, () -> projectService.createProject(otherOrganizationId(), request("Foreign Project"))).id();

        assertThatThrownBy(() -> as(OWNER, () -> {
            projectService.deleteProject(organizationId, foreignProject);
            return null;
        })).isInstanceOf(ProjectNotFoundException.class);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM projects WHERE id = ?", Integer.class, foreignProject))
                .isEqualTo(1);
    }

    @Test
    void titleOfDeletedProjectCanBeReused() {
        UUID projectId = as(OWNER, () -> projectService.createProject(organizationId, request("Backend Rewrite"))).id();
        as(OWNER, () -> {
            projectService.deleteProject(organizationId, projectId);
            return null;
        });

        assertThat(as(OWNER, () -> projectService.createProject(organizationId, request("Backend Rewrite"))).title())
                .isEqualTo("Backend Rewrite");
    }

    @Test
    void callWithoutAuthenticationIsRejectedBeforeTouchingData() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> projectService.createProject(organizationId, request("Anonymous Project")))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        assertThat(projectCount()).isZero();
    }

    private static <T> T as(String email, Supplier<T> action) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null, List.of()));
        try {
            return action.get();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private static CreateProjectRequest request(String title) {
        return new CreateProjectRequest(title, null);
    }
}

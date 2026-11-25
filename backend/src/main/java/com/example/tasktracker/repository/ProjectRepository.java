package com.example.tasktracker.repository;

import com.example.tasktracker.domain.Project;
import com.example.tasktracker.domain.enums.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {
    @Query("select count(p) > 0 from Project p where p.organization.id = :organizationId and lower(p.title) = lower(:title)")
    boolean existsByOrganizationIdAndTitleIgnoreCase(@Param("organizationId") UUID organizationId, @Param("title") String title);

    Optional<Project> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Page<Project> findAllByOrganizationId(UUID organizationId, Pageable pageable);

    Page<Project> findAllByOrganizationIdAndStatus(UUID organizationId, ProjectStatus status, Pageable pageable);

    List<Project> findAllByOrganizationId(UUID organizationId, Sort sort);
}

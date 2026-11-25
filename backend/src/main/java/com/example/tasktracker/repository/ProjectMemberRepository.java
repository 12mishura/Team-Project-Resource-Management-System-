package com.example.tasktracker.repository;

import com.example.tasktracker.domain.ProjectMember;
import com.example.tasktracker.repository.projection.ProjectMemberName;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {
    boolean existsByProjectIdAndOrganizationMemberId(UUID projectId, UUID organizationMemberId);

    Optional<ProjectMember> findByProjectIdAndOrganizationMemberId(UUID projectId, UUID organizationMemberId);

    @EntityGraph(attributePaths = {"organizationMember", "organizationMember.user"})
    Optional<ProjectMember> findByIdAndProjectId(UUID id, UUID projectId);

    @EntityGraph(attributePaths = {"organizationMember", "organizationMember.user"})
    Page<ProjectMember> findAllByProjectId(UUID projectId, Pageable pageable);

    @Query("""
            select pm.project.id as projectId, u.username as username, u.email as email
            from ProjectMember pm join pm.organizationMember om join om.user u
            where pm.project.organization.id = :organizationId
            order by pm.createdAt, pm.id""")
    List<ProjectMemberName> findMemberNamesByOrganizationId(@Param("organizationId") UUID organizationId);
}

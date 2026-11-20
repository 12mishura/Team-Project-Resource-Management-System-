package com.example.tasktracker.repository;

import com.example.tasktracker.domain.ProjectMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}

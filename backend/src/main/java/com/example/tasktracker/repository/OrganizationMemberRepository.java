package com.example.tasktracker.repository;

import java.util.List;
import com.example.tasktracker.domain.OrganizationMember;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, UUID> {
    boolean existsByOrganizationIdAndUserId(UUID organizationId, UUID userId);

    Optional<OrganizationMember> findByOrganizationIdAndUserEmail(UUID organizationId, String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from OrganizationMember m where m.organization.id = :organizationId and m.user.email = :email")
    Optional<OrganizationMember> findForUpdate(@Param("organizationId") UUID organizationId, @Param("email") String email);

    Optional<OrganizationMember> findByIdAndOrganizationId(UUID id, UUID organizationId);

    @EntityGraph(attributePaths = "user")
    Page<OrganizationMember> findAllByOrganizationId(UUID organizationId, Pageable pageable);

    @EntityGraph(attributePaths = "organization")
    Page<OrganizationMember> findAllByUserEmail(String email, Pageable pageable);

    long countByOrganizationId(UUID organizationId);

    @Query("""
            select m from OrganizationMember m
            join fetch m.user
            where m.organization.id = :organizationId
            order by m.createdAt, m.id
            """)
    List<OrganizationMember> findAllWithUsersByOrganizationId(@Param("organizationId") UUID organizationId);
}

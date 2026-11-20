package com.example.tasktracker.repository;

import com.example.tasktracker.domain.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
    boolean existsByInviteCode(String inviteCode);

    Optional<Organization> findByInviteCode(String inviteCode);

    @Query("select count(o) > 0 from Organization o where lower(o.title) = lower(:title)")
    boolean existsByTitleIgnoreCase(@Param("title") String title);
}

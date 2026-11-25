package com.example.tasktracker.repository;

import com.example.tasktracker.domain.WorkLog;
import com.example.tasktracker.repository.projection.WorkLogTotal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkLogRepository extends JpaRepository<WorkLog, UUID> {
    @EntityGraph(attributePaths = "author")
    Optional<WorkLog> findByIdAndIssueId(UUID id, UUID issueId);

    @EntityGraph(attributePaths = "author")
    Page<WorkLog> findAllByIssueId(UUID issueId, Pageable pageable);

    @Query("""
            select coalesce(sum(w.minutes), 0) from WorkLog w
            where w.author.id = :authorId and w.workDate = :workDate and w.id <> :excludedId
            """)
    long sumForDay(@Param("authorId") UUID authorId, @Param("workDate") LocalDate workDate, @Param("excludedId") UUID excludedId);

    @Query("""
            select w from WorkLog w
            join fetch w.issue i
            join fetch i.project p
            join fetch p.organization
            where w.author.email = :email and w.workDate between :from and :to
              and exists (select 1 from OrganizationMember m where m.organization = p.organization and m.user = w.author)
            order by w.workDate, w.createdAt
            """)
    List<WorkLog> findTimesheet(@Param("email") String email, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select w.author.id as userId, w.issue.project.id as projectId, w.workDate as workDate, sum(w.minutes) as total
            from WorkLog w
            where w.issue.project.id = :projectId and w.workDate between :from and :to
            group by w.author.id, w.issue.project.id, w.workDate
            """)
    List<WorkLogTotal> totalsByProject(@Param("projectId") UUID projectId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select w.author.id as userId, w.issue.project.id as projectId, w.workDate as workDate, sum(w.minutes) as total
            from WorkLog w
            where w.issue.project.organization.id = :organizationId and w.workDate between :from and :to
            group by w.author.id, w.issue.project.id, w.workDate
            """)
    List<WorkLogTotal> totalsByOrganization(@Param("organizationId") UUID organizationId,
                                            @Param("from") LocalDate from, @Param("to") LocalDate to);
}

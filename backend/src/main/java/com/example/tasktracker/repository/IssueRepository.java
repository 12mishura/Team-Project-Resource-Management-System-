package com.example.tasktracker.repository;

import com.example.tasktracker.domain.Issue;
import com.example.tasktracker.domain.enums.IssueStatus;
import com.example.tasktracker.repository.projection.ProjectCount;
import com.example.tasktracker.repository.projection.ProjectStatusCount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IssueRepository extends JpaRepository<Issue, UUID>, JpaSpecificationExecutor<Issue> {
    @EntityGraph(attributePaths = {"project", "reporter", "assignee", "assignee.organizationMember", "assignee.organizationMember.user"})
    Optional<Issue> findByIdAndProjectId(UUID id, UUID projectId);

    @Override
    @EntityGraph(attributePaths = {"project", "reporter", "assignee", "assignee.organizationMember", "assignee.organizationMember.user"})
    Page<Issue> findAll(Specification<Issue> specification, Pageable pageable);

    @Query("""
            select i.project.id as projectId, i.status as status, count(i) as total
            from Issue i
            where i.project.organization.id = :organizationId
            group by i.project.id, i.status""")
    List<ProjectStatusCount> countByProjectAndStatus(@Param("organizationId") UUID organizationId);

    @Query("""
            select b.project.id as projectId, count(b) as total
            from Bug b
            where b.project.organization.id = :organizationId and b.status <> :closed
            group by b.project.id""")
    List<ProjectCount> countOpenBugsByProject(@Param("organizationId") UUID organizationId, @Param("closed") IssueStatus closed);

    @Query("""
            select i.project.id as projectId, count(i) as total
            from Issue i
            where i.project.organization.id = :organizationId and i.status <> :closed
              and lower(i.assignee.organizationMember.user.email) = lower(:email)
            group by i.project.id""")
    List<ProjectCount> countOpenAssignedByProject(@Param("organizationId") UUID organizationId,
                                                   @Param("email") String email,
                                                   @Param("closed") IssueStatus closed);
}

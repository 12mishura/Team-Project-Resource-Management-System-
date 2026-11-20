package com.example.tasktracker.repository;

import com.example.tasktracker.domain.Issue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IssueRepository extends JpaRepository<Issue, UUID>, JpaSpecificationExecutor<Issue> {
    @EntityGraph(attributePaths = {"project", "reporter", "assignee", "assignee.organizationMember", "assignee.organizationMember.user"})
    Optional<Issue> findByIdAndProjectId(UUID id, UUID projectId);

    @Override
    @EntityGraph(attributePaths = {"project", "reporter", "assignee", "assignee.organizationMember", "assignee.organizationMember.user"})
    Page<Issue> findAll(Specification<Issue> specification, Pageable pageable);
}

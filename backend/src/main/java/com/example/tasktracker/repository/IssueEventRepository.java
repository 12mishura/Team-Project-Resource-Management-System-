package com.example.tasktracker.repository;

import com.example.tasktracker.domain.IssueEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface IssueEventRepository extends JpaRepository<IssueEvent, UUID> {
    @EntityGraph(attributePaths = "actor")
    Page<IssueEvent> findAllByIssueId(UUID issueId, Pageable pageable);

    @EntityGraph(attributePaths = {"actor", "issue"})
    Page<IssueEvent> findAllByIssueProjectId(UUID projectId, Pageable pageable);
}

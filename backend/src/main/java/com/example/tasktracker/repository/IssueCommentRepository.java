package com.example.tasktracker.repository;

import com.example.tasktracker.domain.IssueComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IssueCommentRepository extends JpaRepository<IssueComment, UUID> {
    @EntityGraph(attributePaths = "author")
    Optional<IssueComment> findByIdAndIssueId(UUID id, UUID issueId);

    @EntityGraph(attributePaths = "author")
    Page<IssueComment> findAllByIssueId(UUID issueId, Pageable pageable);
}

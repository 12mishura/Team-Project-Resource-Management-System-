package com.example.backend.repository;

import com.example.backend.entity.WorkItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkItemRepository extends JpaRepository<WorkItem, Long> {
    Optional<WorkItem> findByIdAndProjectId(Long id, Long projectId);
}
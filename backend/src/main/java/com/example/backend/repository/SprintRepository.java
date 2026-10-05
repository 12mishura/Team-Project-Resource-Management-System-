package com.example.backend.repository;

import com.example.backend.entity.Sprint;
import com.example.backend.entity.enums.SprintPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SprintRepository extends JpaRepository<Sprint, Long> {

    List<Sprint> findAllByProjectId(Long projectId);

    Optional<Sprint> findByIdAndProjectId(Long id, Long projectId);

    boolean existsByProjectIdAndStatus(Long projectId, SprintPlan status);
}
package com.example.backend.repository;

import com.example.backend.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Integer> {
    boolean existsByProjectIdAndUserId(Object projectId, Object userId);
}
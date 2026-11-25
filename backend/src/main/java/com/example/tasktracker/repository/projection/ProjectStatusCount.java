package com.example.tasktracker.repository.projection;

import com.example.tasktracker.domain.enums.IssueStatus;

import java.util.UUID;

public interface ProjectStatusCount {
    UUID getProjectId();

    IssueStatus getStatus();

    long getTotal();
}

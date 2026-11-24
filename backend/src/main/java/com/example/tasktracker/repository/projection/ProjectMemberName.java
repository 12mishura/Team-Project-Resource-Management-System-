package com.example.tasktracker.repository.projection;

import java.util.UUID;

public interface ProjectMemberName {
    UUID getProjectId();

    String getUsername();

    String getEmail();
}

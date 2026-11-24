package com.example.tasktracker.repository.projection;

import java.util.UUID;

public interface ProjectCount {
    UUID getProjectId();

    long getTotal();
}

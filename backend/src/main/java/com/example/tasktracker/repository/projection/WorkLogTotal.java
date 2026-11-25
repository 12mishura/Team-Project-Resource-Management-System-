package com.example.tasktracker.repository.projection;

import java.time.LocalDate;
import java.util.UUID;

public interface WorkLogTotal {
    UUID getUserId();
    UUID getProjectId();
    LocalDate getWorkDate();
    long getTotal();
}

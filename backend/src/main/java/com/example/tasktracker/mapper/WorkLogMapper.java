package com.example.tasktracker.mapper;

import com.example.tasktracker.domain.Issue;
import com.example.tasktracker.domain.Project;
import com.example.tasktracker.domain.WorkLog;
import com.example.tasktracker.dto.response.TimesheetEntryResponse;
import com.example.tasktracker.dto.response.WorkLogResponse;
import org.springframework.stereotype.Component;

@Component
public class WorkLogMapper {

    public WorkLogResponse toResponse(WorkLog workLog){
        return new WorkLogResponse(
                workLog.getId(),
                workLog.getIssue().getId(),
                workLog.getAuthor().getId(),
                workLog.getAuthor().getUsername(),
                workLog.getWorkDate(),
                workLog.getMinutes(),
                workLog.getComment(),
                workLog.getCreatedAt(),
                workLog.getUpdatedAt()
        );
    }

    public TimesheetEntryResponse toTimesheetEntry(WorkLog workLog){
        Issue issue = workLog.getIssue();
        Project project = issue.getProject();
        return new TimesheetEntryResponse(
                workLog.getId(),
                project.getOrganization().getId(),
                project.getOrganization().getTitle(),
                project.getId(),
                project.getTitle(),
                issue.getId(),
                issue.getTitle(),
                issue.getType(),
                issue.getStatus(),
                workLog.getWorkDate(),
                workLog.getMinutes(),
                workLog.getComment()
        );
    }
}

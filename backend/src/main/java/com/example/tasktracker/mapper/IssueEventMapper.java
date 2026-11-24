package com.example.tasktracker.mapper;

import com.example.tasktracker.domain.IssueEvent;
import com.example.tasktracker.dto.response.IssueEventResponse;
import com.example.tasktracker.dto.response.ProjectActivityResponse;
import org.springframework.stereotype.Component;

@Component
public class IssueEventMapper {

    public IssueEventResponse toResponse(IssueEvent event){
        return new IssueEventResponse(
                event.getId(),
                event.getType(),
                event.getField(),
                event.getOldValue(),
                event.getNewValue(),
                event.getActor().getId(),
                event.getActor().getUsername(),
                event.getCreatedAt()
        );
    }

    public ProjectActivityResponse toActivityResponse(IssueEvent event){
        return new ProjectActivityResponse(
                event.getId(),
                event.getIssue().getId(),
                event.getIssue().getTitle(),
                event.getIssue().getType(),
                event.getType(),
                event.getField(),
                event.getOldValue(),
                event.getNewValue(),
                event.getActor().getId(),
                event.getActor().getUsername(),
                event.getCreatedAt()
        );
    }
}

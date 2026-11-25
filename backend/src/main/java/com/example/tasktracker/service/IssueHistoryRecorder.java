package com.example.tasktracker.service;

import com.example.tasktracker.domain.Bug;
import com.example.tasktracker.domain.Issue;
import com.example.tasktracker.domain.IssueEvent;
import com.example.tasktracker.domain.Story;
import com.example.tasktracker.domain.User;
import com.example.tasktracker.domain.enums.IssueEventType;
import com.example.tasktracker.domain.enums.IssueType;
import com.example.tasktracker.repository.IssueEventRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Component
@AllArgsConstructor
public class IssueHistoryRecorder {
    private final IssueEventRepository eventRepository;

    public void recordCreated(Issue issue, User actor){
        save(issue, actor, new IssueEvent(IssueEventType.CREATED, null, null, issue.getType().name()));
    }

    public void recordChange(Issue issue, User actor, IssueEventType type, String field, Object oldValue, Object newValue){
        String before = text(oldValue);
        String after = text(newValue);
        if(!Objects.equals(before, after)){
            save(issue, actor, new IssueEvent(type, field, before, after));
        }
    }

    public void recordWorkLogged(Issue issue, User actor, int minutes){
        save(issue, actor, new IssueEvent(IssueEventType.WORK_LOGGED, "time", null, String.valueOf(minutes)));
    }

    public Map<String, String> snapshot(Issue issue){
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("title", issue.getTitle());
        fields.put("description", issue.getDescription());
        fields.put("priority", text(issue.getPriority()));
        if(issue.getType() != IssueType.EPIC){
            fields.put("estimate", text(issue.getEstimateMinutes()));
        }
        if(issue instanceof Bug bug){
            fields.put("severity", text(bug.getSeverity()));
            fields.put("stepsToReproduce", bug.getStepsToReproduce());
        }
        if(issue instanceof Story story){
            fields.put("storyPoints", text(story.getStoryPoints()));
        }
        return fields;
    }

    public void recordFieldChanges(Issue issue, User actor, Map<String, String> before, Map<String, String> after){
        after.forEach((field, newValue) ->
                recordChange(issue, actor, IssueEventType.FIELD_CHANGED, field, before.get(field), newValue));
    }

    private void save(Issue issue, User actor, IssueEvent event){
        event.setIssue(issue);
        event.setActor(actor);
        eventRepository.save(event);
    }

    private static String text(Object value){
        return value == null ? null : value.toString();
    }
}

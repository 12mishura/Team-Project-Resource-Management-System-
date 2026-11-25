package com.example.tasktracker.mapper;

import com.example.tasktracker.domain.Bug;
import com.example.tasktracker.domain.Epic;
import com.example.tasktracker.domain.Issue;
import com.example.tasktracker.domain.ProjectMember;
import com.example.tasktracker.domain.Story;
import com.example.tasktracker.domain.Task;
import com.example.tasktracker.dto.request.CreateIssueRequest;
import com.example.tasktracker.dto.request.UpdateIssueRequest;
import com.example.tasktracker.dto.response.IssueResponse;
import org.springframework.stereotype.Component;

@Component
public class IssueMapper {

    public IssueResponse toResponse(Issue issue){
        ProjectMember assignee = issue.getAssignee();
        Bug bug = issue instanceof Bug b ? b : null;
        Story story = issue instanceof Story s ? s : null;
        return new IssueResponse(
                issue.getId(),
                issue.getProject().getOrganization().getId(),
                issue.getProject().getId(),
                issue.getType(),
                issue.getTitle(),
                issue.getDescription(),
                issue.getStatus(),
                issue.getPriority(),
                issue.getReporter().getId(),
                issue.getReporter().getUsername(),
                assignee == null ? null : assignee.getId(),
                assignee == null ? null : assignee.getOrganizationMember().getUser().getUsername(),
                issue.getParent() == null ? null : issue.getParent().getId(),
                bug == null ? null : bug.getSeverity(),
                bug == null ? null : bug.getStepsToReproduce(),
                story == null ? null : story.getStoryPoints(),
                issue.getTotalEstimateMinutes(),
                issue.getSpentMinutes(),
                issue.getRemainingMinutes(),
                issue.getVersion(),
                issue.getCreatedAt(),
                issue.getUpdatedAt()
        );
    }

    public Issue toEntity(CreateIssueRequest request){
        Issue issue = switch (request.type()) {
            case EPIC -> new Epic(request.title(), request.description(), request.priority());
            case STORY -> new Story(request.title(), request.description(), request.priority(), request.storyPoints());
            case TASK -> new Task(request.title(), request.description(), request.priority());
            case BUG -> new Bug(request.title(), request.description(), request.priority(),
                    request.severity(), request.stepsToReproduce());
        };
        issue.setEstimateMinutes(request.estimateMinutes());
        return issue;
    }

    public Issue toUpdate(UpdateIssueRequest request, Issue issue){
        if(request.title() != null){
            issue.setTitle(request.title());
        }
        if(request.description() != null){
            issue.setDescription(request.description());
        }
        if(request.priority() != null){
            issue.setPriority(request.priority());
        }
        if(issue instanceof Bug bug){
            if(request.severity() != null){
                bug.setSeverity(request.severity());
            }
            if(request.stepsToReproduce() != null){
                bug.setStepsToReproduce(request.stepsToReproduce());
            }
        }
        if(request.estimateMinutes() != null){
            issue.setEstimateMinutes(request.estimateMinutes() == 0 ? null : request.estimateMinutes());
        }
        if(issue instanceof Story story && request.storyPoints() != null){
            story.setStoryPoints(request.storyPoints());
        }
        return issue;
    }
}

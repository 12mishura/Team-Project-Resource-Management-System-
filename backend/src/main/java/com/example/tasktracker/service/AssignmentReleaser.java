package com.example.tasktracker.service;

import com.example.tasktracker.domain.Issue;
import com.example.tasktracker.domain.OrganizationMember;
import com.example.tasktracker.domain.ProjectMember;
import com.example.tasktracker.domain.User;
import com.example.tasktracker.domain.enums.IssueEventType;
import com.example.tasktracker.repository.IssueRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@AllArgsConstructor
public class AssignmentReleaser {
    private final IssueRepository issueRepository;
    private final IssueHistoryRecorder historyRecorder;

    public void releaseProjectMember(ProjectMember member, User actor){
        release(issueRepository.findAllByAssigneeId(member.getId()), actor);
    }

    public void releaseOrganizationMember(OrganizationMember member, User actor){
        release(issueRepository.findAllByAssigneeOrganizationMemberId(member.getId()), actor);
    }

    private void release(List<Issue> issues, User actor){
        for(Issue issue : issues){
            String previous = issue.getAssignee().getOrganizationMember().getUser().getUsername();
            issue.setAssignee(null);
            historyRecorder.recordChange(issue, actor, IssueEventType.ASSIGNEE_CHANGED, "assignee", previous, null);
        }
    }
}

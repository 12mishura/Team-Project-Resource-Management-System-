package com.example.tasktracker.controller;

import com.example.tasktracker.dto.response.PageResponse;
import com.example.tasktracker.dto.response.ProjectActivityResponse;
import com.example.tasktracker.service.IssueService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/organizations/{organizationId}/projects/{projectId}/activity")
@AllArgsConstructor
public class ProjectActivityController {
    private final IssueService issueService;

    @GetMapping
    public PageResponse<ProjectActivityResponse> getActivity(@PathVariable UUID organizationId,
                                                             @PathVariable UUID projectId,
                                                             Pageable pageable,
                                                             @AuthenticationPrincipal UserDetails userDetails){
        return issueService.getProjectActivity(organizationId, projectId, pageable, userDetails.getUsername());
    }
}

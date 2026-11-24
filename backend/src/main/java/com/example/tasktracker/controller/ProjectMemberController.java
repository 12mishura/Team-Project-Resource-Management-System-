package com.example.tasktracker.controller;

import com.example.tasktracker.dto.request.CreateProjectMemberRequest;
import com.example.tasktracker.dto.request.UpdateProjectMemberRequest;
import com.example.tasktracker.dto.response.PageResponse;
import com.example.tasktracker.dto.response.ProjectMemberResponse;
import com.example.tasktracker.service.ProjectMemberService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/organizations/{organizationId}/projects/{projectId}/members")
@AllArgsConstructor
public class ProjectMemberController {
    private final ProjectMemberService projectMemberService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectMemberResponse addMember(@PathVariable UUID organizationId,
                                           @PathVariable UUID projectId,
                                           @Valid @RequestBody CreateProjectMemberRequest request,
                                           @AuthenticationPrincipal UserDetails userDetails){
        return projectMemberService.addMember(organizationId, projectId, request, userDetails.getUsername());
    }

    @GetMapping
    public PageResponse<ProjectMemberResponse> getMembers(@PathVariable UUID organizationId,
                                                          @PathVariable UUID projectId,
                                                          Pageable pageable){
        return projectMemberService.getMembers(organizationId, projectId, pageable);
    }

    @PatchMapping("/{projectMemberId}")
    public ProjectMemberResponse updateRole(@PathVariable UUID organizationId,
                                            @PathVariable UUID projectId,
                                            @PathVariable UUID projectMemberId,
                                            @Valid @RequestBody UpdateProjectMemberRequest request,
                                            @AuthenticationPrincipal UserDetails userDetails){
        return projectMemberService.updateRole(organizationId, projectId, projectMemberId, request, userDetails.getUsername());
    }

    @DeleteMapping("/{projectMemberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable UUID organizationId,
                             @PathVariable UUID projectId,
                             @PathVariable UUID projectMemberId,
                             @AuthenticationPrincipal UserDetails userDetails){
        projectMemberService.removeMember(organizationId, projectId, projectMemberId, userDetails.getUsername());
    }
}

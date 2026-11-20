package com.example.tasktracker.controller;

import com.example.tasktracker.domain.enums.IssueSortField;
import com.example.tasktracker.domain.enums.IssueStatus;
import com.example.tasktracker.domain.enums.IssueType;
import com.example.tasktracker.dto.request.AssignIssueRequest;
import com.example.tasktracker.dto.request.ChangeIssueParentRequest;
import com.example.tasktracker.dto.request.ChangeIssueStatusRequest;
import com.example.tasktracker.dto.request.CreateIssueRequest;
import com.example.tasktracker.dto.request.IssueFilter;
import com.example.tasktracker.dto.request.UpdateIssueRequest;
import com.example.tasktracker.dto.response.IssueEventResponse;
import com.example.tasktracker.dto.response.IssueResponse;
import com.example.tasktracker.dto.response.PageResponse;
import com.example.tasktracker.service.IssueService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/organizations/{organizationId}/projects/{projectId}/issues")
@AllArgsConstructor
public class IssueController {
    private final IssueService issueService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IssueResponse createIssue(@PathVariable UUID organizationId,
                                     @PathVariable UUID projectId,
                                     @Valid @RequestBody CreateIssueRequest request,
                                     @AuthenticationPrincipal UserDetails userDetails){
        return issueService.createIssue(organizationId, projectId, request, userDetails.getUsername());
    }

    @GetMapping
    public PageResponse<IssueResponse> getIssues(@PathVariable UUID organizationId,
                                                 @PathVariable UUID projectId,
                                                 @RequestParam(required = false) IssueType type,
                                                 @RequestParam(required = false) IssueStatus status,
                                                 @RequestParam(required = false) UUID assigneeId,
                                                 @RequestParam(required = false) UUID parentId,
                                                 @RequestParam(required = false) String q,
                                                 @RequestParam(required = false) IssueSortField sortBy,
                                                 @RequestParam(required = false) Sort.Direction direction,
                                                 Pageable pageable,
                                                 @AuthenticationPrincipal UserDetails userDetails){
        IssueFilter filter = new IssueFilter(type, status, assigneeId, parentId, q, sortBy, direction);
        return issueService.getIssues(organizationId, projectId, filter, pageable, userDetails.getUsername());
    }

    @GetMapping("/{issueId}")
    public IssueResponse getIssue(@PathVariable UUID organizationId,
                                  @PathVariable UUID projectId,
                                  @PathVariable UUID issueId,
                                  @AuthenticationPrincipal UserDetails userDetails){
        return issueService.getIssue(organizationId, projectId, issueId, userDetails.getUsername());
    }

    @GetMapping("/{issueId}/history")
    public PageResponse<IssueEventResponse> getHistory(@PathVariable UUID organizationId,
                                                       @PathVariable UUID projectId,
                                                       @PathVariable UUID issueId,
                                                       Pageable pageable,
                                                       @AuthenticationPrincipal UserDetails userDetails){
        return issueService.getHistory(organizationId, projectId, issueId, pageable, userDetails.getUsername());
    }

    @PatchMapping("/{issueId}")
    public IssueResponse updateIssue(@PathVariable UUID organizationId,
                                     @PathVariable UUID projectId,
                                     @PathVariable UUID issueId,
                                     @Valid @RequestBody UpdateIssueRequest request,
                                     @AuthenticationPrincipal UserDetails userDetails){
        return issueService.updateIssue(organizationId, projectId, issueId, request, userDetails.getUsername());
    }

    @PatchMapping("/{issueId}/status")
    public IssueResponse changeStatus(@PathVariable UUID organizationId,
                                      @PathVariable UUID projectId,
                                      @PathVariable UUID issueId,
                                      @Valid @RequestBody ChangeIssueStatusRequest request,
                                      @AuthenticationPrincipal UserDetails userDetails){
        return issueService.changeStatus(organizationId, projectId, issueId, request, userDetails.getUsername());
    }

    @PatchMapping("/{issueId}/assignee")
    public IssueResponse assignIssue(@PathVariable UUID organizationId,
                                     @PathVariable UUID projectId,
                                     @PathVariable UUID issueId,
                                     @Valid @RequestBody AssignIssueRequest request,
                                     @AuthenticationPrincipal UserDetails userDetails){
        return issueService.assignIssue(organizationId, projectId, issueId, request, userDetails.getUsername());
    }

    @DeleteMapping("/{issueId}/assignee")
    public IssueResponse unassignIssue(@PathVariable UUID organizationId,
                                       @PathVariable UUID projectId,
                                       @PathVariable UUID issueId,
                                       @AuthenticationPrincipal UserDetails userDetails){
        return issueService.unassignIssue(organizationId, projectId, issueId, userDetails.getUsername());
    }

    @PatchMapping("/{issueId}/parent")
    public IssueResponse changeParent(@PathVariable UUID organizationId,
                                      @PathVariable UUID projectId,
                                      @PathVariable UUID issueId,
                                      @Valid @RequestBody ChangeIssueParentRequest request,
                                      @AuthenticationPrincipal UserDetails userDetails){
        return issueService.changeParent(organizationId, projectId, issueId, request, userDetails.getUsername());
    }

    @DeleteMapping("/{issueId}/parent")
    public IssueResponse detachFromEpic(@PathVariable UUID organizationId,
                                        @PathVariable UUID projectId,
                                        @PathVariable UUID issueId,
                                        @AuthenticationPrincipal UserDetails userDetails){
        return issueService.detachFromEpic(organizationId, projectId, issueId, userDetails.getUsername());
    }

    @DeleteMapping("/{issueId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteIssue(@PathVariable UUID organizationId,
                            @PathVariable UUID projectId,
                            @PathVariable UUID issueId,
                            @AuthenticationPrincipal UserDetails userDetails){
        issueService.deleteIssue(organizationId, projectId, issueId, userDetails.getUsername());
    }
}

package com.example.tasktracker.controller;

import com.example.tasktracker.dto.request.CreateWorkLogRequest;
import com.example.tasktracker.dto.request.UpdateWorkLogRequest;
import com.example.tasktracker.dto.response.PageResponse;
import com.example.tasktracker.dto.response.WorkLogResponse;
import com.example.tasktracker.service.WorkLogService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/organizations/{organizationId}/projects/{projectId}/issues/{issueId}/worklogs")
@AllArgsConstructor
public class WorkLogController {
    private final WorkLogService workLogService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkLogResponse addWorkLog(@PathVariable UUID organizationId,
                                      @PathVariable UUID projectId,
                                      @PathVariable UUID issueId,
                                      @Valid @RequestBody CreateWorkLogRequest request,
                                      @AuthenticationPrincipal UserDetails userDetails){
        return workLogService.addWorkLog(organizationId, projectId, issueId, request, userDetails.getUsername());
    }

    @GetMapping
    public PageResponse<WorkLogResponse> getWorkLogs(@PathVariable UUID organizationId,
                                                     @PathVariable UUID projectId,
                                                     @PathVariable UUID issueId,
                                                     Pageable pageable,
                                                     @AuthenticationPrincipal UserDetails userDetails){
        return workLogService.getWorkLogs(organizationId, projectId, issueId, pageable, userDetails.getUsername());
    }

    @PatchMapping("/{workLogId}")
    public WorkLogResponse updateWorkLog(@PathVariable UUID organizationId,
                                         @PathVariable UUID projectId,
                                         @PathVariable UUID issueId,
                                         @PathVariable UUID workLogId,
                                         @Valid @RequestBody UpdateWorkLogRequest request,
                                         @AuthenticationPrincipal UserDetails userDetails){
        return workLogService.updateWorkLog(organizationId, projectId, issueId, workLogId, request, userDetails.getUsername());
    }

    @DeleteMapping("/{workLogId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWorkLog(@PathVariable UUID organizationId,
                              @PathVariable UUID projectId,
                              @PathVariable UUID issueId,
                              @PathVariable UUID workLogId,
                              @AuthenticationPrincipal UserDetails userDetails){
        workLogService.deleteWorkLog(organizationId, projectId, issueId, workLogId, userDetails.getUsername());
    }
}

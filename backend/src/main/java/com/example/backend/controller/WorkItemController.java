package com.example.backend.controller;

import com.example.backend.dto.WorkItemAssigneeUpdateRequest;
import com.example.backend.dto.WorkItemResponse;
import com.example.backend.dto.WorkItemStatusUpdateRequest;
import com.example.backend.service.WorkItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/work-items")
@RequiredArgsConstructor
public class WorkItemController {

    private final WorkItemService workItemService;

    @PatchMapping("/{id}/status")
    public ResponseEntity<WorkItemResponse> updateStatus(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @Valid @RequestBody WorkItemStatusUpdateRequest request
    ) {
        WorkItemResponse response = workItemService.updateStatus(projectId, id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/assignee")
    public ResponseEntity<WorkItemResponse> updateAssignee(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @RequestBody WorkItemAssigneeUpdateRequest request
    ) {
        WorkItemResponse response = workItemService.updateAssignee(projectId, id, request);
        return ResponseEntity.ok(response);
    }
}
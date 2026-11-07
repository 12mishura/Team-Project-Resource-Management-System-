package com.example.backend.service;

import com.example.backend.dto.WorkItemAssigneeUpdateRequest;
import com.example.backend.dto.WorkItemResponse;
import com.example.backend.dto.WorkItemStatusUpdateRequest;

public interface WorkItemService {
    WorkItemResponse updateStatus(Long projectId, Long workItemId, WorkItemStatusUpdateRequest request);

    WorkItemResponse updateAssignee(Long projectId, Long workItemId, WorkItemAssigneeUpdateRequest request);
}
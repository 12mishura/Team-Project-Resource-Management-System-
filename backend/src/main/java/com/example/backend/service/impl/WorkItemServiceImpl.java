package com.example.backend.service.impl;

import com.example.backend.dto.WorkItemAssigneeUpdateRequest;
import com.example.backend.dto.WorkItemResponse;
import com.example.backend.dto.WorkItemStatusUpdateRequest;
import com.example.backend.entity.WorkItem;
import com.example.backend.mapper.WorkItemMapper;
import com.example.backend.repository.WorkItemRepository;
import com.example.backend.service.WorkItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class WorkItemServiceImpl implements WorkItemService {

    private final WorkItemRepository workItemRepository;
    private final WorkItemMapper workItemMapper;

    @Override
    @Transactional
    public WorkItemResponse updateStatus(Long projectId, Long workItemId, WorkItemStatusUpdateRequest request) {
        if (request == null || request.status() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status is required");
        }

        WorkItem workItem = getWorkItemInProject(projectId, workItemId);
        workItem.setStatus(request.status());

        return workItemMapper.toResponse(workItem);
    }

    @Override
    @Transactional
    public WorkItemResponse updateAssignee(Long projectId, Long workItemId, WorkItemAssigneeUpdateRequest request) {
        WorkItem workItem = getWorkItemInProject(projectId, workItemId);

        Long assigneeId = request != null ? request.assigneeId() : null;
        validateAssigneeMembership(projectId, assigneeId);

        workItem.setTaskAssigneeId(assigneeId);

        return workItemMapper.toResponse(workItem);
    }

    private WorkItem getWorkItemInProject(Long projectId, Long workItemId) {
        return workItemRepository.findByIdAndProjectId(workItemId, projectId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "WorkItem with id: " + workItemId + " not found in project: " + projectId
                ));
    }

    private void validateAssigneeMembership(Long projectId, Long assigneeId) {
        if (assigneeId == null) {
            return;
        }

        // TODO: Connect validation via ProjectMemberRepository
    }
}
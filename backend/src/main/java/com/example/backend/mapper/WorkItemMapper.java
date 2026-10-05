package com.example.backend.mapper;

import com.example.backend.dto.WorkItemRequest;
import com.example.backend.dto.WorkItemResponse;
import com.example.backend.entity.WorkItem;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WorkItemMapper {
    public WorkItemResponse toResponse(WorkItem workItem){
        return new WorkItemResponse(
                workItem.getId(),
                workItem.getTitle(),
                workItem.getDescription(),
                workItem.getType(),
                workItem.getStatus(),
                workItem.getPriority(),
                workItem.getEstimatedHours(),
                workItem.getDueDate(),
                workItem.getCreatedAt(),
                workItem.getUpdatedAt(),
                workItem.getProjectId(),
                workItem.getSprintId(),
                workItem.getParent() == null ? null : workItem.getParent().getId(),
                workItem.getTaskCreatorId(),
                workItem.getTaskAssigneeId()
        );
    }

    public List<WorkItemResponse> toListResponse(List<WorkItem> workItemList){
        return workItemList.stream().map(this::toResponse).toList();
    }

    public WorkItem toEntity(WorkItemRequest request){
        WorkItem workItem = new WorkItem();

        workItem.setTitle(request.title());
        workItem.setDescription(request.description());
        workItem.setType(request.type());
        workItem.setPriority(request.priority());
        workItem.setEstimatedHours(request.estimatedHours());
        workItem.setDueDate(request.dueDate());
        if (request.parentId() != null) {
            // Reference an existing parent; do not cascade creation of a new work item.
            WorkItem parent = new WorkItem();
            parent.setId(request.parentId());
            workItem.setParent(parent);
        }
        return workItem;
    }
}

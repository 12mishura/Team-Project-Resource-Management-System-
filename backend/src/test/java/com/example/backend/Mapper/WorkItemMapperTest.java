package com.example.backend.Mapper;

import com.example.backend.DTO.WorkItemRequest;
import com.example.backend.DTO.WorkItemResponse;
import com.example.backend.Entity.Enum.WorkItemPriority;
import com.example.backend.Entity.Enum.WorkItemStatus;
import com.example.backend.Entity.Enum.WorkItemType;
import com.example.backend.Entity.WorkItem;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WorkItemMapperTest {
    private final WorkItemMapper mapper = new WorkItemMapper();

    @Test
    void mapsCreationFieldsAndPreservesServerManagedDefaults() {
        LocalDate dueDate = LocalDate.of(2026, 10, 1);
        WorkItem item = mapper.toEntity(new WorkItemRequest(
                "Fix mapper", "Description", WorkItemType.BUG,
                WorkItemPriority.HIGH, 3, dueDate, 42L));

        assertEquals("Fix mapper", item.getTitle());
        assertEquals("Description", item.getDescription());
        assertEquals(WorkItemType.BUG, item.getType());
        assertEquals(WorkItemPriority.HIGH, item.getPriority());
        assertEquals(3, item.getEstimatedHours());
        assertEquals(dueDate, item.getDueDate());
        assertEquals(42L, item.getParent().getId());
        assertEquals(WorkItemStatus.TO_DO, item.getStatus());
        assertNull(item.getId());
        assertNull(item.getCreatedAt());
        assertNull(item.getUpdatedAt());
    }

    @Test
    void handlesMissingOptionalRelationships() {
        WorkItem item = mapper.toEntity(new WorkItemRequest(
                "Task", null, WorkItemType.TASK, WorkItemPriority.LOW,
                null, null, null));
        assertNull(item.getParent());

        WorkItemResponse response = mapper.toResponse(item);
        assertNull(response.projectId());
        assertNull(response.sprintId());
        assertNull(response.parentId());
        assertNull(response.taskCreatorId());
        assertNull(response.taskAssigneeId());
        assertEquals(List.of(response), mapper.toListResponse(List.of(item)));
        assertTrue(mapper.toListResponse(List.of()).isEmpty());
    }

    @Test
    void keepsEveryResponseFieldInItsCorrectPosition() {
        LocalDate dueDate = LocalDate.of(2026, 10, 1);
        LocalDateTime createdAt = dueDate.minusDays(2).atStartOfDay();
        LocalDateTime updatedAt = createdAt.plusHours(1);
        WorkItem item = mapper.toEntity(new WorkItemRequest(
                "Bug", "Details", WorkItemType.BUG, WorkItemPriority.URGENT,
                8, dueDate, 30L));
        item.setId(1L);
        item.setStatus(WorkItemStatus.IN_PROGRESS);
        item.setCreatedAt(createdAt);
        item.setUpdatedAt(updatedAt);
        item.setProjectId(10L);
        item.setSprintId(20L);
        item.setTaskCreatorId(40L);
        item.setTaskAssigneeId(50L);

        assertEquals(new WorkItemResponse(1L, "Bug", "Details", WorkItemType.BUG,
                WorkItemStatus.IN_PROGRESS, WorkItemPriority.URGENT, 8, dueDate,
                createdAt, updatedAt, 10L, 20L, 30L, 40L, 50L), mapper.toResponse(item));
    }
}

package com.example.backend.service;

import com.example.backend.dto.WorkItemAssigneeUpdateRequest;
import com.example.backend.dto.WorkItemResponse;
import com.example.backend.dto.WorkItemStatusUpdateRequest;
import com.example.backend.entity.WorkItem;
import com.example.backend.entity.enums.WorkItemPriority;
import com.example.backend.entity.enums.WorkItemStatus;
import com.example.backend.entity.enums.WorkItemType;
import com.example.backend.mapper.WorkItemMapper;
import com.example.backend.repository.WorkItemRepository;
import com.example.backend.service.impl.WorkItemServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkItemServiceTest {

    @Mock
    private WorkItemRepository workItemRepository;

    @Mock
    private WorkItemMapper workItemMapper;

    @InjectMocks
    private WorkItemServiceImpl workItemService;

    private WorkItem workItem;
    private WorkItemResponse workItemResponse;

    @BeforeEach
    void setUp() {
        workItem = WorkItem.builder()
                .id(1L)
                .title("Task 1")
                .status(WorkItemStatus.TO_DO)
                .priority(WorkItemPriority.MEDIUM)
                .type(WorkItemType.TASK)
                .projectId(10L)
                .taskAssigneeId(5L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        workItemResponse = new WorkItemResponse(
                1L, "Task 1", null, WorkItemStatus.IN_PROGRESS,
                WorkItemPriority.MEDIUM, WorkItemType.TASK,
                10L, null, 5L, null, LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Test
    void updateStatus_success() {
        WorkItemStatusUpdateRequest request = new WorkItemStatusUpdateRequest(WorkItemStatus.IN_PROGRESS);
        when(workItemRepository.findByIdAndProjectId(1L, 10L)).thenReturn(Optional.of(workItem));
        when(workItemMapper.toResponse(workItem)).thenReturn(workItemResponse);

        WorkItemResponse result = workItemService.updateStatus(10L, 1L, request);

        assertNotNull(result);
        assertEquals(WorkItemStatus.IN_PROGRESS, workItem.getStatus());
        verify(workItemRepository).findByIdAndProjectId(1L, 10L);
    }

    @Test
    void updateStatus_throwsNotFound_whenWorkItemDoesNotExist() {
        WorkItemStatusUpdateRequest request = new WorkItemStatusUpdateRequest(WorkItemStatus.DONE);
        when(workItemRepository.findByIdAndProjectId(99L, 10L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () ->
                workItemService.updateStatus(10L, 99L, request)
        );
    }

    @Test
    void updateStatus_throwsBadRequest_whenStatusIsNull() {
        WorkItemStatusUpdateRequest request = new WorkItemStatusUpdateRequest(null);

        assertThrows(ResponseStatusException.class, () ->
                workItemService.updateStatus(10L, 1L, request)
        );
    }

    @Test
    void updateAssignee_success_changeAssignee() {
        WorkItemAssigneeUpdateRequest request = new WorkItemAssigneeUpdateRequest(42L);
        when(workItemRepository.findByIdAndProjectId(1L, 10L)).thenReturn(Optional.of(workItem));
        when(workItemMapper.toResponse(workItem)).thenReturn(workItemResponse);

        WorkItemResponse result = workItemService.updateAssignee(10L, 1L, request);

        assertNotNull(result);
        assertEquals(42L, workItem.getTaskAssigneeId());
    }

    @Test
    void updateAssignee_success_unassign() {
        WorkItemAssigneeUpdateRequest request = new WorkItemAssigneeUpdateRequest(null);
        when(workItemRepository.findByIdAndProjectId(1L, 10L)).thenReturn(Optional.of(workItem));
        when(workItemMapper.toResponse(workItem)).thenReturn(workItemResponse);

        WorkItemResponse result = workItemService.updateAssignee(10L, 1L, request);

        assertNotNull(result);
        assertNull(workItem.getTaskAssigneeId());
    }
}
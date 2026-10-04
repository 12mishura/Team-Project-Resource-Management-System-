package com.example.backend.Entity;

import com.example.backend.Entity.Enum.WorkItemPriority;
import com.example.backend.Entity.Enum.WorkItemStatus;
import com.example.backend.Entity.Enum.WorkItemType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Setter @Getter
@NoArgsConstructor
public class WorkItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id ;

    @Column(nullable = false , length = 100)
    private String title;
    private String description;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private WorkItemType type;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private WorkItemStatus status = WorkItemStatus.TO_DO;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private WorkItemPriority priority;

    @Column(name = "estimated_hours")
    private Integer estimatedHours;

    private LocalDate dueDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Store external references as IDs until their entities are implemented.
    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "sprint_id")
    private Long sprintId;

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private WorkItem parent;

    @Column(name = "created_by_user_id")
    private Long taskCreatorId;

    @Column(name = "assignee_user_id")
    private Long taskAssigneeId;
}

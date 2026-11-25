package com.example.tasktracker.domain;

import com.example.tasktracker.domain.enums.ProjectStatus;
import com.example.tasktracker.exception.ProjectStatusAlreadyAssignedException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "projects")
@Getter @Setter
@NoArgsConstructor
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "title", nullable = false)
    private String title;
    @Column(name = "description")
    private String description;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Setter(AccessLevel.NONE)
    private ProjectStatus status = ProjectStatus.ACTIVE;

    @CreationTimestamp
    @Setter(AccessLevel.NONE)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @UpdateTimestamp
    @Setter(AccessLevel.NONE)
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    public Project(String title, String description){
        this.title = title;
        this.description = description;
    }

    public void changeStatus(ProjectStatus status){
        if(this.status == status){
            throw new ProjectStatusAlreadyAssignedException(status);
        }
        this.status = status;
    }
}

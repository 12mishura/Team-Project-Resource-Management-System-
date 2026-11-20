package com.example.tasktracker.domain;

import com.example.tasktracker.domain.enums.ProjectMemberRole;
import com.example.tasktracker.exception.ProjectMemberRoleAlreadyAssignedException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "project_members",
        uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "organization_member_id"}))
@Getter @Setter
@NoArgsConstructor
public class ProjectMember {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(value = EnumType.STRING)
    @Setter(AccessLevel.NONE)
    @Column(name = "role", nullable = false)
    private ProjectMemberRole role;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_member_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private OrganizationMember organizationMember;

    @CreationTimestamp
    @Setter(AccessLevel.NONE)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @UpdateTimestamp
    @Setter(AccessLevel.NONE)
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ProjectMember(ProjectMemberRole role){
        this.role = role;
    }

    public void changeRole(ProjectMemberRole newRole){
        if(role == newRole){
            throw new ProjectMemberRoleAlreadyAssignedException(newRole);
        }
        role = newRole;
    }
}

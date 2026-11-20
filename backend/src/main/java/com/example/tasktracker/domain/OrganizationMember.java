package com.example.tasktracker.domain;

import com.example.tasktracker.domain.enums.OrganizationRole;
import com.example.tasktracker.exception.MemberRoleAlreadyAssignedException;
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
@Table(name = "organization_members",
        uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id", "user_id"}))
@Getter @Setter
@NoArgsConstructor
public class OrganizationMember {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(value = EnumType.STRING)
    @Setter(AccessLevel.NONE)
    @Column(name = "role", nullable = false)
    private OrganizationRole role;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Setter(AccessLevel.NONE)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @UpdateTimestamp
    @Setter(AccessLevel.NONE)
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public OrganizationMember(OrganizationRole role){
        this.role = role;
    }

    public void changeRole(OrganizationRole newRole){
        if(role == newRole){
            throw new MemberRoleAlreadyAssignedException(newRole);
        }
        role = newRole;
    }
}

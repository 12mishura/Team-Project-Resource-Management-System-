package com.example.tasktracker.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "organization")
@Getter @Setter
@NoArgsConstructor
public class Organization {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "title", nullable = false, unique = true)
    private String title;
    @Column(name = "description")
    private String description;

    @Column(name = "invite_code", nullable = false, unique = true)
    @Setter(AccessLevel.NONE)
    private String inviteCode;

    @CreationTimestamp
    @Setter(AccessLevel.NONE)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @UpdateTimestamp
    @Setter(AccessLevel.NONE)
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "organization", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<OrganizationMember> members = new HashSet<>();

    @OneToMany(mappedBy = "organization", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Project> projects = new HashSet<>();

    public Organization(String title, String description, String inviteCode){
        this.title = title;
        this.description = description;
        this.inviteCode = inviteCode;
    }

    public void changeInviteCode(String newInviteCode){
        this.inviteCode = newInviteCode;
    }

    public void addMember(OrganizationMember member){
        members.add(member);
        member.setOrganization(this);
    }

    public void removeMember(OrganizationMember member){
        members.remove(member);
        member.setOrganization(null);
    }

    public void addProject(Project project){
        projects.add(project);
        project.setOrganization(this);
    }

    public void removeProject(Project project){
        projects.remove(project);
        project.setOrganization(null);
    }
}

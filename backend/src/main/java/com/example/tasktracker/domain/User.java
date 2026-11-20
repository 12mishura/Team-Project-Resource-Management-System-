package com.example.tasktracker.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter @Setter
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;
    @Column(name = "username", nullable = false, unique = true)
    private String username;
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Setter(AccessLevel.NONE)
    @Column(name = "token_version", nullable = false)
    private int tokenVersion;

    @CreationTimestamp
    @Setter(AccessLevel.NONE)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @UpdateTimestamp
    @Setter(AccessLevel.NONE)
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<OrganizationMember> members = new HashSet<>();

    public User(String email, String username, String passwordHash){
        this.email = email;
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public void addMember(OrganizationMember member){
        members.add(member);
        member.setUser(this);
    }

    public void deleteMember(OrganizationMember member){
        members.remove(member);
        member.setUser(null);
    }

    public void changePassword(String newPasswordHash){
        this.passwordHash = newPasswordHash;
        this.tokenVersion++;
    }
}

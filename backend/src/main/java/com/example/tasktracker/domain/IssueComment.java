package com.example.tasktracker.domain;

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
@Table(name = "issue_comments")
@Getter @Setter
@NoArgsConstructor
public class IssueComment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issue_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Issue issue;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Setter(AccessLevel.NONE)
    @Column(name = "text", nullable = false)
    private String text;

    @Setter(AccessLevel.NONE)
    @Column(name = "edited_at")
    private Instant editedAt;

    @CreationTimestamp
    @Setter(AccessLevel.NONE)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @UpdateTimestamp
    @Setter(AccessLevel.NONE)
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public IssueComment(String text){
        this.text = text;
    }

    public void edit(String newText){
        this.text = newText;
        this.editedAt = Instant.now();
    }
}

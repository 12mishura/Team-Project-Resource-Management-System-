package com.example.tasktracker.domain;

import com.example.tasktracker.domain.enums.IssueEventType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "issue_events")
@Getter @Setter
@NoArgsConstructor
public class IssueEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issue_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Issue issue;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id", nullable = false)
    private User actor;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "type", nullable = false)
    private IssueEventType type;

    @Column(name = "field")
    private String field;
    @Column(name = "old_value")
    private String oldValue;
    @Column(name = "new_value")
    private String newValue;

    @CreationTimestamp
    @Setter(AccessLevel.NONE)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public IssueEvent(IssueEventType type, String field, String oldValue, String newValue){
        this.type = type;
        this.field = field;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }
}

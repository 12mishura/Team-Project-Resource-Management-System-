package com.example.tasktracker.domain;

import com.example.tasktracker.domain.enums.IssuePriority;
import com.example.tasktracker.domain.enums.IssueStatus;
import com.example.tasktracker.domain.enums.IssueType;
import com.example.tasktracker.exception.IllegalStatusTransitionException;
import com.example.tasktracker.exception.InvalidIssueException;
import com.example.tasktracker.exception.IssueStatusAlreadyAssignedException;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "issues")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type", length = 20)
@Getter @Setter
@NoArgsConstructor
public abstract class Issue {
    private static final Map<IssueStatus, Set<IssueStatus>> ALLOWED_TRANSITIONS = Map.of(
            IssueStatus.TODO, Set.of(IssueStatus.IN_PROGRESS),
            IssueStatus.IN_PROGRESS, Set.of(IssueStatus.TODO, IssueStatus.IN_REVIEW),
            IssueStatus.IN_REVIEW, Set.of(IssueStatus.IN_PROGRESS, IssueStatus.DONE),
            IssueStatus.DONE, Set.of(IssueStatus.IN_PROGRESS)
    );

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "title", nullable = false)
    private String title;
    @Column(name = "description")
    private String description;

    @Enumerated(value = EnumType.STRING)
    @Setter(AccessLevel.NONE)
    @Column(name = "status", nullable = false)
    private IssueStatus status = IssueStatus.TODO;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private IssuePriority priority = IssuePriority.MEDIUM;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private ProjectMember assignee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @Setter(AccessLevel.NONE)
    private Issue parent;

    @Version
    @Setter(AccessLevel.NONE)
    @Column(name = "version", nullable = false)
    private Long version;

    @CreationTimestamp
    @Setter(AccessLevel.NONE)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @UpdateTimestamp
    @Setter(AccessLevel.NONE)
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Issue(String title, String description, IssuePriority priority){
        this.title = title;
        this.description = description;
        if(priority != null){
            this.priority = priority;
        }
    }

    public abstract IssueType getType();

    public boolean canHaveParent(){
        return true;
    }

    public void changeStatus(IssueStatus newStatus){
        if(status == newStatus){
            throw new IssueStatusAlreadyAssignedException(newStatus);
        }
        if(!ALLOWED_TRANSITIONS.get(status).contains(newStatus)){
            throw new IllegalStatusTransitionException(status, newStatus);
        }
        status = newStatus;
    }

    public void moveTo(Issue newParent){
        if(newParent == null){
            parent = null;
            return;
        }
        if(!canHaveParent()){
            throw new InvalidIssueException(getType() + " cannot have a parent");
        }
        if(newParent == this || (id != null && id.equals(newParent.getId()))){
            throw new InvalidIssueException("Issue cannot be its own parent");
        }
        if(newParent.getType() != IssueType.EPIC){
            throw new InvalidIssueException("Only EPIC can be a parent");
        }
        if(!project.getId().equals(newParent.getProject().getId())){
            throw new InvalidIssueException("Parent must belong to the same project");
        }
        parent = newParent;
    }
}

package com.example.tasktracker.repository;

import com.example.tasktracker.domain.Bug;
import com.example.tasktracker.domain.Epic;
import com.example.tasktracker.domain.Issue;
import com.example.tasktracker.domain.Story;
import com.example.tasktracker.domain.Task;
import com.example.tasktracker.domain.enums.IssuePriority;
import com.example.tasktracker.domain.enums.IssueSortField;
import com.example.tasktracker.domain.enums.IssueStatus;
import com.example.tasktracker.domain.enums.IssueType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class IssueSpecifications {

    private IssueSpecifications() {
    }

    public static Specification<Issue> inProject(UUID projectId){
        return (root, query, cb) -> cb.equal(root.get("project").get("id"), projectId);
    }

    public static Specification<Issue> hasType(IssueType type){
        Class<? extends Issue> entityClass = switch (type) {
            case EPIC -> Epic.class;
            case STORY -> Story.class;
            case TASK -> Task.class;
            case BUG -> Bug.class;
        };
        return (root, query, cb) -> cb.equal(root.type(), entityClass);
    }

    public static Specification<Issue> hasStatus(IssueStatus status){
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Issue> assignedTo(UUID projectMemberId){
        return (root, query, cb) -> cb.equal(root.get("assignee").get("id"), projectMemberId);
    }

    public static Specification<Issue> assignedToUser(String email){
        return (root, query, cb) -> cb.equal(
                root.join("assignee").join("organizationMember").join("user").get("email"), email);
    }

    public static Specification<Issue> withParent(UUID parentId){
        return (root, query, cb) -> cb.equal(root.get("parent").get("id"), parentId);
    }

    public static Specification<Issue> titleContains(String text){
        String pattern = "%" + text.toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_") + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("title")), pattern, '\\');
    }

    public static Specification<Issue> orderedBy(IssueSortField field, Sort.Direction direction){
        return (root, query, cb) -> {
            if(query.getResultType() != Long.class && query.getResultType() != long.class){
                Expression<?> key = sortKey(field, root, cb);
                Order primary = direction == Sort.Direction.DESC ? cb.desc(key) : cb.asc(key);
                query.orderBy(List.of(primary, cb.asc(root.get("createdAt")), cb.asc(root.get("id"))));
            }
            return null;
        };
    }

    private static Expression<?> sortKey(IssueSortField field, Root<Issue> root, CriteriaBuilder cb){
        return switch (field) {
            case CREATED_AT -> root.get("createdAt");
            case UPDATED_AT -> root.get("updatedAt");
            case PRIORITY -> cb.<Integer>selectCase()
                    .when(cb.equal(root.get("priority"), IssuePriority.LOW), 1)
                    .when(cb.equal(root.get("priority"), IssuePriority.MEDIUM), 2)
                    .when(cb.equal(root.get("priority"), IssuePriority.HIGH), 3)
                    .otherwise(4);
            case STATUS -> cb.<Integer>selectCase()
                    .when(cb.equal(root.get("status"), IssueStatus.TODO), 1)
                    .when(cb.equal(root.get("status"), IssueStatus.IN_PROGRESS), 2)
                    .when(cb.equal(root.get("status"), IssueStatus.IN_REVIEW), 3)
                    .otherwise(4);
        };
    }
}

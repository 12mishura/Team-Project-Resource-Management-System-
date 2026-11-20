package com.example.tasktracker.exception;

import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.example.tasktracker.domain.enums.IssueStatus;
import com.example.tasktracker.domain.enums.ProjectMemberRole;
import com.example.tasktracker.domain.enums.ProjectStatus;
import org.springframework.web.method.annotation.ExceptionHandlerMethodResolver;

import java.sql.SQLException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void unexpectedExceptionBecomesInternalServerErrorWithoutLeakingMessage() {
        ProblemDetail problem = handler.handleUnexpected(new IllegalStateException("database password is 12345"));

        assertThat(problem.getStatus()).isEqualTo(500);
        assertThat(problem.getDetail()).isEqualTo("Internal server error");
    }

    @Test
    void uniqueViolationBecomesConflict() {
        ProblemDetail problem = handler.handleDataIntegrity(violation(ConstraintKind.UNIQUE));

        assertThat(problem.getStatus()).isEqualTo(409);
        assertThat(problem.getDetail()).isEqualTo("Resource already exists");
    }

    @Test
    void foreignKeyViolationBecomesConflictWithOwnMessage() {
        ProblemDetail problem = handler.handleDataIntegrity(violation(ConstraintKind.FOREIGN_KEY));

        assertThat(problem.getStatus()).isEqualTo(409);
        assertThat(problem.getDetail()).isEqualTo("Operation conflicts with related data");
    }

    @Test
    void checkViolationBecomesBadRequest() {
        ProblemDetail problem = handler.handleDataIntegrity(violation(ConstraintKind.CHECK));

        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getDetail()).isEqualTo("Request violates data constraints");
    }

    @Test
    void notNullViolationBecomesBadRequest() {
        assertThat(handler.handleDataIntegrity(violation(ConstraintKind.NOT_NULL)).getStatus()).isEqualTo(400);
    }

    @Test
    void violationWithoutHibernateCauseBecomesBadRequest() {
        ProblemDetail problem = handler.handleDataIntegrity(new DataIntegrityViolationException("unknown"));

        assertThat(problem.getStatus()).isEqualTo(400);
    }

    @Test
    void projectExceptionsAreRoutedToProperHandlers() {
        ExceptionHandlerMethodResolver resolver = new ExceptionHandlerMethodResolver(GlobalExceptionHandler.class);

        assertThat(resolver.resolveMethodByThrowable(new DuplicateProjectTitleException("x")).getName())
                .isEqualTo("handleConflict");
        assertThat(resolver.resolveMethodByThrowable(new ProjectStatusAlreadyAssignedException(ProjectStatus.ACTIVE)).getName())
                .isEqualTo("handleConflict");
        assertThat(resolver.resolveMethodByThrowable(new ProjectNotFoundException(UUID.randomUUID())).getName())
                .isEqualTo("handleNotFound");
        assertThat(resolver.resolveMethodByThrowable(new ProjectMemberRoleAlreadyAssignedException(ProjectMemberRole.ADMIN)).getName())
                .isEqualTo("handleConflict");
    }

    @Test
    void issueExceptionsAreRoutedToProperHandlers() {
        ExceptionHandlerMethodResolver resolver = new ExceptionHandlerMethodResolver(GlobalExceptionHandler.class);

        assertThat(resolver.resolveMethodByThrowable(new IssueNotFoundException(UUID.randomUUID())).getName())
                .isEqualTo("handleNotFound");
        assertThat(resolver.resolveMethodByThrowable(new IssueStatusAlreadyAssignedException(IssueStatus.TODO)).getName())
                .isEqualTo("handleConflict");
        assertThat(resolver.resolveMethodByThrowable(new IllegalStatusTransitionException(IssueStatus.TODO, IssueStatus.DONE)).getName())
                .isEqualTo("handleConflict");
        assertThat(resolver.resolveMethodByThrowable(new CommentNotFoundException(UUID.randomUUID())).getName())
                .isEqualTo("handleNotFound");
        assertThat(resolver.resolveMethodByThrowable(new InvalidPasswordException("x")).getName())
                .isEqualTo("handleBadRequest");
        assertThat(resolver.resolveMethodByThrowable(new InvalidRefreshTokenException()).getName())
                .isEqualTo("handleInvalidRefreshToken");
        assertThat(resolver.resolveMethodByThrowable(new ProjectArchivedException(UUID.randomUUID())).getName())
                .isEqualTo("handleConflict");
        assertThat(resolver.resolveMethodByThrowable(new AccessDeniedToProjectException(UUID.randomUUID())).getName())
                .isEqualTo("handleForbidden");
        assertThat(resolver.resolveMethodByThrowable(new IllegalIssueOperationException("x")).getName())
                .isEqualTo("handleForbidden");
        assertThat(resolver.resolveMethodByThrowable(new InvalidIssueException("x")).getName())
                .isEqualTo("handleBadRequest");
        assertThat(resolver.resolveMethodByThrowable(new ObjectOptimisticLockingFailureException(Object.class, "id")).getName())
                .isEqualTo("handleOptimisticLock");
    }

    @Test
    void optimisticLockBecomesConflict() {
        ProblemDetail problem = handler.handleOptimisticLock(new ObjectOptimisticLockingFailureException(Object.class, "id"));

        assertThat(problem.getStatus()).isEqualTo(409);
        assertThat(problem.getDetail()).isEqualTo("Resource was modified by another request, reload and try again");
    }

    private static DataIntegrityViolationException violation(ConstraintKind kind) {
        ConstraintViolationException cause = new ConstraintViolationException(
                "violation", new SQLException("violation"), kind, "some_constraint");
        return new DataIntegrityViolationException("could not execute statement", cause);
    }
}

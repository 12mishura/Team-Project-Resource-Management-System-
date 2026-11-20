package com.example.tasktracker.exception;

import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ProblemDetail handleInvalidRefreshToken(InvalidRefreshTokenException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Authentication required");
    }

    @ExceptionHandler({UserAlreadyExistsException.class, DuplicateOrganizationTitleException.class,
            MemberRoleAlreadyAssignedException.class, MemberAlreadyExistsException.class,
            ProjectMemberRoleAlreadyAssignedException.class, ProjectMemberAlreadyExistsException.class,
            ProjectStatusAlreadyAssignedException.class, DuplicateProjectTitleException.class,
            IssueStatusAlreadyAssignedException.class, ProjectArchivedException.class,
            IllegalStatusTransitionException.class})
    public ProblemDetail handleConflict(RuntimeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException e) {
        ConstraintKind kind = findConstraintKind(e);
        logger.warn("Data integrity violation: " + kind, e);
        return switch (kind) {
            case UNIQUE -> ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Resource already exists");
            case FOREIGN_KEY -> ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Operation conflicts with related data");
            case NOT_NULL, CHECK, OTHER -> ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request violates data constraints");
        };
    }

    @ExceptionHandler({OrganizationNotFoundException.class, UserNotFoundException.class,
            InviteCodeNotExistsException.class, MemberNotFoundException.class, ProjectNotFoundException.class,
            ProjectMemberNotFoundException.class, IssueNotFoundException.class,
            CommentNotFoundException.class})
    public ProblemDetail handleNotFound(RuntimeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler({AccessDeniedToOrganizationException.class, IllegalMemberOperationException.class,
            AccessDeniedToProjectException.class, IllegalIssueOperationException.class})
    public ProblemDetail handleForbidden(RuntimeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler({InvalidIssueException.class, InvalidPasswordException.class})
    public ProblemDetail handleBadRequest(RuntimeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(OptimisticLockingFailureException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Resource was modified by another request, reload and try again");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception e) {
        logger.error("Unexpected error", e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
    }

    private ConstraintKind findConstraintKind(Throwable e) {
        for (Throwable current = e; current != null; current = current.getCause()) {
            if (current instanceof ConstraintViolationException violation && violation.getKind() != null) {
                return violation.getKind();
            }
        }
        return ConstraintKind.OTHER;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        return handleExceptionInternal(ex, body, headers, status, request);
    }
}

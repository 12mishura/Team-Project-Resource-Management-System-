package com.example.tasktracker.aop;

import com.example.tasktracker.security.OrganizationPermissionChecker;
import lombok.AllArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.UUID;

@Aspect
@Component
@Order(0)
@AllArgsConstructor
public class OrganizationAccessAspect {
    private final OrganizationPermissionChecker permissionChecker;

    @Before("@annotation(access)")
    public void checkAccess(JoinPoint joinPoint, OrganizationAccess access) {
        UUID organizationId = extractOrganizationId(joinPoint);
        permissionChecker.requireRole(organizationId, currentUserEmail(), access.roles());
    }

    private UUID extractOrganizationId(JoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        Object[] args = joinPoint.getArgs();
        for (int i = 0; i < parameterAnnotations.length; i++) {
            for (Annotation annotation : parameterAnnotations[i]) {
                if (annotation instanceof OrganizationId) {
                    if (args[i] instanceof UUID organizationId) {
                        return organizationId;
                    }
                    throw new IllegalStateException("@OrganizationId parameter of " + method + " must be a non-null UUID");
                }
            }
        }
        throw new IllegalStateException(method + " is annotated with @OrganizationAccess but has no @OrganizationId parameter");
    }

    private String currentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new AuthenticationCredentialsNotFoundException("Authentication required");
        }
        return authentication.getName();
    }
}

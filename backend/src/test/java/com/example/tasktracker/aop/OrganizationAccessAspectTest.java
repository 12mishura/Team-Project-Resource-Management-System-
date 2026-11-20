package com.example.tasktracker.aop;

import com.example.tasktracker.domain.enums.OrganizationRole;
import com.example.tasktracker.exception.AccessDeniedToOrganizationException;
import com.example.tasktracker.security.OrganizationPermissionChecker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static com.example.tasktracker.domain.enums.OrganizationRole.ADMIN;
import static com.example.tasktracker.domain.enums.OrganizationRole.OWNER;
import static com.example.tasktracker.domain.enums.OrganizationRole.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class OrganizationAccessAspectTest {
    private static final UUID ORGANIZATION_ID = UUID.randomUUID();
    private static final String EMAIL = "alice@mail.com";

    private OrganizationPermissionChecker checker;
    private SampleService service;
    private SampleService target;

    static class SampleService {
        int calls;

        @OrganizationAccess
        public String anyMember(@OrganizationId UUID organizationId) {
            calls++;
            return "ok";
        }

        @OrganizationAccess(roles = {OWNER, ADMIN})
        public String ownerOrAdmin(String other, @OrganizationId UUID organizationId) {
            calls++;
            return other;
        }

        @OrganizationAccess
        public String missingOrganizationId(UUID organizationId) {
            calls++;
            return "ok";
        }

        public String notAnnotated(UUID organizationId) {
            calls++;
            return "ok";
        }
    }

    @BeforeEach
    void setUp() {
        checker = mock(OrganizationPermissionChecker.class);
        target = new SampleService();
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.setProxyTargetClass(true);
        factory.addAspect(new OrganizationAccessAspect(checker));
        service = factory.getProxy();
        authenticate(EMAIL);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void defaultAccessAllowsAnyMemberRole() {
        assertThat(service.anyMember(ORGANIZATION_ID)).isEqualTo("ok");

        verify(checker).requireRole(ORGANIZATION_ID, EMAIL, OWNER, ADMIN, USER);
        assertThat(target.calls).isEqualTo(1);
    }

    @Test
    void rolesAreTakenFromAnnotationAndIdFromMarkedParameter() {
        assertThat(service.ownerOrAdmin("payload", ORGANIZATION_ID)).isEqualTo("payload");

        verify(checker).requireRole(ORGANIZATION_ID, EMAIL, OWNER, ADMIN);
    }

    @Test
    void deniedCheckStopsMethodExecution() {
        doThrow(new AccessDeniedToOrganizationException(ORGANIZATION_ID))
                .when(checker).requireRole(eq(ORGANIZATION_ID), eq(EMAIL), any(OrganizationRole[].class));

        assertThatThrownBy(() -> service.ownerOrAdmin("payload", ORGANIZATION_ID))
                .isInstanceOf(AccessDeniedToOrganizationException.class);
        assertThat(target.calls).isZero();
    }

    @Test
    void methodWithoutOrganizationIdParameterFailsLoudly() {
        assertThatThrownBy(() -> service.missingOrganizationId(ORGANIZATION_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("has no @OrganizationId parameter");
        assertThat(target.calls).isZero();
        verifyNoInteractions(checker);
    }

    @Test
    void nullOrganizationIdFailsLoudly() {
        assertThatThrownBy(() -> service.anyMember(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be a non-null UUID");
        assertThat(target.calls).isZero();
    }

    @Test
    void missingAuthenticationIsRejected() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> service.anyMember(ORGANIZATION_ID))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        assertThat(target.calls).isZero();
        verifyNoInteractions(checker);
    }

    @Test
    void anonymousAuthenticationIsRejected() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        assertThatThrownBy(() -> service.anyMember(ORGANIZATION_ID))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        verifyNoInteractions(checker);
    }

    @Test
    void methodsWithoutAnnotationAreNotChecked() {
        SecurityContextHolder.clearContext();

        assertThat(service.notAnnotated(ORGANIZATION_ID)).isEqualTo("ok");
        verifyNoInteractions(checker);
    }

    private static void authenticate(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null, List.of()));
    }
}

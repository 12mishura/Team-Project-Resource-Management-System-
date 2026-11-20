package com.example.tasktracker.aop;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.RegexPatternTypeFilter;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class OrganizationAccessUsageTest {

    @Test
    void everyAnnotatedMethodHasExactlyOneUuidOrganizationIdParameter() throws Exception {
        List<Method> annotated = annotatedMethods();
        List<String> problems = new ArrayList<>();

        for (Method method : annotated) {
            Parameter[] marked = Arrays.stream(method.getParameters())
                    .filter(parameter -> parameter.isAnnotationPresent(OrganizationId.class))
                    .toArray(Parameter[]::new);
            if (marked.length != 1 || marked[0].getType() != UUID.class) {
                problems.add(method.toString());
            }
            if (!Modifier.isPublic(method.getModifiers())) {
                problems.add(method + " must be public to be proxied");
            }
        }

        assertThat(annotated).isNotEmpty();
        assertThat(problems).isEmpty();
    }

    @Test
    void expectedServiceMethodsAreProtected() throws Exception {
        List<String> names = annotatedMethods().stream()
                .map(method -> method.getDeclaringClass().getSimpleName() + "." + method.getName())
                .toList();

        assertThat(names).contains(
                "OrganizationService.getOrganizationById",
                "OrganizationService.updateOrganization",
                "OrganizationService.deleteOrganization",
                "OrganizationService.getInviteCode",
                "OrganizationMemberService.getMembers",
                "ProjectService.createProject",
                "ProjectService.updateProject",
                "ProjectService.getProject",
                "ProjectService.getProjects",
                "ProjectService.changeStatus",
                "ProjectService.deleteProject",
                "ProjectMemberService.addMember",
                "ProjectMemberService.getMembers",
                "ProjectMemberService.updateRole",
                "ProjectMemberService.removeMember",
                "IssueService.createIssue",
                "IssueService.getIssues",
                "IssueService.getIssue",
                "IssueService.updateIssue",
                "IssueService.changeStatus",
                "IssueService.assignIssue",
                "IssueService.changeParent",
                "IssueService.deleteIssue",
                "IssueService.unassignIssue",
                "IssueService.detachFromEpic",
                "IssueService.getHistory",
                "OrganizationService.regenerateInviteCode",
                "IssueCommentService.addComment",
                "IssueCommentService.getComments",
                "IssueCommentService.editComment",
                "IssueCommentService.deleteComment");
    }

    private static List<Method> annotatedMethods() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*")));
        List<Method> methods = new ArrayList<>();
        for (BeanDefinition definition : scanner.findCandidateComponents("com.example.tasktracker")) {
            Class<?> type = Class.forName(definition.getBeanClassName());
            if (type.getName().contains("Test")) {
                continue;
            }
            for (Method method : type.getDeclaredMethods()) {
                if (method.isAnnotationPresent(OrganizationAccess.class)) {
                    methods.add(method);
                }
            }
        }
        return methods;
    }
}

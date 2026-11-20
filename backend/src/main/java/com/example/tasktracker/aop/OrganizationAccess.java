package com.example.tasktracker.aop;

import com.example.tasktracker.domain.enums.OrganizationRole;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OrganizationAccess {
    OrganizationRole[] roles() default {OrganizationRole.OWNER, OrganizationRole.ADMIN, OrganizationRole.USER};
}

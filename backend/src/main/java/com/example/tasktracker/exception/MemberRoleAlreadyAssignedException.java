package com.example.tasktracker.exception;

import com.example.tasktracker.domain.enums.OrganizationRole;

public class MemberRoleAlreadyAssignedException extends RuntimeException {
    public MemberRoleAlreadyAssignedException(OrganizationRole role) {
        super("Member already has role " + role);
    }
}

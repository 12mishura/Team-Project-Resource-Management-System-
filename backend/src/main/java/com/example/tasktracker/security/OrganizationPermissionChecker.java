package com.example.tasktracker.security;

import com.example.tasktracker.domain.OrganizationMember;
import com.example.tasktracker.domain.enums.OrganizationRole;
import com.example.tasktracker.exception.AccessDeniedToOrganizationException;
import com.example.tasktracker.exception.IllegalMemberOperationException;
import com.example.tasktracker.exception.OrganizationNotFoundException;
import com.example.tasktracker.repository.OrganizationMemberRepository;
import com.example.tasktracker.repository.OrganizationRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class OrganizationPermissionChecker {
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;

    public OrganizationMember requireMember(UUID organizationId, String email){
        if(!organizationRepository.existsById(organizationId)){
            throw new OrganizationNotFoundException(organizationId);
        }
        return organizationMemberRepository.findByOrganizationIdAndUserEmail(organizationId, email)
                .orElseThrow(() -> new AccessDeniedToOrganizationException(organizationId));
    }

    public OrganizationMember requireRole(UUID organizationId, String email, OrganizationRole... allowedRoles){
        OrganizationMember member = requireMember(organizationId, email);
        if(!Arrays.asList(allowedRoles).contains(member.getRole())){
            String required = Arrays.stream(allowedRoles).map(Enum::name).collect(Collectors.joining(" or "));
            throw new IllegalMemberOperationException("Only " + required + " can perform this operation");
        }
        return member;
    }

    public void checkCanChangeRole(OrganizationMember actor, OrganizationMember target, OrganizationRole newRole){
        if(actor.getRole() == OrganizationRole.USER){
            throw new IllegalMemberOperationException("Only owner or admin can change roles");
        }
        if(actor.getId().equals(target.getId())){
            throw new IllegalMemberOperationException("Cannot change your own role");
        }
        if(newRole == OrganizationRole.OWNER || target.getRole() == OrganizationRole.OWNER){
            throw new IllegalMemberOperationException("Owner role can only be changed via ownership transfer");
        }
        if(actor.getRole() == OrganizationRole.ADMIN && target.getRole() == OrganizationRole.ADMIN){
            throw new IllegalMemberOperationException("Admin cannot change role of another admin");
        }
    }

    public void checkCanRemove(OrganizationMember actor, OrganizationMember target){
        if(actor.getRole() == OrganizationRole.USER){
            throw new IllegalMemberOperationException("Only owner or admin can remove members");
        }
        if(target.getRole() == OrganizationRole.OWNER){
            throw new IllegalMemberOperationException("Owner cannot be removed");
        }
        if(actor.getRole() == OrganizationRole.ADMIN && target.getRole() == OrganizationRole.ADMIN){
            throw new IllegalMemberOperationException("Admin cannot remove another admin");
        }
    }

    public void checkCanTransferOwnership(OrganizationMember actor, UUID newOwnerMemberId){
        if(actor.getRole() != OrganizationRole.OWNER){
            throw new IllegalMemberOperationException("Only OWNER can transfer ownership");
        }
        if(actor.getId().equals(newOwnerMemberId)){
            throw new IllegalMemberOperationException("Cannot transfer ownership to yourself");
        }
    }

    public void checkCanLeave(OrganizationMember actor){
        if(actor.getRole() == OrganizationRole.OWNER){
            throw new IllegalMemberOperationException("Owner cannot leave organization, transfer ownership or delete organization");
        }
    }
}

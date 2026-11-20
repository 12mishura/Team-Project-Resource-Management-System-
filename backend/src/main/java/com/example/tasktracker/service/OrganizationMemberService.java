package com.example.tasktracker.service;

import com.example.tasktracker.aop.OrganizationAccess;
import com.example.tasktracker.aop.OrganizationId;
import com.example.tasktracker.domain.Organization;
import com.example.tasktracker.domain.OrganizationMember;
import com.example.tasktracker.domain.User;
import com.example.tasktracker.domain.enums.OrganizationRole;
import com.example.tasktracker.dto.request.JoinOrganizationRequest;
import com.example.tasktracker.dto.request.TransferOwnershipRequest;
import com.example.tasktracker.dto.request.UpdateOrganizationMemberRequest;
import com.example.tasktracker.dto.response.OrganizationMemberResponse;
import com.example.tasktracker.dto.response.PageResponse;
import com.example.tasktracker.exception.AccessDeniedToOrganizationException;
import com.example.tasktracker.exception.InviteCodeNotExistsException;
import com.example.tasktracker.exception.MemberAlreadyExistsException;
import com.example.tasktracker.exception.MemberNotFoundException;
import com.example.tasktracker.exception.OrganizationNotFoundException;
import com.example.tasktracker.exception.UserNotFoundException;
import com.example.tasktracker.mapper.OrganizationMemberMapper;
import com.example.tasktracker.repository.OrganizationMemberRepository;
import com.example.tasktracker.repository.OrganizationRepository;
import com.example.tasktracker.repository.UserRepository;
import com.example.tasktracker.security.OrganizationPermissionChecker;
import com.example.tasktracker.util.PageRequests;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@AllArgsConstructor
public class OrganizationMemberService {
    private final OrganizationMemberRepository organizationMemberRepository;
    private final OrganizationMemberMapper organizationMemberMapper;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final OrganizationPermissionChecker permissionChecker;

    @Transactional
    public OrganizationMemberResponse joinToOrganization(JoinOrganizationRequest request, String email){
        Organization organization = organizationRepository.findByInviteCode(request.inviteCode()).orElseThrow(() -> new InviteCodeNotExistsException(request.inviteCode()));
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException(email));

        if(organizationMemberRepository.existsByOrganizationIdAndUserId(organization.getId(), user.getId())){
            throw new MemberAlreadyExistsException(organization.getId());
        }
        OrganizationMember member = new OrganizationMember(OrganizationRole.USER);
        organization.addMember(member);
        user.addMember(member);
        organizationMemberRepository.saveAndFlush(member);
        return organizationMemberMapper.toResponse(member);
    }

    @OrganizationAccess
    @Transactional(readOnly = true)
    public PageResponse<OrganizationMemberResponse> getMembers(@OrganizationId UUID organizationId, Pageable pageable){
        return PageResponse.from(
                organizationMemberRepository.findAllByOrganizationId(organizationId, PageRequests.byCreation(pageable)),
                organizationMemberMapper::toResponse);
    }

    @Transactional
    public OrganizationMemberResponse updateRole(UUID organizationId, UUID memberId, UpdateOrganizationMemberRequest request, String email){
        OrganizationMember actor = permissionChecker.requireMember(organizationId, email);
        OrganizationMember target = findTarget(organizationId, memberId);

        permissionChecker.checkCanChangeRole(actor, target, request.role());

        target.changeRole(request.role());
        organizationMemberRepository.flush();
        return organizationMemberMapper.toResponse(target);
    }

    @Transactional
    public void removeMember(UUID organizationId, UUID memberId, String email){
        OrganizationMember actor = permissionChecker.requireMember(organizationId, email);
        OrganizationMember target = findTarget(organizationId, memberId);

        if(actor.getId().equals(target.getId())){
            permissionChecker.checkCanLeave(actor);
        } else {
            permissionChecker.checkCanRemove(actor, target);
        }
        organizationMemberRepository.delete(target);
    }

    @Transactional
    public void leaveOrganization(UUID organizationId, String email){
        OrganizationMember actor = permissionChecker.requireMember(organizationId, email);
        permissionChecker.checkCanLeave(actor);
        organizationMemberRepository.delete(actor);
    }

    @Transactional
    public OrganizationMemberResponse transferOwnership(UUID organizationId, TransferOwnershipRequest request, String email){
        if(!organizationRepository.existsById(organizationId)){
            throw new OrganizationNotFoundException(organizationId);
        }
        OrganizationMember owner = organizationMemberRepository.findForUpdate(organizationId, email)
                .orElseThrow(() -> new AccessDeniedToOrganizationException(organizationId));
        permissionChecker.checkCanTransferOwnership(owner, request.memberId());
        OrganizationMember newOwner = findTarget(organizationId, request.memberId());

        owner.changeRole(OrganizationRole.ADMIN);
        organizationMemberRepository.flush();
        newOwner.changeRole(OrganizationRole.OWNER);
        organizationMemberRepository.flush();
        return organizationMemberMapper.toResponse(newOwner);
    }

    private OrganizationMember findTarget(UUID organizationId, UUID memberId){
        return organizationMemberRepository.findByIdAndOrganizationId(memberId, organizationId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));
    }
}

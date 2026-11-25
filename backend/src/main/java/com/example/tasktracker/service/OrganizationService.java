package com.example.tasktracker.service;

import com.example.tasktracker.aop.OrganizationAccess;
import com.example.tasktracker.aop.OrganizationId;
import com.example.tasktracker.domain.Organization;
import com.example.tasktracker.domain.OrganizationMember;
import com.example.tasktracker.domain.User;
import com.example.tasktracker.domain.enums.OrganizationRole;
import com.example.tasktracker.dto.request.CreateOrganizationRequest;
import com.example.tasktracker.dto.request.UpdateOrganizationRequest;
import com.example.tasktracker.dto.response.InviteCodeResponse;
import com.example.tasktracker.dto.response.MyOrganizationResponse;
import com.example.tasktracker.dto.response.OrganizationResponse;
import com.example.tasktracker.dto.response.PageResponse;
import com.example.tasktracker.exception.DuplicateOrganizationTitleException;
import com.example.tasktracker.exception.OrganizationNotFoundException;
import com.example.tasktracker.exception.UserNotFoundException;
import com.example.tasktracker.mapper.OrganizationMapper;
import com.example.tasktracker.repository.OrganizationMemberRepository;
import com.example.tasktracker.repository.OrganizationRepository;
import com.example.tasktracker.repository.UserRepository;
import com.example.tasktracker.util.GenerateInviteCode;
import com.example.tasktracker.util.PageRequests;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@AllArgsConstructor
public class OrganizationService {
    private final OrganizationRepository organizationRepository;
    private final GenerateInviteCode generateInviteCode;
    private final OrganizationMapper organizationMapper;
    private final UserRepository userRepository;
    private final OrganizationMemberRepository organizationMemberRepository;

    @Transactional
    public OrganizationResponse createOrganization(CreateOrganizationRequest request, String email){
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException(email));
        if(organizationRepository.existsByTitleIgnoreCase(request.title())){
            throw new DuplicateOrganizationTitleException(request.title());
        }
        Organization organization = organizationMapper.toEntity(request, generateInviteCode.getCode());
        OrganizationMember owner = new OrganizationMember(OrganizationRole.OWNER);
        organization.addMember(owner);
        user.addMember(owner);
        organizationRepository.saveAndFlush(organization);
        return organizationMapper.toResponse(organization);
    }

    @Transactional(readOnly = true)
    public PageResponse<MyOrganizationResponse> getMyOrganizations(String email, Pageable pageable){
        return PageResponse.from(
                organizationMemberRepository.findAllByUserEmail(email, PageRequests.byCreation(pageable)),
                organizationMapper::toMyResponse);
    }

    @OrganizationAccess
    @Transactional(readOnly = true)
    public OrganizationResponse getOrganizationById(@OrganizationId UUID id){
        Organization organization = organizationRepository.findById(id).orElseThrow(() -> new OrganizationNotFoundException(id));
        return organizationMapper.toResponse(organization);
    }

    @OrganizationAccess(roles = {OrganizationRole.OWNER, OrganizationRole.ADMIN})
    @Transactional
    public OrganizationResponse updateOrganization(@OrganizationId UUID id, UpdateOrganizationRequest request){
         Organization organization = organizationRepository.findById(id).orElseThrow(() -> new OrganizationNotFoundException(id));
         if(request.title() != null && !request.title().equalsIgnoreCase(organization.getTitle())
                 && organizationRepository.existsByTitleIgnoreCase(request.title())){
             throw new DuplicateOrganizationTitleException(request.title());
         }
         organizationMapper.toUpdate(request, organization);
         organizationRepository.flush();
         return organizationMapper.toResponse(organization);
    }

    @OrganizationAccess(roles = OrganizationRole.OWNER)
    @Transactional
    public void deleteOrganization(@OrganizationId UUID id){
        Organization organization = organizationRepository.findById(id).orElseThrow(() -> new OrganizationNotFoundException(id));
        organizationRepository.delete(organization);
    }

    @OrganizationAccess(roles = {OrganizationRole.OWNER, OrganizationRole.ADMIN})
    @Transactional(readOnly = true)
    public InviteCodeResponse getInviteCode(@OrganizationId UUID id){
        Organization organization = organizationRepository.findById(id).orElseThrow(() -> new OrganizationNotFoundException(id));
        return new InviteCodeResponse(organization.getInviteCode());
    }

    @OrganizationAccess(roles = {OrganizationRole.OWNER, OrganizationRole.ADMIN})
    @Transactional
    public InviteCodeResponse regenerateInviteCode(@OrganizationId UUID id){
        Organization organization = organizationRepository.findById(id).orElseThrow(() -> new OrganizationNotFoundException(id));
        organization.changeInviteCode(generateInviteCode.getCode());
        organizationRepository.flush();
        return new InviteCodeResponse(organization.getInviteCode());
    }
}

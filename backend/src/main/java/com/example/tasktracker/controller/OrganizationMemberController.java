package com.example.tasktracker.controller;

import com.example.tasktracker.dto.request.JoinOrganizationRequest;
import com.example.tasktracker.dto.request.TransferOwnershipRequest;
import com.example.tasktracker.dto.request.UpdateOrganizationMemberRequest;
import com.example.tasktracker.dto.response.OrganizationMemberResponse;
import com.example.tasktracker.dto.response.PageResponse;
import com.example.tasktracker.service.OrganizationMemberService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/organizations")
@AllArgsConstructor
public class OrganizationMemberController {
    private final OrganizationMemberService organizationMemberService;

    @PostMapping("/join")
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationMemberResponse joinByInviteCode(@Valid @RequestBody JoinOrganizationRequest request, @AuthenticationPrincipal UserDetails userDetails){
        return organizationMemberService.joinToOrganization(request, userDetails.getUsername());
    }

    @GetMapping("/{organizationId}/members")
    public PageResponse<OrganizationMemberResponse> getMembers(@PathVariable UUID organizationId, Pageable pageable){
        return organizationMemberService.getMembers(organizationId, pageable);
    }

    @PatchMapping("/{organizationId}/members/{memberId}")
    public OrganizationMemberResponse updateRole(@PathVariable UUID organizationId,
                                                 @PathVariable UUID memberId,
                                                 @Valid @RequestBody UpdateOrganizationMemberRequest request,
                                                 @AuthenticationPrincipal UserDetails userDetails){
        return organizationMemberService.updateRole(organizationId, memberId, request, userDetails.getUsername());
    }

    @PostMapping("/{organizationId}/transfer-ownership")
    public OrganizationMemberResponse transferOwnership(@PathVariable UUID organizationId,
                                                        @Valid @RequestBody TransferOwnershipRequest request,
                                                        @AuthenticationPrincipal UserDetails userDetails){
        return organizationMemberService.transferOwnership(organizationId, request, userDetails.getUsername());
    }

    @DeleteMapping("/{organizationId}/members/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leaveOrganization(@PathVariable UUID organizationId, @AuthenticationPrincipal UserDetails userDetails){
        organizationMemberService.leaveOrganization(organizationId, userDetails.getUsername());
    }

    @DeleteMapping("/{organizationId}/members/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable UUID organizationId,
                             @PathVariable UUID memberId,
                             @AuthenticationPrincipal UserDetails userDetails){
        organizationMemberService.removeMember(organizationId, memberId, userDetails.getUsername());
    }
}

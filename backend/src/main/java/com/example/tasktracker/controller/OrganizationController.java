package com.example.tasktracker.controller;

import com.example.tasktracker.dto.request.CreateOrganizationRequest;
import com.example.tasktracker.dto.request.UpdateOrganizationRequest;
import com.example.tasktracker.dto.response.InviteCodeResponse;
import com.example.tasktracker.dto.response.MyOrganizationResponse;
import com.example.tasktracker.dto.response.OrganizationResponse;
import com.example.tasktracker.dto.response.PageResponse;
import com.example.tasktracker.service.OrganizationService;
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
public class OrganizationController {
    private final OrganizationService organizationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationResponse createOrganization(@Valid @RequestBody CreateOrganizationRequest request, @AuthenticationPrincipal UserDetails userDetails){
        String email = userDetails.getUsername();
        return organizationService.createOrganization(request, email);
    }

    @GetMapping
    public PageResponse<MyOrganizationResponse> getMyOrganizations(@AuthenticationPrincipal UserDetails userDetails, Pageable pageable){
        return organizationService.getMyOrganizations(userDetails.getUsername(), pageable);
    }

    @GetMapping("/{id}")
    public OrganizationResponse getOrganizationById(@PathVariable UUID id){
        return organizationService.getOrganizationById(id);
    }

    @GetMapping("/{id}/invite-code")
    public InviteCodeResponse getInviteCode(@PathVariable UUID id){
        return organizationService.getInviteCode(id);
    }

    @PostMapping("/{id}/invite-code/regenerate")
    public InviteCodeResponse regenerateInviteCode(@PathVariable UUID id){
        return organizationService.regenerateInviteCode(id);
    }

    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public OrganizationResponse updateOrganization(@PathVariable UUID id, @Valid @RequestBody UpdateOrganizationRequest request){
        return organizationService.updateOrganization(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOrganization(@PathVariable UUID id){
        organizationService.deleteOrganization(id);
    }
}

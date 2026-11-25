package com.example.tasktracker.controller;

import com.example.tasktracker.dto.request.UpdateCapacityRequest;
import com.example.tasktracker.dto.response.OrganizationMemberResponse;
import com.example.tasktracker.dto.response.WorkloadResponse;
import com.example.tasktracker.service.WorkloadService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/organizations/{organizationId}")
@AllArgsConstructor
public class WorkloadController {
    private final WorkloadService workloadService;

    @GetMapping("/workload")
    public WorkloadResponse getOrganizationWorkload(@PathVariable UUID organizationId,
                                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to){
        return workloadService.getOrganizationWorkload(organizationId, from, to);
    }

    @GetMapping("/projects/{projectId}/workload")
    public WorkloadResponse getProjectWorkload(@PathVariable UUID organizationId,
                                               @PathVariable UUID projectId,
                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                               @AuthenticationPrincipal UserDetails userDetails){
        return workloadService.getProjectWorkload(organizationId, projectId, from, to, userDetails.getUsername());
    }

    @PatchMapping("/members/{memberId}/capacity")
    public OrganizationMemberResponse updateCapacity(@PathVariable UUID organizationId,
                                                     @PathVariable UUID memberId,
                                                     @Valid @RequestBody UpdateCapacityRequest request){
        return workloadService.updateCapacity(organizationId, memberId, request);
    }
}

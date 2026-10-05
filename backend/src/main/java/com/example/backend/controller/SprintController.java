package com.example.backend.controller;

import com.example.backend.dto.SprintRequest;
import com.example.backend.dto.SprintResponse;
import com.example.backend.dto.SprintUpdateRequest;
import com.example.backend.service.SprintService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/sprints")
@RequiredArgsConstructor
public class SprintController {

    private final SprintService sprintService;

    @PostMapping
    public ResponseEntity<SprintResponse> createSprint(
            @PathVariable Long projectId,
            @RequestBody SprintRequest request) {
        SprintResponse created = sprintService.createSprint(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<SprintResponse>> getProjectSprints(
            @PathVariable Long projectId) {
        return ResponseEntity.ok(sprintService.getSprintsByProject(projectId));
    }

    @GetMapping("/{sprintId}")
    public ResponseEntity<SprintResponse> getSprint(
            @PathVariable Long projectId,
            @PathVariable Long sprintId) {
        return ResponseEntity.ok(sprintService.getSprintById(projectId, sprintId));
    }

    @PatchMapping("/{sprintId}")
    public ResponseEntity<SprintResponse> updateSprint(
            @PathVariable Long projectId,
            @PathVariable Long sprintId,
            @RequestBody SprintUpdateRequest request) {
        return ResponseEntity.ok(sprintService.updateSprint(projectId, sprintId, request));
    }

    @DeleteMapping("/{sprintId}")
    public ResponseEntity<Void> deleteSprint(
            @PathVariable Long projectId,
            @PathVariable Long sprintId) {
        sprintService.deleteSprint(projectId, sprintId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{sprintId}/start")
    public ResponseEntity<SprintResponse> startSprint(
            @PathVariable Long projectId,
            @PathVariable Long sprintId) {
        return ResponseEntity.ok(sprintService.startSprint(projectId, sprintId));
    }

    @PostMapping("/{sprintId}/complete")
    public ResponseEntity<SprintResponse> completeSprint(
            @PathVariable Long projectId,
            @PathVariable Long sprintId) {
        return ResponseEntity.ok(sprintService.completeSprint(projectId, sprintId));
    }
}
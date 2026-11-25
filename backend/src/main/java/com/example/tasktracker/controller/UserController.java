package com.example.tasktracker.controller;

import com.example.tasktracker.domain.enums.IssueSortField;
import com.example.tasktracker.domain.enums.IssueStatus;
import com.example.tasktracker.domain.enums.IssueType;
import com.example.tasktracker.dto.request.ChangePasswordRequest;
import com.example.tasktracker.dto.request.IssueFilter;
import com.example.tasktracker.dto.request.UpdateProfileRequest;
import com.example.tasktracker.dto.response.IssueResponse;
import com.example.tasktracker.dto.response.PageResponse;
import com.example.tasktracker.dto.response.UserResponse;
import com.example.tasktracker.service.IssueService;
import com.example.tasktracker.service.UserService;
import com.example.tasktracker.service.WorkLogService;
import com.example.tasktracker.dto.response.TimesheetEntryResponse;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/users/me")
@AllArgsConstructor
public class UserController {
    private final IssueService issueService;
    private final UserService userService;
    private final WorkLogService workLogService;

    @PatchMapping
    public UserResponse updateProfile(@Valid @RequestBody UpdateProfileRequest request,
                                      @AuthenticationPrincipal UserDetails userDetails){
        return userService.updateProfile(userDetails.getUsername(), request);
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request,
                               @AuthenticationPrincipal UserDetails userDetails){
        userService.changePassword(userDetails.getUsername(), request);
    }

    @GetMapping("/issues")
    public PageResponse<IssueResponse> getMyIssues(@RequestParam(required = false) IssueType type,
                                                   @RequestParam(required = false) IssueStatus status,
                                                   @RequestParam(required = false) UUID parentId,
                                                   @RequestParam(required = false) String q,
                                                   @RequestParam(required = false) IssueSortField sortBy,
                                                   @RequestParam(required = false) Sort.Direction direction,
                                                   Pageable pageable,
                                                   @AuthenticationPrincipal UserDetails userDetails){
        IssueFilter filter = new IssueFilter(type, status, null, parentId, q, sortBy, direction);
        return issueService.getMyIssues(userDetails.getUsername(), filter, pageable);
    }

    @GetMapping("/worklogs")
    public List<TimesheetEntryResponse> getTimesheet(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                     @AuthenticationPrincipal UserDetails userDetails){
        return workLogService.getTimesheet(userDetails.getUsername(), from, to);
    }
}

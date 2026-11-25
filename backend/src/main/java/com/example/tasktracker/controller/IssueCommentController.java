package com.example.tasktracker.controller;

import com.example.tasktracker.dto.request.CreateCommentRequest;
import com.example.tasktracker.dto.request.UpdateCommentRequest;
import com.example.tasktracker.dto.response.CommentResponse;
import com.example.tasktracker.dto.response.PageResponse;
import com.example.tasktracker.service.IssueCommentService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/organizations/{organizationId}/projects/{projectId}/issues/{issueId}/comments")
@AllArgsConstructor
public class IssueCommentController {
    private final IssueCommentService commentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse addComment(@PathVariable UUID organizationId,
                                      @PathVariable UUID projectId,
                                      @PathVariable UUID issueId,
                                      @Valid @RequestBody CreateCommentRequest request,
                                      @AuthenticationPrincipal UserDetails userDetails){
        return commentService.addComment(organizationId, projectId, issueId, request, userDetails.getUsername());
    }

    @GetMapping
    public PageResponse<CommentResponse> getComments(@PathVariable UUID organizationId,
                                                     @PathVariable UUID projectId,
                                                     @PathVariable UUID issueId,
                                                     Pageable pageable,
                                                     @AuthenticationPrincipal UserDetails userDetails){
        return commentService.getComments(organizationId, projectId, issueId, pageable, userDetails.getUsername());
    }

    @PatchMapping("/{commentId}")
    public CommentResponse editComment(@PathVariable UUID organizationId,
                                       @PathVariable UUID projectId,
                                       @PathVariable UUID issueId,
                                       @PathVariable UUID commentId,
                                       @Valid @RequestBody UpdateCommentRequest request,
                                       @AuthenticationPrincipal UserDetails userDetails){
        return commentService.editComment(organizationId, projectId, issueId, commentId, request, userDetails.getUsername());
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable UUID organizationId,
                              @PathVariable UUID projectId,
                              @PathVariable UUID issueId,
                              @PathVariable UUID commentId,
                              @AuthenticationPrincipal UserDetails userDetails){
        commentService.deleteComment(organizationId, projectId, issueId, commentId, userDetails.getUsername());
    }
}

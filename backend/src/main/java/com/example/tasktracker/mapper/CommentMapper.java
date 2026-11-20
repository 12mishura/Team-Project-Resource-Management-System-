package com.example.tasktracker.mapper;

import com.example.tasktracker.domain.IssueComment;
import com.example.tasktracker.dto.response.CommentResponse;
import org.springframework.stereotype.Component;

@Component
public class CommentMapper {

    public CommentResponse toResponse(IssueComment comment){
        return new CommentResponse(
                comment.getId(),
                comment.getIssue().getId(),
                comment.getAuthor().getId(),
                comment.getAuthor().getUsername(),
                comment.getText(),
                comment.getCreatedAt(),
                comment.getEditedAt()
        );
    }
}

package com.example.tasktracker.mapper;

import com.example.tasktracker.domain.User;
import com.example.tasktracker.dto.request.RegisterUserRequest;
import com.example.tasktracker.dto.response.UserResponse;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public UserResponse toResponse(User user){
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getCreatedAt()
        );
    }

    public User toEntity(RegisterUserRequest request, String passwordHash){
        return new User(request.email(), request.username(), passwordHash);
    }
}

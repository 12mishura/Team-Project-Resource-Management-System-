package com.example.tasktracker.service;

import com.example.tasktracker.domain.User;
import com.example.tasktracker.dto.request.ChangePasswordRequest;
import com.example.tasktracker.dto.request.UpdateProfileRequest;
import com.example.tasktracker.dto.response.UserResponse;
import com.example.tasktracker.exception.InvalidPasswordException;
import com.example.tasktracker.exception.UserAlreadyExistsException;
import com.example.tasktracker.exception.UserNotFoundException;
import com.example.tasktracker.mapper.UserMapper;
import com.example.tasktracker.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public UserResponse updateProfile(String email, UpdateProfileRequest request){
        User user = findUser(email);
        if(!request.username().equalsIgnoreCase(user.getUsername())
                && userRepository.existsByUsernameIgnoreCase(request.username())){
            throw new UserAlreadyExistsException("username", request.username());
        }
        user.setUsername(request.username());
        userRepository.flush();
        return userMapper.toResponse(user);
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request){
        User user = findUser(email);
        if(!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())){
            throw new InvalidPasswordException("Current password is incorrect");
        }
        if(passwordEncoder.matches(request.newPassword(), user.getPasswordHash())){
            throw new InvalidPasswordException("New password must be different from the current one");
        }
        user.changePassword(passwordEncoder.encode(request.newPassword()));
        userRepository.flush();
        refreshTokenService.revokeAll(user.getId());
    }

    private User findUser(String email){
        return userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException(email));
    }
}

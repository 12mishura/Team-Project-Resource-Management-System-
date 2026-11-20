package com.example.tasktracker.service;

import com.example.tasktracker.domain.User;
import com.example.tasktracker.dto.request.LoginUserRequest;
import com.example.tasktracker.dto.request.RefreshTokenRequest;
import com.example.tasktracker.dto.request.RegisterUserRequest;
import com.example.tasktracker.dto.response.AuthResponse;
import com.example.tasktracker.dto.response.UserResponse;
import com.example.tasktracker.exception.InvalidRefreshTokenException;
import com.example.tasktracker.exception.UserAlreadyExistsException;
import com.example.tasktracker.exception.UserNotFoundException;
import com.example.tasktracker.mapper.UserMapper;
import com.example.tasktracker.repository.UserRepository;
import com.example.tasktracker.security.JwtService;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class AuthService {
    private static final String TOKEN_TYPE = "Bearer";

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserDetailsService userDetailsService;

    @Transactional
    public UserResponse register(RegisterUserRequest request){
        if(userRepository.existsByEmail(request.email())){
            throw new UserAlreadyExistsException("email", request.email());
        }
        if(userRepository.existsByUsernameIgnoreCase(request.username())){
            throw new UserAlreadyExistsException("username", request.username());
        }
        User user = userMapper.toEntity(request, passwordEncoder.encode(request.password()));
        userRepository.saveAndFlush(user);
        return userMapper.toResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginUserRequest request){
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UserNotFoundException(userDetails.getUsername()));
        return tokensFor(user, userDetails);
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public AuthResponse refresh(RefreshTokenRequest request){
        User user = refreshTokenService.consume(request.refreshToken());
        return tokensFor(user, userDetailsService.loadUserByUsername(user.getEmail()));
    }

    public void logout(RefreshTokenRequest request){
        refreshTokenService.revoke(request.refreshToken());
    }

    private AuthResponse tokensFor(User user, UserDetails userDetails){
        return new AuthResponse(
                jwtService.generateToken(userDetails),
                refreshTokenService.issue(user),
                TOKEN_TYPE,
                jwtService.getExpirationSeconds(),
                jwtService.getRefreshExpirationSeconds());
    }

    public UserResponse getCurrentUser(String email){
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException(email));
        return userMapper.toResponse(user);
    }
}

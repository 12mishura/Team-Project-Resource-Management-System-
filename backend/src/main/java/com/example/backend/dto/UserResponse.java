package com.example.backend.DTO;

import com.example.backend.Entity.Enum.OrgRole;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private UUID id;

    private String username;

    private String email;

    private OrgRole role;

    private UUID organizationId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

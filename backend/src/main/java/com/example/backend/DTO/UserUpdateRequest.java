package com.example.backend.DTO;

import com.example.backend.Entity.Enum.OrgRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUpdateRequest {

    @Size(max = 100)
    private String username;

    @Email
    @Size(max = 100)
    private String email;

    private OrgRole role;
}

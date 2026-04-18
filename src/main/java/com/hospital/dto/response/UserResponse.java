package com.hospital.dto.response;

import com.hospital.model.Role;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO for User data in API responses.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String username;
    private String phone;
    private Role role;
    private boolean enabled;
    private LocalDateTime createdAt;
}

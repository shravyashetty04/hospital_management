package com.hospital.dto.request;

import com.hospital.model.Role;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Request payload for registering a new system user.
 *
 * <p>Aligned to {@link com.hospital.model.User} which has:
 * {@code name}, {@code email}, {@code password}, {@code role}.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    /** Full display name of the user (e.g., "Jane Smith"). */
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    /** Unique email address — used as the login identifier. */
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid address")
    private String email;

    /** Plain-text password — will be BCrypt-hashed before storage. */
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    /**
     * Role assigned to this account.
     * Allowed values: ROLE_ADMIN, ROLE_DOCTOR, ROLE_PATIENT
     */
    @NotNull(message = "Role is required")
    private Role role;
}

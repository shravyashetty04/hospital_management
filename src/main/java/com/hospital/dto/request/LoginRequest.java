package com.hospital.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Request payload for the login endpoint.
 *
 * <p>Uses {@code email} as the identifier (aligned with the
 * {@link com.hospital.model.User} entity which has no {@code username} field).</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    /** The user's registered email address. */
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid address")
    private String email;

    /** The user's plain-text password (matched against the BCrypt hash). */
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
}

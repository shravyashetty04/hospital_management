package com.hospital.dto.response;

import com.hospital.model.Role;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Response payload returned after a successful login or registration.
 *
 * <p>Contains the JWT access token, refresh token, and enough user
 * context for the client to avoid an extra profile fetch.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    // ── JWT tokens ───────────────────────────────────────────────────────────

    /** Short-lived JWT used to authenticate API calls (default: 24 h). */
    private String accessToken;

    /** Long-lived token used to obtain a new access token (default: 7 days). */
    private String refreshToken;

    /** Always "Bearer". */
    @Builder.Default
    private String tokenType = "Bearer";

    // ── User context ─────────────────────────────────────────────────────────

    private Long   userId;   // User.id
    private String name;     // User.name
    private String email;    // User.email
    private Role   role;     // User.role

    // ── Token metadata ───────────────────────────────────────────────────────

    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;
}

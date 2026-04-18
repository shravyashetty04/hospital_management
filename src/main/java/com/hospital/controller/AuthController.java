package com.hospital.controller;

import com.hospital.dto.request.LoginRequest;
import com.hospital.dto.request.RegisterRequest;
import com.hospital.dto.response.ApiResponse;
import com.hospital.dto.response.AuthResponse;
import com.hospital.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public REST controller for authentication.
 *
 * <p>All endpoints here are <b>permit-all</b> (no JWT required) — configured
 * in {@link com.hospital.config.SecurityConfig}.</p>
 *
 * <h3>Base path</h3>
 * {@code /api/v1/auth}
 *
 * <h3>Endpoints</h3>
 * <pre>
 *  POST /register  → creates account, returns tokens
 *  POST /login     → verifies email+password, returns tokens
 *  POST /refresh   → rotates tokens using a valid refresh token
 * </pre>
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "1. Authentication",
     description = "Public endpoints: register, login, refresh token — no JWT required")
public class AuthController {

    private final AuthService authService;

    // ── POST /auth/register ───────────────────────────────────────────────────

    /**
     * Register a new user account and receive JWT tokens immediately.
     *
     * <p>Request body must include: {@code name}, {@code email},
     * {@code password} (min 8 chars), {@code role}.</p>
     *
     * <p>HTTP 201 Created on success.</p>
     */
    @PostMapping("/register")
    @Operation(
        summary = "Register a new user",
        description = "Creates an account for ADMIN / DOCTOR / PATIENT and returns JWT tokens."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201", description = "User registered",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", description = "Validation error or duplicate email")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        AuthResponse response = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Registration successful. Welcome!"));
    }

    // ── POST /auth/login ──────────────────────────────────────────────────────

    /**
     * Authenticate with email and password.
     *
     * <p>Returns a short-lived <b>access token</b> (24 h) and a long-lived
     * <b>refresh token</b> (7 days). Include the access token in subsequent
     * requests as: {@code Authorization: Bearer <accessToken>}.</p>
     */
    @PostMapping("/login")
    @Operation(
        summary = "Login — obtain JWT tokens",
        description = "Authenticates via email + password. Returns access & refresh tokens."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "Login successful"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401", description = "Invalid credentials")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(
                ApiResponse.success(response, "Login successful."));
    }

    // ── POST /auth/refresh ────────────────────────────────────────────────────

    /**
     * Issue a new access token from a valid refresh token.
     *
     * <p>Implements refresh-token rotation: the server returns a new refresh
     * token on each call. The client should replace the stored one.</p>
     *
     * @param refreshToken the refresh token previously issued at login/register
     */
    @PostMapping("/refresh")
    @Operation(
        summary = "Refresh access token",
        description = "Rotates the refresh token and issues a new access token (7-day refresh, 24-h access)."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "Tokens refreshed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", description = "Refresh token invalid or expired")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @RequestParam String refreshToken) {

        AuthResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(
                ApiResponse.success(response, "Token refreshed successfully."));
    }
}

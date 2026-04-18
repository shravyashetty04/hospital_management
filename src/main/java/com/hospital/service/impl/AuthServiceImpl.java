package com.hospital.service.impl;

import com.hospital.dto.request.LoginRequest;
import com.hospital.dto.request.RegisterRequest;
import com.hospital.dto.response.AuthResponse;
import com.hospital.exception.BadRequestException;
import com.hospital.model.User;
import com.hospital.repository.UserRepository;
import com.hospital.security.JwtTokenProvider;
import com.hospital.security.UserPrincipal;
import com.hospital.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Implementation of {@link AuthService} — handles user registration,
 * email/password login, and access-token refresh.
 *
 * <h3>Login flow</h3>
 * <pre>
 *  LoginRequest{email, password}
 *       │
 *       ▼
 *  AuthenticationManager.authenticate()
 *       │  ← DaoAuthenticationProvider calls CustomUserDetailsService.loadUserByUsername(email)
 *       │  ← BCrypt.matches(rawPassword, hashedPassword)
 *       │
 *       ▼ success → Authentication object
 *  JwtTokenProvider.generateAccessToken(authentication)  → signed JWT (24 h)
 *  JwtTokenProvider.generateRefreshToken(email)          → signed JWT (7 d)
 *       │
 *       ▼
 *  AuthResponse{ accessToken, refreshToken, userId, name, email, role, issuedAt, expiresAt }
 * </pre>
 *
 * <h3>Registration flow</h3>
 * <pre>
 *  RegisterRequest{name, email, password, role}
 *       │
 *       ├── existsByEmail check  → 400 if duplicate
 *       ├── BCrypt.encode(password)
 *       ├── save User
 *       └── generate tokens (same as login) → AuthResponse
 * </pre>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository       userRepository;
    private final PasswordEncoder      passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider     jwtTokenProvider;

    // ── Register ──────────────────────────────────────────────────────────────

    /**
     * Registers a new user and returns JWT tokens so the client is
     * immediately logged in after signing up.
     */
    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {

        // 1. Guard: duplicate email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException(
                    "An account with email '" + request.getEmail() + "' already exists.");
        }

        // 2. Persist user (password BCrypt-hashed)
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build();

        userRepository.save(user);
        log.info("[REGISTER] New user: id={}, email={}, role={}",
                 user.getId(), user.getEmail(), user.getRole());

        // 3. Generate tokens (no Authentication object exists yet at registration)
        String accessToken  = jwtTokenProvider.generateAccessToken(
                user.getEmail(), user.getId(), user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    /**
     * Authenticates via email + password and returns JWT tokens.
     *
     * <p>{@link AuthenticationManager#authenticate} will throw
     * {@link org.springframework.security.authentication.BadCredentialsException}
     * if credentials are wrong — caught by {@link com.hospital.exception.GlobalExceptionHandler}
     * and returned as HTTP 401.</p>
     */
    @Override
    public AuthResponse login(LoginRequest request) {

        // 1. Delegate to Spring Security (loads user by email, checks BCrypt hash)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),    // Spring Security "username" = email
                        request.getPassword()
                )
        );

        // 2. Extract the principal built by CustomUserDetailsService
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        // 3. Load full User for the response fields (name, role enum)
        User user = userRepository.findByEmail(principal.getEmail())
                .orElseThrow(() -> new BadRequestException("User not found."));

        // 4. Generate tokens
        String accessToken  = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        log.info("[LOGIN] User authenticated: id={}, email={}, role={}",
                 user.getId(), user.getEmail(), user.getRole());

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    // ── Refresh Token ─────────────────────────────────────────────────────────

    /**
     * Issues a new access token (and rotates the refresh token) when the
     * client provides a valid refresh token.
     *
     * <p>Refresh token rotation means the old refresh token is effectively
     * invalidated after each use — the client must store the new one.</p>
     */
    @Override
    public AuthResponse refreshToken(String refreshToken) {

        // 1. Validate the refresh token
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BadRequestException("Refresh token is invalid or expired.");
        }

        // 2. Identify the user from the token subject (email)
        String email = jwtTokenProvider.getEmailFromToken(refreshToken);
        User   user  = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException(
                        "User not found for email: " + email));

        // 3. Issue fresh tokens (rotation)
        String newAccessToken  = jwtTokenProvider.generateAccessToken(
                user.getEmail(), user.getId(), user.getRole().name());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

        log.info("[REFRESH] Tokens rotated for: id={}, email={}", user.getId(), user.getEmail());

        return buildAuthResponse(user, newAccessToken, newRefreshToken);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    /**
     * Builds the {@link AuthResponse} DTO from a persisted {@link User} and
     * newly minted JWT strings.
     */
    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        LocalDateTime issuedAt  = LocalDateTime.now();
        LocalDateTime expiresAt = jwtTokenProvider
                .getExpirationFromToken(accessToken)
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
    }
}

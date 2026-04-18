package com.hospital.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

/**
 * Stateless JWT utility: generates, parses, and validates HS256-signed tokens.
 *
 * <h3>Token anatomy</h3>
 * <pre>
 * Header  : { "alg": "HS256", "typ": "JWT" }
 * Payload : {
 *   "sub"  : "user@example.com",   ← email (Spring Security "username")
 *   "id"   : 42,                   ← User.id  (for fast DB lookup)
 *   "role" : "ROLE_DOCTOR",        ← Role name (avoid extra DB round-trip)
 *   "iat"  : <issued-at epoch>,
 *   "exp"  : <expiry epoch>
 * }
 * Signature: HMAC-SHA256(base64Url(header) + "." + base64Url(payload), secret)
 * </pre>
 *
 * <h3>Configuration keys (application.properties)</h3>
 * <pre>
 * app.jwt.secret              = &lt;at-least-32-character random string&gt;
 * app.jwt.expiration-ms       = 86400000     # 24 h
 * app.jwt.refresh-expiration-ms = 604800000  # 7 days
 * </pre>
 */
@Component
@Slf4j
public class JwtTokenProvider {

    // Custom claim keys embedded in the token payload
    private static final String CLAIM_USER_ID = "id";
    private static final String CLAIM_ROLE    = "role";

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    // ── Signing Key ───────────────────────────────────────────────────────────

    /**
     * Derives a {@link Key} from the configured secret.
     * Using raw UTF-8 bytes ensures the key is deterministic and avoids
     * the double-encode bug in the original implementation.
     */
    private Key signingKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    // ── Token Generation ──────────────────────────────────────────────────────

    /**
     * Generates an <b>access token</b> from the fully authenticated principal.
     * Embeds {@code id} and {@code role} as extra claims so controllers
     * can read them without an extra DB query.
     *
     * @param authentication the successful Spring Security authentication object
     * @return signed JWT string
     */
    public String generateAccessToken(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return buildToken(principal.getEmail(), principal.getId(),
                          principal.getAuthorities().iterator().next().getAuthority(),
                          jwtExpirationMs);
    }

    /**
     * Generates an <b>access token</b> directly from a {@link com.hospital.model.User}.
     * Used during registration when no {@link Authentication} object exists yet.
     *
     * @param email  the user's email (JWT subject)
     * @param userId the user's DB primary key
     * @param role   the role string (e.g., "ROLE_PATIENT")
     * @return signed JWT string
     */
    public String generateAccessToken(String email, Long userId, String role) {
        return buildToken(email, userId, role, jwtExpirationMs);
    }

    /**
     * Generates a <b>refresh token</b>.
     * Carries only the subject (email) — no role/id claims needed since
     * it is only used to mint a new access token.
     *
     * @param email the user's email
     * @return signed JWT string with a longer expiry
     */
    public String generateRefreshToken(String email) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + refreshExpirationMs);
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(signingKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // ── Token Parsing ─────────────────────────────────────────────────────────

    /**
     * Extracts the email address (JWT subject) from a token.
     *
     * @param token a validated JWT string
     * @return email stored in the {@code sub} claim
     */
    public String getEmailFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Extracts the {@code User.id} claim embedded in the access token.
     *
     * @param token a validated JWT string
     * @return the user's DB primary key
     */
    public Long getUserIdFromToken(String token) {
        return parseClaims(token).get(CLAIM_USER_ID, Long.class);
    }

    /**
     * Extracts the role claim embedded in the access token.
     *
     * @param token a validated JWT string
     * @return role string e.g. "ROLE_DOCTOR"
     */
    public String getRoleFromToken(String token) {
        return parseClaims(token).get(CLAIM_ROLE, String.class);
    }

    /**
     * Returns the expiry {@link Date} of the token.
     *
     * @param token any JWT string (access or refresh)
     * @return expiry date
     */
    public Date getExpirationFromToken(String token) {
        return parseClaims(token).getExpiration();
    }

    // ── Validation ────────────────────────────────────────────────────────────

    /**
     * Validates the token's signature, structure, and expiry.
     *
     * @param token JWT string from the {@code Authorization: Bearer} header
     * @return {@code true} if the token is valid; {@code false} otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(signingKey())
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.error("[JWT] Invalid signature / malformed token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("[JWT] Token expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("[JWT] Unsupported JWT: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("[JWT] Empty or null JWT claims: {}", e.getMessage());
        }
        return false;
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private String buildToken(String subject, Long userId, String role, long expirationMs) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .setSubject(subject)               // email
                .claim(CLAIM_USER_ID, userId)      // User.id
                .claim(CLAIM_ROLE, role)            // e.g. ROLE_PATIENT
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(signingKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}

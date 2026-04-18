package com.hospital.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Intercepts every HTTP request <b>once</b> and, if a valid JWT is found in
 * the {@code Authorization} header, populates the Spring Security
 * {@link org.springframework.security.core.context.SecurityContext}.
 *
 * <h3>Filter flow</h3>
 * <pre>
 *  HTTP Request
 *       │
 *       ▼
 *  extractToken()  ──── no token ──────────────────────▶  filterChain (unauthenticated)
 *       │
 *       ▼ token present
 *  jwtTokenProvider.validateToken()
 *       │
 *       ├── invalid ────────────────────────────────────▶  filterChain (unauthenticated)
 *       │
 *       ▼ valid
 *  getEmailFromToken()
 *       │
 *       ▼
 *  userDetailsService.loadUserByUsername(email)
 *       │
 *       ▼
 *  SecurityContextHolder.setAuthentication(...)
 *       │
 *       ▼
 *  filterChain (authenticated — @PreAuthorize / hasRole() now works)
 * </pre>
 *
 * <h3>Why {@link OncePerRequestFilter}?</h3>
 * <p>Guarantees this filter runs exactly once per request even in
 * async / forwarded-request scenarios, preventing duplicate authentication.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX    = "Bearer ";
    private static final String AUTH_HEADER       = "Authorization";

    private final JwtTokenProvider         jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest  request,
                                    HttpServletResponse response,
                                    FilterChain         filterChain)
            throws ServletException, IOException {

        try {
            String token = extractBearerToken(request);

            if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {

                // Extract the email (JWT subject) and load the full UserDetails
                String      email       = jwtTokenProvider.getEmailFromToken(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                // Build an authentication token and inject it into the SecurityContext
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,                          // credentials not needed post-auth
                                userDetails.getAuthorities()); // roles from UserPrincipal

                // Attach request metadata (IP, session id) for audit / logging
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("[JWT Filter] Authenticated user: {} | URI: {}",
                          email, request.getRequestURI());
            }

        } catch (Exception ex) {
            // Never let a filter exception leak into the response —
            // log it and let Spring Security return a 401 via the entry point.
            log.error("[JWT Filter] Could not set user authentication: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extracts the raw JWT string from the {@code Authorization: Bearer <token>} header.
     *
     * @param request the incoming HTTP request
     * @return the JWT string, or {@code null} if the header is absent / malformed
     */
    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader(AUTH_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}

package com.hospital.config;

import com.hospital.security.CustomUserDetailsService;
import com.hospital.security.JwtAuthenticationEntryPoint;
import com.hospital.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Central Spring Security configuration.
 *
 * <h3>Architecture overview</h3>
 * <pre>
 *  HTTP Request
 *      │
 *      ▼
 *  JwtAuthenticationFilter          ← validates Bearer token, populates SecurityContext
 *      │
 *      ▼
 *  SecurityFilterChain rules         ← permit PUBLIC_URLS; enforce roles on the rest
 *      │
 *      ├── /auth/**          → permitAll   (login, register, refresh)
 *      ├── GET /doctors/**   → permitAll   (public doctor listing)
 *      ├── /appointments/**  → authenticated (roles enforced at method level)
 *      ├── /doctors/**       → ADMIN | DOCTOR
 *      ├── /patients/**      → ADMIN | PATIENT
 *      └── everything else   → authenticated
 * </pre>
 *
 * <h3>Session strategy</h3>
 * <p>STATELESS — no HttpSession is created or used. Each request must carry
 * a valid JWT in the {@code Authorization: Bearer <token>} header.</p>
 *
 * <h3>Role-based access</h3>
 * <p>Coarse-grained rules are defined here in the filter chain.
 * Fine-grained rules (e.g., a patient can only view their own appointments)
 * are handled with {@code @PreAuthorize} at the service/controller method level,
 * enabled by {@link EnableMethodSecurity}.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService    userDetailsService;
    private final JwtAuthenticationFilter     jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final CorsConfigurationSource     corsConfigurationSource;

    // ── Public URLs ───────────────────────────────────────────────────────────

    /** Endpoints that require NO authentication. */
    private static final String[] PUBLIC_URLS = {
            "/auth/**",          // register, login, refresh
            "/swagger-ui/**",    // Swagger UI static resources
            "/swagger-ui.html",
            "/v3/api-docs/**",   // OpenAPI spec JSON
            "/api-docs/**"
    };

    // ── Beans ─────────────────────────────────────────────────────────────────

    /**
     * BCrypt password encoder (strength 12 rounds).
     * Used by {@link DaoAuthenticationProvider} to verify passwords at login
     * and by {@link com.hospital.service.impl.AuthServiceImpl} to hash them at registration.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Wires the {@link CustomUserDetailsService} and {@link PasswordEncoder}
     * into Spring's authentication pipeline.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Exposes the {@link AuthenticationManager} as a bean so that
     * {@link com.hospital.service.impl.AuthServiceImpl} can call
     * {@code authenticationManager.authenticate(...)} during login.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg)
            throws Exception {
        return cfg.getAuthenticationManager();
    }

    // ── Security Filter Chain ─────────────────────────────────────────────────

    /**
     * Builds and returns the main {@link SecurityFilterChain}.
     *
     * <p>Key decisions:</p>
     * <ul>
     *   <li>CSRF disabled — APIs are stateless; CSRF tokens are only needed for
     *       browser form-based sessions.</li>
     *   <li>Session creation policy STATELESS — Spring Security never creates
     *       an {@code HttpSession}.</li>
     *   <li>{@link JwtAuthenticationFilter} runs before
     *       {@link UsernamePasswordAuthenticationFilter} so that JWT-authenticated
     *       requests are handled before the default form-login flow.</li>
     * </ul>
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            // 1. CORS — must be first so OPTIONS pre-flight requests are handled
            .cors(cors -> cors.configurationSource(corsConfigurationSource))

            // 2. Disable CSRF (stateless REST API)
            .csrf(AbstractHttpConfigurer::disable)

            // 2. Return structured 401 JSON for unauthenticated access
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint(jwtAuthenticationEntryPoint))

            // 3. No HTTP sessions — fully stateless
            .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 4. URL-level authorization rules
            .authorizeHttpRequests(auth -> auth

                    // Public — no token required
                    .requestMatchers(PUBLIC_URLS).permitAll()

                    // Doctor listing is public (patients browse without login)
                    .requestMatchers(HttpMethod.GET, "/doctors/**").permitAll()

                    // Appointments — all roles can access; fine-grained via @PreAuthorize
                    .requestMatchers("/appointments/**").authenticated()

                    // Doctor management — ADMIN or DOCTOR
                    .requestMatchers("/doctors/**").hasAnyRole("ADMIN", "DOCTOR")

                    // Patient management — ADMIN or PATIENT
                    .requestMatchers("/patients/**").hasAnyRole("ADMIN", "PATIENT")

                    // Everything else — must be authenticated
                    .anyRequest().authenticated()
            )

            // 5. Register the DAO authentication provider
            .authenticationProvider(authenticationProvider())

            // 6. Insert JWT filter before Spring's own username/password filter
            .addFilterBefore(jwtAuthenticationFilter,
                             UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

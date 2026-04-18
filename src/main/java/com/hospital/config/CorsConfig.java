package com.hospital.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * CORS configuration allowing the React frontend (localhost:3000)
 * to call the Spring Boot backend (localhost:8080).
 *
 * <h3>Allowed origins</h3>
 * <ul>
 *   <li>{@code http://localhost:3000} — Create React App dev server</li>
 *   <li>{@code http://localhost:5173} — Vite dev server (if migrated)</li>
 * </ul>
 *
 * <p>In production, replace the allowed origins with your actual
 * frontend domain (e.g., {@code https://your-app.vercel.app}).</p>
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // ── Allowed origins ──────────────────────────────────────────────────
        config.setAllowedOrigins(List.of(
                "http://localhost:3000",   // CRA dev server
                "http://localhost:5173"    // Vite dev server
        ));

        // ── Allowed HTTP methods ─────────────────────────────────────────────
        config.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        // ── Allowed headers ──────────────────────────────────────────────────
        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With"
        ));

        // ── Expose Authorization header to JS ────────────────────────────────
        config.setExposedHeaders(List.of("Authorization"));

        // Allow cookies / credentials (needed if you use HttpOnly cookies later)
        config.setAllowCredentials(true);

        // Cache pre-flight response for 1 hour
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);  // apply to all endpoints

        return new CorsFilter(source);
    }

    /**
     * Exposes the CORS configuration as a {@link CorsConfigurationSource}
     * bean so {@link SecurityConfig} can reference it explicitly via
     * {@code http.cors(c -> c.configurationSource(corsConfigurationSource()))}.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:5173"
        ));
        config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization","Content-Type","Accept","Origin","X-Requested-With"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

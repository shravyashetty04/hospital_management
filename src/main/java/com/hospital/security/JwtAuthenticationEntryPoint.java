package com.hospital.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hospital.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handles unauthenticated requests reaching a protected endpoint.
 *
 * <p>Spring Security calls {@link #commence} when:</p>
 * <ul>
 *   <li>No {@code Authorization} header is present.</li>
 *   <li>The JWT token is missing, malformed, or expired.</li>
 * </ul>
 *
 * <p>Returns a structured {@link ApiResponse} JSON body with HTTP 401
 * instead of the default Spring Security HTML error page.</p>
 *
 * <h3>Example response</h3>
 * <pre>
 * HTTP/1.1 401 Unauthorized
 * Content-Type: application/json
 *
 * {
 *   "success"   : false,
 *   "statusCode": 401,
 *   "message"   : "Unauthorized: Full authentication is required to access this resource",
 *   "timestamp" : "2025-08-15T10:30:00"
 * }
 * </pre>
 */
@Component
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /**
     * ObjectMapper configured with {@link JavaTimeModule} so that
     * {@link java.time.LocalDateTime} fields in {@link ApiResponse} serialize
     * correctly to ISO-8601 strings (not arrays).
     */
    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint() {
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void commence(HttpServletRequest  request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        log.warn("[401] Unauthorized request to '{}': {}",
                 request.getRequestURI(), authException.getMessage());

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpStatus.UNAUTHORIZED.value());

        ApiResponse<Void> body = ApiResponse.error(
                "Unauthorized: " + authException.getMessage(),
                HttpStatus.UNAUTHORIZED.value());

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}

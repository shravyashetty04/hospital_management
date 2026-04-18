package com.hospital.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Request payload for creating or updating a {@link com.hospital.model.Patient} profile.
 *
 * <p>Fields map to {@code Patient}: {@code age}, {@code medicalHistory}.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientRequest {

    @NotNull(message = "Age is required")
    @Min(value = 0, message = "Age must be 0 or greater")
    @Max(value = 150, message = "Age must be realistic (≤ 150)")
    private Integer age;

    @Size(max = 5000, message = "Medical history must not exceed 5000 characters")
    private String medicalHistory;
}

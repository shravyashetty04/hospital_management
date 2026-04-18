package com.hospital.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalTime;

/**
 * Request payload for creating or updating a {@link com.hospital.model.Doctor} profile.
 *
 * <p>Fields map to {@code Doctor}: {@code specialization},
 * {@code availableFrom}, {@code availableTo}.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorRequest {

    @NotBlank(message = "Specialization is required")
    @Size(max = 100, message = "Specialization must not exceed 100 characters")
    private String specialization;

    @NotNull(message = "Available-from time is required")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime availableFrom;

    @NotNull(message = "Available-to time is required")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime availableTo;
}

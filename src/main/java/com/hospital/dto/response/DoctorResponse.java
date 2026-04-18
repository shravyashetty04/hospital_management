package com.hospital.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalTime;

/**
 * Response payload carrying full doctor profile details.
 *
 * <p>Flattens {@link com.hospital.model.Doctor} + linked
 * {@link com.hospital.model.User} into a single DTO.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorResponse {

    private Long id;                    // Doctor.id

    // ── Linked user info ─────────────────────────────────────────────────────
    private Long userId;                // Doctor.user.id
    private String name;                // Doctor.user.name
    private String email;               // Doctor.user.email

    // ── Professional details ─────────────────────────────────────────────────
    private String specialization;      // Doctor.specialization

    @JsonFormat(pattern = "HH:mm")
    private LocalTime availableFrom;    // Doctor.availableFrom

    @JsonFormat(pattern = "HH:mm")
    private LocalTime availableTo;      // Doctor.availableTo
}

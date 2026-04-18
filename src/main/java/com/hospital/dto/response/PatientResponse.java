package com.hospital.dto.response;

import lombok.*;

/**
 * Response payload carrying full patient profile details.
 *
 * <p>Flattens {@link com.hospital.model.Patient} + linked
 * {@link com.hospital.model.User} into a single DTO.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientResponse {

    private Long id;                // Patient.id

    // ── Linked user info ─────────────────────────────────────────────────────
    private Long userId;            // Patient.user.id
    private String name;            // Patient.user.name
    private String email;           // Patient.user.email

    // ── Medical profile ──────────────────────────────────────────────────────
    private int age;                // Patient.age
    private String medicalHistory;  // Patient.medicalHistory
}

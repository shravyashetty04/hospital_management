package com.hospital.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hospital.model.AppointmentStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Response payload carrying full appointment details.
 *
 * <p>Maps from {@link com.hospital.model.Appointment} plus the linked
 * doctor and patient names for convenience.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentResponse {

    private Long id;

    // ── Patient info ─────────────────────────────────────────────────────────
    private Long patientId;
    private String patientName;     // patient.user.name

    // ── Doctor info ──────────────────────────────────────────────────────────
    private Long doctorId;
    private String doctorName;      // doctor.user.name
    private String specialization;  // doctor.specialization

    // ── Appointment details ──────────────────────────────────────────────────
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime time;

    private AppointmentStatus status;
}

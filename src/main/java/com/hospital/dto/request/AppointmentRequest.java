package com.hospital.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Request payload for booking or rescheduling an appointment.
 *
 * <p>Fields map directly to {@link com.hospital.model.Appointment}:
 * {@code doctor}, {@code date}, {@code time}.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentRequest {

    /** ID of the {@link com.hospital.model.Doctor} to book with. */
    @NotNull(message = "Doctor ID is required")
    private Long doctorId;

    /**
     * The calendar date of the appointment.
     * Must be today or a future date.
     */
    @NotNull(message = "Appointment date is required")
    @FutureOrPresent(message = "Appointment date must be today or in the future")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    /**
     * The clock time of the appointment (24-hour format).
     * Must fall within the doctor's availableFrom–availableTo window
     * (validated at the service layer).
     */
    @NotNull(message = "Appointment time is required")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime time;
}

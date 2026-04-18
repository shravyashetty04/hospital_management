package com.hospital.model;

/**
 * Lifecycle states of a hospital appointment.
 *
 * <ul>
 *   <li>{@code PENDING}   — Booked but not yet reviewed by the doctor.</li>
 *   <li>{@code CONFIRMED} — Doctor has confirmed the slot.</li>
 *   <li>{@code CANCELLED} — Cancelled by patient or admin.</li>
 *   <li>{@code COMPLETED} — Appointment has taken place.</li>
 *   <li>{@code NO_SHOW}   — Patient did not attend.</li>
 * </ul>
 */
public enum AppointmentStatus {
    PENDING,
    CONFIRMED,
    CANCELLED,
    COMPLETED,
    NO_SHOW
}

package com.hospital.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Represents a scheduled appointment between a {@link Patient} and a {@link Doctor}.
 *
 * <p>Relationships:</p>
 * <ul>
 *   <li><b>ManyToOne → Doctor</b> — Many appointments can belong to one doctor.</li>
 *   <li><b>ManyToOne → Patient</b> — Many appointments can belong to one patient.</li>
 * </ul>
 *
 * <pre>
 *  Appointment ──(ManyToOne)──▶ Doctor
 *  Appointment ──(ManyToOne)──▶ Patient
 * </pre>
 *
 * The inverse side (OneToMany) is declared in both {@link Doctor} and {@link Patient}
 * using {@code mappedBy = "doctor"} / {@code mappedBy = "patient"}.
 */
@Entity
@Table(name = "appointments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ManyToOne → Doctor
     * Many appointments are linked to a single doctor.
     * LAZY loading avoids unnecessary JOINs when iterating appointment lists.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_appointment_doctor"))
    private Doctor doctor;

    /**
     * ManyToOne → Patient
     * Many appointments are linked to a single patient.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_appointment_patient"))
    private Patient patient;

    /** Calendar date of the appointment (e.g., 2025-08-15). */
    @Column(nullable = false)
    private LocalDate date;

    /** Clock time of the appointment (e.g., 10:30). */
    @Column(nullable = false)
    private LocalTime time;

    /**
     * Current lifecycle state of this appointment.
     * Stored as a string for readability in the DB.
     * Defaults to {@link AppointmentStatus#PENDING} on creation.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AppointmentStatus status = AppointmentStatus.PENDING;
}

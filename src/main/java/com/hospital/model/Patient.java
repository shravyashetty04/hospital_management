package com.hospital.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

/**
 * Represents a Patient's medical profile.
 *
 * <p>Relationships:</p>
 * <ul>
 *   <li><b>ManyToOne → User</b> — A patient IS a user (account holder).</li>
 *   <li><b>OneToMany → Appointment</b> — A patient can have many appointments.</li>
 * </ul>
 *
 * <pre>
 *  Patient ──(ManyToOne)──▶ User
 *  Patient ──(OneToMany)──▶ Appointment
 * </pre>
 */
@Entity
@Table(name = "patients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "appointments")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ManyToOne → User
     * Links this patient profile to an existing user account.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_patient_user"))
    private User user;

    /** Patient's age in years. */
    @Column(nullable = false)
    private int age;

    /**
     * Free-text field storing known conditions, allergies, past surgeries, etc.
     * Stored as TEXT to accommodate long histories.
     */
    @Column(columnDefinition = "TEXT")
    private String medicalHistory;

    /**
     * OneToMany → Appointment
     * mappedBy = "patient" refers to the field name in {@link Appointment}.
     */
    @OneToMany(
        mappedBy = "patient",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    private List<Appointment> appointments;
}

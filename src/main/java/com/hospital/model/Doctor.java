package com.hospital.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.List;

/**
 * Represents a Doctor's professional profile.
 *
 * <p>Relationships:</p>
 * <ul>
 *   <li><b>ManyToOne → User</b> — A doctor IS a user (one user, one doctor profile).</li>
 *   <li><b>OneToMany → Appointment</b> — A doctor can have many appointments.</li>
 * </ul>
 *
 * <pre>
 *  Doctor ──(ManyToOne)──▶ User
 *  Doctor ──(OneToMany)──▶ Appointment
 * </pre>
 */
@Entity
@Table(name = "doctors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "appointments")
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ManyToOne → User
     * Multiple doctors cannot share the same user, but the FK column
     * is defined as ManyToOne to keep the mapping flexible for future
     * admin-created profiles. Use a UNIQUE constraint if strictly 1-to-1.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_doctor_user"))
    private User user;

    /** Medical specialization (e.g., Cardiology, Orthopedics). */
    @Column(nullable = false, length = 100)
    private String specialization;

    /** Start of the doctor's available working hours (e.g., 09:00). */
    @Column(nullable = false)
    private LocalTime availableFrom;

    /** End of the doctor's available working hours (e.g., 17:00). */
    @Column(nullable = false)
    private LocalTime availableTo;

    /**
     * OneToMany → Appointment
     * mappedBy = "doctor" refers to the field name in {@link Appointment}.
     * CascadeType.ALL: persisting/deleting a doctor cascades to appointments.
     * orphanRemoval: removes appointment rows when removed from this list.
     */
    @OneToMany(
        mappedBy = "doctor",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    private List<Appointment> appointments;
}

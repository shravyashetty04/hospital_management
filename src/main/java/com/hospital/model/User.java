package com.hospital.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Represents a system user (Admin, Doctor, or Patient).
 *
 * <p>Relationships:</p>
 * <ul>
 *   <li>{@link Doctor} has a OneToOne back-reference to this entity.</li>
 *   <li>{@link Patient} has a OneToOne back-reference to this entity.</li>
 * </ul>
 */
@Entity
@Table(
    name = "users",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_email", columnNames = "email")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "password")   // Never print password in logs
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Full display name of the user. */
    @Column(nullable = false, length = 100)
    private String name;

    /** Unique email address used for login / communication. */
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    /** BCrypt-hashed password — never stored in plain text. */
    @Column(nullable = false)
    private String password;

    /**
     * Role assigned to this user; determines access permissions.
     * Stored as a string (e.g., "ROLE_DOCTOR") for readability.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;
}

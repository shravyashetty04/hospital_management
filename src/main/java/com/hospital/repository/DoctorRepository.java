package com.hospital.repository;

import com.hospital.model.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Doctor} entity.
 *
 * <p>Inherits full CRUD from {@link JpaRepository}:</p>
 * <ul>
 *   <li>{@code save(Doctor)}          — INSERT or UPDATE</li>
 *   <li>{@code findById(Long)}         — SELECT by primary key</li>
 *   <li>{@code findAll()}              — SELECT all doctors</li>
 *   <li>{@code deleteById(Long)}       — DELETE by primary key</li>
 *   <li>{@code existsById(Long)}       — existence check by PK</li>
 *   <li>{@code count()}                — total row count</li>
 * </ul>
 *
 * <p>Custom query methods are aligned to the {@link Doctor} entity fields:
 * {@code user}, {@code specialization}, {@code availableFrom}, {@code availableTo}.</p>
 */
@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    // ── Lookup by linked User ────────────────────────────────────────────────

    /**
     * Find a doctor profile by the ID of the linked {@link com.hospital.model.User}.
     * Used to load a doctor's profile once the logged-in user is known.
     *
     * <p>Spring Data traverses the {@code doctor.user.id} path automatically.</p>
     *
     * <pre>SELECT * FROM doctors WHERE user_id = ?</pre>
     */
    Optional<Doctor> findByUserId(Long userId);

    /**
     * Check whether a doctor profile already exists for the given user ID.
     * Prevents creating duplicate profiles for a single user account.
     *
     * <pre>SELECT COUNT(*) > 0 FROM doctors WHERE user_id = ?</pre>
     */
    boolean existsByUserId(Long userId);

    // ── Specialization Queries ───────────────────────────────────────────────

    /**
     * Retrieve all doctors with a given specialization (case-insensitive).
     *
     * <pre>SELECT * FROM doctors WHERE LOWER(specialization) = LOWER(?)</pre>
     *
     * @param specialization e.g., "Cardiology", "orthopedics"
     */
    List<Doctor> findBySpecializationIgnoreCase(String specialization);

    /**
     * Search doctors whose specialization contains the given keyword (case-insensitive).
     * Useful for autocomplete / partial-match search.
     *
     * <pre>SELECT * FROM doctors WHERE LOWER(specialization) LIKE LOWER('%keyword%')</pre>
     */
    @Query("SELECT d FROM Doctor d WHERE LOWER(d.specialization) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Doctor> searchBySpecialization(@Param("keyword") String keyword);

    // ── Availability Queries ─────────────────────────────────────────────────

    /**
     * Retrieve all doctors who are available during a requested time slot.
     * A doctor is considered available if the requested time falls within
     * their {@code availableFrom}–{@code availableTo} window.
     *
     * <pre>
     * SELECT * FROM doctors
     * WHERE available_from <= :requestedTime
     *   AND available_to   >= :requestedTime
     * </pre>
     *
     * @param requestedTime the clock time the patient wants to book (e.g., 10:30)
     */
    @Query("SELECT d FROM Doctor d WHERE d.availableFrom <= :time AND d.availableTo >= :time")
    List<Doctor> findAvailableAt(@Param("time") LocalTime requestedTime);

    /**
     * Retrieve all doctors whose availability window overlaps with the given range.
     * Useful when listing doctors for a proposed time range.
     *
     * <pre>
     * SELECT * FROM doctors
     * WHERE available_from < :to
     *   AND available_to   > :from
     * </pre>
     *
     * @param from start of the desired time window
     * @param to   end of the desired time window
     */
    @Query("SELECT d FROM Doctor d WHERE d.availableFrom < :to AND d.availableTo > :from")
    List<Doctor> findAvailableBetween(@Param("from") LocalTime from,
                                      @Param("to") LocalTime to);

    // ── Name Search ──────────────────────────────────────────────────────────

    /**
     * Search doctors by the linked user's name (case-insensitive partial match).
     * Navigates the {@code doctor.user.name} association in JPQL.
     *
     * <pre>
     * SELECT d FROM Doctor d
     * WHERE LOWER(d.user.name) LIKE LOWER('%keyword%')
     * </pre>
     *
     * @param keyword partial name to search for
     */
    @Query("SELECT d FROM Doctor d WHERE LOWER(d.user.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Doctor> searchByUserName(@Param("keyword") String keyword);
}

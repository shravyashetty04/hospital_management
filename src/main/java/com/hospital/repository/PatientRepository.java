package com.hospital.repository;

import com.hospital.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Patient} entity.
 *
 * <p>Inherits full CRUD from {@link JpaRepository}:</p>
 * <ul>
 *   <li>{@code save(Patient)}         — INSERT or UPDATE</li>
 *   <li>{@code findById(Long)}         — SELECT by primary key</li>
 *   <li>{@code findAll()}              — SELECT all patients</li>
 *   <li>{@code deleteById(Long)}       — DELETE by primary key</li>
 *   <li>{@code existsById(Long)}       — existence check by PK</li>
 *   <li>{@code count()}                — total row count</li>
 * </ul>
 *
 * <p>Custom query methods are aligned to the {@link Patient} entity fields:
 * {@code user}, {@code age}, {@code medicalHistory}.</p>
 */
@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    // ── Lookup by linked User ────────────────────────────────────────────────

    /**
     * Find a patient profile by the ID of the linked {@link com.hospital.model.User}.
     * Used after authentication to load the patient's health profile.
     *
     * <pre>SELECT * FROM patients WHERE user_id = ?</pre>
     */
    Optional<Patient> findByUserId(Long userId);

    /**
     * Check whether a patient profile already exists for the given user ID.
     * Prevents creating duplicate profiles for a single user account.
     *
     * <pre>SELECT COUNT(*) > 0 FROM patients WHERE user_id = ?</pre>
     */
    boolean existsByUserId(Long userId);

    // ── Age-based Queries ────────────────────────────────────────────────────

    /**
     * Retrieve all patients within an inclusive age range.
     * Useful for age-segmented health reports.
     *
     * <pre>SELECT * FROM patients WHERE age BETWEEN ? AND ?</pre>
     *
     * @param minAge minimum age (inclusive)
     * @param maxAge maximum age (inclusive)
     */
    List<Patient> findByAgeBetween(int minAge, int maxAge);

    /**
     * Retrieve all patients older than the given age.
     *
     * <pre>SELECT * FROM patients WHERE age > ?</pre>
     */
    List<Patient> findByAgeGreaterThan(int age);

    // ── Medical History Search ───────────────────────────────────────────────

    /**
     * Search patients whose medical history contains the given keyword
     * (case-insensitive). Helpful for clinical queries (e.g., "diabetes").
     *
     * <pre>
     * SELECT * FROM patients
     * WHERE LOWER(medical_history) LIKE LOWER('%keyword%')
     * </pre>
     *
     * @param keyword term to look for inside medicalHistory
     */
    @Query("SELECT p FROM Patient p WHERE LOWER(p.medicalHistory) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Patient> searchByMedicalHistory(@Param("keyword") String keyword);

    // ── Name Search (via User) ───────────────────────────────────────────────

    /**
     * Search patients by the linked user's name (case-insensitive partial match).
     * Navigates the {@code patient.user.name} association in JPQL.
     *
     * <pre>
     * SELECT p FROM Patient p
     * WHERE LOWER(p.user.name) LIKE LOWER('%keyword%')
     * </pre>
     *
     * @param keyword partial name to search for
     */
    @Query("SELECT p FROM Patient p WHERE LOWER(p.user.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Patient> searchByUserName(@Param("keyword") String keyword);
}

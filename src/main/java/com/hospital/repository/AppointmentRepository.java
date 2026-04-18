package com.hospital.repository;

import com.hospital.model.Appointment;
import com.hospital.model.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Repository for {@link Appointment} entity.
 *
 * <p>Inherits full CRUD from {@link JpaRepository}:</p>
 * <ul>
 *   <li>{@code save(Appointment)}     — INSERT or UPDATE</li>
 *   <li>{@code findById(Long)}         — SELECT by primary key</li>
 *   <li>{@code findAll()}              — SELECT all appointments</li>
 *   <li>{@code deleteById(Long)}       — DELETE by primary key</li>
 *   <li>{@code existsById(Long)}       — existence check by PK</li>
 *   <li>{@code count()}                — total row count</li>
 * </ul>
 *
 * <p>Custom query methods are aligned to the {@link Appointment} entity fields:
 * {@code doctor}, {@code patient}, {@code date}, {@code time}, {@code status}.</p>
 */
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // ── Filter by Doctor ─────────────────────────────────────────────────────

    /**
     * Retrieve all appointments assigned to a specific doctor, ordered by date then time.
     *
     * <pre>SELECT * FROM appointments WHERE doctor_id = ? ORDER BY date, time</pre>
     */
    List<Appointment> findByDoctorIdOrderByDateAscTimeAsc(Long doctorId);

    /**
     * Retrieve all appointments for a doctor with a particular status.
     *
     * <pre>SELECT * FROM appointments WHERE doctor_id = ? AND status = ?</pre>
     */
    List<Appointment> findByDoctorIdAndStatus(Long doctorId, AppointmentStatus status);

    /**
     * Retrieve all appointments for a doctor on a specific date, ordered by time.
     *
     * <pre>SELECT * FROM appointments WHERE doctor_id = ? AND date = ? ORDER BY time</pre>
     */
    List<Appointment> findByDoctorIdAndDateOrderByTimeAsc(Long doctorId, LocalDate date);

    // ── Filter by Patient ────────────────────────────────────────────────────

    /**
     * Retrieve all appointments booked by a specific patient, ordered by date then time.
     *
     * <pre>SELECT * FROM appointments WHERE patient_id = ? ORDER BY date, time</pre>
     */
    List<Appointment> findByPatientIdOrderByDateAscTimeAsc(Long patientId);

    /**
     * Retrieve all appointments for a patient with a particular status.
     *
     * <pre>SELECT * FROM appointments WHERE patient_id = ? AND status = ?</pre>
     */
    List<Appointment> findByPatientIdAndStatus(Long patientId, AppointmentStatus status);

    // ── Filter by Date / Status ──────────────────────────────────────────────

    /**
     * Retrieve all appointments on a specific calendar date.
     *
     * <pre>SELECT * FROM appointments WHERE date = ? ORDER BY time</pre>
     */
    List<Appointment> findByDateOrderByTimeAsc(LocalDate date);

    /**
     * Retrieve all appointments within a date range (inclusive).
     *
     * <pre>SELECT * FROM appointments WHERE date BETWEEN ? AND ? ORDER BY date, time</pre>
     *
     * @param from start date (inclusive)
     * @param to   end date (inclusive)
     */
    List<Appointment> findByDateBetweenOrderByDateAscTimeAsc(LocalDate from, LocalDate to);

    /**
     * Retrieve all appointments with a given status, ordered by date and time.
     *
     * <pre>SELECT * FROM appointments WHERE status = ? ORDER BY date, time</pre>
     */
    List<Appointment> findByStatusOrderByDateAscTimeAsc(AppointmentStatus status);

    // ── Conflict Detection ───────────────────────────────────────────────────

    /**
     * Detect whether a doctor already has an active (non-cancelled, non-no-show)
     * appointment at the exact same date and time.
     *
     * <p>Use this before booking to prevent double-booking a slot.</p>
     *
     * <pre>
     * SELECT COUNT(*) > 0 FROM appointments
     * WHERE doctor_id = ?
     *   AND date      = ?
     *   AND time      = ?
     *   AND status NOT IN ('CANCELLED', 'NO_SHOW')
     * </pre>
     *
     * @param doctorId the doctor to check
     * @param date     requested appointment date
     * @param time     requested appointment time
     */
    @Query("""
           SELECT COUNT(a) > 0 FROM Appointment a
           WHERE a.doctor.id = :doctorId
             AND a.date      = :date
             AND a.time      = :time
             AND a.status NOT IN ('CANCELLED', 'NO_SHOW')
           """)
    boolean existsConflict(@Param("doctorId") Long doctorId,
                           @Param("date") LocalDate date,
                           @Param("time") LocalTime time);

    /**
     * Retrieve all conflicting (active) appointments for a doctor on a given date and time.
     * Returns the full list so the caller can inspect or report conflicts.
     *
     * <pre>
     * SELECT * FROM appointments
     * WHERE doctor_id = ?
     *   AND date      = ?
     *   AND time      = ?
     *   AND status NOT IN ('CANCELLED', 'NO_SHOW')
     * </pre>
     */
    @Query("""
           SELECT a FROM Appointment a
           WHERE a.doctor.id = :doctorId
             AND a.date      = :date
             AND a.time      = :time
             AND a.status NOT IN ('CANCELLED', 'NO_SHOW')
           """)
    List<Appointment> findConflicts(@Param("doctorId") Long doctorId,
                                    @Param("date") LocalDate date,
                                    @Param("time") LocalTime time);

    // ── Bulk Status Update ───────────────────────────────────────────────────

    /**
     * Bulk-cancel all PENDING appointments for a given doctor.
     * Useful when a doctor becomes unavailable (e.g., emergency leave).
     *
     * <pre>
     * UPDATE appointments
     * SET status = 'CANCELLED'
     * WHERE doctor_id = ? AND status = 'PENDING'
     * </pre>
     *
     * @param doctorId the doctor whose pending appointments should be cancelled
     * @return number of rows updated
     */
    @Modifying
    @Query("""
           UPDATE Appointment a
           SET    a.status = 'CANCELLED'
           WHERE  a.doctor.id = :doctorId
             AND  a.status    = 'PENDING'
           """)
    int cancelAllPendingByDoctor(@Param("doctorId") Long doctorId);

    // ── Ordered Full List ────────────────────────────────────────────────────

    /**
     * Retrieve all appointments ordered by date (ascending) then time (ascending).
     * Convenient for admin dashboards and scheduled reports.
     *
     * <pre>SELECT * FROM appointments ORDER BY date ASC, time ASC</pre>
     */
    List<Appointment> findAllByOrderByDateAscTimeAsc();
}

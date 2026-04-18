package com.hospital.service.impl;

import com.hospital.dto.request.AppointmentRequest;
import com.hospital.dto.response.AppointmentResponse;
import com.hospital.exception.BadRequestException;
import com.hospital.exception.ResourceNotFoundException;
import com.hospital.model.*;
import com.hospital.repository.AppointmentRepository;
import com.hospital.repository.DoctorRepository;
import com.hospital.repository.PatientRepository;
import com.hospital.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Core business logic for appointment management.
 *
 * <h3>Double-Booking Prevention Strategy</h3>
 * <p>Before any booking or reschedule, the service calls
 * {@link AppointmentRepository#existsConflict} which issues a single
 * {@code COUNT > 0} query checking that no active appointment
 * (status ≠ CANCELLED / NO_SHOW) already exists for the same
 * {@code (doctor, date, time)} triple. This guarantees that a doctor
 * can only have one appointment per time slot.</p>
 *
 * <h3>Availability Window Check</h3>
 * <p>The requested {@code time} is also validated against the doctor's
 * {@code availableFrom}–{@code availableTo} window before persisting.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional                     // write methods are transactional by default
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository     patientRepository;
    private final DoctorRepository      doctorRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // BOOK APPOINTMENT
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Books a new appointment after running all business-rule validations:
     * <ol>
     *   <li>Patient profile must exist (resolved from the User ID in the JWT).</li>
     *   <li>Doctor profile must exist.</li>
     *   <li>Requested time must be within the doctor's availability window.</li>
     *   <li>No active appointment for the same doctor at the same date+time
     *       (double-booking check).</li>
     * </ol>
     *
     * @param userId the {@code User.id} extracted from the JWT principal
     *               (NOT the Patient profile id — the service resolves the profile internally)
     */
    @Override
    public AppointmentResponse bookAppointment(Long userId, AppointmentRequest request) {

        // 1. Resolve patient profile from the User ID supplied by the JWT principal
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Patient profile not found for user ID: " + userId +
                        ". Please create a patient profile first."));

        // 2. Resolve doctor profile
        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", request.getDoctorId()));

        // 3. Validate time within doctor's availability window
        validateWithinAvailability(doctor, request);

        // 4. Double-booking guard  ←── KEY BUSINESS RULE
        checkForConflict(doctor.getId(), request);

        // 5. Persist the new appointment
        Appointment appointment = Appointment.builder()
                .patient(patient)
                .doctor(doctor)
                .date(request.getDate())
                .time(request.getTime())
                .status(AppointmentStatus.PENDING)
                .build();

        Appointment saved = appointmentRepository.save(appointment);

        log.info("[BOOKED] appointmentId={} | patient={} | doctor={} | {}_{}",
                saved.getId(), patient.getId(), doctor.getId(),
                request.getDate(), request.getTime());

        return toResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CANCEL APPOINTMENT
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Cancels an appointment.
     *
     * <ul>
     *   <li>COMPLETED → throws {@link BadRequestException} (cannot undo a finished visit).</li>
     *   <li>CANCELLED → no-op (idempotent; already cancelled).</li>
     *   <li>Any other status → sets status to CANCELLED and saves.</li>
     * </ul>
     */
    @Override
    public void cancelAppointment(Long appointmentId) {

        Appointment appointment = findByIdOrThrow(appointmentId);

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new BadRequestException(
                    "Appointment [" + appointmentId + "] is already COMPLETED and cannot be cancelled.");
        }

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            log.warn("[CANCEL] Appointment {} is already CANCELLED — no-op.", appointmentId);
            return; // idempotent
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appointment);

        log.info("[CANCELLED] appointmentId={}", appointmentId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FETCH OPERATIONS
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponse getAppointmentById(Long appointmentId) {
        return toResponse(findByIdOrThrow(appointmentId));
    }

    /**
     * All appointments for a patient, sorted by date then time (earliest first).
     */
    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointmentsByPatient(Long userId) {
        // Look up patient profile by User.id (from JWT) — not the Patient.id
        // Return empty list if no patient profile exists yet (new user hasn't created one)
        return patientRepository.findByUserId(userId)
                .map(patient -> appointmentRepository
                        .findByPatientIdOrderByDateAscTimeAsc(patient.getId())
                        .stream()
                        .map(this::toResponse)
                        .toList())
                .orElse(List.of());
    }

    /**
     * All appointments for a doctor, sorted by date then time (earliest first).
     */
    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointmentsByDoctor(Long doctorId) {
        // Verify doctor exists
        doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", doctorId));

        return appointmentRepository
                .findByDoctorIdOrderByDateAscTimeAsc(doctorId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * All appointments for a doctor on a specific calendar date.
     */
    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointmentsByDoctorAndDate(Long doctorId, LocalDate date) {
        return appointmentRepository
                .findByDoctorIdAndDateOrderByTimeAsc(doctorId, date)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Patient's appointments filtered by status.
     */
    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointmentsByPatientAndStatus(
            Long patientId, AppointmentStatus status) {
        return appointmentRepository
                .findByPatientIdAndStatus(patientId, status)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Doctor's appointments filtered by status.
     */
    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointmentsByDoctorAndStatus(
            Long doctorId, AppointmentStatus status) {
        return appointmentRepository
                .findByDoctorIdAndStatus(doctorId, status)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * All appointments in the system (admin dashboard), earliest first.
     */
    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAllAppointments() {
        return appointmentRepository
                .findAllByOrderByDateAscTimeAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RESCHEDULE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Reschedules an appointment to a new date/time.
     *
     * <p>Business rules:</p>
     * <ul>
     *   <li>CANCELLED or COMPLETED appointments cannot be rescheduled.</li>
     *   <li>New time must be within the doctor's availability window.</li>
     *   <li>New date+time must not conflict with the doctor's other appointments
     *       (the current appointment is excluded from the conflict check).</li>
     *   <li>Status is reset to PENDING after a successful reschedule.</li>
     * </ul>
     */
    @Override
    public AppointmentResponse rescheduleAppointment(Long appointmentId, AppointmentRequest request) {

        Appointment existing = findByIdOrThrow(appointmentId);

        // Guard: terminal states cannot be rescheduled
        if (existing.getStatus() == AppointmentStatus.CANCELLED
                || existing.getStatus() == AppointmentStatus.COMPLETED) {
            throw new BadRequestException(
                    "Cannot reschedule a " + existing.getStatus() + " appointment.");
        }

        Doctor doctor = existing.getDoctor();

        // Availability window check for new time
        validateWithinAvailability(doctor, request);

        // Double-booking check — exclude the current appointment itself so it
        // doesn't conflict with its own old slot when same doctor+date+time is reused
        boolean hasConflict = appointmentRepository
                .findConflicts(doctor.getId(), request.getDate(), request.getTime())
                .stream()
                .anyMatch(a -> !a.getId().equals(appointmentId)); // exclude self

        if (hasConflict) {
            throw new BadRequestException(
                    "The new time slot " + request.getDate() + " " + request.getTime()
                    + " conflicts with another active appointment for this doctor.");
        }

        existing.setDate(request.getDate());
        existing.setTime(request.getTime());
        existing.setStatus(AppointmentStatus.PENDING);  // reset to PENDING

        Appointment updated = appointmentRepository.save(existing);

        log.info("[RESCHEDULED] appointmentId={} → {}_{}", appointmentId,
                request.getDate(), request.getTime());

        return toResponse(updated);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STATUS UPDATE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Updates the status of an appointment (e.g., CONFIRMED → COMPLETED).
     */
    @Override
    public AppointmentResponse updateStatus(Long appointmentId, AppointmentStatus status) {
        Appointment appointment = findByIdOrThrow(appointmentId);
        AppointmentStatus previous = appointment.getStatus();
        appointment.setStatus(status);
        Appointment updated = appointmentRepository.save(appointment);
        log.info("[STATUS CHANGE] appointmentId={} | {} → {}", appointmentId, previous, status);
        return toResponse(updated);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Fetches an appointment by ID or throws a 404.
     */
    private Appointment findByIdOrThrow(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", id));
    }

    /**
     * Validates that the requested time falls within the doctor's
     * {@code availableFrom}–{@code availableTo} window.
     *
     * @throws BadRequestException if the time is outside the window
     */
    private void validateWithinAvailability(Doctor doctor, AppointmentRequest request) {
        boolean beforeStart = request.getTime().isBefore(doctor.getAvailableFrom());
        boolean afterEnd    = request.getTime().isAfter(doctor.getAvailableTo());

        if (beforeStart || afterEnd) {
            throw new BadRequestException(
                    "Requested time " + request.getTime()
                    + " is outside Dr. " + doctor.getUser().getName()
                    + "'s availability window ("
                    + doctor.getAvailableFrom() + " – " + doctor.getAvailableTo() + ").");
        }
    }

    /**
     * Checks whether the doctor already has an active appointment at the
     * exact same {@code (date, time)}.
     *
     * <p>Uses {@link AppointmentRepository#existsConflict} which issues a
     * single lightweight {@code COUNT > 0} query — no data is loaded.</p>
     *
     * @throws BadRequestException if a conflict is found
     */
    private void checkForConflict(Long doctorId, AppointmentRequest request) {
        boolean conflict = appointmentRepository.existsConflict(
                doctorId, request.getDate(), request.getTime());

        if (conflict) {
            throw new BadRequestException(
                    "Dr. ID " + doctorId + " already has an active appointment on "
                    + request.getDate() + " at " + request.getTime()
                    + ". Please choose a different time slot.");
        }
    }

    /**
     * Maps an {@link Appointment} entity to an {@link AppointmentResponse} DTO.
     * Navigates lazy associations — must be called within a transaction.
     */
    private AppointmentResponse toResponse(Appointment a) {
        return AppointmentResponse.builder()
                .id(a.getId())
                .patientId(a.getPatient().getId())
                .patientName(a.getPatient().getUser().getName())
                .doctorId(a.getDoctor().getId())
                .doctorName(a.getDoctor().getUser().getName())
                .specialization(a.getDoctor().getSpecialization())
                .date(a.getDate())
                .time(a.getTime())
                .status(a.getStatus())
                .build();
    }
}

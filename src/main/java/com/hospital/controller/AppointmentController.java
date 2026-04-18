package com.hospital.controller;

import com.hospital.dto.request.AppointmentRequest;
import com.hospital.dto.response.ApiResponse;
import com.hospital.dto.response.AppointmentResponse;
import com.hospital.model.AppointmentStatus;
import com.hospital.security.UserPrincipal;
import com.hospital.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for Appointment management.
 *
 * <h3>Base path</h3>
 * {@code /api/v1/appointments}
 *
 * <h3>Role-based access summary</h3>
 * <pre>
 *  POST   /                        → PATIENT      (book own appointment)
 *  DELETE /{id}/cancel             → PATIENT | ADMIN
 *  GET    /{id}                    → authenticated (any role)
 *  GET    /my                      → PATIENT      (own appointments via JWT)
 *  GET    /patient/{patientId}     → ADMIN        (view any patient's list)
 *  GET    /doctor/{doctorId}       → DOCTOR | ADMIN
 *  GET    /doctor/{doctorId}/date  → DOCTOR | ADMIN (daily schedule)
 *  GET    /                        → ADMIN        (all appointments)
 *  PATCH  /{id}/status             → DOCTOR | ADMIN
 *  PUT    /{id}/reschedule         → PATIENT | ADMIN
 * </pre>
 *
 * <p>JWT required on all endpoints — configured via
 * {@link com.hospital.config.SecurityConfig}.</p>
 */
@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
@Tag(name = "3. Appointments",
     description = "Book, cancel, reschedule, and view appointments")
@SecurityRequirement(name = "bearerAuth")
public class AppointmentController {

    private final AppointmentService appointmentService;

    // ── BOOK ─────────────────────────────────────────────────────────────────

    /**
     * Book a new appointment.
     *
     * <p>The patient ID is taken from the JWT principal — patients can only
     * book appointments for themselves. Prevents double-booking automatically.</p>
     *
     * <p>Returns HTTP 201 Created with the saved appointment.</p>
     */
    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(
        summary  = "Book an appointment [PATIENT]",
        description = "Books a slot for the authenticated patient. " +
                      "Validates doctor availability window and prevents double-booking."
    )
    public ResponseEntity<ApiResponse<AppointmentResponse>> bookAppointment(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AppointmentRequest request) {

        // We pass the UserPrincipal.id (User.id), not the Patient profile id.
        // AppointmentServiceImpl resolves the Patient profile via patientRepository.findByUserId().
        // NOTE: the service was updated to accept userId — ensure alignment.
        AppointmentResponse response =
                appointmentService.bookAppointment(principal.getId(), request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Appointment booked successfully."));
    }

    // ── CANCEL ────────────────────────────────────────────────────────────────

    /**
     * Cancel an appointment by ID.
     *
     * <p>COMPLETED appointments cannot be cancelled (returns 400).
     * Already-CANCELLED is a no-op (idempotent).</p>
     */
    @DeleteMapping("/{id}/cancel")
    @PreAuthorize("hasRole('PATIENT') or hasRole('ADMIN')")
    @Operation(summary = "Cancel an appointment [PATIENT | ADMIN]")
    public ResponseEntity<ApiResponse<Void>> cancelAppointment(
            @PathVariable Long id) {

        appointmentService.cancelAppointment(id);
        return ResponseEntity.ok(
                ApiResponse.success(null, "Appointment [" + id + "] cancelled."));
    }

    // ── VIEW — SINGLE ─────────────────────────────────────────────────────────

    /**
     * Retrieve a single appointment by its primary key.
     * Any authenticated user can call this endpoint.
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get appointment by ID [any role]")
    public ResponseEntity<ApiResponse<AppointmentResponse>> getById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        appointmentService.getAppointmentById(id),
                        "Appointment found."));
    }

    // ── VIEW — OWN (PATIENT) ──────────────────────────────────────────────────

    /**
     * Retrieve the authenticated patient's own appointments.
     *
     * <p>Uses the JWT to identify the caller — patients never need to
     * know or supply their own patient profile ID.</p>
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(
        summary = "My appointments [PATIENT]",
        description = "Returns all appointments for the currently authenticated patient."
    )
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getMyAppointments(
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        appointmentService.getAppointmentsByPatient(principal.getId()),
                        "Your appointments fetched."));
    }

    // ── VIEW — BY PATIENT (ADMIN) ─────────────────────────────────────────────

    /**
     * Retrieve all appointments for any patient (admin use).
     */
    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "All appointments for a patient [ADMIN]")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getByPatient(
            @PathVariable Long patientId) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        appointmentService.getAppointmentsByPatient(patientId),
                        "Appointments for patient " + patientId + " fetched."));
    }

    // ── VIEW — BY DOCTOR ──────────────────────────────────────────────────────

    /**
     * Retrieve all appointments for a specific doctor.
     */
    @GetMapping("/doctor/{doctorId}")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    @Operation(summary = "All appointments for a doctor [DOCTOR | ADMIN]")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getByDoctor(
            @PathVariable Long doctorId) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        appointmentService.getAppointmentsByDoctor(doctorId),
                        "Appointments for doctor " + doctorId + " fetched."));
    }

    /**
     * Retrieve a doctor's appointments on a specific date (daily schedule view).
     *
     * @param date ISO-8601 date string, e.g. {@code 2025-08-15}
     */
    @GetMapping("/doctor/{doctorId}/date")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Doctor's schedule on a specific date [DOCTOR | ADMIN]")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getByDoctorAndDate(
            @PathVariable Long doctorId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(description = "Date in yyyy-MM-dd format", example = "2025-08-15")
            LocalDate date) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        appointmentService.getAppointmentsByDoctorAndDate(doctorId, date),
                        "Schedule for " + date + " fetched."));
    }

    // ── VIEW — FILTERED BY STATUS ─────────────────────────────────────────────

    /**
     * Retrieve a doctor's appointments filtered by status.
     */
    @GetMapping("/doctor/{doctorId}/status")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Doctor's appointments filtered by status [DOCTOR | ADMIN]")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getByDoctorAndStatus(
            @PathVariable Long doctorId,
            @RequestParam AppointmentStatus status) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        appointmentService.getAppointmentsByDoctorAndStatus(doctorId, status),
                        "Filtered appointments fetched."));
    }

    /**
     * Retrieve a patient's appointments filtered by status.
     */
    @GetMapping("/patient/{patientId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Patient's appointments filtered by status [ADMIN]")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getByPatientAndStatus(
            @PathVariable Long patientId,
            @RequestParam AppointmentStatus status) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        appointmentService.getAppointmentsByPatientAndStatus(patientId, status),
                        "Filtered appointments fetched."));
    }

    // ── VIEW — ALL (ADMIN) ────────────────────────────────────────────────────

    /**
     * Retrieve every appointment in the system ordered by date then time.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "All appointments in the system [ADMIN]")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getAll() {

        return ResponseEntity.ok(
                ApiResponse.success(
                        appointmentService.getAllAppointments(),
                        "All appointments fetched."));
    }

    // ── STATUS UPDATE ─────────────────────────────────────────────────────────

    /**
     * Update the status of an appointment.
     *
     * <p>Typical transitions:</p>
     * <ul>
     *   <li>PENDING → CONFIRMED  (doctor confirms)</li>
     *   <li>CONFIRMED → COMPLETED (after the visit)</li>
     *   <li>CONFIRMED → NO_SHOW  (patient did not attend)</li>
     * </ul>
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    @Operation(summary = "Update appointment status [DOCTOR | ADMIN]")
    public ResponseEntity<ApiResponse<AppointmentResponse>> updateStatus(
            @PathVariable Long id,
            @RequestParam AppointmentStatus status) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        appointmentService.updateStatus(id, status),
                        "Status updated to " + status + "."));
    }

    // ── RESCHEDULE ────────────────────────────────────────────────────────────

    /**
     * Reschedule an appointment to a new date/time.
     *
     * <p>Rules: cannot reschedule CANCELLED or COMPLETED; new slot must not
     * conflict; status resets to PENDING.</p>
     */
    @PutMapping("/{id}/reschedule")
    @PreAuthorize("hasRole('PATIENT') or hasRole('ADMIN')")
    @Operation(summary = "Reschedule an appointment [PATIENT | ADMIN]")
    public ResponseEntity<ApiResponse<AppointmentResponse>> reschedule(
            @PathVariable Long id,
            @Valid @RequestBody AppointmentRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        appointmentService.rescheduleAppointment(id, request),
                        "Appointment rescheduled."));
    }
}

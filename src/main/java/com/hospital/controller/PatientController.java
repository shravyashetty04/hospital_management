package com.hospital.controller;

import com.hospital.dto.request.PatientRequest;
import com.hospital.dto.response.ApiResponse;
import com.hospital.dto.response.AppointmentResponse;
import com.hospital.dto.response.PatientResponse;
import com.hospital.security.UserPrincipal;
import com.hospital.service.AppointmentService;
import com.hospital.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Patient profile and appointment history.
 *
 * <h3>Base path</h3>
 * {@code /api/v1/patients}
 *
 * <h3>Access summary</h3>
 * <pre>
 *  POST /user/{userId}             → ADMIN | PATIENT  (create profile)
 *  GET  /me                        → PATIENT          (own profile via JWT)
 *  GET  /me/history                → PATIENT          (own appointment history)
 *  GET  /{id}                      → ADMIN | PATIENT  (by profile ID)
 *  GET  /user/{userId}             → ADMIN | PATIENT  (by user ID)
 *  GET  /                          → ADMIN            (all patients)
 *  PUT  /{id}                      → ADMIN | PATIENT  (update profile)
 *  DELETE /{id}                    → ADMIN            (delete profile)
 * </pre>
 *
 * <p>JWT required on all endpoints.</p>
 */
@RestController
@RequestMapping("/patients")
@RequiredArgsConstructor
@Tag(name = "4. Patients",
     description = "Patient profile management and appointment history")
@SecurityRequirement(name = "bearerAuth")
public class PatientController {

    private final PatientService     patientService;
    private final AppointmentService appointmentService;

    // ── CREATE PROFILE ────────────────────────────────────────────────────────

    /**
     * Create a patient profile for an existing user account.
     *
     * <p>A patient must create their health profile before booking
     * any appointment.</p>
     */
    @PostMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PATIENT')")
    @Operation(
        summary = "Create patient profile [ADMIN | PATIENT]",
        description = "Creates a medical profile (age, history) linked to a user account. " +
                      "Must be done once before booking appointments."
    )
    public ResponseEntity<ApiResponse<PatientResponse>> createProfile(
            @PathVariable Long userId,
            @Valid @RequestBody PatientRequest request) {

        PatientResponse response = patientService.createPatientProfile(userId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Patient profile created."));
    }

    // ── OWN PROFILE (via JWT) ─────────────────────────────────────────────────

    /**
     * Get the authenticated patient's own profile using their JWT.
     *
     * <p>Patients do not need to know their profile ID — the server
     * resolves it from the JWT subject (User.id → Patient profile).</p>
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(
        summary = "My profile [PATIENT]",
        description = "Returns the authenticated patient's profile without requiring a profile ID."
    )
    public ResponseEntity<ApiResponse<PatientResponse>> getMyProfile(
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        patientService.getPatientByUserId(principal.getId()),
                        "Your profile fetched."));
    }

    // ── OWN APPOINTMENT HISTORY (via JWT) ─────────────────────────────────────

    /**
     * Get the authenticated patient's full appointment history.
     *
     * <p>Returns all appointments (past and upcoming) ordered by date + time.
     * Patients can filter by status on the client side.</p>
     */
    @GetMapping("/me/history")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(
        summary = "My appointment history [PATIENT]",
        description = "Returns all appointments for the authenticated patient ordered by date."
    )
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getMyHistory(
            @AuthenticationPrincipal UserPrincipal principal) {

        // Resolve User.id → Patient profile id first
        PatientResponse patient = patientService.getPatientByUserId(principal.getId());

        return ResponseEntity.ok(
                ApiResponse.success(
                        appointmentService.getAppointmentsByPatient(patient.getId()),
                        "Your appointment history fetched."));
    }

    // ── READ — BY PROFILE ID ──────────────────────────────────────────────────

    /**
     * Retrieve a patient's profile by their patient profile ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PATIENT')")
    @Operation(summary = "Get patient by profile ID [ADMIN | PATIENT]")
    public ResponseEntity<ApiResponse<PatientResponse>> getPatientById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        patientService.getPatientById(id),
                        "Patient found."));
    }

    // ── READ — BY USER ID ─────────────────────────────────────────────────────

    /**
     * Retrieve a patient's profile by their linked User ID.
     * Useful for admin lookups when only the user account ID is known.
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PATIENT')")
    @Operation(summary = "Get patient by user ID [ADMIN | PATIENT]")
    public ResponseEntity<ApiResponse<PatientResponse>> getPatientByUserId(
            @PathVariable Long userId) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        patientService.getPatientByUserId(userId),
                        "Patient found."));
    }

    // ── READ — ALL (ADMIN) ────────────────────────────────────────────────────

    /**
     * Retrieve all patients in the system. ADMIN only.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all patients [ADMIN]")
    public ResponseEntity<ApiResponse<List<PatientResponse>>> getAllPatients() {

        return ResponseEntity.ok(
                ApiResponse.success(
                        patientService.getAllPatients(),
                        "All patients fetched."));
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    /**
     * Update a patient's medical profile (age, medical history).
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PATIENT')")
    @Operation(summary = "Update patient profile [ADMIN | PATIENT]")
    public ResponseEntity<ApiResponse<PatientResponse>> updatePatient(
            @PathVariable Long id,
            @Valid @RequestBody PatientRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        patientService.updatePatientProfile(id, request),
                        "Patient profile updated."));
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    /**
     * Delete a patient profile. ADMIN only.
     * Cascades to all associated appointments ({@code orphanRemoval = true}).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete patient profile [ADMIN]")
    public ResponseEntity<ApiResponse<Void>> deletePatient(
            @PathVariable Long id) {

        patientService.deletePatient(id);
        return ResponseEntity.ok(
                ApiResponse.success(null, "Patient [" + id + "] deleted."));
    }
}

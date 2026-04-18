package com.hospital.controller;

import com.hospital.dto.request.DoctorRequest;
import com.hospital.dto.response.ApiResponse;
import com.hospital.dto.response.DoctorResponse;
import com.hospital.service.DoctorService;
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
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;

/**
 * REST controller for Doctor management.
 *
 * <h3>Base path</h3>
 * {@code /api/v1/doctors}
 *
 * <h3>Access summary</h3>
 * <pre>
 *  GET  /                          → public (no JWT needed)
 *  GET  /{id}                      → public
 *  GET  /specialization/{spec}     → public
 *  GET  /available?time=HH:mm      → public  ← "view doctor availability"
 *  GET  /search?keyword=           → public
 *  POST /user/{userId}             → ADMIN | DOCTOR
 *  PUT  /{id}                      → ADMIN | DOCTOR
 *  DELETE /{id}                    → ADMIN
 * </pre>
 */
@RestController
@RequestMapping("/doctors")
@RequiredArgsConstructor
@Tag(name = "2. Doctors",
     description = "Add doctor profiles, view availability, search and filter")
public class DoctorController {

    private final DoctorService doctorService;

    // ── CREATE ────────────────────────────────────────────────────────────────

    /**
     * Create a doctor profile for an existing user.
     *
     * <p>Only ADMIN or DOCTOR roles can create doctor profiles.
     * JWT required.</p>
     */
    @PostMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Add a doctor profile [ADMIN | DOCTOR]",
        description = "Creates a professional profile for an existing user account. " +
                      "Sets specialization and availability window (availableFrom – availableTo)."
    )
    public ResponseEntity<ApiResponse<DoctorResponse>> addDoctor(
            @PathVariable Long userId,
            @Valid @RequestBody DoctorRequest request) {

        DoctorResponse response = doctorService.createDoctorProfile(userId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Doctor profile created."));
    }

    // ── READ — ALL ────────────────────────────────────────────────────────────

    /**
     * Retrieve all registered doctors.
     * <b>Public endpoint — no JWT required.</b>
     */
    @GetMapping
    @Operation(summary = "List all doctors [PUBLIC]")
    public ResponseEntity<ApiResponse<List<DoctorResponse>>> getAllDoctors() {

        return ResponseEntity.ok(
                ApiResponse.success(doctorService.getAllDoctors(), "Doctors fetched."));
    }

    // ── READ — SINGLE ─────────────────────────────────────────────────────────

    /**
     * Retrieve a doctor by their profile ID.
     * <b>Public endpoint — no JWT required.</b>
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get doctor by ID [PUBLIC]")
    public ResponseEntity<ApiResponse<DoctorResponse>> getDoctorById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                ApiResponse.success(doctorService.getDoctorById(id), "Doctor found."));
    }

    // ── READ — BY USER ID ─────────────────────────────────────────────────────

    /**
     * Retrieve a doctor profile by the linked User ID.
     * <b>Public endpoint — no JWT required.</b>
     * Used by the frontend to resolve a doctor's profile ID from their JWT userId claim.
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get doctor profile by user ID [PUBLIC]")
    public ResponseEntity<ApiResponse<DoctorResponse>> getDoctorByUserId(
            @PathVariable Long userId) {

        return ResponseEntity.ok(
                ApiResponse.success(doctorService.getDoctorByUserId(userId), "Doctor found."));
    }



    /**
     * View doctor availability — list all doctors whose working window includes
     * the requested time.
     *
     * <p><b>Public endpoint — no JWT required.</b>
     * Patients use this to find available doctors before deciding to register.</p>
     *
     * @param time wall-clock time in {@code HH:mm} format, e.g. {@code 14:30}
     */
    @GetMapping("/available")
    @Operation(
        summary = "Doctors available at a given time [PUBLIC]",
        description = "Returns all doctors whose availableFrom ≤ time ≤ availableTo. " +
                      "Use this before booking to show valid doctor options."
    )
    public ResponseEntity<ApiResponse<List<DoctorResponse>>> getDoctorsAvailableAt(
            @RequestParam
            @DateTimeFormat(pattern = "HH:mm")
            @Parameter(description = "Time in HH:mm format", example = "10:30")
            LocalTime time) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        doctorService.getDoctorsAvailableAt(time),
                        "Available doctors at " + time + " fetched."));
    }

    // ── READ — BY SPECIALIZATION ──────────────────────────────────────────────

    /**
     * Filter doctors by medical specialization (case-insensitive exact match).
     * <b>Public endpoint — no JWT required.</b>
     *
     * @param specialization e.g. {@code Cardiology}, {@code orthopedics}
     */
    @GetMapping("/specialization/{specialization}")
    @Operation(summary = "Filter doctors by specialization [PUBLIC]")
    public ResponseEntity<ApiResponse<List<DoctorResponse>>> getBySpecialization(
            @PathVariable String specialization) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        doctorService.getDoctorsBySpecialization(specialization),
                        "Doctors in specialization '" + specialization + "' fetched."));
    }

    // ── READ — SEARCH ─────────────────────────────────────────────────────────

    /**
     * Search doctors by name keyword (case-insensitive partial match).
     * <b>Public endpoint — no JWT required.</b>
     *
     * @param keyword partial name, e.g. {@code "Smith"} or {@code "jo"}
     */
    @GetMapping("/search")
    @Operation(summary = "Search doctors by name keyword [PUBLIC]")
    public ResponseEntity<ApiResponse<List<DoctorResponse>>> searchByName(
            @RequestParam String keyword) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        doctorService.searchDoctorsByName(keyword),
                        "Search results for '" + keyword + "'."));
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    /**
     * Update a doctor's specialization or availability window.
     * JWT required. ADMIN or DOCTOR role.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update doctor profile [ADMIN | DOCTOR]")
    public ResponseEntity<ApiResponse<DoctorResponse>> updateDoctor(
            @PathVariable Long id,
            @Valid @RequestBody DoctorRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        doctorService.updateDoctorProfile(id, request),
                        "Doctor profile updated."));
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    /**
     * Delete a doctor profile. ADMIN only.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete a doctor profile [ADMIN]")
    public ResponseEntity<ApiResponse<Void>> deleteDoctor(
            @PathVariable Long id) {

        doctorService.deleteDoctor(id);
        return ResponseEntity.ok(
                ApiResponse.success(null, "Doctor [" + id + "] deleted."));
    }
}

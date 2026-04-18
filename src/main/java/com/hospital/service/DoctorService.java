package com.hospital.service;

import com.hospital.dto.request.DoctorRequest;
import com.hospital.dto.response.DoctorResponse;

import java.time.LocalTime;
import java.util.List;

/**
 * Business contract for Doctor profile management.
 */
public interface DoctorService {

    /** Create a new doctor profile linked to an existing user. */
    DoctorResponse createDoctorProfile(Long userId, DoctorRequest request);

    /** Fetch a doctor by their profile ID. */
    DoctorResponse getDoctorById(Long id);

    /** Fetch a doctor by their linked user ID. */
    DoctorResponse getDoctorByUserId(Long userId);

    /** List all doctors. */
    List<DoctorResponse> getAllDoctors();

    /** List all doctors available at the given time. */
    List<DoctorResponse> getDoctorsAvailableAt(LocalTime time);

    /** List doctors by specialization (case-insensitive). */
    List<DoctorResponse> getDoctorsBySpecialization(String specialization);

    /** Partial-name search across doctors. */
    List<DoctorResponse> searchDoctorsByName(String keyword);

    /** Update an existing doctor profile. */
    DoctorResponse updateDoctorProfile(Long id, DoctorRequest request);

    /** Delete a doctor profile by ID. */
    void deleteDoctor(Long id);
}

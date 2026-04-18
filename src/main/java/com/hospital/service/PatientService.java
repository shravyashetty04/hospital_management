package com.hospital.service;

import com.hospital.dto.request.PatientRequest;
import com.hospital.dto.response.PatientResponse;

import java.util.List;

/**
 * Business contract for Patient profile management.
 */
public interface PatientService {

    /** Create a new patient profile linked to an existing user. */
    PatientResponse createPatientProfile(Long userId, PatientRequest request);

    /** Fetch a patient by their profile ID. */
    PatientResponse getPatientById(Long id);

    /** Fetch a patient by their linked user ID. */
    PatientResponse getPatientByUserId(Long userId);

    /** List all patients (admin use). */
    List<PatientResponse> getAllPatients();

    /** Update an existing patient profile. */
    PatientResponse updatePatientProfile(Long id, PatientRequest request);

    /** Delete a patient profile by ID. */
    void deletePatient(Long id);
}

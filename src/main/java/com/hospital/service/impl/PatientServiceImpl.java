package com.hospital.service.impl;

import com.hospital.dto.request.PatientRequest;
import com.hospital.dto.response.PatientResponse;
import com.hospital.exception.BadRequestException;
import com.hospital.exception.ResourceNotFoundException;
import com.hospital.model.Patient;
import com.hospital.model.User;
import com.hospital.repository.PatientRepository;
import com.hospital.repository.UserRepository;
import com.hospital.service.PatientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of {@link PatientService}.
 *
 * <p>All write methods run inside a transaction. Read methods use
 * {@code readOnly = true} to enable Hibernate's read optimizations.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;
    private final UserRepository    userRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Creates a new patient profile for an existing user.
     *
     * <p>Validates that the user exists and does not already have a
     * patient profile.</p>
     */
    @Override
    public PatientResponse createPatientProfile(Long userId, PatientRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (patientRepository.existsByUserId(userId)) {
            throw new BadRequestException(
                    "A patient profile already exists for user ID: " + userId);
        }

        Patient patient = Patient.builder()
                .user(user)
                .age(request.getAge())
                .medicalHistory(request.getMedicalHistory())
                .build();

        Patient saved = patientRepository.save(patient);
        log.info("[CREATED] Patient profile id={} for userId={}", saved.getId(), userId);
        return toResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PatientResponse getPatientById(Long id) {
        return toResponse(findByIdOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PatientResponse getPatientByUserId(Long userId) {
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "userId", userId));
        return toResponse(patient);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientResponse> getAllPatients() {
        return patientRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public PatientResponse updatePatientProfile(Long id, PatientRequest request) {
        Patient patient = findByIdOrThrow(id);

        patient.setAge(request.getAge());
        patient.setMedicalHistory(request.getMedicalHistory());

        Patient updated = patientRepository.save(patient);
        log.info("[UPDATED] Patient id={}", id);
        return toResponse(updated);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void deletePatient(Long id) {
        Patient patient = findByIdOrThrow(id);
        patientRepository.delete(patient);
        log.info("[DELETED] Patient id={}", id);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private Patient findByIdOrThrow(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", id));
    }

    /**
     * Maps a {@link Patient} entity to a {@link PatientResponse} DTO.
     */
    private PatientResponse toResponse(Patient patient) {
        User user = patient.getUser();
        return PatientResponse.builder()
                .id(patient.getId())
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .age(patient.getAge())
                .medicalHistory(patient.getMedicalHistory())
                .build();
    }
}

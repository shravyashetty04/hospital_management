package com.hospital.service.impl;

import com.hospital.dto.request.DoctorRequest;
import com.hospital.dto.response.DoctorResponse;
import com.hospital.exception.BadRequestException;
import com.hospital.exception.ResourceNotFoundException;
import com.hospital.model.Doctor;
import com.hospital.model.User;
import com.hospital.repository.DoctorRepository;
import com.hospital.repository.UserRepository;
import com.hospital.service.DoctorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

/**
 * Implementation of {@link DoctorService}.
 *
 * <p>All write methods run inside a transaction. Read methods use
 * {@code readOnly = true} to enable Hibernate's read optimizations.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DoctorServiceImpl implements DoctorService {

    private final DoctorRepository doctorRepository;
    private final UserRepository   userRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Creates a new doctor profile for an existing user.
     *
     * <p>Validates that:</p>
     * <ul>
     *   <li>The user exists.</li>
     *   <li>No doctor profile already exists for that user.</li>
     *   <li>{@code availableFrom} is strictly before {@code availableTo}.</li>
     * </ul>
     */
    @Override
    public DoctorResponse createDoctorProfile(Long userId, DoctorRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (doctorRepository.existsByUserId(userId)) {
            throw new BadRequestException(
                    "A doctor profile already exists for user ID: " + userId);
        }

        validateAvailabilityWindow(request.getAvailableFrom(), request.getAvailableTo());

        Doctor doctor = Doctor.builder()
                .user(user)
                .specialization(request.getSpecialization())
                .availableFrom(request.getAvailableFrom())
                .availableTo(request.getAvailableTo())
                .build();

        Doctor saved = doctorRepository.save(doctor);
        log.info("[CREATED] Doctor profile id={} for userId={}", saved.getId(), userId);
        return toResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public DoctorResponse getDoctorById(Long id) {
        return toResponse(findByIdOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public DoctorResponse getDoctorByUserId(Long userId) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "userId", userId));
        return toResponse(doctor);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorResponse> getAllDoctors() {
        return doctorRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Returns all doctors whose working window contains the requested time.
     * Delegates to {@link DoctorRepository#findAvailableAt}.
     */
    @Override
    @Transactional(readOnly = true)
    public List<DoctorResponse> getDoctorsAvailableAt(LocalTime time) {
        return doctorRepository.findAvailableAt(time)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorResponse> getDoctorsBySpecialization(String specialization) {
        return doctorRepository.findBySpecializationIgnoreCase(specialization)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorResponse> searchDoctorsByName(String keyword) {
        return doctorRepository.searchByUserName(keyword)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public DoctorResponse updateDoctorProfile(Long id, DoctorRequest request) {
        Doctor doctor = findByIdOrThrow(id);

        validateAvailabilityWindow(request.getAvailableFrom(), request.getAvailableTo());

        doctor.setSpecialization(request.getSpecialization());
        doctor.setAvailableFrom(request.getAvailableFrom());
        doctor.setAvailableTo(request.getAvailableTo());

        Doctor updated = doctorRepository.save(doctor);
        log.info("[UPDATED] Doctor id={}", id);
        return toResponse(updated);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void deleteDoctor(Long id) {
        Doctor doctor = findByIdOrThrow(id);
        doctorRepository.delete(doctor);
        log.info("[DELETED] Doctor id={}", id);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private Doctor findByIdOrThrow(Long id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", id));
    }

    /**
     * Ensures the availability window is logically valid
     * (start must be strictly before end).
     */
    private void validateAvailabilityWindow(LocalTime from, LocalTime to) {
        if (!from.isBefore(to)) {
            throw new BadRequestException(
                    "availableFrom (" + from + ") must be before availableTo (" + to + ").");
        }
    }

    /**
     * Maps a {@link Doctor} entity to a {@link DoctorResponse} DTO.
     */
    private DoctorResponse toResponse(Doctor doctor) {
        User user = doctor.getUser();
        return DoctorResponse.builder()
                .id(doctor.getId())
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .specialization(doctor.getSpecialization())
                .availableFrom(doctor.getAvailableFrom())
                .availableTo(doctor.getAvailableTo())
                .build();
    }
}

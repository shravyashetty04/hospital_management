package com.hospital.service;

import com.hospital.dto.request.AppointmentRequest;
import com.hospital.dto.response.AppointmentResponse;
import com.hospital.model.AppointmentStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * Business contract for all appointment operations.
 *
 * <p>Key responsibilities:</p>
 * <ul>
 *   <li>Book an appointment (with double-booking prevention)</li>
 *   <li>Cancel an appointment</li>
 *   <li>Fetch appointments by doctor, patient, date, or status</li>
 *   <li>Reschedule an existing appointment</li>
 *   <li>Update appointment status (CONFIRMED, COMPLETED, NO_SHOW…)</li>
 * </ul>
 */
public interface AppointmentService {

    /**
     * Book a new appointment for the given patient with the specified doctor.
     *
     * <p>Business rules enforced:</p>
     * <ol>
     *   <li>The doctor must exist.</li>
     *   <li>The patient profile is resolved from the {@code userId} (User ID from JWT).</li>
     *   <li>The requested date+time must not clash with another active appointment
     *       for the same doctor (double-booking prevention).</li>
     *   <li>The requested time must fall within the doctor's availability window.</li>
     * </ol>
     *
     * @param userId  the {@code User.id} from the JWT principal
     *                (resolved internally to a Patient profile)
     * @param request appointment details (doctorId, date, time)
     * @return saved appointment as a response DTO
     */
    AppointmentResponse bookAppointment(Long userId, AppointmentRequest request);

    /**
     * Cancel an existing appointment by its ID.
     *
     * <p>Rules:</p>
     * <ul>
     *   <li>COMPLETED appointments cannot be cancelled.</li>
     *   <li>Already-CANCELLED appointments are a no-op (idempotent).</li>
     * </ul>
     *
     * @param appointmentId the appointment to cancel
     */
    void cancelAppointment(Long appointmentId);

    /**
     * Retrieve a single appointment by its primary key.
     *
     * @param appointmentId appointment ID
     * @return the appointment DTO
     * @throws com.hospital.exception.ResourceNotFoundException if not found
     */
    AppointmentResponse getAppointmentById(Long appointmentId);

    /**
     * Retrieve all appointments for a specific patient, ordered by date then time.
     *
     * @param patientId the Patient profile ID
     * @return list of appointment DTOs
     */
    List<AppointmentResponse> getAppointmentsByPatient(Long patientId);

    /**
     * Retrieve all appointments for a specific doctor, ordered by date then time.
     *
     * @param doctorId the Doctor profile ID
     * @return list of appointment DTOs
     */
    List<AppointmentResponse> getAppointmentsByDoctor(Long doctorId);

    /**
     * Retrieve all appointments for a doctor on a specific date.
     *
     * @param doctorId the Doctor profile ID
     * @param date     the calendar date to query
     * @return list of appointment DTOs ordered by time
     */
    List<AppointmentResponse> getAppointmentsByDoctorAndDate(Long doctorId, LocalDate date);

    /**
     * Retrieve all appointments for a patient with a given status.
     *
     * @param patientId the Patient profile ID
     * @param status    the appointment status to filter by
     * @return list of appointment DTOs
     */
    List<AppointmentResponse> getAppointmentsByPatientAndStatus(Long patientId, AppointmentStatus status);

    /**
     * Retrieve all appointments for a doctor with a given status.
     *
     * @param doctorId the Doctor profile ID
     * @param status   the appointment status to filter by
     * @return list of appointment DTOs
     */
    List<AppointmentResponse> getAppointmentsByDoctorAndStatus(Long doctorId, AppointmentStatus status);

    /**
     * Retrieve every appointment in the system (admin view), ordered by date + time.
     *
     * @return list of all appointment DTOs
     */
    List<AppointmentResponse> getAllAppointments();

    /**
     * Reschedule an existing appointment to a new date/time.
     *
     * <p>Rules:</p>
     * <ul>
     *   <li>CANCELLED or COMPLETED appointments cannot be rescheduled.</li>
     *   <li>New slot must not conflict with the doctor's other active appointments.</li>
     *   <li>New slot must fall within the doctor's availability window.</li>
     *   <li>Status is reset to PENDING after rescheduling.</li>
     * </ul>
     *
     * @param appointmentId the appointment to reschedule
     * @param request       new date and time
     * @return updated appointment DTO
     */
    AppointmentResponse rescheduleAppointment(Long appointmentId, AppointmentRequest request);

    /**
     * Update the status of an appointment (e.g., CONFIRMED, COMPLETED, NO_SHOW).
     *
     * @param appointmentId the appointment to update
     * @param status        the new status
     * @return updated appointment DTO
     */
    AppointmentResponse updateStatus(Long appointmentId, AppointmentStatus status);
}

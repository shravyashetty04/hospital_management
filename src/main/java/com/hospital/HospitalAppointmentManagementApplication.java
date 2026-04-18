package com.hospital;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Hospital Appointment Management System.
 *
 * <p>Base package: {@code com.hospital}</p>
 * <ul>
 *   <li>controller  — REST controllers</li>
 *   <li>service     — Business logic interfaces + impl</li>
 *   <li>repository  — Spring Data JPA repositories</li>
 *   <li>model       — JPA entities and enums</li>
 *   <li>dto         — Request / response DTOs</li>
 *   <li>security    — JWT infrastructure</li>
 *   <li>config      — Spring beans and configuration</li>
 *   <li>exception   — Custom exceptions and global handler</li>
 * </ul>
 */
@SpringBootApplication
public class HospitalAppointmentManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(HospitalAppointmentManagementApplication.class, args);
    }
}

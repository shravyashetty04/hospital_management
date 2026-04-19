🏥 Project Report: Hospital Appointment Management System
1. Project Overview
The Hospital Appointment Management System is a full-stack web application designed to streamline the process of booking, managing, and tracking medical appointments. The system bridges the gap between patients, doctors, and hospital administrators by providing a secure, role-based platform. It eliminates manual scheduling conflicts, enhances patient experience, and optimizes doctor availability.

2. Technology Stack
The project is built using modern, industry-standard frameworks and technologies:

Backend (Server-Side)
Framework: Spring Boot 3.2 (Java 17)
Security: Spring Security with stateless JWT (JSON Web Tokens) Authentication
Database: PostgreSQL (Hosted on Aiven Cloud)
ORM: Spring Data JPA / Hibernate
Build Tool: Maven
Frontend (Client-Side)
Library: React.js (v19)
Routing: React Router DOM (v7)
HTTP Client: Axios (with interceptors for automatic JWT token attachment)
Styling: Pure CSS with a modern UI design language (Glassmorphism, CSS Variables, Flexbox/Grid).
3. System Architecture & Security
RESTful API: The backend exposes a strictly defined REST API mapped under /api/v1.
Role-Based Access Control (RBAC): The system enforces authorization at both the API level (Spring Security hasRole) and the UI level (PrivateRoute wrappers in React).
Stateless Authentication: Passwords are encrypted using BCrypt. Upon login, the server issues a JWT. The React frontend stores this token in localStorage and attaches it to the Authorization header of subsequent API requests.
Cloud Database: The PostgreSQL database is hosted on Aiven, ensuring high availability and secure remote connections via TLS/SSL.
4. Key Features by User Role
👤 Patient Role (ROLE_PATIENT)
Registration & Authentication: Secure sign-up and login.
Dashboard: View upcoming appointments and historical medical visits.
Book Appointments: Browse a directory of registered doctors, filter by specialization, and book an appointment slot.
Manage Appointments: Cancel or view the status (Pending, Confirmed, Completed) of booked appointments.
Profile Management: Update personal medical history, contact info, and emergency details.
👨‍⚕️ Doctor Role (ROLE_DOCTOR)
Doctor Dashboard: View a schedule of appointments booked by patients.
Appointment Management: Update the status of appointments (e.g., mark as "Completed" or "No Show").
Profile Management: View their own directory listing, including their specialization (e.g., Cardiology, Neurology), department, and consultation fees.
🛡️ Administrator Role (ROLE_ADMIN)
System Oversight: Full access to all hospital records.
Patient Directory: View and manage all registered patients in the system.
Doctor Directory: Manage doctor profiles and specializations.
(Future Scope): Approve new doctor registrations and view hospital analytics.
5. Core API Endpoints (Backend)
Module	Endpoint	Method	Description
Auth	/api/v1/auth/login	POST	Authenticates user & returns JWT.
Auth	/api/v1/auth/register	POST	Registers a new user.
Doctors	/api/v1/doctors	GET	Fetches the directory of all doctors.
Patients	/api/v1/patients/me	GET	Fetches the logged-in patient's profile.
Appointments	/api/v1/appointments	POST	Books a new appointment.
Appointments	/api/v1/appointments/my	GET	Gets appointments for the logged-in patient.
Appointments	/api/v1/appointments/doctor/{id}	GET	Gets the schedule for a specific doctor.
6. Challenges Solved During Development
Handling JWT State: Implemented Axios interceptors on the React frontend to seamlessly attach authorization tokens to every outgoing request and handle token expiration gracefully.
Role-Based UI Rendering: Designed a dynamic sidebar and routing system in React (App.js & Sidebar.js) that automatically hides unauthorized links based on the user's role (e.g., hiding the "Book Appointment" button from Doctors).
Data Hydration: Solved complex relational data fetching, such as resolving a logged-in user's userId from their JWT token to fetch their specific Doctor or Patient profile entity from the database.
Deployment Security: Successfully sanitized environment secrets by moving hardcoded database credentials out of application.properties and utilizing system environment variables for GitHub deployment.
7. Future Enhancements
Time-Slot Validation: Implement rigid time-window validation to prevent two patients from booking the exact same time slot for a single doctor.
Email/SMS Notifications: Integrate SendGrid or Twilio to notify patients when an appointment status changes.
Billing & Prescriptions: Add modules for doctors to upload digital prescriptions and for patients to pay consultation fees online.

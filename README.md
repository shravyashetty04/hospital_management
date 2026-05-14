# 🏥 Hospital Appointment Management System

> A full-stack web application designed to streamline the process of booking, managing, and tracking medical appointments, bridging the gap between patients, doctors, and hospital administrators.

---

## 📖 Project Overview

The Hospital Appointment Management System provides a secure, role-based platform that eliminates manual scheduling conflicts, enhances the patient experience, and optimizes doctor availability. 

### 🧰 Technology Stack

#### Frontend
<p align="left">
  <a href="https://skillicons.dev">
    <img src="https://skillicons.dev/icons?i=react,js,css,html" />
  </a>
</p>

* **Library:** React.js (v19)
* **Routing:** React Router DOM (v7)
* **HTTP Client:** Axios (with interceptors for automatic JWT token attachment)
* **Styling:** Pure CSS (Glassmorphism, CSS Variables, Flexbox/Grid)

#### Backend & Database
<p align="left">
  <a href="https://skillicons.dev">
    <img src="https://skillicons.dev/icons?i=java,spring,postgres,maven" />
  </a>
</p>

* **Framework:** Spring Boot 3.2 (Java 17)
* **Security:** Spring Security with stateless JWT Authentication
* **Database:** PostgreSQL (Hosted on Aiven Cloud)
* **ORM:** Spring Data JPA / Hibernate

---

## 🔐 System Architecture & Security

* **RESTful API:** The backend exposes a strictly defined REST API mapped under `/api/v1`.
* **Role-Based Access Control (RBAC):** Enforces authorization at both the API level (`hasRole` in Spring Security) and the UI level (`PrivateRoute` wrappers in React).
* **Stateless Authentication:** Passwords are encrypted using BCrypt. The server issues a JWT upon login, which is stored in `localStorage` and attached to the `Authorization` header of subsequent API requests.
* **Cloud Database:** High availability and secure remote connections via TLS/SSL on Aiven Cloud.

---

## ✨ Key Features by User Role

### 👤 Patient (`ROLE_PATIENT`)
* **Authentication:** Secure sign-up and login.
* **Dashboard:** View upcoming appointments and historical visits.
* **Book Appointments:** Browse doctor directories, filter by specialization, and book slots.
* **Manage Appointments:** Cancel or view status (Pending, Confirmed, Completed).
* **Profile Management:** Update medical history, contact info, and emergency details.

### 👨‍⚕️ Doctor (`ROLE_DOCTOR`)
* **Doctor Dashboard:** View daily/weekly schedules of patient appointments.
* **Appointment Management:** Update appointment statuses (e.g., "Completed", "No Show").
* **Profile Management:** View and manage directory listings, specialization, and consultation fees.

### 🛡️ Administrator (`ROLE_ADMIN`)
* **System Oversight:** Full access to all hospital records.
* **Directories:** View and manage all registered patient and doctor profiles.
* *(Future)* Approve new doctor registrations and view hospital analytics.

---

## 📡 Core API Endpoints

| Module | Endpoint | Method | Description |
| :--- | :--- | :---: | :--- |
| **Auth** | `/api/v1/auth/login` | `POST` | Authenticates user & returns JWT |
| **Auth** | `/api/v1/auth/register` | `POST` | Registers a new user |
| **Doctors** | `/api/v1/doctors` | `GET` | Fetches the directory of all doctors |
| **Patients** | `/api/v1/patients/me` | `GET` | Fetches the logged-in patient's profile |
| **Appointments** | `/api/v1/appointments` | `POST` | Books a new appointment |
| **Appointments** | `/api/v1/appointments/my` | `GET` | Gets appointments for the logged-in patient |
| **Appointments** | `/api/v1/appointments/doctor/{id}`| `GET` | Gets the schedule for a specific doctor |

---

## 🧠 Challenges Solved

* **Handling JWT State:** Implemented Axios interceptors on the frontend to seamlessly attach authorization tokens to every outgoing request and handle token expiration gracefully.
* **Role-Based UI Rendering:** Designed a dynamic React sidebar and routing system (`App.js` & `Sidebar.js`) that automatically hides unauthorized links based on user roles.
* **Data Hydration:** Solved complex relational data fetching, resolving a logged-in user's ID from their JWT to fetch specific Doctor or Patient profile entities.
* **Deployment Security:** Sanitized environment secrets by moving hardcoded database credentials out of `application.properties` and utilizing system environment variables for deployment.

---

## 🚀 Future Enhancements

* [ ] **Time-Slot Validation:** Rigid time-window validation to prevent double-booking a single doctor.
* [ ] **Automated Notifications:** Integrate SendGrid or Twilio for email/SMS alerts on appointment status changes.
* [ ] **Billing & Prescriptions:** Modules for digital prescription uploads and online consultation fee payments.

---

## 💻 Getting Started (Local Development)

### Prerequisites
* Node.js (v18+)
* Java 17 & Maven
* PostgreSQL

### Setup Instructions

1.  **Clone the repository:**
    ```bash
    git clone [https://github.com/yourusername/hospital-management-system.git](https://github.com/yourusername/hospital-management-system.git)
    ```

2.  **Environment Variables:**
    * Create a `.env` file in your frontend directory based on the `.env.example`.
    * Update your `application.properties` (or set system environment variables) with your local PostgreSQL credentials and JWT Secret. 
    * *Note: Ensure your `.env` and `application.properties` are listed in your `.gitignore`!*

3.  **Run Backend:**
    ```bash
    cd backend
    mvn spring-boot:run
    ```

4.  **Run Frontend:**
    ```bash
    cd frontend
    npm install
    npm start
    ```

# Backend Application

The backend application for the Web-Based Vaccination Portal (WBVP-SE2030) is a modular monolith built with Java 21 and Spring Boot. It uses Maven for build automation, MySQL for persistent relational storage with Flyway database migrations, and an in-memory H2 database alongside MySQL Testcontainers for multi-tier verification.

## Declared Technology Stack

- **Language**: Java 21
- **Framework**: Spring Boot (v4.1.0) with Spring Web MVC
- **Persistence**: Spring Data JPA / Hibernate ORM
- **Database Migrations**: Flyway (`spring-boot-starter-flyway`, `flyway-mysql`)
- **Security**: Spring Security with stateless JWT (`jjwt` v0.12.6) and Role-Based Access Control (RBAC)
- **Validation**: Jakarta Bean Validation (`spring-boot-starter-validation`)
- **Database Engine (Runtime)**: MySQL 8.4 LTS (via `mysql-connector-j`)
- **Database Engine (Testing)**: In-memory H2 database (MySQL mode) & Testcontainers MySQL 8.4
- **Testing & Verification**: JUnit 5, Spring Boot Test, Spring WebMvc Test, Spring Security Test, Testcontainers
- **Monitoring & Metrics**: Spring Boot Actuator
- **Build Tool**: Maven (with embedded Maven Wrapper)

## Architecture & Module Organization

The backend follows an explicit hexagonal / ports-and-adapters architecture under package `com.se2030.vaccinationportal`:

- **Shared Authentication & Access Control (`shared/auth`)**:
  - Stateless JWT token lifecycle (`JwtTokenService`, `JwtAuthenticationFilter`, `SecurityConfig`).
  - User identity models (`UserAccount`, `Role`, `Permission`, `StaffRegistrationRequest`).
  - Endpoints for login (`/api/v1/auth/login`), citizen self-registration (`/api/v1/auth/register/citizen`), staff registration requests (`/api/v1/auth/register/staff`), authenticated user inspection (`/api/v1/auth/me`), and administrator request review (`/api/v1/admin/staff-requests`).
- **MF-01: Screening & Medical Eligibility (`screening`)**:
  - Questionnaire versioning, dynamic rule evaluation, answer option definitions, and submission processing.
  - Endpoints for screening configuration, questionnaire definitions, submissions, and healthcare officer eligibility reviews.
- **MF-02: Campaign, Center & Session Management (`campaign`)**:
  - Campaign lifecycle management (`VaccinationCampaign`, `CampaignStatus`), dose programme versioning, target group models, and geographic location targeting.
  - Vaccination centers (`VaccinationCenter`), operating hours, center closure tracking, and occupancy ceilings.
  - Vaccination sessions (`VaccinationSession`), recurrence rule series generation, time slot capacity models, staff rostering, and operational readiness diagnostics.
  - Operational analytics reporting (`OperationalAnalyticsController`).
- **MF-03: Appointment, Waitlist & Check-In (`appointment`)**:
  - Concurrency-safe slot reservations with citizen booking guard (`AppointmentBookingService`).
  - Rescheduling, cancellations, check-in processing, check-in reversal, and waitlist queue management (`AppointmentController`, `WaitlistController`).
  - Inter-module decoupling via explicit ports (`EligibilityLookupPort`, `SlotCapacityPort`).
- **MF-04: Vaccine Inventory & Batch Management (`inventory`)**:
  - Planned module boundary; package structure and dependency interfaces prepared for upcoming stock, cold-chain, and batch allocation slices.
- **MF-05: Vaccination Administration & Records (`vaccination`)**:
  - Vaccination draft records with full CRUD (`VaccinationDraftController`).
  - Finalized vaccination records and verifiable digital certificates (`DigitalCertificateController`, `DigitalCertificateService`).

## Database & Flyway Migrations

Database schema versioning is managed strictly through Flyway migrations located in `src/main/resources/db/migration/`.

- **Active Migration Range**: Current migrations span `V0` through `V12` (13 active migrations).
- **Migration Execution**: Flyway migrations run automatically upon application startup when connected to a configured MySQL database.
- **DDL Validation**: Production safety is enforced via `spring.jpa.hibernate.ddl-auto=${JPA_DDL_AUTO:validate}`, verifying that Hibernate entities match the Flyway-applied schema without runtime auto-modification.
- **Testing Profile (`application-test.properties`)**: Standard unit and integration tests run against an in-memory H2 database (`MODE=MySQL`) with `create-drop` lifecycle for fast, isolated execution.

## Environment & Security Configuration

Environment variables and defaults are declared in `src/main/resources/application.properties`. Refer to `.env.example` for recommended local development settings:

```env
SERVER_PORT=8080
DB_URL=jdbc:mysql://localhost:3306/vaccination_portal
DB_USERNAME=vaccination_app
DB_PASSWORD=replace_with_local_password
JPA_DDL_AUTO=validate
JWT_SECRET=replace_with_base64_or_hex_jwt_secret_min_256_bits
JWT_EXPIRATION_MINUTES=60
```

### Optional First Administrator Bootstrap
The backend supports automatic creation of an initial administrative account on startup if all four bootstrap environment variables are provided:
- `BOOTSTRAP_ADMIN_EMAIL`
- `BOOTSTRAP_ADMIN_PASSWORD`
- `BOOTSTRAP_ADMIN_FULL_NAME`
- `BOOTSTRAP_ADMIN_PHONE`

### Setting Environment Variables

**Windows PowerShell:**
```powershell
$env:SERVER_PORT="8080"
$env:DB_URL="jdbc:mysql://localhost:3306/vaccination_portal"
$env:DB_USERNAME="vaccination_app"
$env:DB_PASSWORD="your_mysql_password"
$env:JWT_SECRET="your_secure_random_jwt_secret_key"
```

**Unix / macOS / Linux:**
```bash
export SERVER_PORT=8080
export DB_URL=jdbc:mysql://localhost:3306/vaccination_portal
export DB_USERNAME=vaccination_app
export DB_PASSWORD=your_mysql_password
export JWT_SECRET=your_secure_random_jwt_secret_key
```

*Note: Never commit `.env` or configuration files containing real passwords or keys to version control.*

## Available Commands

Execute commands from the `backend/` directory using the embedded Maven Wrapper:

### Windows (PowerShell)

```powershell
# Run standard unit & integration test suites (in-memory H2)
.\mvnw.cmd test

# Run full verification build (H2 test suite + package compilation)
.\mvnw.cmd verify

# Run full verification with real MySQL Testcontainers concurrency assurance
.\mvnw.cmd verify -Pmysql-concurrency

# Run Spring Boot application locally (requires running MySQL database)
.\mvnw.cmd spring-boot:run
```

### Unix-like Systems (macOS / Linux)

```bash
# Run standard unit & integration test suites (in-memory H2)
./mvnw test

# Run full verification build (H2 test suite + package compilation)
./mvnw verify

# Run full verification with real MySQL Testcontainers concurrency assurance
./mvnw verify -Pmysql-concurrency

# Run Spring Boot application locally
./mvnw spring-boot:run
```

## Digital Certificate API

The backend provides independent digital certificate management and public verification via `DigitalCertificateController` under `/api/v1/vaccination/certificates`:

| Method | Endpoint | Access Control | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/vaccination/certificates/{id}` | Authenticated | Retrieve complete digital certificate record by database ID. |
| `GET` | `/api/v1/vaccination/certificates/reference/{certificateReference}` | Authenticated | Retrieve digital certificate record by unique certificate reference (e.g. `CERT-...`). |
| `GET` | `/api/v1/vaccination/certificates/verify/{token}` | Public (`permitAll`) | Verify a digital certificate by its unique verification token. Returns a privacy-preserving masked response (`CertificateVerificationResponse`) confirming issuance without exposing sensitive citizen data. |

- **Issuance**: Digital certificates are issued via `DigitalCertificateService.issueCertificateForVaccinationRecord()` upon vaccination finalization, automatically generating a collision-resistant certificate reference and a secure verification token.

## Actuator Health Endpoint

When the backend application is running, the Spring Boot Actuator health endpoint is available at:

```text
GET http://localhost:8080/actuator/health
```

Expected response:
```json
{
  "status": "UP"
}
```

## Directory Structure

```text
backend/
|-- README.md
|-- pom.xml
|-- mvnw
|-- mvnw.cmd
|-- .env.example
|-- .mvn/
|   \-- wrapper/
|       \-- maven-wrapper.properties
\-- src/
    |-- main/
    |   |-- java/
    |   |   \-- com/
    |   |       \-- se2030/
    |   |           \-- vaccinationportal/
    |   |               |-- VaccinationPortalApplication.java
    |   |               |-- appointment/
    |   |               |-- campaign/
    |   |               |-- inventory/
    |   |               |-- screening/
    |   |               |-- shared/
    |   |               |   \-- auth/
    |   |               \-- vaccination/
    |   \-- resources/
    |       |-- application.properties
    |       \-- db/
    |           \-- migration/
    |               |-- V0__core_campaign_center_session_foundation.sql
    |               \-- ... (V1 through V12)
    \-- test/
        |-- java/
        |   \-- com/
        |       \-- se2030/
        |           \-- vaccinationportal/
        \-- resources/
            \-- application-test.properties
```

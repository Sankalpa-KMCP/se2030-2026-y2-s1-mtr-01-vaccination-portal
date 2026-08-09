# Backend Application

The backend application for the Web-Based Vaccination Portal (WBVP-SE2030) is built with Java 21 and Spring Boot, using Maven for build automation and dependency management, MySQL for persistent relational storage, and an in-memory H2 database for isolated testing.

## Declared Technology Stack

- **Language**: Java 21
- **Framework**: Spring Boot (v4.1.0) with Web MVC
- **Persistence**: Spring Data JPA / Hibernate ORM
- **Validation**: Jakarta Bean Validation (`spring-boot-starter-validation`)
- **Database Engine (Runtime)**: MySQL 8.0+ (via `mysql-connector-j`)
- **Database Engine (Testing)**: H2 Database in MySQL compatibility mode
- **Testing & Verification**: JUnit 5, Spring Boot Test, Spring WebMvc Test
- **Monitoring & Metrics**: Spring Boot Actuator
- **Build Tool**: Maven (with embedded Maven Wrapper)

## Current MF-02 Backend Foundations

The backend currently contains the core persistence, domain models, and service layer for **MF-02 (Vaccination Campaign, Center, and Session Management)** under Sprint 1:

- **Campaign Lifecycle & Master Data**: `VaccinationCampaign`, `CampaignStatus` (`DRAFT`, `SCHEDULED`, `PUBLISHED`, `ACTIVE`, `SUSPENDED`, `COMPLETED`, `CANCELLED`, `ARCHIVED`), `VaccineType`, and `TargetGroup` models and repositories.
- **Dose-Programme Versioning**: `CampaignDoseProgramme`, `DoseCategory` (`PRIMARY`, `BOOSTER`), unique version constraint per campaign, effective-date interval management (`BR-028` / `DEC-030`), and future-only replacement enforcement (`MF02-BR-18`).
- **Campaign Location Targeting**: `CampaignLocationTarget`, `LocationTargetType` (`PROVINCE`, `DISTRICT`, `HEALTH_REGION`, `CENTER`) supporting administrative hierarchy validation.
- **Campaign Questionnaire Assignment**: `CampaignQuestionnaireAssignment`, effective-dated assignment of published questionnaire versions, and future-only replacement enforcement (`MF02-BR-17`).
- **Vaccination Center Master Data**: `VaccinationCenter`, `CenterStatus` (`ACTIVE`, `INACTIVE`), unique center code validation, and occupancy/ceiling constraints.
- **Concurrency Serialization**: Parent `VaccinationCampaign` row-level pessimistic write locking (`findByIdForUpdate` with `LockModeType.PESSIMISTIC_WRITE`) preventing race conditions during dose-programme and questionnaire-assignment configuration changes (per Section 12.4).
- **History Preservation**: Effective-dated versioning with bounded `activeTo` dates (no hard deletions or retroactive overwriting of historical records).

### Cross-Module Boundaries (MF-01 / MF-02)

- **MF-01** owns questionnaire version definitions and medical eligibility rules.
- **MF-02** owns campaign questionnaire selection via `CampaignQuestionnaireAssignment`.
- Until full MF-01 integration is completed, cross-module questionnaire publication validation is mediated through the `QuestionnaireVersionPublicationPort` interface.

## Database & Profiles

- **Runtime Profile**: Connects to a MySQL 8.0+ instance. Schema DDL is managed by default with `spring.jpa.hibernate.ddl-auto=none` for production safety.
- **Test Profile (`application-test.properties`)**: Configured with an isolated in-memory H2 database (`MODE=MySQL`) with `create-drop` lifecycle for fast, independent test execution.

## Environment Configuration

Environment variables and defaults are defined in `src/main/resources/application.properties`. Refer to `.env.example` for recommended local development settings:

```env
SERVER_PORT=8080
DB_URL=jdbc:mysql://localhost:3306/vaccination_portal
DB_USERNAME=vaccination_app
DB_PASSWORD=replace_with_local_password
JPA_DDL_AUTO=none
```

### Setting Environment Variables

**Windows PowerShell:**
```powershell
$env:SERVER_PORT="8080"
$env:DB_URL="jdbc:mysql://localhost:3306/vaccination_portal"
$env:DB_USERNAME="vaccination_app"
$env:DB_PASSWORD="your_mysql_password"
```

**Unix / macOS / Linux:**
```bash
export SERVER_PORT=8080
export DB_URL=jdbc:mysql://localhost:3306/vaccination_portal
export DB_USERNAME=vaccination_app
export DB_PASSWORD=your_mysql_password
```

## Available Commands

Execute commands from the `backend/` directory using the embedded Maven Wrapper:

### Windows (PowerShell / Command Prompt)

Run all unit & persistence integration tests:
```powershell
.\mvnw.cmd test
```

Execute full verification build:
```powershell
.\mvnw.cmd verify
```

Run application locally (requires running MySQL database):
```powershell
.\mvnw.cmd spring-boot:run
```

### Unix-like Systems (macOS / Linux)

Run all unit & persistence integration tests:
```bash
./mvnw test
```

Execute full verification build:
```bash
./mvnw verify
```

Run application locally:
```bash
./mvnw spring-boot:run
```

## Actuator Health Endpoint

When the application is running, the Actuator health endpoint is available at:

```text
GET http://localhost:8080/actuator/health
```

Expected health status response:
```json
{
  "status": "UP"
}
```

## Directory Structure

```text
backend/
├── README.md
├── pom.xml
├── mvnw
├── mvnw.cmd
├── .env.example
├── .mvn/
│   └── wrapper/
│       └── maven-wrapper.properties
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/
    │   │       └── se2030/
    │   │           └── vaccinationportal/
    │   │               ├── VaccinationPortalApplication.java
    │   │               ├── appointment/
    │   │               └── campaign/
    │   │                   ├── model/
    │   │                   ├── repository/
    │   │                   └── service/
    │   └── resources/
    │       └── application.properties
    └── test/
        ├── java/
        │   └── com/
        │       └── se2030/
        │           └── vaccinationportal/
        │               ├── HealthEndpointIntegrationTests.java
        │               ├── VaccinationPortalApplicationTests.java
        │               └── campaign/
        └── resources/
            └── application-test.properties
```

## Planned & Pending Backend Areas

The following areas are part of the roadmap and will be delivered in upcoming slices:

- **MF-02 Domain Slices**: Operating Hours (`OperatingHours`), Center Closures (`CenterClosure`), Campaign-Center Assignments (`CampaignCenterAssignment`), Session management & capacity planning, Campaign publication transition workflow.
- **API & Controller Layer**: REST controllers, DTOs, mappers, and HTTP request validation for MF-02.
- **Shared Infrastructure**: Flyway database migrations, Spring Security with Role-Based Access Control (RBAC), and OpenAPI / Swagger documentation.
- **Cross-Module Integration**: Integration of concrete MF-01 QuestionnaireVersion entities and database foreign-key/adapter mappings.
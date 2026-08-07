# Backend Application

The backend application for the Web-Based Vaccination Portal is built with Java 21 and Spring Boot, using Maven for build automation and dependency management, and MySQL for persistent relational storage.

## Declared Technology Baseline

- **Language**: Java 21
- **Framework**: Spring Boot 4.1 (v4.1.0) with Web MVC
- **Build Tool**: Maven (with Maven Wrapper)
- **Database Engine**: MySQL (with Spring Data JPA / Hibernate ORM)
- **Monitoring & Metrics**: Spring Boot Actuator

## Prerequisites

- **Java Development Kit (JDK)**: Java 21 or later
- **Relational Database**: MySQL Server 8.0+ (required for normal runtime execution)

## Environment Configuration

Environment-variable placeholders are defined in `src/main/resources/application.properties`.

Refer to `.env.example` for recommended local development settings:

```env
SERVER_PORT=8080
DB_URL=jdbc:mysql://localhost:3306/vaccination_portal
DB_USERNAME=vaccination_app
DB_PASSWORD=replace_with_local_password
JPA_DDL_AUTO=none
```

To set up environment variables locally on Windows PowerShell:

```powershell
$env:SERVER_PORT="8080"
$env:DB_URL="jdbc:mysql://localhost:3306/vaccination_portal"
$env:DB_USERNAME="vaccination_app"
$env:DB_PASSWORD="your_mysql_password"
```

To set up on Unix/Linux/macOS:

```bash
export SERVER_PORT=8080
export DB_URL=jdbc:mysql://localhost:3306/vaccination_portal
export DB_USERNAME=vaccination_app
export DB_PASSWORD=your_mysql_password
```

*Note: Running the application with default production settings requires a running, accessible MySQL database instance.*

## Available Commands

Execute commands from the `backend/` directory using the embedded Maven Wrapper:

### Windows (PowerShell / Command Prompt)

Run unit & integration tests (uses in-memory H2 test profile):
```powershell
.\mvnw.cmd test
```

Execute full verification build:
```powershell
.\mvnw.cmd verify
```

Run application locally (requires active MySQL instance):
```powershell
.\mvnw.cmd spring-boot:run
```

### Unix-like Systems (macOS / Linux)

Run unit & integration tests:
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
    │   │               └── VaccinationPortalApplication.java
    │   └── resources/
    │       └── application.properties
    └── test/
        ├── java/
        │   └── com/
        │       └── se2030/
        │           └── vaccinationportal/
        │               ├── VaccinationPortalApplicationTests.java
        │               └── HealthEndpointIntegrationTests.java
        └── resources/
            └── application-test.properties
```

## Scope and Current Status

This directory currently contains only the **shared backend foundation** (Spring Boot entry point, Actuator health endpoint configuration, JPA/MySQL property binding, and H2 test profile).

- No authentication or authorization has been implemented yet.
- No business domain entities or migrations are present in this foundation.
- Business functionality (MF-01 through MF-05) will be added incrementally through reviewed feature branches in accordance with canonical project specifications.
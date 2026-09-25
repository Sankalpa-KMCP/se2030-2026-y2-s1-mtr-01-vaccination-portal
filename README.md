# Web-Based Vaccination Portal

## Project Overview

The Web-Based Vaccination Portal (WBVP-SE2030) is a healthcare orchestration platform supporting vaccination program operations for citizens, operational staff, healthcare professionals, and system administrators. The portal is architected as a layered modular monolith with explicit hexagonal boundaries (ports and adapters) ensuring decoupled domain interactions.

## Technology Baseline

- **Frontend**: React 19, React Router DOM 7, Vite 8, Bootstrap 5 / Vanilla CSS, Vitest 4, Testing Library
- **Backend**: Java 21, Spring Boot (Spring Web MVC, Spring Security, Spring Data JPA, Jakarta Validation, Spring Boot Actuator)
- **Database Engine**: MySQL 8.4 LTS (Flyway migration framework)
- **Security & Auth**: Stateless JWT authentication (`jjwt` v0.12.6) with Role-Based Access Control (RBAC)
- **Build Tooling**: Maven (with embedded wrapper) & npm
- **Continuous Integration**: GitHub Actions (Node.js 24 for frontend; Java 21 Temurin for backend)

## System Architecture & Hexagonal Decoupling

The backend follows an explicit hexagonal architecture (ports and adapters) under package `com.se2030.vaccinationportal`:

- **Decoupled Inter-Module Communication**: Domain interactions across module boundaries use explicit ports (e.g. `EligibilityLookupPort`, `SlotCapacityPort`) to prevent direct database coupling between domains.
- **Relational Integrity & Migrations**: Schema evolution is controlled entirely via Flyway database migrations in `backend/src/main/resources/db/migration/`. Current migrations span `V0` through `V12` (13 active migrations).
- **Dual Verification Suite**: Backend verification combines fast in-memory H2 unit/integration tests with real MySQL Testcontainers for database schema and concurrency assurance.

## Functional Capabilities & Implementation Status

| Module / Capability | Backend Package | Frontend Feature | Implementation Status | Scope & Responsibilities |
| :--- | :--- | :--- | :--- | :--- |
| **Shared Authentication & Access Control** | `shared/auth` | `src/auth/` | **Implemented** | Stateless JWT authentication, citizen self-registration, staff registration requests, admin review, and role-based route protection. |
| **MF-01: Pre-Vaccination Screening & Eligibility** | `screening` | `src/features/screening/` | **Implemented** | Screening question definitions, questionnaire versioning, dynamic rule evaluation, submission processing, and clinical eligibility reviews. |
| **MF-02: Campaign, Center & Session Management** | `campaign` | `campaign-management`, `session-management`, `center-operations` | **Implemented** | Campaigns, target groups, dose programmes, geographic target allocations, vaccination centers, operating hours, closures, recurrence rule series generation, time slot capacity, and staff rostering. |
| **MF-03: Appointment, Waitlist & Check-In** | `appointment` | `src/features/appointment-management/` | **Partial** | **Backend Implemented**: Citizen booking guard, concurrency-safe slot reservations, rescheduling, cancellations, check-in, check-in reversal, and waitlist queue management. **Frontend Partial**: Availability search, appointment booking (`/app/appointments/book`), appointment listing, rescheduling, and cancellation UI (`/app/appointments/mine`) implemented for citizens; waitlist join/management and staff check-in queue UI remain pending. |
| **MF-04: Vaccine Inventory & Batch Management** | `inventory` | `src/features/inventory-management/` | **Planned** | Module boundaries and package structure prepared; stock, cold-chain, and batch allocation slices are planned in upcoming milestones. |
| **MF-05: Vaccination Administration & Records** | `vaccination` | `src/features/vaccination-administration/` | **Partial** | **Backend Implemented**: Vaccination draft CRUD, finalization integration, and digital certificate issuance/verification. **Frontend Partial**: Digital certificate verification component is available; full clinical administration workflow UI is pending. |
| **Operational Analytics** | `campaign` | `src/features/analytics/` | **Implemented** | Interactive operational analytics view with filterable vaccination program metrics. |
| **Public Landing & Information** | N/A | `src/features/public-landing/` | **Implemented** | Responsive public landing experience, accessible navigation, workflow guide (`/how-it-works`), and mission overview (`/about`). |
| **Project-Wide AI Assistant** | `assistant` | `src/features/assistant/` | **Implemented** | Secure, read-only conversational guidance, role-aware route suggestions, deterministic safety policy, and personalized citizen lookups. See [AI Assistant Setup & Demo Guide](docs/AI_ASSISTANT_SETUP_AND_DEMO.md). |

## Public Application Routes

The frontend application provides public navigation and dedicated access flows:

| Route | Component / View | Description |
| :--- | :--- | :--- |
| `/` | `LandingPage` | Home portal landing page with program overview, readiness gates, and trust metrics. |
| `/how-it-works` | `HowItWorksPage` | Informational guide explaining the vaccination journey for citizens and providers. |
| `/about` | `AboutPage` | Portal mission, architecture, and governance standards. |
| `/login` | `LoginPage` | Universal login portal for all registered accounts (legacy `/login/citizen` and `/login/staff` routes redirect here). |
| `/register/citizen` | `CitizenRegistrationPage` | Citizen self-registration for vaccination access. |
| `/register/staff` | `StaffRegistrationRequestPage` | Operational/medical staff registration request submission. |

All active citizen and staff accounts sign in through `/login`. After authentication, the application redirects users to the appropriate area based on their assigned role (`/app/citizen`, `/app/staff`, `/app/admin`) or authorized management interfaces. Citizen registration (`/register/citizen`) and staff access-request registration (`/register/staff`) remain separate workflows. Citizen users can access session availability search and appointment booking at `/app/appointments/book`, and personal appointment history, rescheduling, and cancellation management at `/app/appointments/mine`.

## Digital Certificate Management & Verification API

The portal provides verifiable digital vaccination certificates with privacy-preserving public verification via `DigitalCertificateController` under `/api/v1/vaccination/certificates`:

| Method | Endpoint | Access Control | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/vaccination/certificates/{id}` | Authenticated | Retrieve complete digital certificate record by ID. |
| `GET` | `/api/v1/vaccination/certificates/reference/{certificateReference}` | Authenticated | Retrieve digital certificate record by unique certificate reference (e.g. `CERT-...`). |
| `GET` | `/api/v1/vaccination/certificates/verify/{token}` | Public (`permitAll`) | Verify a digital certificate by its verification token. Returns a privacy-preserving masked response confirming certificate validity and dose details without exposing full medical history. |

- **Issuance**: Issued via `DigitalCertificateService` upon vaccination finalization, automatically generating a collision-resistant certificate reference and a secure verification token.

## Repository Structure

- `docs/` - Canonical project documentation (specifications, module handoffs, sprint evidence)
- `frontend/` - Frontend React 19 application (Vite, React Router DOM, Bootstrap 5, modular feature structure)
- `backend/` - Backend Spring Boot application (Java 21, Spring Data JPA, Flyway, MySQL 8.4)
- `database/` - Local Docker Compose environment, seed guidelines, and ER diagrams
- `tests/` - Cross-module, integration, system, end-to-end, and acceptance test resources
- `.github/` - GitHub Actions CI/CD workflows and PR templates

## Application Setup & Local Development

### Prerequisites

- **Java**: JDK 21 or higher
- **Node.js**: v18+ or v20+ (v24 recommended; npm v9+)
- **Container Engine**: Docker Desktop or Docker Engine with Docker Compose (for local database)
- **Database**: MySQL 8.4 LTS (or local Docker Compose container)

### 1. Local Database Setup (Docker Compose)

Start the local MySQL database:
```bash
docker compose --env-file database/.env up -d mysql
```

- **Connection URL**: `jdbc:mysql://localhost:3306/vaccination_portal`
- **Flyway Migrations**: Applied automatically on backend application startup. Current migrations span `V0` through `V12`.
- See [database/README.md](database/README.md) for full database workflow instructions.

### 2. Backend Development

From the `backend/` directory:

#### Windows (PowerShell)
```powershell
# Run standard unit & integration test suites (in-memory H2)
.\mvnw.cmd test

# Run full verification build (H2 test suite + package build)
.\mvnw.cmd verify

# Run full verification with MySQL Testcontainers concurrency assurance
.\mvnw.cmd verify -Pmysql-concurrency

# Run Spring Boot application locally (requires running MySQL database)
.\mvnw.cmd spring-boot:run
```

#### Unix-like (macOS / Linux)
```bash
# Run standard unit & integration test suites (in-memory H2)
./mvnw test

# Run full verification build (H2 test suite + package build)
./mvnw verify

# Run full verification with MySQL Testcontainers concurrency assurance
./mvnw verify -Pmysql-concurrency

# Run Spring Boot application locally
./mvnw spring-boot:run
```

#### Backend Health Endpoint
When the backend application is running, check Actuator health:
```text
GET http://localhost:8080/actuator/health
```

### 3. Frontend Development

From the `frontend/` directory:
```bash
npm install
npm run dev
npm test
npm run lint
npm run build
```

- Local Vite dev server runs at `http://localhost:5173`.
- Requests to `/api` are automatically proxied to `http://localhost:8080`.

## Configuration & Local Safety

Configuration templates are provided across modules:
- `backend/.env.example` - Server port, database connection, JPA DDL validation, and JWT secret settings
- `frontend/.env.example` - API base URL override
- `database/.env.example` - MySQL container credentials

### Local Developer Configuration Policy
- Use environment variables or local developer configuration profiles to supply local credentials.
- **Never commit `.env` files, passwords, API keys, JWT secrets, or private tokens.**
- Any developer-specific local properties must remain strictly untracked and unstaged.
- Never use real citizen, patient, or medical records; all development seed data must be 100% synthetic.

## Continuous Integration & Quality Gates

GitHub Actions workflows validate pull requests and merged commits targeting `develop` and `main`:

- **Frontend CI** (`.github/workflows/frontend-ci.yml`): Runs on Node 24 to execute `npm ci`, `npm run lint`, and `npm run build`.
- **Backend CI** (`.github/workflows/backend-ci.yml`): Runs on Java 21 Temurin to execute:
  1. `./mvnw --batch-mode --no-transfer-progress verify` (H2 test suite)
  2. `./mvnw --batch-mode --no-transfer-progress verify -Pmysql-concurrency` (MySQL Testcontainers concurrency assurance)

## Canonical Documentation

- [AI Canonical Project Specification](docs/Web_Based_Vaccination_Portal_AI_Canonical_Context_v3.3.md) - Authoritative normative source of truth (version 3.3-AI, revision 2026-08-14-R9)
- [MF-02 Module Handoff Specification](docs/MF02_Vaccination_Campaign_Center_and_Session_Management_AI_Handoff.md)
- [AI Assistant Setup & Evaluation Guide](docs/AI_ASSISTANT_SETUP_AND_DEMO.md) - Architecture, environment configuration, cURL commands, and evaluation walkthrough
- [Database Configuration Guide](database/README.md)
- [Contribution Guidelines](CONTRIBUTING.md)

# Web-Based Vaccination Portal

## Project Overview

The Web-Based Vaccination Portal is a healthcare orchestration platform supporting vaccination program operations for citizens, operational staff, healthcare professionals, and system administrators. The portal is architected as a layered modular monolith with explicit hexagonal boundaries (ports and adapters) ensuring decoupled domain interactions.

## Five Major Functional Modules

| Module | Backend Package | Frontend Feature | Primary Responsibilities |
| :--- | :--- | :--- | :--- |
| **MF-01: Pre-Vaccination Screening & Eligibility** | [`screening`](file:///e:/SE%20PROJECT_Vac/se2030-2026-y2-s1-mtr-01-vaccination-portal/backend/src/main/java/com/se2030/vaccinationportal/screening) | `screening` / `citizen-flow` | Questionnaire versioning, dynamic rule evaluation, contraindications, and citizen eligibility tracking. |
| **MF-02: Campaign, Center & Session Management** | [`campaign`](file:///e:/SE%20PROJECT_Vac/se2030-2026-y2-s1-mtr-01-vaccination-portal/backend/src/main/java/com/se2030/vaccinationportal/campaign) | [`campaign-management`](file:///e:/SE%20PROJECT_Vac/se2030-2026-y2-s1-mtr-01-vaccination-portal/frontend/src/features/campaign-management), [`session-management`](file:///e:/SE%20PROJECT_Vac/se2030-2026-y2-s1-mtr-01-vaccination-portal/frontend/src/features/session-management), [`center-operations`](file:///e:/SE%20PROJECT_Vac/se2030-2026-y2-s1-mtr-01-vaccination-portal/frontend/src/features/center-operations) | Campaigns, target groups, dose programmes, geographic target allocations, vaccination centers, recurrence rule series generation, slot capacity models, staff rostering, and center closures. |
| **MF-03: Appointment, Waitlist & Check-In** | [`appointment`](file:///e:/SE%20PROJECT_Vac/se2030-2026-y2-s1-mtr-01-vaccination-portal/backend/src/main/java/com/se2030/vaccinationportal/appointment) | [`appointment-management`](file:///e:/SE%20PROJECT_Vac/se2030-2026-y2-s1-mtr-01-vaccination-portal/frontend/src/features/appointment-management) | Citizen booking guard, concurrency-safe slot reservations, rescheduling/cancellation workflows, waitlist allocations, and check-in processing. |
| **MF-04: Vaccine Inventory & Batch Management** | [`inventory`](file:///e:/SE%20PROJECT_Vac/se2030-2026-y2-s1-mtr-01-vaccination-portal/backend/src/main/java/com/se2030/vaccinationportal/inventory) | `inventory-management` | Cold-chain monitoring, lot/batch tracking, center inventory stock, and wastage management. |
| **MF-05: Vaccination Administration & Records** | [`vaccination`](file:///e:/SE%20PROJECT_Vac/se2030-2026-y2-s1-mtr-01-vaccination-portal/backend/src/main/java/com/se2030/vaccinationportal/vaccination) | [`vaccination-administration`](file:///e:/SE%20PROJECT_Vac/se2030-2026-y2-s1-mtr-01-vaccination-portal/frontend/src/features/vaccination-administration) | Clinical administration logs, dual-confirmation draft records, adverse event tracking, and verifiable digital certificate issuance. |

## Current Implementation Status

- **Sprint 1 / Active Development**: Core business services, API controllers, database persistence, and user interfaces are actively implemented across MF-01, MF-02, MF-03, and MF-05.
- **Database Migrations**: 12 active Flyway schema migrations ([`V0` through `V11`](file:///e:/SE%20PROJECT_Vac/se2030-2026-y2-s1-mtr-01-vaccination-portal/backend/src/main/resources/db/migration/)) define database tables, relational constraints, foreign keys, and indexes.
- **Hexagonal Decoupling**: Inter-module communication uses explicit ports (e.g. [`EligibilityLookupPort`](file:///e:/SE%20PROJECT_Vac/se2030-2026-y2-s1-mtr-01-vaccination-portal/backend/src/main/java/com/se2030/vaccinationportal/appointment/dependency/EligibilityLookupPort.java), [`SlotCapacityPort`](file:///e:/SE%20PROJECT_Vac/se2030-2026-y2-s1-mtr-01-vaccination-portal/backend/src/main/java/com/se2030/vaccinationportal/appointment/dependency/SlotCapacityPort.java)) to prevent direct database coupling between domains.
- **Dual Test Suite**: Full test coverage spanning fast in-memory H2 unit/integration tests and real MySQL Testcontainers for database migrations and concurrency guarantees.

## Canonical Documentation

- [AI Canonical Project Specification](file:///e:/SE%20PROJECT_Vac/se2030-2026-y2-s1-mtr-01-vaccination-portal/docs/Web_Based_Vaccination_Portal_AI_Canonical_Context_v3.3.md)
- [MF-02 Module Handoff Specification](file:///e:/SE%20PROJECT_Vac/se2030-2026-y2-s1-mtr-01-vaccination-portal/docs/MF02_Vaccination_Campaign_Center_and_Session_Management_AI_Handoff.md)
- [Database Configuration Guide](file:///e:/SE%20PROJECT_Vac/se2030-2026-y2-s1-mtr-01-vaccination-portal/database/README.md)

The Markdown canonical document is the sole authoritative project source of truth (version 3.3-AI, revision 2026-08-14-R9) and defines the baseline business rules, data models, and technical standards.

## Repository Structure

- `docs/` — Canonical project documentation (AI-readable Markdown source of truth, module handoffs, and sprint evidence)
- `frontend/` — Frontend React 19 application (Vite, Bootstrap 5, modular feature structure)
- `backend/` — Backend Spring Boot application (Java 21, Spring Data JPA, Flyway, MySQL 8.4)
- `database/` — Local Docker Compose environment, seed guidelines, and ER diagrams
- `tests/` — Cross-module, integration, system, end-to-end, and acceptance test resources
- `.github/` — GitHub Actions CI/CD workflows and PR templates

## Technology Baseline

- **Frontend**: React 19, Vite, Bootstrap 5 / Vanilla CSS
- **Backend**: Java 21, Spring Boot (Spring Web, Spring Security, Spring Data JPA)
- **Database Engine**: MySQL 8.4.10 LTS (Flyway migration framework)
- **Build Tooling**: Maven (with embedded wrapper) & npm

## Application Setup & Commands

### Prerequisites

- **Java**: JDK 21 or higher
- **Node.js**: v18+ or v20+ (with npm)
- **Container Engine**: Docker Desktop or Docker Engine with Docker Compose (for local database setup)
- **Database**: MySQL 8.4.10 LTS (or local Docker Compose instance)

### Local Database Setup (Docker Compose)

Start the local MySQL database:
```bash
docker compose --env-file database/.env up -d mysql
```

- **Connection URL**: `jdbc:mysql://localhost:3306/vaccination_portal`
- **Flyway Migrations**: Applied automatically on backend application startup.
- See [database/README.md](file:///e:/SE%20PROJECT_Vac/se2030-2026-y2-s1-mtr-01-vaccination-portal/database/README.md) for full database workflow instructions.

### Frontend Development

From `frontend/`:
```bash
npm install
npm run dev
npm run build
npm run lint
```

### Backend Development

From `backend/`:

#### Windows (PowerShell)
```powershell
# Run unit & integration test suites
.\mvnw.cmd test

# Run full verification including package builds
.\mvnw.cmd verify

# Run Spring Boot application locally
.\mvnw.cmd spring-boot:run
```

#### Unix-like (macOS / Linux)
```bash
# Run unit & integration test suites
./mvnw test

# Run full verification including package builds
./mvnw verify

# Run Spring Boot application locally
./mvnw spring-boot:run
```

### Backend Health Endpoint

When the backend application is running, check Actuator health:
```text
GET http://localhost:8080/actuator/health
```

### Environment Variables Setup

Both `frontend/` and `backend/` contain `.env.example` templates outlining required local environment variables (e.g. `VITE_API_BASE_URL`, `SERVER_PORT`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`). Do not commit real `.env` files.

## Continuous Integration & Quality Gates

GitHub Actions workflows automatically validate pull requests and merged commits targeting `develop` and `main`:

- **Frontend CI** (`.github/workflows/frontend-ci.yml`): Runs on Node 24 to execute `npm ci`, `npm run lint`, and `npm run build`.
- **Backend CI** (`.github/workflows/backend-ci.yml`): Runs on Java 21 Temurin to execute `./mvnw --batch-mode --no-transfer-progress verify`. Backend tests run against the in-memory H2 database test profile (`application-test.properties`) and do not require external infrastructure.

## Security and Privacy Guidelines

Contributors must strictly adhere to the following policies:

- Never commit passwords, API keys, tokens, or `.env` files.
- Never use real citizen information, medical records, or live vaccination data.
- All development and testing seed data must be 100% synthetic.
- Never commit production database exports.


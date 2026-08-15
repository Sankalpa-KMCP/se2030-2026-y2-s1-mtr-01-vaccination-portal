# Web-Based Vaccination Portal

## Project Overview

The Web-Based Vaccination Portal supports vaccination-program operations for citizens, healthcare personnel, administrators, and authorized external integrations. The exact scope of the supported operations is defined by the canonical project specification.

## Five Major Functions

1. Pre-Vaccination Screening and Eligibility Rule Management
2. Vaccination Campaign, Center and Session Management
3. Appointment, Waitlist and Check-In Management
4. Vaccine Inventory and Batch Management
5. Vaccination Administration, Draft Records and Digital Certificate Management

## Current Status

Frontend and backend application foundations initialized. Business modules (MF-01 through MF-05) are ready to be developed on feature branches.

## Canonical Documentation

- [AI Canonical Project Specification](docs/Web_Based_Vaccination_Portal_AI_Canonical_Context_v3.3.md)

The Markdown canonical document is the sole authoritative project source of truth (version 3.3-AI, revision 2026-08-14-R9) and defines the project baseline, business rules, data model, and technical decisions.

Contributors must review the canonical document before making functional or architectural changes.

## Repository Structure

- `docs/` — Canonical project documentation (AI-readable Markdown source of truth, module handoffs, and sprint evidence)
- `frontend/` — Frontend application source (React, Vite, Bootstrap 5)
- `backend/` — Backend application, REST APIs, and integrations (Java 21, Spring Boot, Maven, MySQL)
- `database/` — Database migrations, seed data, and design diagrams
- `tests/` — Cross-module, integration, system, end-to-end, security, and acceptance tests
- `.github/` — GitHub issue and pull-request templates

## Development Workflow

- Feature work must not be performed directly on `main`
- Work should use short-lived branches
- Pull requests are required for all changes
- Changes must be reviewed
- Tests and documentation should be updated when applicable
- Canonical project requirements must be followed

## Declared Technology Baseline

The canonical v3.3 project specification defines the following technology baseline:

- Frontend: React 19, Vite 8, and Bootstrap 5
- Backend: Java 21 with Spring Boot 4.1
- Database: MySQL 8.4.10 LTS (with Docker Compose support)
- Build tool: Maven (with embedded wrapper)

## Application Setup & Commands

### Prerequisites

- **Java**: JDK 21 or higher
- **Node.js**: v18+ or v20+ (with npm)
- **Container Engine**: Docker Desktop or Docker Engine with Docker Compose (for local database setup)
- **Database**: MySQL 8.4.10 LTS (required for backend production runtime execution)

### Local Database Development (Docker Compose)

The repository provides a Docker Compose environment for local database development. See [database/README.md](file:///e:/SE%20PROJECT_Vac/se2030-2026-y2-s1-mtr-01-vaccination-portal/database/README.md) for full database workflow instructions.

Start local MySQL database:
```bash
docker compose --env-file database/.env up -d mysql
```

Database connection: `jdbc:mysql://localhost:3306/vaccination_portal`
*Note: No business database schema migrations exist yet.*

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
Run tests:
```powershell
.\mvnw.cmd test
```

Verify build:
```powershell
.\mvnw.cmd verify
```

Run application (requires MySQL configured via environment variables):
```powershell
.\mvnw.cmd spring-boot:run
```

#### Unix-like (macOS / Linux)
Run tests:
```bash
./mvnw test
```

Verify build:
```bash
./mvnw verify
```

Run application:
```bash
./mvnw spring-boot:run
```

### Backend Health Endpoint

When the backend application is running, check Actuator health:
```text
GET http://localhost:8080/actuator/health
```

### Environment Variables Setup

Both `frontend/` and `backend/` contain `.env.example` templates outlining required local environment variables (e.g. `VITE_API_BASE_URL`, `SERVER_PORT`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`). Do not commit real `.env` files.

## Continuous Integration

GitHub Actions workflows automatically validate pull requests and merged commits targeting `develop` and `main`.

- **Frontend CI** (`.github/workflows/frontend-ci.yml`): Runs on Node 24 (`ubuntu-latest`) to execute `npm ci`, `npm run lint`, and `npm run build`.
- **Backend CI** (`.github/workflows/backend-ci.yml`): Runs on Java 21 Temurin (`ubuntu-latest`) to execute `./mvnw --batch-mode --no-transfer-progress verify`. Backend tests run against the in-memory H2 database test profile (`application-test.properties`) and do not require a live MySQL server.

*Note: Deployment triggers are not configured in CI. Automated business feature tests will be added incrementally alongside new features.*

## Security and Privacy

Contributors must never commit:

- Passwords
- API keys
- Access tokens
- `.env` files
- Production credentials
- Real citizen information
- Real medical information
- Real vaccination records
- Production database exports

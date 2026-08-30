# Database Configuration and Local Environment Guide

This directory contains database-related documentation, environment configuration, seed data guidelines, and design diagrams for the Web-Based Vaccination Portal.

## Declared Technology Baseline

- **Database Engine**: MySQL 8.4.10 LTS
- **Local Runtime Environment**: Docker Compose (recommended reproducible development workflow)
- **Port**: 3306 (default local mapping)
- **Database Name**: `vaccination_portal`
- **Migration Framework**: Flyway (managed through Spring Boot backend)

## Database Architecture & Flyway Migrations

Database schema versioning is managed via Flyway migrations located in [`backend/src/main/resources/db/migration/`](file:///e:/SE%20PROJECT_Vac/se2030-2026-y2-s1-mtr-01-vaccination-portal/backend/src/main/resources/db/migration/). Flyway automatically executes pending migrations upon Spring Boot application startup against the configured MySQL instance.

### Active Migration Schedule

| Version | Migration Script | Module / Domain Focus |
| :--- | :--- | :--- |
| `V0` | `V0__core_campaign_center_session_foundation.sql` | Core foundation: Campaigns, Centers, Sessions, Vaccines, Target Groups |
| `V1` | `V1__mf03_appointment_foundation.sql` | MF-03: Appointments & Booking Guard constraints |
| `V2` | `V2__mf02_operational_staff_identity.sql` | MF-02: Operational Staff Identity & User linking |
| `V3` | `V3__mf02_time_slot.sql` | MF-02: Time Slot Capacity & Interval definitions |
| `V4` | `V4__shared_authentication_foundation.sql` | Shared Auth: Users, Roles, Permissions, Auth tokens |
| `V5` | `V5__mf01_screening_foundation.sql` | MF-01: Screening Questionnaires, Rules, Assessments |
| `V6` | `V6__mf01_screening_corrections.sql` | MF-01: Screening schema corrections & constraints |
| `V7` | `V7__mf03_waitlist_foundation.sql` | MF-03: Waitlist queue & priority models |
| `V8` | `V8__mf01_screening_triggered_rule_evidence.sql` | MF-01: Evaluation rule evidence snapshots |
| `V9` | `V9__mf02_session_booking_suspension.sql` | MF-02: Session booking suspension audit flags |
| `V10` | `V10__mf02_campaign_session_supporting_tables.sql` | MF-02: Recurrence rules, Rosters, Closures, Location targets |
| `V11` | `V11__mf05_vaccination_foundation.sql` | MF-05: Vaccination administration, Drafts & Certificates |

## Directory Structure

- `migrations/` — Reference DDL exports and supplemental database scripts
- `seeds/` — Synthetic development, testing, and benchmark seed data
- `diagrams/` — Entity-Relationship (ER) diagrams and database design materials

## Local Database Setup

### 1. Environment Setup

Copy `.env.example` to create your local environment configuration file:

#### Windows (PowerShell)
```powershell
Copy-Item database/.env.example database/.env
```

#### Unix-like (macOS / Linux)
```bash
cp database/.env.example database/.env
```

*Important: Replace placeholder passwords in `database/.env` with your secure local values. Do not commit `database/.env` to version control.*

### 2. Management Commands

Execute the following commands from the repository root:

#### Start MySQL Container
```bash
docker compose --env-file database/.env up -d mysql
```

#### Check Container Health Status
```bash
docker compose ps
```

#### Stop Container (Preserve Local Data)
```bash
docker compose --env-file database/.env down
```

#### Stop Container and Wipe Volume (Full Reset)
```bash
docker compose --env-file database/.env down -v
```

> [!WARNING]
> Running `down -v` permanently removes the project's local MySQL Docker volume (`mysql-data`) and all local data stored inside it.

## Backend Connection Details

- **JDBC URL**: `jdbc:mysql://localhost:3306/vaccination_portal`
- **Default Application User**: Configured via `MYSQL_USER` in `database/.env` (matches `DB_USERNAME` in backend configuration)
- **Password**: Configured via `MYSQL_PASSWORD` in `database/.env` (matches `DB_PASSWORD` in backend configuration)

Ensure your backend application configuration (`backend/src/main/resources/application.properties` or environment variables) matches your local `database/.env` settings.

## Security & Privacy Guidelines

Contributors must strictly adhere to the following policies:

- **Never commit `database/.env`** or any file containing real passwords or credentials.
- **Never use real citizen information**, medical records, or actual vaccination data.
- **Future seed data must be 100% synthetic.**
- **Never commit production database dumps** or exports to this repository.


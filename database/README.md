# Database Configuration and Local Environment Guide

This directory contains database-related documentation, environment configuration, seed data guidelines, and design diagrams for the Web-Based Vaccination Portal.

## Architecture ownership (current)

See the [current architecture and file tree](../docs/Web_Based_Vaccination_Portal_Recommended_Full_File_Tree.md) for capability packages, provider-owned APIs, transaction invariants and enforced import boundaries. Historical handoffs do not define the current package tree.

## Declared Technology Baseline

- **Database Engine**: MySQL 8.4 LTS
- **Local Runtime Environment**: Docker Compose (recommended reproducible development workflow)
- **Port**: 3306 (default local mapping)
- **Database Name**: `vaccination_portal`
- **Migration Framework**: Flyway (managed through Spring Boot backend)

## Database Architecture & Flyway Migrations

Database schema versioning is managed via Flyway migrations located in [`../backend/src/main/resources/db/migration/`](../backend/src/main/resources/db/migration/). Flyway automatically executes pending migrations upon Spring Boot application startup against the configured MySQL instance. Current active migrations span `V0` through `V27` (28 active migrations).

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
| `V12` | `V12__shared_auth_registration_foundation.sql` | Shared Auth: Citizen self-registration & staff registration request audit |
| `V13` | `V13__mf02_time_slot_integrity_constraints.sql` | MF-02: Time slot integrity constraints |
| `V14` | `V14__citizen_profile_foundation.sql` | Citizen Profile: Demographic and contact profile foundation |
| `V15` | `V15__mf04_inventory_foundation.sql` | MF-04: Vaccine batches, manufacturers, center stock & transactions |
| `V16` | `V16__add_super_admin_role.sql` | Shared Auth: Super Admin role definition |
| `V17` | `V17__auth_verification_and_password_recovery.sql` | Shared Auth: Verification & password recovery tokens |
| `V18` | `V18__mf05_correction_certificate_lifecycle.sql` | MF-05: Certificate lifecycle, revocation audit & corrections |
| `V19` | `V19__add_vaccinator_auth_role.sql` | Shared Auth: Dedicated Vaccinator role definition |
| `V20` | `V20__vaccination_draft_deletion_audit.sql` | MF-05: Vaccination draft deletion audit ledger |
| `V21` | `V21__screening_submission_active_uniqueness.sql` | MF-01: Screening submission active uniqueness |
| `V22` | `V22__add_email_verification.sql` | Shared Auth: Citizen OTP challenge and active verification backfill |
| `V23` | `V23__user_account_login_lockout.sql` | Shared Auth: Failed-login counters and timed lockout state |
| `V24` | `V24__add_refresh_token_sessions_family_revocation_index.sql` | Shared Auth: Refresh-family revocation index (token_family_id, revoked_at) |
| `V25` | `V25__mf03_waitlist_session_uniqueness.sql` | MF-03: Session-level waitlist uniqueness constraint |
| `V26` | `V26__mf03_history_and_event_consumption.sql` | MF-03: Appointment history, event consumption & notification outbox |
| `V27` | `V27__mf05_correction_request_type.sql` | MF-05: Vaccination correction request type categorization |

## Directory Structure

- `migrations/` - Historical reference scripts only: `V1__mf03_appointment_foundation.sql`, `V2__mf03_waitlist_foundation.sql`, `V002__mf01_screening_foundation.sql`. These versions overlap the active history and are not runtime inputs. Do not add this directory to Flyway locations or run it alongside the active scripts.
- `seeds/` - Synthetic development, testing, and benchmark seed data
- Database design diagrams are maintained under `../docs/diagrams/current/` (including `physical-er-model.md`).

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


## Current evidence and refresh persistence

V4 created refresh_token_sessions; V24 adds its family-revocation index. Only cryptographic token hashes are stored. Rotation claims/successor creation are transactional; reuse security writes commit despite outward authentication rejection. Password reset revokes all account sessions. See [physical ER excerpt](../docs/diagrams/current/physical-er-model.md) and [shared evidence](../docs/evidence/current/shared-authentication.md). Local Compose MySQL 8.4.10 and CI refresh-test MySQL 8.4.3 are distinct versioned environments.

# MF-02 — Vaccination Campaign, Center and Session Management
## AI Engineering Handoff

**Project:** Web-Based Vaccination Portal (`WBVP-SE2030`)
**Function:** MF-02 — Vaccination Campaign, Center and Session Management
**Canonical authority:** `3.3-AI / 2026-08-14-R9`
**Canonical source model:** Markdown-only; the canonical Markdown file is the sole normative semantic source.
**Sprint:** Sprint 1 — **IN PROGRESS**
**MF-02 Planned Implementation:** **COMPLETE**
**MF-02 Implementation Milestones:** **COMPLETE / MERGED / FROZEN**
**MF-02 Final Audit:** **MF02_FINAL_AUDIT_PASS_SUBMISSION_READY**
**US-13:** **IN PROGRESS**
**Last synchronized implementation checkpoint:** PR #145
**Reviewed PR #145 commits:** `5a1d1b81733c82e73aa47def9c458f2403b26afa` (`feat(mf02): add session readiness diagnostics UI`), `0db2eb2bb82c99eb8d493d2cb14073150d096ba8` (`fix(mf02): invalidate stale readiness after failed refresh`)
**PR #145 candidate development base:** `99e03a72c24790e2ab148e740a92b56834d10785`
**PR #145 merge:** `122547a4a0391bc43be835300635ec97aecfc430`
**Preceding F6 readiness query prerequisite:** PR #144 (`7eab0d290c5cd0c6fdbfa32dd9bb342f4af7b756` / merge `99e03a72c24790e2ab148e740a92b56834d10785`)
**Preceding F4D session recurrence management UI:** PR #141 (`c2b5147`, `f85ef46` / merge `d9e923f`)
**Preceding F4D trusted actor fix:** PR #140 (`69466a1` / merge `fdd6699`)
**Preceding F4C session change control UI:** PR #139 (`8b43a7e`, `43305bb` / merge `165a553`)
**Preceding F4C trusted actor fix:** PR #138 (`49c7eee` / merge `a1c385a`)
**Preceding F4B simple session lifecycle UI:** PR #137 (`97fc3d2`)
**Preceding slot capacity integration milestone:** PR #117 (`b248541bd5efeedbfbb6a3708a4b4ef60b509156` / merge `c89cdcb610fd4690ee98e11f13955c8b9ef68966`)
**Current verified backend baseline:**
- **Full backend test suite:** **1280 backend tests** across all modules (0 failures, 0 errors, 0 skipped, `BUILD SUCCESS`).
- **Representative targeted MF-02 test suite:** **446 tests across 31 classes** (0 failures, 0 errors, 0 skipped).
- **F6 targeted regression suite:** **117 / 117 tests PASS** across 8 classes.
- **Authoritative MySQL assurance:** **24 MySQL/Testcontainers assurance tests** across 10 classes on `mysql:8.4.3` (0 failures, 0 errors, 0 skipped).
- **Frontend assurance:** ESLint PASS (0 errors, 0 warnings); Vite production build PASS (548.78 kB JS / 135.45 kB gzip, 230.37 kB CSS / 30.88 kB gzip; build time ~217ms).
- **Frontend automated test runner:** `NOT_AVAILABLE_NO_EXISTING_RUNNER` (no runner configured in `package.json`; non-blocking technical debt).
- **Thread.sleep in tests:** 0 occurrences across `backend/src/test`.

> This file is a maintained engineering snapshot. Canonical Markdown controls semantics; live Git/code/tests control implementation truth. This handoff must not be used to override canonical rules.

## 1. Authority Order

Use this order when facts conflict:

1. `docs/Web_Based_Vaccination_Portal_AI_Canonical_Context_v3.3.md` — semantic/business source of truth.
2. Live `origin/develop`, merged PRs, production code, tests, and CI — implementation truth.
3. `docs/MF02_Vaccination_Campaign_Center_and_Session_Management_AI_Handoff.md` — current MF-02 engineering state.
4. `docs/evidence/sprint-1/MF02_Sprint1_Evidence.md` — implementation/verification chronology.
5. `ai/PROJECT_CONTEXT.md` — orientation summary only (do not edit).

Future agents must fetch live `origin/develop` before treating `122547a4a0391bc43be835300635ec97aecfc430` as the repository tip.

## 2. Current Milestone

### Milestone reached

**MF-02 PLANNED IMPLEMENTATION (F4A, F4B, F4C, F4D, F5A, F5B, F6 READINESS QUERY, F6 FRONTEND) IS COMPLETE, MERGED, AND FROZEN. FINAL INDEPENDENT AUDIT VERDICT: MF02_FINAL_AUDIT_PASS_SUBMISSION_READY.**

All planned implementation milestones for MF-02 are integrated and frozen:
1. **F4A — Campaign and Center Configuration UI / Core REST:** COMPLETE / MERGED / FROZEN
2. **F4B — Session Core and Lifecycle UI (PR #137):** COMPLETE / MERGED / FROZEN
3. **F4C — Session Change Control UI (PR #139, PR #138):** COMPLETE / MERGED / FROZEN
4. **F4D — Session Recurrence and Series Management UI (PR #141, PR #140):** COMPLETE / MERGED / FROZEN
5. **F5A — Session Operational Assignments UI / REST:** COMPLETE / MERGED / FROZEN
6. **F5B — Session Capacity Planning UI / REST:** COMPLETE / MERGED / FROZEN
7. **F6 Readiness Query Prerequisite (PR #144):** COMPLETE / MERGED / FROZEN
8. **F6 Session Readiness and Diagnostics Frontend (PR #145):** COMPLETE / MERGED / FROZEN

Total MF-02 REST routes integrated: **79** ($21 \text{ Campaign-family [9 Core + 12 Configuration]} + 22 \text{ Center-family [14 Core + 8 Operational Resource/Staff Roster]} + 27 \text{ Session-family [6 Core + 7 Change + 8 Operational Assignments + 5 Capacity Plans + 1 Readiness Query]} + 9 \text{ Recurrence/Series} = 79$).

### Current verified integrations

1. **PR #145 — `feat(mf02): add session readiness diagnostics UI`**
   - reviewed candidate commits:
     - initial candidate: `5a1d1b81733c82e73aa47def9c458f2403b26afa` (`feat(mf02): add session readiness diagnostics UI`)
     - remediation commit: `0db2eb2bb82c99eb8d493d2cb14073150d096ba8` (`fix(mf02): invalidate stale readiness after failed refresh`)
   - candidate development base: `99e03a72c24790e2ab148e740a92b56834d10785`
   - merge: `122547a4a0391bc43be835300635ec97aecfc430`
   - merged at: `2026-08-25T21:40:00Z`
   - merge method: normal merge commit
   - Backend CI: PASS (1280 backend tests + 24 Testcontainers MySQL tests on `mysql:8.4.3`, 0 failures, 0 errors, 0 skipped, `BUILD SUCCESS` in 3m04s; Run 32929442329)
   - Frontend CI: PASS (11s; Run 32929442459)
   - scope: 3 files (`sessionApi.js`, `SessionDetailsModal.jsx`, `SessionReadinessTab.jsx`)
   - behavior: Implements the final F6 session readiness and diagnostics tab in `SessionDetailsModal.jsx`. Consumes `GET /api/v1/sessions/{id}/readiness` via centralized `getSessionReadiness(sessionId)`. Displays point-in-time readiness status banner (READY / NOT READY), blocking violations list in backend order, 6-cell operational metrics grid, and generic corrective navigation. Enforces synchronous request mutex (`activeRequestRef`) and monotonic token sequencing (`requestSeqRef`) to prevent duplicate dispatches and race conditions. Invalidates prior snapshot state upon failed refresh to ensure error alerts and stale snapshots are strictly mutually exclusive.

2. **PR #144 — `feat(mf02): expose session readiness query`**
   - reviewed source: `7eab0d290c5cd0c6fdbfa32dd9bb342f4af7b756`
   - candidate development base: `328670bdd9b9112b1133eb09a74d0af42aef5e1b`
   - merge: `99e03a72c24790e2ab148e740a92b56834d10785`
   - behavior: Exposes existing side-effect-free evaluator `SessionOperationalReadinessService.evaluateReadiness(Long sessionId)` as `GET /api/v1/sessions/{id}/readiness`. Returns structured `SessionReadinessResponse` with `ready`, `violations`, `assignedVaccinatorCount`, `availableStationCount`, `approvedCapacity`, `durationMinutes`, `expectedServiceMinutes`, `revision`, and `evaluatedAt`. Preserves `POST /api/v1/sessions/{id}/schedule` as the sole transactional, locking-aware state transition.

3. **PR #141 — `feat(mf02): add session recurrence management UI (F4D)`**
   - reviewed commits: `c2b5147` (`feat(mf02): add session recurrence management UI`), `f85ef46` (`fix(mf02): harden recurrence management state handling`)
   - merge: `d9e923f`
   - behavior: Implements `SessionRecurrencePanel.jsx` supporting rule creation, 366-day bounded occurrence generation, series schedule preview/confirm, series cancellation preview/confirm, and occurrence drill-down.

4. **PR #140 — `fix(mf02): derive series change actor from authentication`**
   - reviewed source: `69466a1`
   - merge: `fdd6699`
   - behavior: Enforces server-side derivation of `actorId` from authenticated `UserPrincipal.getUserId()` for `POST /api/v1/recurrence-rules/{id}/change` and `POST /api/v1/recurrence-rules/{id}/cancel`, ignoring any client-provided actor identity.

5. **PR #139 — `feat(mf02): add session change control UI (F4C)`**
   - reviewed commits: `8b43a7e` (`feat(mf02): add session change control UI`), `43305bb` (`fix(mf02): harden session transfer metadata handling`)
   - merge: `165a553`
   - behavior: Implements `SessionChangeControlTab.jsx` with tabbed preview-confirm workflows for reschedule, center transfer, cancellation, and historical change event audit inspection.

6. **PR #138 — `fix(mf02): derive session change actor from authentication`**
   - reviewed source: `49c7eee`
   - merge: `a1c385a`
   - behavior: Enforces server-side derivation of `actorId` from authenticated `UserPrincipal.getUserId()` for `POST /api/v1/sessions/{id}/reschedule`, `transfer`, and `cancel`, ignoring any client-provided actor identity.

7. **PR #137 — `feat(mf02): add simple session lifecycle UI (F4B)`**
   - merge: `97fc3d2`
   - behavior: Implements `SessionLifecycleActionCard` in `SessionDetailsModal.jsx` supporting lifecycle actions (schedule, start, complete).

8. **PR #117 — `feat(mf02): integrate slot capacity with booking port`**
   - merge: `c89cdcb610fd4690ee98e11f13955c8b9ef68966`
   - behavior: Implements internal `TimeSlot` persistence, `TimeSlotCapacityService`, and `SlotCapacityPort` cross-module integration contract.

## 3. Implemented MF-02 Scope

### Campaign management & configuration
- `VaccineType` and `TargetGroup` (seeded reference data)
- `VaccinationCampaign` CRUD and full status lifecycle (`DRAFT`, `SCHEDULED`, `PUBLISHED`, `ACTIVE`, `SUSPENDED`, `CLOSED`)
- `CampaignDoseProgramme` versioned programmes
- `CampaignLocationTarget` administrative targeting
- `CampaignQuestionnaireAssignment` consuming `QuestionnaireVersionPublicationPort` from MF-01
- Append-only `CampaignLifecycleEvent` audit trail with server-derived actor identity

### Center planning & operational resources
- `VaccinationCenter` CRUD with explicit timezone normalization
- `OperatingHours` and `CenterClosure` reverse-impact validation
- `CampaignCenterAssignment` with nested campaign ownership checks
- `OperationalResource` (physical stations with statuses `ACTIVE`, `MAINTENANCE`, `INACTIVE`)
- `OperationalStaffRoster` (qualified vaccinators, supervisors, check-in staff) with identity locking

### Session core, lifecycle, and readiness (F4B, F6)
- One-off session creation with operating hours and center containment validation
- Full session lifecycle (`DRAFT`, `SCHEDULED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`)
- Early-start prevention (`Clock`-enforced start validation)
- Readiness diagnostics endpoint `GET /api/v1/sessions/{id}/readiness` (side-effect-free point-in-time snapshot)
- Transactional schedule enforcement `POST /api/v1/sessions/{id}/schedule` with pessimistic write locking

### Session change control & recurrence (F4C, F4D)
- Two-phase preview-confirm workflows with SHA-256 HMAC tokens for reschedule, center transfer, and cancellation
- 366-day safety horizon for recurrence rules and occurrence materialization
- Series schedule change and series cancellation with atomic occurrence updates
- Append-only `SessionChangeEvent` recording old/new schedule snapshots, reason, actor ID, and affected counts
- Trusted actor extraction from authenticated JWT principal (`UserPrincipal.getUserId()`)

### Operational assignments & capacity planning (F5A, F5B)
- DRAFT-only session station and staff assignments with interval containment and collision checks
- Session capacity planning calculation engine: duration, throughput, safe concurrent occupancy, service ceiling
- Immutable `SessionCapacityPlan` revision history (`revision` 1, 2, 3...)
- Authorization for capacity override: `ROLE_CENTER_MANAGER` is authorized; `ROLE_ADMIN` is denied (ADMIN is restricted to administrative functions, not clinical/operational capacity overrides).

## 4. Security and Shared Infrastructure

### Authentication & Authorization
- **Transport:** Stateless JWT authentication via `JwtAuthenticationFilter` and `JwtTokenService` using 256-bit secret signing.
- **Frontend Injection:** Centralized Bearer JWT injection via `apiClient.js` reading token from `authStorage.js` (not cookie-based).
- **Trusted Actor Identity:** Server derives actor ID from `@AuthenticationPrincipal UserPrincipal principal.getUserId()` across all mutation confirmation endpoints.
- **Capacity Override Authorization:** Enforced via `AuthenticatedActor.hasRole("ROLE_CENTER_MANAGER")`. `ROLE_ADMIN` is denied capacity overrides.

### Audit Logging vs Technical Logging
- **Business Audit Logging:** Fully implemented via first-class relational entities (`SessionChangeEvent`, `CampaignLifecycleEvent`, and revision-based `SessionCapacityPlan`). Records actor, reason, timestamps, and state snapshots. Fully queryable via REST endpoints.
- **Technical Application Logging:** Standard application logging via SLF4J / Logback. Centralized log aggregation (e.g. ELK/Loki) is classified as an optional production hardening follow-on.

### Monitoring & Actuator
- **Basic Health Endpoint:** Implemented at `/actuator/health` via Spring Boot Actuator.
- **Centralized Metrics & Tracing:** Optional production hardening follow-on (no Prometheus/Grafana or distributed tracing configured).

### Database Migrations
- **SQL Scripts:** `V1__mf03_appointment_foundation.sql`, `V2__mf02_operational_staff_identity.sql`, `V3__mf02_time_slot.sql`, `V4__shared_authentication_foundation.sql` located under `backend/src/main/resources/db/migration`.
- **Runtime Runner:** Flyway runtime dependency is not configured in `pom.xml`. Test and dev environments utilize Hibernate DDL generation (`ddl-auto=create-drop`). Fresh production bootstrap solely from migrations is unproven (retained as non-blocking technical debt).

## 5. Cross-Module Boundaries

- **`QuestionnaireVersionPublicationPort`:** Defined in MF-02, implemented by MF-01 `QuestionnaireVersionService`. Consumed by MF-02 to revalidate published questionnaire status.
- **`SessionImpactReader`:** Defined in MF-02 to read affected appointment and waitlist counts. Provider belongs to MF-03. When external provider is unmounted, DRAFT modifications proceed with zero impact while SCHEDULED modifications fail closed safely (`EXTERNAL MF03 DEPENDENCY`).
- **`SlotCapacityPort`:** Cross-module booking contract provided by MF-02 (`TimeSlotCapacityService`) for consumption by MF-03 appointment booking.

## 6. Current Test / Verification State

### Final Verification / Current Baseline (develop @ `122547a4a0391bc43be835300635ec97aecfc430`)
- **Full Backend Test Suite:** **1280 / 1280 PASS** (0 failures, 0 errors, 0 skipped).
- **Representative Targeted MF-02 Suite:** **446 tests across 31 classes PASS** (0 failures, 0 errors, 0 skipped).
- **F6 Targeted Regression Suite:** **117 / 117 PASS** across 8 classes.
- **MySQL/Testcontainers Concurrency Suite:** **24 / 24 PASS** across 10 classes on `mysql:8.4.3`.
- **Frontend Assurance:** ESLint clean (0 errors, 0 warnings); Vite production build PASS (548.78 kB JS / 135.45 kB gzip, 230.37 kB CSS / 30.88 kB gzip; build time ~217ms).
- **Frontend Automated Test Runner:** `NOT_AVAILABLE_NO_EXISTING_RUNNER` (non-blocking technical debt).

### Historical Checkpoints Summary
- PR #71: **591**
- PR #80: **616**
- PR #84: **645 H2 + 3 MySQL** (648)
- PR #86: **680 H2 + 3 MySQL** (683)
- PR #88: **724 H2 + 3 MySQL** (727)
- PR #90: **763 H2 + 3 MySQL** (766)
- PR #92: **790 H2 + 3 MySQL** (793)
- PR #97: **799 H2 + 4 MySQL** (803)
- PR #100: **926 H2 + 4 MySQL** (930)
- PR #104: **953 H2 + 4 MySQL** (957)
- PR #106: **998 H2 + 4 MySQL** (1002)
- PR #108: **1041 H2 + 4 MySQL** (1045)
- PR #110: **1080 H2 + 6 MySQL** (1086)
- PR #112: **1103 H2 + 7 MySQL** (1110)
- PR #114: **1123 H2 + 15 MySQL** (1138)
- PR #117: **1160 H2 + 23 MySQL** (1183)
- PR #144 / PR #145 Final Baseline: **1280 Full Backend + 24 MySQL**

## 7. Known Maintainability & Technical Debt

1. **Frontend Large Components (Maintainability Debt):**
   - `SessionRecurrencePanel.jsx`: ~1,670 LOC (MEDIUM complexity; multi-tab recurrence management)
   - `SessionChangeControlTab.jsx`: ~1,458 LOC (MEDIUM complexity; two-phase change control workflows)
   - `SessionCapacityPlanTab.jsx`: ~848 LOC (LOW complexity; capacity calculations and revision history)
2. **Database Migration Runner (Deployment Debt):** Flyway runtime execution is not configured in `pom.xml`, relying on Hibernate DDL generation in development/test profiles.
3. **Frontend Automated Test Runner (Testing Debt):** Vitest / Jest runner is not configured in `package.json`; frontend quality is guarded via ESLint, Vite production builds, and full backend API integration tests.

## 8. Status Discipline

Never claim:
- Whole project complete (MF-03, MF-04, MF-05 remain);
- Sprint 1 complete;
- US-13 complete;
- Production Flyway runtime auto-migration complete without configuration in `pom.xml`;
- Centralized ELK/Loki logging or Prometheus monitoring complete when only basic Actuator health is present.

Current status:
- Sprint 1: **IN PROGRESS**
- MF-02 Planned Implementation: **COMPLETE**
- MF-02 Implementation Milestones: **COMPLETE / MERGED / FROZEN**
- MF-02 Final Audit Verdict: **MF02_FINAL_AUDIT_PASS_SUBMISSION_READY**
- US-13: **IN PROGRESS**
- Backend Verified Baseline: **1280 backend tests + 24 Testcontainers MySQL assurance tests**
- Frontend Verified Baseline: **0 ESLint errors, Vite production build PASS**

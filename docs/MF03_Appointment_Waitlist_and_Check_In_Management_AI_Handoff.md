# MF-03 AI Development Handoff and Scrum Roadmap

> Historical specification/evidence: package trees, implementation status and migration counts below describe their original revision. Current ownership and setup are recorded in the [current architecture](Web_Based_Vaccination_Portal_Recommended_Full_File_Tree.md). Applied runtime Flyway history is V0–V21 under `backend/src/main/resources/db/migration`; reference scripts are not runtime migrations. Business requirements remain reference material.

## Web-Based Vaccination Portal

**Function:** Appointment, Waitlist and Check-In Management  
**Canonical project:** `WBVP-SE2030`  
**Canonical version:** `3.3-AI`  
**Canonical revision:** `2026-08-08-R8`  
**Repository:** `https://github.com/Sankalpa-KMCP/se2030-2026-y2-s1-mtr-01-vaccination-portal.git`

> **Repository metadata note:** The canonical document originally leaves the repository URL as `USER_DATA_REQUIRED`; the URL above is the repository value supplied and verified during the team's actual project setup.

> **Purpose:** Give this file to an AI coding assistant so it can understand the function's scope, ownership, dependencies, Scrum timing, likely code structure, current shared repository baseline, and the safest next implementation step.
>
> **Source authority:** This handoff is derived from the canonical v3.3 project baseline. It is not allowed to override the canonical source. If anything here conflicts with `docs/Web_Based_Vaccination_Portal_AI_Canonical_Context_v3.3.md`, the canonical Markdown controls.
>
> **Implementation-state rule:** Never infer that planned functionality is already implemented. Inspect the repository. Anything not supported by code/commits/tests is `NOT_VERIFIED`.

---

# 1. AI Startup Contract

Before changing code, the AI must:

1. Read this handoff.
2. Read `docs/Web_Based_Vaccination_Portal_AI_Canonical_Context_v3.3.md`.
3. Verify repository root, remote, branch, and working-tree status.
4. Run `git fetch origin` without pruning or rewriting history.
5. Compare the working branch with `origin/develop`.
6. Inspect the actual existing module implementation.
7. Inspect relevant shared contracts and dependent modules.
8. Identify the next incomplete logical slice.
9. Make only reviewable changes for that slice.
10. Run relevant tests before proposing a commit.

Recommended initial commands:

```bash
git rev-parse --show-toplevel
git remote -v
git branch --show-current
git status --short
git fetch origin
git rev-parse HEAD
git rev-parse origin/develop
```

Do not automatically reset, rebase, force-push, or discard local work.

---

# 2. Project Architecture and Technical Baseline

The canonical architecture is a **layered modular monolith**:

```text
React SPA
   ↓ REST/JSON under /api/v1
Java 21 Spring Boot application
   ↓ Spring Data JPA
MySQL
```

Canonical technology baseline:

```text
Frontend: React + Vite + Bootstrap 5
Backend: Java 21 + Spring Boot + Spring Web + Spring Security
Persistence: Spring Data JPA + Bean Validation
Database: MySQL + Flyway migrations
Build: Maven
API: versioned REST/JSON under /api/v1 + OpenAPI
Version control: Git + GitHub
```

The actual shared foundation last verified in project work included:

```text
Node.js 24 LTS
React 19.2.8
React DOM 19.2.8
Vite 8.1.5
@vitejs/plugin-react 6.0.5
Bootstrap 5.3.8

Java 21
Spring Boot 4.1.0
Maven wrapper
MySQL 8.4.10 LTS development environment
H2 for current automated backend foundation tests
Frontend CI
Backend CI
```

Important implementation gaps/differences to verify instead of assuming:

- Canonical v3.3 requires Spring Security/RBAC, but authentication/security implementation is **not verified complete** by this handoff.
- Canonical v3.3 requires Flyway migrations, but business migration tooling is **not verified complete** by this handoff.
- Canonical v3.3 defines OpenAPI documentation, but OpenAPI implementation is **not verified complete** by this handoff.
- Canonical v3.3 names the API root `/api/v1`; any existing foundation configuration using `/api` must be treated as a shared contract mismatch to review before adding feature endpoints.
- Canonical testing mentions MySQL/Testcontainers and frontend Vitest/React Testing Library; current repository support must be inspected before claiming they exist.

Do not silently change shared architecture from a feature branch. Shared baseline mismatches should be coordinated and reviewed.

---


# 2A. Canonical Cross-Cutting Decisions

These are canonical v3.3 baseline decisions that affect every function:

- **Authentication:** Spring Security with role-based authorization. The canonical token model is a JWT access token plus a Secure HttpOnly refresh-token cookie. Every protected endpoint requires server-side authorization; hiding UI controls is not sufficient.
- **API contract:** Versioned REST/JSON under `/api/v1`, with OpenAPI documentation.
- **Database evolution:** MySQL with Flyway migrations. Business schema changes must be version-controlled; do not rely on Hibernate `create`, `create-drop` or `update` as the committed schema-management strategy.
- **Conditional uniqueness/concurrency:** Canonical design uses MySQL generated active-key columns plus ordinary unique indexes where conditional uniqueness is needed, together with explicit locking/guard-row strategies for critical citizen-campaign concurrency.
- **Event delivery:** Cross-module/event notification delivery uses a transactional outbox with idempotent consumers and retryable dispatch. Do not invent silent cross-module record rewriting.
- **Audit:** Material administrative transitions, overrides and corrections require actor, reason and timestamp where the canonical rules specify them.
- **Deletion:** Hard delete is limited to unused MF-01 configuration and unfinalized MF-05 drafts. Operational history such as appointments, eligibility decisions, stock transactions, finalized vaccination records, certificate issues and schedule-change history is retained.
- **Notifications:** In-app and email notifications are baseline. SMS is outside the core baseline.
- **Public verification/privacy:** Public certificate verification exposes only approved minimum data. Sensitive profile, medical/screening answers and notes must not be exposed unnecessarily.
- **Testing target:** Canonical testing baseline includes JUnit 5, Mockito, Spring Boot integration tests, Testcontainers/MySQL, Vitest and React Testing Library. The live repository must be inspected before assuming all of these are already configured.
- **Deployment target:** Canonical demo deployment is Docker Compose for frontend, backend and MySQL with secrets supplied through environment variables. The current repository may have only part of that deployment configuration; verify rather than assume.

The shared repository foundation may temporarily lag these canonical requirements. A feature AI must report a shared-baseline gap instead of silently implementing an incompatible private substitute inside one feature module.

---

# 3. Shared Repository State Last Verified

Last known verified shared state from the project workflow:

```text
origin/develop:
5984c17c01fac6d6f6119572b1666e234f94e92e

Frontend CI:
GREEN

Backend CI:
GREEN

Shared application foundation:
MERGED

MySQL local-development foundation:
MERGED

Cross-platform npm lockfile fix:
MERGED
```

This is historical handoff context, **not a permanent guarantee**. Always re-check GitHub and `origin/develop`.

The team branching model is:

```text
main
└── develop
    ├── feature/mf01-...
    ├── feature/mf02-...
    ├── feature/mf03-...
    ├── feature/mf04-...
    └── feature/mf05-...
```

Feature work goes to `develop` through pull requests. `main` is the stable integration/release branch.

---

# 3A. Assessment Alignment Warning

The canonical v3.3 baseline explicitly notes that the supplied assignment specification describes six major functions and six-member groups, while this project baseline uses five combined functions and five members. The canonical document does **not** claim lecturer approval. The team must retain written approval/evidence or be prepared to explain the mapping during assessment.

---

# 4. Scrum Baseline

> **Correction (2026-08-12):** An earlier draft of this handoff described a six-sprint (Sprint 0–5, two-week) canonical baseline. That model does not match the actual Lab 02 assessment submission (`2026-Y2-S1-MTR-01_-_Lab_02.pdf`), which is scored against **four three-week sprints**. The canonical context doc itself confirms the four-sprint windows are the Lab-aligned baseline and the finer-grained sprint breakdown is an internal team planning refinement, not a separate mandated schedule. The table below replaces the six-sprint version so this handoff no longer conflicts with the graded Lab 02 Sprint Plan.

The **assessed** project baseline (Lab 02) uses **four three-week sprints**:

| Sprint | Weeks | Team focus | Integrated demonstration |
|---|---|---|---|
| Sprint 1 | 1–3 | Foundation & Campaign Setup: shared auth/registration (US-01), MF-02 campaign lifecycle/questionnaire link/dose programme/areas/center assignment (US-11–16) | Published questionnaire linked to a valid campaign; center availability foundation in place |
| Sprint 2 | 4–6 | Screening & Eligibility (US-02, 06–08, 10), MF-02 session slots/staff/capacity (US-17–19), MF-04 batch registration (US-21) | Reviewed eligible citizen; validated session availability; batch foundation ready |
| Sprint 3 | 7–9 | **MF-03: appointment booking (US-03), waitlist (US-05), checked-in queue (US-26)**; MF-04 stock transfer/transactions/quarantine (US-22, 23, 25); MF-05 draft create/edit/delete (US-27, 28) | Citizen books and is checked in; vaccinator sees the queue and can hold a draft |
| Sprint 4 | 10–12 | MF-05 finalization/certificate (US-04, 29, 30), eligibility revocation (US-09), MF-02 reschedule/cancel impact preview (US-20), MF-04 alerts (US-24) | Complete citizen-to-certificate vertical slice |

MF-03's implementation work is concentrated in **Sprint 3**, but the domain model, status enums and API contracts can and should be prepared during Sprints 1–2 once MF-01/MF-02 contracts stabilize (see Section 13, Sprint 0–2 rows), so Sprint 3 is integration rather than a cold start.

Scrum working rules:

- Maintain one prioritized Product Backlog.
- Plan one Sprint Backlog at a time.
- All five members are Developers.
- Require at least one peer review before merge.
- Integrate continuously rather than building five isolated modules.
- Record decisions and blockers in lightweight notes.
- Keep API contracts, enum values, and migrations under version control.
- A test owner other than the primary implementer is preferred where practical.

Canonical role defaults:

```text
Product Owner representative: Member 1 / MF-01 owner
Scrum Master + integration coordinator: Member 2 / MF-02 owner
Developers: all five members
```

Names/student IDs remain external team data and must not be invented.

---

# 5. Definition of Done

A feature is done only when applicable requirements are satisfied and:

- requirement/acceptance criteria are mapped;
- API, UI, persistence and migrations are integrated where required;
- role and object-level authorization are implemented;
- positive/negative tests pass;
- concurrency tests pass where applicable;
- no known critical defect remains;
- code is reviewed and merged;
- OpenAPI/project documentation is updated where applicable;
- screenshots, commit, PR and test evidence are retained;
- the assigned member can explain the implementation.


# 6. Function Ownership

**Canonical owner slot:** Member 3  
**Secondary responsibility:** Frontend integration and concurrency tests

**Primary backend module:**

```text
backend/src/main/java/com/se2030/vaccinationportal/appointment/
```

**Primary frontend module:**

```text
frontend/src/features/appointment-management/
```

**Primary backend test module:**

```text
backend/src/test/java/com/se2030/vaccinationportal/appointment/
```

**Recommended/actual feature branch:**

```text
feature/mf03-appointment-waitlist-checkin
```

**Branch state note:** Recommended branch name; existence and synchronization are NOT_VERIFIED.

The AI must not create the branch blindly if it already exists. Verify local and remote branches first.

---

# 6A. Canonical Traceability

The canonical Appendix B maps this function to:

```text
Requirements: FR-10 to FR-13
Use cases:    UC-11 to UC-14
Tests:        TC-12 to TC-15
```

Use these IDs in Product Backlog items, acceptance criteria, test/evidence notes and final report traceability where applicable. Do not invent additional canonical IDs.

**Lab 02 assessment backlog mapping (SE2030, Group 2026-Y2-S1-MTR-01):**

```text
US-03  Citizen: search for and book an available appointment slot        (High, Sprint 3, Member 3)
US-05  Citizen: join a waitlist when a session is full                   (Medium, Sprint 3, Member 3)
US-26  Vaccinator: view list of checked-in and eligible appointments     (Medium, Sprint 3, Member 3 / Member 5)
US-27  Vaccinator: create/edit unfinalized vaccination draft             (High, Sprint 3, Member 5 — MF-05, MF-03 supplies CHECKED_IN state)
US-28  Vaccinator: delete an unfinalized draft                           (Low, Sprint 3, Member 5 — MF-05)
```

MF-03's own deliverables against Lab 02 are US-03, US-05 and the appointment-status side of US-26. US-27/US-28 belong to MF-05 but consume the CHECKED_IN appointment state that MF-03 produces. Use these IDs (not only FR/UC/TC IDs) in Sprint 3 evidence records, since the Lab 02 rubric grades against user-story IDs.

---

# 7. Canonical Purpose and Ownership Boundary

**Purpose:** Allow eligible citizens to reserve valid session capacity and support deterministic waitlist, rescheduling, cancellation, session-change handling and traceable arrival/check-in processing.

**Primary users:** Citizen, Receptionist, Center Staff

Main records:

- `Appointment`
- `AppointmentChangeEvent`
- `WaitlistEntry`
- `WaitlistChangeEvent`
- `CheckInEvent`
- `ModuleEventConsumption`

The canonical source controls exact fields, foreign keys and deletion rules. Do not infer missing medical/policy data.

# 8. Core Capabilities

- Search available sessions/slots for currently bookable PUBLISHED or ACTIVE campaigns.
- Book one available slot atomically.
- View, reschedule and cancel an appointment.
- Join/leave a session waitlist when no slot is available.
- Promote earliest still-valid waitlist entry by deterministic order.
- Validate appointment identity, center, session and time at check-in.
- Record no-show and authorized check-in reversal.
- Consume each MF-02 SessionChangeEvent exactly once per consumer and preserve old/new schedule details.
- Update affected appointments/waitlists through normal MF-03 transitions with history and notifications.
- Never silently move a citizen to a different session.

---

# 9. Canonical Business Rules

- **MF03-BR-01:** Booking requires a currently valid ELIGIBLE assessment for the same campaign.
- **MF03-BR-02:** One active appointment per citizen per campaign is enforced by unique constraint/equivalent locking.
- **MF03-BR-03:** Capacity allocation is atomic and prevents two users taking the last place.
- **MF03-BR-04:** Citizen self-cancellation/rescheduling is allowed until 24 hours before slot start; later changes require staff override with reason.
- **MF03-BR-05:** Rescheduling releases old capacity and reserves new capacity in one transaction.
- **MF03-BR-06:** Waitlist order is joined_at ascending, then waitlist_entry_id ascending; only one WAITING entry per citizen/session.
- **MF03-BR-07:** Promotion rechecks eligibility, campaign status and duplicate appointment rules; invalid entries are skipped and marked.
- **MF03-BR-08:** Successful promotion creates confirmed appointment and marks waitlist entry PROMOTED.
- **MF03-BR-09:** Standard check-in window is 30 minutes before to 30 minutes after slot start; staff override requires reason.
- **MF03-BR-10:** Check-in is allowed only at appointment center and for CONFIRMED appointment.
- **MF03-BR-11:** A COMPLETED appointment cannot be cancelled, rescheduled or checked in again.
- **MF03-BR-12:** Each MF-02 session-change event is processed exactly once per consumer using unique (consumer_code, source_event_id); status/attempts/completion/error retained.
- **MF03-BR-13:** Cancelled session cancels affected appointments/waitlist entries with linked change events/reason; rebooking is normal atomic reschedule/book flow and never silent/automatic.
- **MF03-BR-14:** Citizen cannot hold WAITING entry for a session when already holding active appointment for same campaign; promotion protected by active-appointment constraint.
- **MF03-BR-15:** Check-in reversal returns CHECKED_IN to CONFIRMED, requires reason, and is rejected after finalization/completion.
- **MF03-BR-16:** If an unfinalized VaccinationDraft exists, check-in reversal is rejected until the draft is deleted/explicitly invalidated through authorized MF-05 workflow.

Critical rules require both database constraints/locking where appropriate and service-level validation. Frontend-only validation is insufficient.

---

# 10. Separate Status Models

## AppointmentStatus

```text
CONFIRMED, CHECKED_IN, COMPLETED, CANCELLED, NO_SHOW
```

## WaitlistStatus

```text
WAITING, PROMOTED, SKIPPED_INELIGIBLE, SKIPPED_DUPLICATE_APPOINTMENT, CANCELLED, SESSION_CANCELLED
```

## CheckInEventType

```text
CHECKED_IN, REVERSED
```

Do not merge unrelated status models into one global enum.

---

# 11. Cross-Module Dependencies and Boundaries

- **MF-01:** Reads current campaign-specific ELIGIBLE assessment at booking/promotion.
- **MF-02:** Reads bookable campaign/session/slot/capacity and consumes SessionChangeEvent; MF-02 does not mutate appointments.
- **MF-05:** Provides CHECKED_IN appointment state required for draft/finalization. Check-in reversal is blocked by active draft/permanent record.
- **Shared notifications:** Requests booking/cancellation/waitlist-promotion/reminder notifications.
- **Concurrency/data layer:** Atomic last-slot allocation and reschedule behavior are core correctness requirements.

When an integration depends on another function, inspect the actual current contract before coding. Do not duplicate the other function's business ownership.

For MF-02 → MF-03 schedule-change integration, `SessionChangeEvent` delivery must follow the shared transactional-outbox/idempotent-consumer baseline rather than direct silent cross-module mutation.

---

# 12. Recommended Target File Structure

> This is a target/reference tree, not an instruction to create every file now. Create files only as each logical slice is implemented.

```text
backend/.../appointment/
├── controller/
│   ├── AvailabilityController.java
│   ├── AppointmentController.java
│   ├── WaitlistController.java
│   └── CheckInController.java
├── dto/
│   ├── availability/
│   ├── appointment/
│   ├── waitlist/
│   └── checkin/
├── model/
│   ├── Appointment.java
│   ├── AppointmentStatus.java
│   ├── AppointmentChangeEvent.java
│   ├── WaitlistEntry.java
│   ├── WaitlistStatus.java
│   ├── WaitlistChangeEvent.java
│   ├── CheckInEvent.java
│   ├── CheckInEventType.java
│   └── ModuleEventConsumption.java
├── repository/
│   ├── AppointmentRepository.java
│   ├── AppointmentChangeEventRepository.java
│   ├── WaitlistEntryRepository.java
│   ├── WaitlistChangeEventRepository.java
│   ├── CheckInEventRepository.java
│   └── ModuleEventConsumptionRepository.java
├── service/
│   ├── AvailabilityService.java
│   ├── AppointmentBookingService.java
│   ├── AppointmentRescheduleService.java
│   ├── AppointmentCancellationService.java
│   ├── WaitlistService.java
│   ├── CheckInService.java
│   └── SessionChangeConsumerService.java
├── mapper/
└── validation/

frontend/src/features/appointment-management/
├── api/
│   ├── availabilityApi.js
│   ├── appointmentApi.js
│   ├── waitlistApi.js
│   └── checkInApi.js
├── components/
│   ├── AvailabilitySearch.jsx
│   ├── SessionAvailabilityCard.jsx
│   ├── AppointmentCard.jsx
│   ├── AppointmentStatusBadge.jsx
│   ├── RescheduleDialog.jsx
│   ├── WaitlistCard.jsx
│   ├── WaitlistPosition.jsx
│   └── CheckInPanel.jsx
├── pages/
│   ├── AppointmentBookingPage.jsx
│   ├── MyAppointmentsPage.jsx
│   ├── WaitlistPage.jsx
│   └── CheckInManagementPage.jsx
├── hooks/
└── validation/
```

---

# 13. Function-Specific Scrum Roadmap

> Remapped onto the four-sprint Lab 02 structure (Section 4). MF-03 has no dedicated early sprint of its own in the Lab plan, so Sprints 1–2 are preparation work done alongside MF-01/MF-02, and Sprint 3 is where MF-03 is actually built and demonstrated.

## Sprint 1 (Weeks 1–3)

**Function focus:** Agree shared identity/status/API/locking conventions with MF-01/MF-02 while they build campaign, questionnaire and center foundations. No isolated appointment implementation should assume unfinished MF-01/MF-02 contracts. MF-03 can draft its domain/API contracts (Appointment, AppointmentStatus, WaitlistEntry, WaitlistStatus) but must not claim availability or eligibility integration is ready.

**Expected integration outcome:** Shared contracts identified; MF-03 domain design drafted.

## Sprint 2 (Weeks 4–6)

**Function focus:** Review the now-stabilizing screening/eligibility (MF-01) and session-slot/staff/capacity (MF-02) contracts; finalize MF-03's API/domain design and concurrency approach (active-appointment guard, optimistic locking on TimeSlot) without implementing against unverified endpoints.

**Expected integration outcome:** Dependency contracts ready; MF-03 ready to build.

## Sprint 3 (Weeks 7–9) — MF-03's primary implementation sprint

**Function focus:** Implement availability search, atomic booking (US-03), cancel/reschedule, waitlist FIFO/promotion (US-05), checked-in/eligible appointment queue (US-26), check-in/no-show/reversal, and the SessionChangeEvent idempotent consumer.

**Expected integration outcome:** Citizen can book, join a waitlist, and be checked in; the vaccinator queue (feeding MF-05's US-27/US-28 draft workflow) is demonstrable.

## Sprint 4 (Weeks 10–12)

**Function focus:** Integrate with MF-05 finalization (US-29, US-30): CHECKED_IN→COMPLETED ownership, reversal blocking once a draft/record exists, and support MF-02's reschedule/cancel impact preview (US-20) by correctly processing the resulting SessionChangeEvent. Close out concurrency/security/evidence hardening for MF-03 as part of general regression.

**Expected integration outcome:** MF-05 can finalize safely; MF-03 is release-ready with evidence package.

---

# 14. Recommended Implementation Phases

1. Availability read contract with MF-02
2. Appointment domain + unique active-booking guard
3. Atomic booking service/concurrency
4. Cancellation + capacity release
5. Atomic rescheduling
6. Waitlist domain + deterministic FIFO
7. Promotion with eligibility/duplicate recheck
8. Check-in/no-show/reversal
9. MF-02 SessionChangeEvent idempotent consumer
10. MF-05 finalization/check-in integration
11. Frontend booking/waitlist/check-in
12. Concurrency/security/evidence hardening

Do not skip directly to a later phase if a required dependency or invariant from an earlier phase is missing.

---

# 15. Current/Next Implementation Checkpoint


Current MF-03 implementation is NOT_VERIFIED. Inspect the repository and current Scrum position first.

Canonical main implementation belongs primarily to Sprint 3, after MF-01 eligibility and MF-02 validated session availability exist. MF-03 can still prepare domain/API contracts earlier, but it must not invent capacity or eligibility behavior.

If dependencies are available and MF-03 has not started, first implement the **Appointment domain + AppointmentStatus + active-appointment uniqueness/locking design**, then the atomic booking service.

First likely logical commit:

`feat(mf03): add appointment domain model and status`

Do not implement waitlist promotion before the booking invariants are stable.

---

# 16. Recommended Logical Commit Sequence

- `feat(mf03): add appointment domain model and status`
- `feat(mf03): implement atomic appointment booking`
- `feat(mf03): implement cancellation and rescheduling`
- `feat(mf03): add deterministic waitlist workflow`
- `feat(mf03): implement check-in and reversal`
- `feat(mf03): consume session change events idempotently`
- `test(mf03): add last-slot and reschedule concurrency tests`
- `feat(mf03): add booking waitlist and check-in interfaces`

Combine tiny adjacent changes when they form one coherent slice. Do not create one commit per file.

---

# 17. Acceptance Criteria to Preserve

- Ineligible/expired citizen cannot book.
- Two concurrent users cannot both receive final place.
- Session-change processing is idempotent and preserves history.
- Cancelled session never silently moves citizens.
- Rescheduling is all-or-nothing.
- Cancellation recalculates availability and can trigger promotion.
- Wrong-center/out-of-window check-in is rejected unless authorized override with reason.

These are condensed handoff checks; the canonical v3.3 section remains authoritative.

---

# 18. Function-Specific Testing Priorities

- last-slot concurrent booking
- one active appointment per citizen/campaign
- atomic reschedule
- FIFO waitlist tie-break
- promotion eligibility recheck
- check-in window/override
- reversal blocking with MF-05 draft/record
- idempotent SessionChangeEvent consumption

---

# 19. Function-Specific AI Guardrails

- Do not decide medical eligibility; read MF-01 current assessment.
- Do not invent session capacity; use MF-02 authoritative capacity/availability.
- Do not silently rebook citizens after MF-02 schedule changes.
- Do not allow non-atomic last-place booking or rescheduling.
- Do not reverse check-in after MF-05 draft/finalization constraints forbid it.

---


# 20. Common Git and Commit Rules

Prefer **multiple meaningful logical commits**, not one giant commit and not one commit per file.

Good pattern:

```text
domain model
→ persistence
→ API contracts/validation
→ service/business logic
→ controller/API
→ frontend slice
→ tests/integration
```

Commit-message examples should be function-scoped:

```text
feat(mfXX): ...
test(mfXX): ...
fix(mfXX): ...
docs(mfXX): ...
refactor(mfXX): ...
```

Before a PR, run all relevant regression checks.

Backend:

```bash
cd backend
./mvnw verify
```

Windows:

```powershell
cd backend
.\mvnw.cmd verify
```

Frontend:

```bash
cd frontend
npm ci
npm run lint
npm run build
```

Do not weaken CI to make a feature pass.

---

# 21. Evidence Record Per Sprint

Keep lightweight evidence:

```text
SPRINT:
SPRINT GOAL:

PLANNED:
- ...

COMPLETED:
- ...

NOT COMPLETED:
- ...

BLOCKERS:
- ...

DECISIONS:
- ...

COMMITS:
- <hash> <message>

PULL REQUESTS:
- PR #...

TESTING:
- ...

EVIDENCE:
- UI screenshots
- API request/response
- migration/database evidence
- positive/negative tests
- CI result
- integration demonstration

RETROSPECTIVE:
Worked well:
- ...

Improve next sprint:
- ...
```

For each major function, the canonical evidence checklist expects:

- owner name/student ID;
- requirement/user-story IDs where applicable;
- design/data model excerpt;
- UI screenshots;
- API evidence;
- database/migration evidence;
- positive and negative tests;
- commit and PR links;
- integration demonstration;
- short individual reflection.

---

# 22. Universal Guardrails

Never commit:

- passwords, tokens, API keys;
- `.env` files;
- real citizen identity data;
- real medical/screening data;
- real vaccination records;
- production database dumps.

Do not:

- modify the canonical files from a feature implementation task;
- create one global status enum for unrelated entities;
- hard-delete operational history where the canonical baseline requires retention;
- rely on frontend validation for critical business rules;
- expose JPA entities directly as public API contracts;
- implement another module's business rules inside this module;
- bypass peer review or branch rules;
- force-push shared branches;
- claim implementation exists without code/test evidence.

Use synthetic/anonymized development data only.

---

# 23. AI Session Completion Report

At the end of a coding session, report:

```text
Repository:
Branch:
Starting HEAD:
Ending HEAD:

Canonical section used:
Current phase:
Task completed:

Files created:
Files modified:
Files deleted:

Tests:
Frontend lint:
Frontend build:
Backend verify:
Feature-specific tests:

Business rules covered:
Acceptance criteria covered:

Commit(s):
Push/PR status:

Cross-module impacts:
Unresolved blockers:

Exact next recommended task:
```

Do not move to a later phase if the current phase is failing tests or has unresolved ownership/contract issues.

# MF-02 — Vaccination Campaign, Center and Session Management
## AI Engineering Handoff

**Project:** Web-Based Vaccination Portal (`WBVP-SE2030`)
**Function:** MF-02 — Vaccination Campaign, Center and Session Management
**Canonical authority:** `3.3-AI / 2026-08-14-R9`
**Canonical source model:** Markdown-only; the canonical Markdown file is the sole normative semantic source.
**Sprint:** Sprint 1 — **IN PROGRESS**
**MF-02:** **IN PROGRESS**
**US-13:** **IN PROGRESS**
**Last synchronized implementation checkpoint:** PR #110
**Reviewed PR #110 candidate:** `d3a1d787cd0b5b8f29a62709d4fd26e853ba4fbd`
**PR #110 candidate development base:** `94af45a27636deb4e057c94846086da018da8d9e`
**PR #110 MySQL test fixture repair commit:** `228dde476b5b86354bdb07f7887c99e76f035348`
**PR #110 production concurrency correction commit:** `26db0433045478876c3ea1c4cbc1ba1708aad16c`
**PR #110 actual merge Parent 1:** `94af45a27636deb4e057c94846086da018da8d9e`
**PR #110 actual merge Parent 2:** `26db0433045478876c3ea1c4cbc1ba1708aad16c`
**PR #110 merge:** `1c7a09c772018eaf56481aa77ff38688587879fc`
**Preceding documentation synchronization milestone:** PR #109 (`a4a289af163628e36fa78831e5f8fdf4e45cf5dc` / merge `94af45a27636deb4e057c94846086da018da8d9e`)
**Preceding center operational resource and staff roster milestone:** PR #108 (`5dc5e2be5f30d211bf075afd9323141822c63290` / merge `6edcaca67af0dca9d4e0a76bd69b8f180b6e7b85`)
**Preceding documentation synchronization milestone:** PR #107 (`02257217fb8bcab4810f6cb3883a9ceb00244729` / merge `e63bb76ac3dbb08b5009976233bf4255a059a712`)
**Preceding session operational core milestone:** PR #106 (`46de335ec2654c89417366d65ca439c3ff1c5501` / merge `888f9d4b53d5206ff9fdfaadcedaf86092ab62f1`)
**Preceding documentation synchronization milestone:** PR #105 (`03c6511fa9d1b017b2b6aa6b1397b9195977926c` / merge `c8d5b452ee88ec97988874e211ffb26ea321b3e1`)
**Preceding campaign configuration REST milestone:** PR #104 (`eb41c2210fa8fdce80a8524746386b9c341ff2ec` / merge `6454a8faf2fe7a511f324deb5227c34d6197d81c`)
**Preceding documentation synchronization milestone:** PR #103 (`220553c7d171946be68113d8a2b22c4d5f694a9e` / merge `d8d7c3afcea889e9e65b6c188afbc23ef7bde548`)
**Preceding session operational prerequisite guard milestone:** PR #102 (`d7663abeb39dd76861e817cbf750b9de7d5d70d5` / merge `616636762687e5c2497db8e0227ee8692311703c`)
**Preceding documentation synchronization milestone:** PR #101 (`1c73a21cb5a07374b3303666ff7cb1ca01a2eb31` / merge `046f475f13bb1e7c734492ae58f3e3bb3eb7f9ff`)
**Preceding recurrence/series REST milestone:** PR #100 (`f5c385ae7d43a17e60a2ee7647fe96f7cfa0446c` / merge `684a4d6bffbe1d1b21f10ad9fa141b62fb23625a`)
**Preceding documentation synchronization milestone:** PR #98 (`09da5f9d9ac512210be9d7086f0f96ced2e3cb02` / merge `6be1c7f78328c58c02b274db774936d951ef9fc0`)
**Preceding pre-recurrence remediation milestone:** PR #97 (`ff60fb065bba498a8da05f41c1d3e1e56791c3c6` / merge `e83e54bff1f2c682fbdb6d760856bde993faf162`)
**Preceding documentation synchronization milestone:** PR #93 (`71de894870f7dbbfa34a4cb69fb04a11f26a117b` / merge `1b90e2ee4567502a5d90d72af28eda4998f554b7`)
**Preceding session change REST milestone:** PR #92 (`ccf5c3d58035d35e56d316207848d58388446547` / merge `6ff63bdd04f34d3bd334d011fdf55dee8010a140`)
**Preceding documentation synchronization milestone:** PR #91 (`debc5666f2284991507df089ea49d0dd1310114a` / merge `1531ad9f89e3dc75b174cc3deb3a2204e2712a52`)
**Preceding session core REST milestone:** PR #90 (`3324ab4b2a84960dea946c531115b5aee829ad61` / merge `9cc7a606260e24e243090f70f5e957b0544bfa3b`)
**Preceding documentation synchronization milestone:** PR #89 (`77c9fbeb75e268cadf519fcb1fa297822712f7bb` / merge `4eaa8cd31c04aedf23f3887f25de27ec924165b8`)
**Preceding center REST milestone:** PR #88 (`263c26bc9d9a7b51bd81e28d326a90b004c548f0` / merge `faf455ff6f07d096258e7cd977765e6e9e25b10f`)
**Preceding documentation synchronization milestone:** PR #87 (`9371e0e14f07670175674b79ec98978096136158` / merge `37574bb02e157971c3ca2250d4953834cfe6d132`)
**Preceding campaign REST milestone:** PR #86 (`0fab4afa983252e927dda01278193a584ad9a0d8` / merge `a9a71114738ce8aed6c1f0cff8a8f42d955a033c`)
**Preceding documentation synchronization milestone:** PR #85 (`ac860a9d271daa2ecf4211571e44a93a2f2622f4` / merge `9d40ddbd55139a28707c3bdb68fb6a222b5b1c28`)
**Preceding MySQL assurance milestone:** PR #84 (`ded72c17d8bb4aaf222d6e2a96b44fa93208e880` / merge `34aa7666dc2d37b1b71faf25a7f51cfba10ee26a`)
**Current verified backend baseline:**
- **Candidate-local functional H2 baseline:** **1080 functional H2 tests** across locally verified Surefire XML reports (0 failures, 0 errors, 0 skipped; +39 over immediate parent baseline 1041).
- **Fresh PR #110 merge-context Backend CI:** **1080 functional H2 tests** across the combined tree context (0 failures, 0 errors, 0 skipped, `BUILD SUCCESS` in 2m16s; Run 32462530177, Job 96712222310).
- **Authoritative fresh MySQL assurance:** **6 MySQL/Testcontainers assurance tests** across 4 classes on `mysql:8.4.3` (0 failures, 0 errors, 0 skipped; 4 historical + 2 M2B operational assignment tests).
- **Combined logical executions:** **1086 logical executions** (1080 H2 + 6 MySQL) across separate validation paths.
- Clean Maven test/verify PASS; Backend CI PASS; Frontend CI PASS (15s; Run 32462530139, Job 96712222180).
- **PR #110 net-new executions:** +39 H2 tests across 3 test files (+20 `SessionOperationalAssignmentControllerTests`, +9 `SessionResourceAssignmentServiceIntegrationTests`, +10 `StaffAssignmentServiceIntegrationTests`) + 2 MySQL assurance tests (`SessionOperationalAssignmentMySqlConcurrencyIntegrationTests`).
- **Thread.sleep in tests:** 0 occurrences across `backend/src/test`.

> This file is a maintained engineering snapshot. Canonical Markdown controls semantics; live Git/code/tests control implementation truth. This handoff must not be used to override canonical rules.

## 1. Authority Order

Use this order when facts conflict:

1. `docs/Web_Based_Vaccination_Portal_AI_Canonical_Context_v3.3.md` — semantic/business source of truth.
2. Live `origin/develop`, merged PRs, production code, tests, and CI — implementation truth.
3. `docs/MF02_Vaccination_Campaign_Center_and_Session_Management_AI_Handoff.md` — current MF-02 engineering state.
4. `docs/evidence/sprint-1/MF02_Sprint1_Evidence.md` — implementation/verification chronology.
5. `PROJECT_CONTEXT.md` — orientation summary only.
6. `MF02_Completion_and_Hardening_Plan.md` — planned work, not evidence of implementation.

Future agents must fetch live `origin/develop` before treating `1c7a09c772018eaf56481aa77ff38688587879fc` as the repository tip.

## 2. Current Milestone

### Milestone reached

**MF-02 SESSION OPERATIONAL ASSIGNMENTS REST API (PR #110; MILESTONE 2B; 8 ENDPOINTS; COMPLETE), CENTER OPERATIONAL RESOURCE AND STAFF ROSTER REST API (PR #108; MILESTONE 2A; 8 ENDPOINTS; COMPLETE), SESSION OPERATIONAL CORE MODEL AND CALCULATION ENGINE (PR #106; COMPLETE), CAMPAIGN CONFIGURATION REST API (PR #104; 12 ENDPOINTS; COMPLETE_WITH_SEEDED_MASTER_DATA), SESSION OPERATIONAL PREREQUISITE GUARD (PR #102; COMPLETE), RECURRENCE AND SERIES REST API MILESTONE 5 (PR #100; 9 ENDPOINTS; COMPLETE_WITH_MF03_RUNTIME_CONSTRAINT), PRE-RECURRENCE CORRECTNESS AND SAFETY REMEDIATION (PR #97; F-01, F-02, F-03, F-04, SESSION SERIES MYSQL CONCURRENCY ASSURANCE), VACCINATION SESSION CHANGE CONTROL REST API (7 ENDPOINTS; COMPLETE_WITH_MF03_RUNTIME_CONSTRAINT), VACCINATION SESSION CORE REST API (6 ENDPOINTS; COMPLETE_WITH_OPERATIONAL_SCHEDULING_GUARD), VACCINATION CENTER MANAGEMENT REST API (14 ENDPOINTS), CAMPAIGN MANAGEMENT REST API (9 ENDPOINTS), MYSQL/TESTCONTAINERS CONCURRENCY ASSURANCE, CAMPAIGN RESUMPTION, CAMPAIGN ACTIVATION, SCHEDULING APPROVAL, AND SESSION-CHANGE FOUNDATIONS ARE INTEGRATED.**

This does **not** mean MF-02 is complete.

The current checkpoint includes the Session Operational Assignments REST API (PR #110; Milestone 2B; 8 endpoints), the Center Operational Resource and Staff Roster REST API (PR #108; Milestone 2A; 8 endpoints), the Session Operational Core Model and Calculation Engine (PR #106; Milestone 1), the Campaign Configuration REST API (PR #104; 12 endpoints), the Session Operational Prerequisite Guard (PR #102), the Vaccination Session Recurrence / Series REST API Milestone 5 (PR #100), the Pre-Recurrence Correctness and Safety Remediation (PR #97), the Vaccination Session Change Control REST API vertical slice (PR #92), the Vaccination Session Core REST API vertical slice (PR #90), the Vaccination Center Management REST API vertical slice (PR #88), the Campaign Management REST API vertical slice (PR #86), representative production-equivalent MySQL/Testcontainers concurrency assurance (PR #84, hardened in PR #97, expanded in PR #110), authoritative campaign resumption `SUSPENDED -> PUBLISHED / ACTIVE` (PR #82), authoritative campaign activation `PUBLISHED -> ACTIVE` (PR #80), deterministic reverse-impact concurrency test hardening (PR #78), the behavior-preserving extraction of `SessionSeriesPreviewCalculator` from `SessionSeriesChangeService` (PR #75), defensive questionnaire publication invariant revalidation (PR #73), and campaign scheduling approval (PR #71) on top of the already integrated campaign, center, recurrence, session, reverse-impact, publication, suspension, terminal-planning, and SUSPENDED-planning foundations.

Total MF-02 REST routes currently integrated: **73** ($21 \text{ Campaign-family [9 Core + 12 Configuration]} + 22 \text{ Center-family [14 Core + 8 Operational Resource/Staff Roster]} + 21 \text{ Session-family [6 Core + 7 Change + 8 Operational Assignments]} + 9 \text{ Recurrence/Series} = 73$). Total Campaign-family routes: **21** ($9 \text{ Existing} + 12 \text{ Configuration} = 21$). Total Center-family routes: **22** ($14 \text{ Existing} + 4 \text{ Resources} + 4 \text{ Staff Roster} = 22$). Total Session routes: **21** ($6 \text{ Core} + 7 \text{ Change} + 8 \text{ Operational Assignments} = 21$). Total Recurrence/Series routes: **9**. PR #110 added 8 REST routes ($65 \rightarrow 73$).

### Current verified integrations

1. **PR #110 — `feat(mf02): expose session operational assignment rest api`**
   - reviewed source: `d3a1d787cd0b5b8f29a62709d4fd26e853ba4fbd`
   - candidate development base: `94af45a27636deb4e057c94846086da018da8d9e`
   - MySQL fixture repair commit: `228dde476b5b86354bdb07f7887c99e76f035348`
   - production concurrency correction commit: `26db0433045478876c3ea1c4cbc1ba1708aad16c`
   - actual merge Parent 1: `94af45a27636deb4e057c94846086da018da8d9e`
   - actual merge Parent 2: `26db0433045478876c3ea1c4cbc1ba1708aad16c`
   - merge: `1c7a09c772018eaf56481aa77ff38688587879fc`
   - merged at: `2026-08-21T08:22:21Z`
   - merge method: normal merge commit (two parents: `94af45a27636deb4e057c94846086da018da8d9e` and `26db0433045478876c3ea1c4cbc1ba1708aad16c`)
   - integration drift: 0
   - Backend CI: PASS (1080 H2 verify in combined merge context + 6 Testcontainers MySQL verify on `mysql:8.4.3`, 0 failures, 0 errors, 0 skipped, `BUILD SUCCESS` in 2m16s; Run 32462530177, Job 96712222310)
   - Frontend CI: PASS (15s; Run 32462530139, Job 96712222180)
   - authoritative backend baseline: **1080 functional H2 tests** (0 failures, 0 errors, 0 skipped; +39 over immediate parent baseline 1041) and **1080 combined merge-context H2 tests** in CI + **6 MySQL assurance tests** across 4 classes
   - net-new executions: **+39 H2 tests** across 3 test files (+20 `SessionOperationalAssignmentControllerTests`, +9 `SessionResourceAssignmentServiceIntegrationTests`, +10 `StaffAssignmentServiceIntegrationTests`) + **2 MySQL concurrency assurance tests** (`SessionOperationalAssignmentMySqlConcurrencyIntegrationTests`)
   - scope: 14 files (9 new production, 2 modified production, 3 new test files; +1995 / -1 lines)
   - integrated endpoints under `/api/v1/sessions/{sessionId}` (8):
     1. `POST /api/v1/sessions/{sessionId}/resource-assignments` (Assign center resource to DRAFT session; 201 Created + Location header `/api/v1/sessions/{sessionId}/resource-assignments/{id}`)
     2. `GET /api/v1/sessions/{sessionId}/resource-assignments` (List session resource assignments, ordered by `startAt` ASC, `id` ASC; 200 OK)
     3. `GET /api/v1/sessions/{sessionId}/resource-assignments/{assignmentId}` (Get single resource assignment; 200 OK)
     4. `DELETE /api/v1/sessions/{sessionId}/resource-assignments/{assignmentId}` (Delete resource assignment from DRAFT session; 204 No Content)
     5. `POST /api/v1/sessions/{sessionId}/staff-assignments` (Assign staff member to DRAFT session; 201 Created + Location header `/api/v1/sessions/{sessionId}/staff-assignments/{id}`)
     6. `GET /api/v1/sessions/{sessionId}/staff-assignments` (List session staff assignments, ordered by `startAt` ASC, `id` ASC; 200 OK)
     7. `GET /api/v1/sessions/{sessionId}/staff-assignments/{assignmentId}` (Get single staff assignment; 200 OK)
     8. `DELETE /api/v1/sessions/{sessionId}/staff-assignments/{assignmentId}` (Delete staff assignment from DRAFT session; 204 No Content)
   - Operational Assignment Semantics:
     - DRAFT-only mutation: Assignments can only be created or deleted when session lifecycle status is `DRAFT`. Attempting to mutate assignments for non-DRAFT sessions throws `IllegalStateException` (HTTP 409 Conflict).
     - Lifecycle-independent reads: GET endpoints operate across all session lifecycle states.
     - Operating window containment: Assignment intervals `[startAt, endAt)` must fall strictly within the session operating window `[session.startAt, session.endAt]`.
     - Half-open interval overlap checks: `existing.startAt < requested.endAt AND requested.startAt < existing.endAt`.
     - Active entity checks: Resources and staff roster entries must have status `ACTIVE` at the time of assignment.
     - Same-center qualification: The assigned resource or staff roster entry must belong to the same vaccination center as the session.
     - Role qualification: The requested assignment role (`StaffRole`) must match the staff member's qualified roster role.
     - Historical preservation: CANCELLED sessions are excluded from temporal overlap collision detection.
     - Nested ownership & 404 normalization: Missing parent sessions or cross-session assignment ID mismatches normalize to HTTP 404 Not Found.
   - Concurrency & Lock Sequencing:
     - Resource assignment lock sequence: `VaccinationSession` lock -> `OperationalResource` row lock (`findByIdForUpdate`) -> Overlap check (`existsOverlappingAssignment`) -> Save.
     - Staff assignment lock sequence: `VaccinationSession` lock -> Non-locking target user resolution (`findById`) -> Global user roster lock set (`findByUserIdForUpdateOrderByIdAsc`) in deterministic `id ASC` order -> Locked roster qualification -> Pessimistic write current-read overlap query (`findOverlappingAssignmentsForUpdate`) -> Save.
     - Concurrency finding & fix: Real MySQL/InnoDB Testcontainers execution revealed that under REPEATABLE READ, non-locking overlap queries read from MVCC snapshots established before lock acquisition. Upgrading the post-lock overlap check to a locking current read query (`findOverlappingAssignmentsForUpdate` with `@Lock(LockModeType.PESSIMISTIC_WRITE)`) guarantees that serialized transactions observe committed overlapping assignments immediately upon acquiring global user roster locks, preserving deterministic global lock order without deadlock inversion risk.
   - Scheduling state & exclusions:
     - PR #102 temporary fail-closed scheduling guard remains `COMPLETE`.
     - `DRAFT -> SCHEDULED` is still fail-closed with HTTP 409 Conflict.
     - Excludes M2C capacity plans, M3 readiness evaluation, and M4 slot capacity integration.
   - REST routes added: 8 (total MF-02 routes: 65 -> 73).

2. **PR #108 — `feat(mf02): expose center operational resources and staff roster rest api`**
   - reviewed source: `5dc5e2be5f30d211bf075afd9323141822c63290`
   - candidate development base: `e63bb76ac3dbb08b5009976233bf4255a059a712`
   - actual merge Parent 1: `e63bb76ac3dbb08b5009976233bf4255a059a712`
   - actual merge Parent 2: `5dc5e2be5f30d211bf075afd9323141822c63290`
   - merge: `6edcaca67af0dca9d4e0a76bd69b8f180b6e7b85`
   - merged at: `2026-08-21T04:21:37Z`
   - merge method: normal merge commit (two parents: `e63bb76ac3dbb08b5009976233bf4255a059a712` and `5dc5e2be5f30d211bf075afd9323141822c63290`)
   - integration drift: 0
   - Backend CI: PASS (1041 H2 verify in combined merge context + 4 Testcontainers MySQL verify on `mysql:8.4.3`, 0 failures, 0 errors, 0 skipped, `BUILD SUCCESS` in 2m37s; Run 32446414058, Job 96666733750)
   - Frontend CI: PASS (14s; Run 32446414084, Job 96666733813)
   - authoritative backend baseline: **1041 functional H2 tests** (0 failures, 0 errors, 0 skipped; +43 over immediate parent baseline 998) and **1041 combined merge-context H2 tests** in CI + **4 MySQL assurance tests** across 3 classes
   - net-new executions: **+43 H2 tests** across 6 test files (+14 `CenterOperationalResourceControllerTests`, +19 `CenterStaffRosterControllerTests`, +3 `OperationalConcurrencyTranslationUnitTests`, +2 `OperationalResourcePersistenceIntegrationTests`, -2 net / +23 additional assertion paths `OperationalResourceServiceIntegrationTests`, +7 `OperationalStaffRosterServiceIntegrationTests`)
   - scope: 21 files (10 new production, 5 modified production, 4 new test, 2 modified test; +1783 / -59 lines)
   - integrated endpoints under `/api/v1/centers/{centerId}` (8):
     1. `POST /api/v1/centers/{centerId}/resources` (Create center operational resource; 201 Created + Location header `/api/v1/centers/{centerId}/resources/{id}`)
     2. `GET /api/v1/centers/{centerId}/resources` (List center operational resources with optional `?resourceType=` filter; returns all statuses `ACTIVE`, `MAINTENANCE`, `INACTIVE` by default; 200 OK)
     3. `GET /api/v1/centers/{centerId}/resources/{resourceId}` (Get center operational resource by ID; 200 OK)
     4. `PUT /api/v1/centers/{centerId}/resources/{resourceId}` (Update operational resource name/status with pessimistic locking; 200 OK)
     5. `POST /api/v1/centers/{centerId}/staff-roster` (Register staff member to center roster; 201 Created + Location header `/api/v1/centers/{centerId}/staff-roster/{id}`)
     6. `GET /api/v1/centers/{centerId}/staff-roster` (List center staff roster with optional `?role=` and `?status=` filters; returns all roles and statuses by default; 200 OK)
     7. `GET /api/v1/centers/{centerId}/staff-roster/{rosterId}` (Get center staff roster entry by ID; 200 OK)
     8. `PUT /api/v1/centers/{centerId}/staff-roster/{rosterId}` (Update staff roster role/status with pessimistic locking; 200 OK)
   - OperationalResourceStatus Canonical Correction:
     - Canonical enum contains: `ACTIVE`, `MAINTENANCE`, `INACTIVE`.
     - `MAINTENANCE` was the canonical Section 6.6 value missing from the initial M1 implementation and was added in M2A.
     - No database schema migration required because JPA persistence uses `EnumType.STRING` and the existing database column accommodates `MAINTENANCE`.
    - StaffRole:
      - `StaffRole` remains the live implementation-defined three-value set `VACCINATOR`, `CHECK_IN_STAFF`, and `SUPERVISOR`; canonical requires the operational vaccinator/check-in semantics but does not define a closed StaffRole enum.
   - Resource and Roster Semantics & Concurrency:
     - Resource create defaults status `ACTIVE`.
     - Resource list returns all statuses (`ACTIVE`, `MAINTENANCE`, `INACTIVE`) by default; `resourceType` filter does not filter by status.
     - Resource `code` and `resourceType` are immutable.
     - Resource `name` and `status` are mutable.
     - Resource updates use pessimistic locking on the resource row (`findByIdForUpdate`).
     - Duplicate resource code for same center -> HTTP 409 Conflict.
     - Nested center ownership mismatch -> HTTP 404 Not Found.
     - Staff roster registration defaults status `ACTIVE`.
     - Staff roster list returns all roles and statuses by default; optional `role` and `status` filters.
     - Staff roster `userId` is immutable.
     - Staff roster `staffRole` and `status` are mutable.
     - Staff roster updates use pessimistic locking on the roster row (`findByIdForUpdate`).
     - Duplicate roster user for same center -> HTTP 409 Conflict.
     - Nested center ownership mismatch -> HTTP 404 Not Found.
     - Database uniqueness constraint races on concurrent creates/registrations are narrowly caught and mapped to HTTP 409 Conflict. Unrelated data integrity violations are rethrown.
     - Existing assignment history (`SessionResourceAssignment` rows, `StaffAssignment` snapshots) is preserved during status mutations.
   - Scheduling state & exclusions:
     - PR #102 temporary fail-closed scheduling guard remains `COMPLETE`.
     - `DRAFT -> SCHEDULED` is still fail-closed with HTTP 409 Conflict.
     - Excludes M2B session resource/staff assignments, M2C capacity plans, M3 readiness evaluation, and M4 slot capacity integration.
   - REST routes added: 8 (total MF-02 routes: 57 -> 65).

3. **PR #106 — `feat(mf02): implement session operational core model and calculation engine`**
   - reviewed source: `46de335ec2654c89417366d65ca439c3ff1c5501` (original M1 commit `e91269927355e3adcaba11eec75e45728d1889d7`, cross-center staff-lock correction `46de335ec2654c89417366d65ca439c3ff1c5501`)
   - candidate development base: `c8d5b452ee88ec97988874e211ffb26ea321b3e1`
   - actual merge Parent 1: `c8d5b452ee88ec97988874e211ffb26ea321b3e1`
   - actual merge Parent 2: `46de335ec2654c89417366d65ca439c3ff1c5501`
   - merge: `888f9d4b53d5206ff9fdfaadcedaf86092ab62f1`
   - merged at: `2026-08-20T09:58:52Z`
   - merge method: normal merge commit (two parents: `c8d5b452ee88ec97988874e211ffb26ea321b3e1` and `46de335ec2654c89417366d65ca439c3ff1c5501`)
   - integration drift: 0
   - Backend CI: PASS (998 H2 verify in combined merge context + 4 Testcontainers MySQL verify on `mysql:8.4.3`, 0 failures, 0 errors, 0 skipped, `BUILD SUCCESS` in 2m28s / H2 suite 1m25s; Run 32356332347, Job 96386194220)
   - Frontend CI: PASS (14s; Run 32356332549, Job 96386194726)
   - authoritative backend baseline: **998 functional H2 tests** (0 failures, 0 errors, 0 skipped; +45 over immediate parent baseline 953) and **998 combined merge-context H2 tests** in CI + **4 MySQL assurance tests** across 3 classes
   - net-new executions: **+45 H2 tests** across 8 test files
   - scope: 27 files (19 new production Java files, 0 modified production files, 8 new test files; +3009 / -0 lines)
   - integrated domain model & services:
     - Enums: `ResourceType`, `OperationalResourceStatus`, `StaffRole`, `OperationalStaffStatus`
     - Entities: `OperationalResource`, `SessionResourceAssignment`, `OperationalStaffRoster`, `StaffAssignment`, `SessionCapacityPlan`
     - Value Objects: `OccupancyInterval`, `OccupancyEvaluationResult`
     - Repositories: `OperationalResourceRepository`, `SessionResourceAssignmentRepository`, `OperationalStaffRosterRepository`, `StaffAssignmentRepository`, `SessionCapacityPlanRepository`
     - Services: `OperationalResourceService`, `OperationalStaffRosterService`, `SessionCapacityCalculationService`
   - Capacity & Throughput Semantics:
     - `recommendedThroughput = effectiveStationCount * floor(durationMinutes / expectedServiceMinutes)`
     - `effectiveStationCount = min(assignedVaccinatorCount, availableStationCount, fullyEquippedStationCount)` where `fullyEquippedStationCount` is an explicit scalar snapshot/input (not derived by counting generic EQUIPMENT resources).
     - `approvedCapacity`: Operationally approved booking capacity. If `sessionServiceCeiling != null`, then `approvedCapacity <= sessionServiceCeiling`.
     - `overrideReason`: Mandatory when `approvedCapacity > recommendedThroughput`. The hard `sessionServiceCeiling` cannot be overridden.
     - `sessionServiceCeiling`: Configured on `VaccinationCenter`, nullable, snapshotted into `SessionCapacityPlan`; NOT calculated from throughput; NOT calculated from availableStationCount.
     - `approvedBy`: String audit actor metadata (authenticated RBAC is not implemented).
   - Staff Concurrency & Roster-Lock Foundation:
     - `OperationalStaffRoster` is center-scoped: `unique(center_id, user_id)`
     - `StaffAssignment` overlap detection is global by external `userId`
     - Repository provides `findByUserIdForUpdateOrderByIdAsc(userId)` with `PESSIMISTIC_WRITE` covering all roster rows for the user in deterministic ID order
     - Safe claim: MF-02 has a deterministic global pessimistic-lock repository foundation for cross-center staff scheduling. Live scheduling concurrency is `NOT_YET_PROVEN` as live scheduling does not use this contract yet.
     - Future M3 obligation: Concurrent roster registration vs future staff scheduling must receive dedicated MySQL/Testcontainers concurrency proof in Milestone 3 before the PR #102 scheduling guard is replaced (`REQUIRED_IN_M3`).
   - Scheduling state:
     - PR #102 temporary fail-closed scheduling guard remains `COMPLETE`.
     - `DRAFT -> SCHEDULED` is still fail-closed with HTTP 409 Conflict. These foundations are NOT wired into `DRAFT -> SCHEDULED` yet.
   - REST routes added: 0 (total MF-02 routes remain 57).

4. **PR #104 — `feat(mf02): expose campaign configuration rest api`**
   - reviewed source: `eb41c2210fa8fdce80a8524746386b9c341ff2ec`
   - candidate development base: `d8d7c3afcea889e9e65b6c188afbc23ef7bde548`
   - actual merge Parent 1: `d8d7c3afcea889e9e65b6c188afbc23ef7bde548`
   - actual merge Parent 2: `eb41c2210fa8fdce80a8524746386b9c341ff2ec`
   - merge: `6454a8faf2fe7a511f324deb5227c34d6197d81c`
   - merged at: `2026-08-20T06:45:46Z`
   - merge method: normal merge commit (two parents: `d8d7c3afcea889e9e65b6c188afbc23ef7bde548` and `eb41c2210fa8fdce80a8524746386b9c341ff2ec`)
   - integration drift: 0
   - Backend CI: PASS (953 H2 verify in combined merge context + 4 Testcontainers MySQL verify on `mysql:8.4.3`, 0 failures, 0 errors, 0 skipped, `BUILD SUCCESS` in 2m39s)
   - Frontend CI: PASS (13s)
   - authoritative backend baseline at PR #104: **953 functional H2 tests** (101 locally verified Surefire XML reports; +25 over immediate parent baseline 928) and **953 combined merge-context H2 tests** in CI + **4 MySQL assurance tests** across 3 classes
   - net-new executions: **+25 H2 tests** (+8 `CampaignConfigurationDtoMapperTests`, +16 `CampaignConfigurationControllerTests`, +1 `CampaignConfigurationWorkflowIntegrationTests`)
   - scope: 21 files (13 new production, 5 modified production, 3 new test files; +1505 / -21 lines)
   - integrated endpoints under `/api/v1/campaigns/{id}` (12):
     1. `POST /api/v1/campaigns/{id}/dose-programmes` (Create initial dose programme; 201 Created + `Location: /api/v1/campaigns/{id}/dose-programmes`)
     2. `POST /api/v1/campaigns/{id}/dose-programmes/versions` (Create new dose programme version; 201 Created + `Location: /api/v1/campaigns/{id}/dose-programmes`)
     3. `GET /api/v1/campaigns/{id}/dose-programmes` (Get dose programme version history, ordered by `versionNumber` DESC; 200 OK)
     4. `POST /api/v1/campaigns/{id}/questionnaire-assignments` (Assign published questionnaire version; 201 Created + `Location: /api/v1/campaigns/{id}/questionnaire-assignments`)
     5. `GET /api/v1/campaigns/{id}/questionnaire-assignments` (Get questionnaire assignment history, ordered by `activeFrom` DESC, `id` DESC; 200 OK)
     6. `POST /api/v1/campaigns/{id}/center-assignments` (Create campaign-center assignment; 201 Created + `Location: /api/v1/campaigns/{id}/center-assignments/{id}`)
     7. `GET /api/v1/campaigns/{id}/center-assignments` (Get campaign-center assignments, ordered by `activeFrom` ASC, `id` ASC; 200 OK)
     8. `PUT /api/v1/campaigns/{id}/center-assignments/{assignmentId}` (Update mutable future campaign-center assignment; 200 OK)
     9. `POST /api/v1/campaigns/{id}/center-assignments/{assignmentId}/end-date` (End currently effective campaign-center assignment early; 200 OK)
     10. `POST /api/v1/campaigns/{id}/center-assignments/{assignmentId}/cancel` (Cancel mutable future campaign-center assignment; 200 OK)
     11. `POST /api/v1/campaigns/{id}/location-targets` (Add administrative location target; 201 Created + `Location: /api/v1/campaigns/{id}/location-targets`)
     12. `GET /api/v1/campaigns/{id}/location-targets` (Get location targets with optional `?type=` filter, ordered by `targetType` ASC, `targetCode` ASC, `id` ASC; 200 OK)
   - audit finding resolution: Resolves confirmed P1 audit finding MF02-003 (Campaign Configuration API Blocker). Exposes existing domain services `CampaignDoseProgrammeService`, `CampaignQuestionnaireAssignmentService`, `CampaignCenterAssignmentService`, and `CampaignLocationTargetService` through clean REST endpoints.
   - master data boundary: `VaccineType` and `TargetGroup` remain seeded reference data without public administration REST routes (`PUBLIC_WORKFLOW_CLOSED_WITH_SEEDED_MASTER_DATA`).
   - nested ownership & 404 normalization: `CampaignCenterAssignmentService` enforces nested campaign ownership on mutations (`PUT`, `end-date`, `cancel`), throwing `ResourceNotFoundException` (HTTP 404) on cross-campaign assignment mismatches. Genuine missing resources normalize to 404, while invalid business arguments remain 400 and lifecycle/overlap conflicts remain 409.
   - deterministic ordering: Enforced at repository query level across all configuration history lists.
   - cross-module boundary: MF-01 questionnaire publication lifecycle remains strictly behind `QuestionnaireVersionPublicationPort`; 0 direct MF-01 repository access. 0 MF-03 implementation added.

5. **PR #102 — `fix(mf02): guard session scheduling until operational readiness`**
   - reviewed source: `d7663abeb39dd76861e817cbf750b9de7d5d70d5`
   - merge: `616636762687e5c2497db8e0227ee8692311703c`
   - behavior: The public route `POST /api/v1/sessions/{id}/schedule` remains available. An otherwise-valid DRAFT session schedule attempt fails closed with `IllegalStateException` -> `HTTP 409 Conflict` and message `"Session scheduling is unavailable until operational resource and capacity readiness can be validated"`. The persisted session remains `DRAFT`.
   - audit finding resolution: Addresses confirmed P1 audit finding MF02-001 (Scheduling). Scheduling is intentionally fail-closed rather than inventing unvalidated state transitions or production bypasses.

6. **PR #100 — `feat(mf02): implement recurrence rule and series rest api`**
   - reviewed source: `f5c385ae7d43a17e60a2ee7647fe96f7cfa0446c`
   - merge: `684a4d6bffbe1d1b21f10ad9fa141b62fb23625a`
   - integrated endpoints under `/api/v1/recurrence-rules` (9): `createRule`, `getRuleById`, `getRules`, `replaceRule`, `generateOccurrences`, `previewSeriesChange`, `confirmSeriesChange`, `previewSeriesCancellation`, `confirmSeriesCancellation`. Qualified as `COMPLETE_WITH_MF03_RUNTIME_CONSTRAINT`.

7. **PR #97 — `fix(mf02): harden pre-recurrence correctness and safety`**
   - reviewed source: `ff60fb065bba498a8da05f41c1d3e1e56791c3c6`
   - merge: `e83e54bff1f2c682fbdb6d760856bde993faf162`
   - remediation contents: F-01 (Early session start prevention), F-02 (366-day recurrence safety horizon), F-03 (Campaign service-date consistency), F-04 (Recurrence/series missing-resource HTTP 404 normalization), MySQL series concurrency assurance (`SessionSeriesMySqlConcurrencyIntegrationTests`).

8. **PR #92 — `feat(mf02): add session change control REST API`**
   - reviewed source: `ccf5c3d58035d35e56d316207848d58388446547`
   - merge: `6ff63bdd04f34d3bd334d011fdf55dee8010a140`
   - integrated endpoints under `/api/v1/sessions` (7): `cancellation-preview`, `cancel`, `reschedule-preview`, `reschedule`, `transfer-preview`, `transfer`, `change-events`. Qualified as `COMPLETE_WITH_MF03_RUNTIME_CONSTRAINT`.

9. **PR #90 — `feat(mf02): implement vaccination session core and lifecycle rest api`**
   - reviewed source: `3324ab4b2a84960dea946c531115b5aee829ad61`
   - merge: `9cc7a606260e24e243090f70f5e957b0544bfa3b`
   - integrated endpoints under `/api/v1/sessions` (6): `createSession`, `getSessionById`, `getSessions`, `scheduleSession`, `startSession`, `completeSession`. Qualified as `COMPLETE_WITH_OPERATIONAL_SCHEDULING_GUARD via PR #102`.

10. **PR #88 — `feat(mf02): add vaccination center management REST API`**
    - merge: `faf455ff6f07d096258e7cd977765e6e9e25b10f`
    - integrated endpoints under `/api/v1/centers` (14): Center Master (6), Operating Hours (4), Center Closures (4).

11. **PR #86 — `feat(mf02): add campaign management REST API`**
    - merge: `a9a71114738ce8aed6c1f0cff8a8f42d955a033c`
    - integrated endpoints under `/api/v1/campaigns` (9): Campaign Master & Lifecycle transitions.

## 3. Implemented MF-02 Backend Scope

The repository contains substantial implemented MF-02 backend behavior:

### Campaign configuration
- `VaccineType` (seeded reference data)
- `TargetGroup` (seeded reference data)
- `VaccinationCampaign`
- `CampaignDoseProgramme`
- `CampaignLocationTarget`
- `CampaignQuestionnaireAssignment`
- effective-history validation and mutation protection
- deterministic version and assignment history ordering
- campaign publication prerequisites
- campaign lifecycle state-integrity hardening
- `DRAFT -> SCHEDULED`
- `DRAFT/SCHEDULED -> PUBLISHED`
- `PUBLISHED -> ACTIVE`
- `PUBLISHED/ACTIVE -> SUSPENDED`
- `SUSPENDED -> PUBLISHED`
- `SUSPENDED -> ACTIVE`

### Center planning
- `VaccinationCenter`
- explicit normalized scheduling timezone
- `OperatingHours`
- `CenterClosure`
- `CampaignCenterAssignment` (with nested campaign ownership enforcement)
- center/hours/closure reverse-impact protection
- center and campaign pessimistic serialization where required

### Center operational resources & staff roster (Milestone 2A — PR #108)
- `CenterOperationalResourceController`, `CreateOperationalResourceRequest`, `UpdateOperationalResourceRequest`, `OperationalResourceResponse`, `OperationalResourceDtoMapper`
- `CenterStaffRosterController`, `RegisterStaffRosterRequest`, `UpdateStaffRosterRequest`, `OperationalStaffRosterResponse`, `OperationalStaffRosterDtoMapper`
- `OperationalResourceStatus` canonical correction (`ACTIVE`, `MAINTENANCE`, `INACTIVE`)
- Pessimistic write locking on resource and roster row updates (`findByIdForUpdate`)
- Narrow mapping of duplicate key database violations to HTTP 409 Conflict

### Session operational assignments (Milestone 2B — PR #110)
- `SessionOperationalAssignmentController`, `CreateSessionResourceAssignmentRequest`, `CreateStaffAssignmentRequest`, `SessionResourceAssignmentResponse`, `StaffAssignmentResponse`, `SessionResourceAssignmentDtoMapper`, `StaffAssignmentDtoMapper`
- `SessionResourceAssignmentService`: DRAFT-only session resource assignment mutation (create/delete), lifecycle-independent reads, session window containment, active resource verification, same-center verification, temporal interval overlap detection, pessimistic write locking (`findByIdForUpdate`)
- `StaffAssignmentService`: DRAFT-only session staff assignment mutation (create/delete), lifecycle-independent reads, session window containment, active roster verification, same-center verification, role qualification (`StaffRole` matches roster role), global user roster locking (`findByUserIdForUpdateOrderByIdAsc`), post-lock pessimistic current-read overlap query (`findOverlappingAssignmentsForUpdate`)
- `StaffAssignmentRepository`: added `findOverlappingAssignmentsForUpdate` with `@Lock(LockModeType.PESSIMISTIC_WRITE)` ensuring current-read visibility under MySQL/InnoDB `REPEATABLE READ` without target-first lock inversion risk
- `SessionOperationalAssignmentMySqlConcurrencyIntegrationTests`: real MySQL 8.4.3 Testcontainers concurrency assurance proving serialization of overlapping same-resource assignments and cross-center same-user staff assignments

### Recurrence and sessions
- `SessionRecurrenceRule` (hardened in PR #97 with 366-day inclusive safety horizon `MAX_RECURRENCE_HORIZON_DAYS`)
- `VaccinationSession` (hardened in PR #97 with Clock-based early-start rejection; scheduling fail-closed in PR #102)
- authoritative session schedule validator
- one-off DRAFT creation
- recurrence occurrence generation (bounded by 366-day horizon)
- deterministic occurrence identity
- retry/idempotency behavior
- DST-aware conversion
- individual reschedule / center transfer / cancellation
- series-wide preview / confirmation (with 366-day horizon and SHA-256 preview fingerprint)
- full-series cancellation
- append-only-style `SessionChangeEvent`
- stale-preview SHA-256 fingerprints
- terminal-state planning guard
- SUSPENDED new-planning freeze

### Session operational core model & calculation engine (Milestone 1 — PR #106)
- `OperationalResource`, `OperationalResourceStatus`, `ResourceType`
- `SessionResourceAssignment`
- `OperationalStaffRoster`, `OperationalStaffStatus`, `StaffRole`
- `StaffAssignment`
- `SessionCapacityPlan`
- `OccupancyInterval`, `OccupancyEvaluationResult`
- `SessionCapacityCalculationService` (recommended throughput, effective station count, approved capacity, override rules, ceiling snapshot)
- `OperationalResourceService`
- `OperationalStaffRosterService`
- Global staff roster locking query `findByUserIdForUpdateOrderByIdAsc(userId)` (PESSIMISTIC_WRITE)

### Session lifecycle
The current session lifecycle foundation includes controlled DRAFT/SCHEDULED/IN_PROGRESS/COMPLETED/CANCELLED behavior, prevents start attempts prior to `startAt`, and guards DRAFT -> SCHEDULED transitions with HTTP 409 Conflict until operational readiness can be evaluated in Milestone 3.

## 4. Campaign Lifecycle Contracts

### 4.1 Campaign `DRAFT -> SCHEDULED` Contract — PR #71
Internal campaign scheduling approval under `VaccinationCampaignService.scheduleCampaign(Long campaignId)`.

### 4.2 Campaign `PUBLISHED -> ACTIVE` Activation Contract — PR #80
Authoritative campaign activation according to Canonical R9 Appendix E.5 under `VaccinationCampaignService.activateCampaign(Long campaignId)`.

### 4.3 Campaign `SUSPENDED -> PUBLISHED / ACTIVE` Resumption Contract — PR #82
Authoritative campaign resumption according to Canonical R9 Appendix E.5 under `VaccinationCampaignService.resumeCampaign(Long campaignId, String reason, String actorId)`.

## 5. Locking and Transaction Conventions

Preserve established parent-first ordering and authoritative post-lock refresh.

Common lock paths include:
- campaign configuration writes: Campaign
- center calendar/configuration writes: Center
- recurrence generation: Campaign -> Center -> Rule
- individual same-center session change: Campaign -> Center -> Session
- individual center transfer: Campaign -> Center(min ID) -> Center(max ID) -> Session
- series confirmation: Campaign -> Center -> Rule -> affected Sessions by ascending ID
- campaign scheduling: Campaign
- campaign activation: Campaign
- campaign resumption: Campaign
- session scheduling guard: Campaign -> Session
- resource row update: `OperationalResource` via `findByIdForUpdate`
- staff roster row update: `OperationalStaffRoster` via `findByIdForUpdate`
- session resource assignment write: `VaccinationSession` -> `OperationalResource` via `findByIdForUpdate` -> overlap check -> save
- session staff assignment write: `VaccinationSession` -> resolve target userId non-locking -> global user roster set via `findByUserIdForUpdateOrderByIdAsc(userId)` in deterministic ID order -> locked roster qualification -> current-read overlap check via `findOverlappingAssignmentsForUpdate` (PESSIMISTIC_WRITE) -> save
- staff roster cross-center lock query: `findByUserIdForUpdateOrderByIdAsc(userId)` (PESSIMISTIC_WRITE across all roster rows for the user in ascending ID order)

Critical write services are transactional. Read-only preview operations use read-only transactions where appropriate.

## 6. Current Test / Verification State

### Distinct Verification Checkpoints

1. **Historical Checkpoints:**
   - Pre-Remediation Checkpoint (Base `1b90e2ee...`): **790 functional H2 tests** across 81 XML reports + **3 MySQL assurance tests** (793 logical executions).
   - PR #97 Remediation Candidate Checkpoint: **799 functional H2 tests** (81 local XML reports).
   - PR #97 Combined Merge-Context Checkpoint: **835 functional H2 tests** executed in CI (incorporating 36 intervening MF-01 screening tests).
   - Milestone 5 Immediate Parent Baseline (`8f58ae23...`): **882 functional H2 tests** (835 PR #97 base + 47 unrelated tests landed on develop: 5 MF-01 + 42 MF-03).
   - Milestone 5 Verified Backend Baseline (PR #100 / Merge `684a4d6...`): **926 functional H2 tests** (+44 net-new tests over parent baseline 882) + **4 MySQL assurance tests**.
   - Session Scheduling Guard Verified Backend Baseline (PR #102 / Merge `6166367...`): **928 functional H2 tests** (+2 net-new tests over parent baseline 926) + **4 MySQL assurance tests**.
   - Campaign Configuration REST Verified Backend Baseline (PR #104 / Merge `6454a8f...`): **953 functional H2 tests** (+25 net-new tests over immediate parent baseline 928) + **4 MySQL assurance tests**.
   - Session Operational Core Model and Calculation Engine Verified Backend Baseline (PR #106 / Merge `888f9d4...`): **998 functional H2 tests** (+45 net-new tests over immediate parent baseline 953) + **4 MySQL assurance tests**.
   - Center Operational Resource and Staff Roster REST API Verified Backend Baseline (PR #108 / Merge `6edcaca...`): **1041 functional H2 tests** (+43 net-new tests over immediate parent baseline 998) + **4 MySQL assurance tests**.

2. **Session Operational Assignments REST API Verified Backend Baseline (PR #110 / Merge `1c7a09c...`):**
   - **Candidate-local functional H2 baseline:** **1080 functional H2 tests** across locally verified Surefire XML reports (0 failures, 0 errors, 0 skipped; +39 net-new tests over immediate parent baseline 1041).
   - **Fresh PR #110 merge-context Backend CI:** **1080 functional H2 tests** (0 failures, 0 errors, 0 skipped, `BUILD SUCCESS` in 2m16s; Run 32462530177, Job 96712222310).
   - **Net-new candidate executions (+39 H2 tests across 3 test files):**
     - +20 `SessionOperationalAssignmentControllerTests`
     - +9 `SessionResourceAssignmentServiceIntegrationTests`
     - +10 `StaffAssignmentServiceIntegrationTests`
   - **MySQL CI:** **6 / 6 PASS** across 4 classes on `mysql:8.4.3` (+2 net-new M2B operational assignment tests in `SessionOperationalAssignmentMySqlConcurrencyIntegrationTests`).
   - **Frontend CI:** PASS (15s; Run 32462530139, Job 96712222180).

### Authoritative MySQL Concurrency Assurance Checkpoint

Fresh GitHub Actions CI executed **6 tests across 4 classes** under `-Pmysql-concurrency` on `mysql:8.4.3`:
- `CampaignLifecycleMySqlConcurrencyIntegrationTests`: 2 tests (0 failures, 0 errors, 0 skipped)
- `CampaignReverseImpactMySqlConcurrencyIntegrationTests`: 1 test (0 failures, 0 errors, 0 skipped)
- `SessionSeriesMySqlConcurrencyIntegrationTests`: 1 test (0 failures, 0 errors, 0 skipped)
- `SessionOperationalAssignmentMySqlConcurrencyIntegrationTests`: 2 tests (0 failures, 0 errors, 0 skipped: 1 resource assignment concurrency test + 1 staff assignment cross-center concurrency test)
- **Total:** **6 / 6 PASS** (0 failures, 0 errors, 0 skipped).
- *Supported Claim:* Representative single-row lifecycle, parent/child reverse-impact, series generation, and operational resource/staff assignment concurrency serialization is validated against real MySQL/InnoDB in CI on `mysql:8.4.3`. Exact low-level JDBC blocking timing is not instrumented, and non-concurrency domain paths remain verified by the fast H2 suite. Dedicated MySQL concurrency proof for concurrent roster registration versus future staff scheduling is required in Milestone 3.

### Test Architecture
- Fast in-memory H2 default suite in MySQL compatibility mode.
- Dedicated production-equivalent MySQL 8.4.3 Testcontainers suite via `AbstractMySqlTestContainersIntegrationTest` under Maven profile `-Pmysql-concurrency`.
- Separate validation paths: H2 functional tests and MySQL concurrency assurance tests are kept strictly distinct.

### Baseline reconciliation history
- PR #71: **591**
- PR #75: **599** (+8 from `SessionSeriesPreviewCalculatorTests`)
- PR #80: **616** (+17 net-new)
- PR #82: **645** (+29 net-new)
- PR #84: **645 functional H2 + 3 MySQL** (**648 logical executions**)
- PR #86: **680 functional H2 + 3 MySQL** (**683 logical executions**; +35 H2)
- PR #88: **724 functional H2 + 3 MySQL** (**727 logical executions**; +44 H2)
- PR #90: **763 functional H2 + 3 MySQL** (**766 logical executions**; +39 H2)
- PR #92: **790 functional H2 + 3 MySQL** (**793 logical executions**; +27 H2)
- PR #97: **799 candidate functional H2 (835 combined merge-context H2) + 4 MySQL** (**803 logical candidate executions / 839 logical combined merge executions**)
- PR #100: **926 functional H2 + 4 MySQL** (**930 logical executions**; 882 immediate parent baseline + 44 candidate net-new)
- PR #102: **928 functional H2 + 4 MySQL** (**932 logical executions**; 926 immediate parent baseline + 2 candidate net-new)
- PR #104: **953 functional H2 + 4 MySQL** (**957 logical executions**; 928 immediate parent baseline + 25 candidate net-new)
- PR #106: **998 functional H2 + 4 MySQL** (**1002 logical executions**; 953 immediate parent baseline + 45 candidate net-new)
- PR #108: **1041 functional H2 + 4 MySQL** (**1045 logical executions**; 998 immediate parent baseline + 43 candidate net-new)
- PR #110: **1080 functional H2 + 6 MySQL** (**1086 logical executions**; 1041 immediate parent baseline + 39 candidate net-new H2 + 2 net-new MySQL)

Do not resurrect 566, 575, 599, 616, 645, 680, 724, 763, 790, 799, 835, 882, 926, 928, 953, 998, or 1041 as authoritative current evidence (they are historical baselines).

## 7. Architecture / Maintainability Audit — Current Repository Snapshot

The current repository maintains a **layered modular monolith**.

Verified component sizes after PR #110:
- `SessionOperationalAssignmentController.java`: ~136 lines
- `SessionResourceAssignmentService.java`: ~164 lines
- `StaffAssignmentService.java`: ~181 lines
- `SessionResourceAssignmentDtoMapper.java`: ~31 lines
- `StaffAssignmentDtoMapper.java`: ~30 lines
- `CenterOperationalResourceController.java`: ~88 lines
- `CenterStaffRosterController.java`: ~89 lines
- `OperationalResourceDtoMapper.java`: ~30 lines
- `OperationalStaffRosterDtoMapper.java`: ~29 lines
- `OperationalResourceService.java`: ~217 lines
- `OperationalStaffRosterService.java`: ~204 lines
- `CampaignConfigurationController.java`: ~250 lines
- `CampaignConfigurationDtoMapper.java`: ~85 lines
- `RecurrenceRuleController.java`: ~174 lines
- `RecurrenceRuleDtoMapper.java`: ~160 lines
- `SessionSeriesChangeService.java`: ~694 lines
- `SessionSeriesPreviewCalculator.java`: ~456 lines
- `VaccinationSessionService.java`: ~869 lines
- `VaccinationCampaignService.java`: ~428 lines
- `CampaignCenterAssignmentService.java`: ~376 lines
- `OperatingHoursService.java`: ~488 lines
- `CenterClosureService.java`: ~424 lines
- `VaccinationCenterService.java`: ~245 lines
- `CenterController.java`: ~228 lines
- `CampaignController.java`: ~184 lines
- `SessionController.java`: ~188 lines
- `SessionDtoMapper.java`: ~98 lines
- `SessionCapacityCalculationService.java`: ~122 lines
- `SessionCapacityPlan.java`: ~423 lines

Current MF-02 production Java files under the campaign module: 168.
Current MF-02 test Java files in the campaign test package: 104.

Remaining bounded maintainability targets:
1. Evaluation/focused decomposition of `VaccinationSessionService` (~869 lines) if future evidence justifies it (`DEFER`).
2. Duplicated preview/impact/fingerprint/event-construction helpers across session services.

## 8. Production-Layer Gaps

The following summarizes the current production-layer status:

- MF-02 Campaign REST API (9 endpoints) — **RESOLVED in PR #86**
- MF-02 VaccinationCenter REST API (14 endpoints) — **RESOLVED in PR #88**
- MF-02 VaccinationSession Core REST API (6 endpoints) — **RESOLVED in PR #90 (COMPLETE_WITH_OPERATIONAL_SCHEDULING_GUARD via PR #102)**
- MF-02 VaccinationSession Change Control REST API (7 endpoints) — **RESOLVED in PR #92 (COMPLETE_WITH_MF03_RUNTIME_CONSTRAINT)**
- Pre-Recurrence Correctness and Safety Remediation — **RESOLVED in PR #97**
- MF-02 VaccinationSession Recurrence / Series REST API (9 endpoints) — **RESOLVED in PR #100 (COMPLETE_WITH_MF03_RUNTIME_CONSTRAINT)**
- Session Operational Prerequisite Guard — **RESOLVED in PR #102**
- Campaign Configuration REST API (12 endpoints) — **RESOLVED in PR #104 (PUBLIC_WORKFLOW_CLOSED_WITH_SEEDED_MASTER_DATA)**
- Session Operational Core Model and Calculation Engine (Milestone 1) — **RESOLVED in PR #106 (COMPLETE)**
- Center Operational Resources & Staff Roster REST API (Milestone 2A; 8 endpoints) — **RESOLVED in PR #108 (COMPLETE)**
- Session Resource & Staff Assignment REST API (Milestone 2B; 8 endpoints) — **RESOLVED in PR #110 (COMPLETE)**
- Session Capacity Plan REST API (Milestone 2C) — **PENDING_M2C**
- Session Operational Configuration REST API (Milestone 2 Overall) — **IN_PROGRESS**
- Session Operational Readiness & Guard Replacement (Milestone 3) — **PENDING_M3**
- MF02/MF03 Slot Capacity Integration (Milestone 4) — **PENDING_M4**
- Blocked lifecycle endpoints (`complete`, `cancel`, `archive`) (pending specification/integration)
- Spring Security/RBAC/authentication enforcement (pending)
- OpenAPI documentation (pending)
- Flyway business migrations (pending)
- representative production MySQL Testcontainers assurance (RESOLVED in PR #84, PR #97, & PR #110; broad production database certification deferred)
- MF-02 frontend business screens (pending)
- frontend automated tests (pending)
- production MF-03 impact adapter (pending)
- reliable transactional outbox delivery (pending)

## 9. Known Issues / Risks to Resolve Before More Lifecycle Growth

### 9.1 Campaign questionnaire-version invariant — RESOLVED (PR #73)
Defensively enforces `MF02-BR-02` revalidating `QuestionnaireVersionPublicationPort.isPublished(...)` under campaign lock.

### 9.2 Oversized orchestration services — PARTIALLY RESOLVED (PR #75)
`SessionSeriesChangeService` decomposed via `SessionSeriesPreviewCalculator`. `VaccinationSessionService` maintainability review remains `DEFER`.

### 9.3 Optional critical integration ports
Optional injection boundaries remain for `QuestionnaireVersionPublicationPort` (production bean provided by MF-01 `QuestionnaireVersionService`) and `SessionImpactReader` (MF-03 port; adapter missing).

### 9.4 Timing-based concurrency test — RESOLVED (PR #78)
Eliminated `Thread.sleep(300)` in `CampaignAssignmentReverseImpactConcurrencyIntegrationTests` with deterministic latches. 0 `Thread.sleep` calls remain under `backend/src/test`.

### 9.5 Representative MySQL Testcontainers concurrency assurance — RESOLVED (PR #84, PR #97, & PR #110)
- Containerized MySQL 8.4.3 InnoDB integration tests via Testcontainers under profile `-Pmysql-concurrency`.
- PR #84 validated single-row lifecycle duplicate-command race serialization and parent/child reverse-impact locking.
- PR #97 added `SessionSeriesMySqlConcurrencyIntegrationTests` validating concurrent series confirmation versus occurrence generation.
- PR #110 added `SessionOperationalAssignmentMySqlConcurrencyIntegrationTests` validating concurrent session resource assignment and cross-center staff assignment serialization.
- Total suite in CI: 6 tests across 4 classes on `mysql:8.4.3`.

### 9.6 Append-only audit limitation
Lifecycle/change repositories inherit normal repository delete operations. Domain/API convention rather than deletion-proof database enforcement.

### 9.7 Pre-Recurrence Correctness and Safety Remediation — RESOLVED (PR #97)
Prerequisite engineering review findings resolved: early session start rejection (F-01), 366-day recurrence safety horizon (F-02), campaign service date consistency (F-03), missing-resource HTTP 404 normalization (F-04), and MySQL series concurrency assurance.

### 9.8 Recurrence and Series REST API Milestone 5 — RESOLVED (PR #100)
Exposed 9 public REST routes under `/api/v1/recurrence-rules` matching Prompt 115 frozen design. Preserved `cancelRule()` as internal only, while exposing impact-aware preview/confirm series cancellation. Qualified as `COMPLETE_WITH_MF03_RUNTIME_CONSTRAINT`.

### 9.9 Session Operational Prerequisite Guard — RESOLVED (PR #102)
Resolved confirmed P1 audit finding MF02-001 (Scheduling). `POST /api/v1/sessions/{id}/schedule` fails closed with HTTP 409 Conflict while physical resource, staff assignment, and capacity planning models are absent, preserving `DRAFT` persistence state.

### 9.10 Campaign Configuration REST API — RESOLVED (PR #104)
Resolved confirmed P1 audit finding MF02-003 (Campaign Configuration API Blocker). Exposed 12 public REST routes under `/api/v1/campaigns/{id}` covering dose programmes, questionnaire assignments, campaign-center assignments, and location targets with deterministic ordering and nested ownership checks. Qualified as `PUBLIC_WORKFLOW_CLOSED_WITH_SEEDED_MASTER_DATA`.

### 9.11 Session Operational Core Model, Staff Concurrency Foundation, and Capacity Semantics — RESOLVED (PR #106)
- **Operational Core Models:** Implemented entities, repositories, and domain services for center-owned physical resources (`OperationalResource`), center operational staff roster (`OperationalStaffRoster`), session resource assignments (`SessionResourceAssignment`), session staff assignments (`StaffAssignment`), and session capacity plans (`SessionCapacityPlan`).
- **Calculation Engine:** Implemented pure calculations for recommended throughput (`recommendedThroughput = effectiveStationCount * floor(durationMinutes / expectedServiceMinutes)`), effective station count (`min(assignedVaccinatorCount, availableStationCount, fullyEquippedStationCount)` where `fullyEquippedStationCount` is an explicit scalar snapshot/input), approved capacity (`approvedCapacity <= sessionServiceCeiling` when ceiling configured; overrideReason required if exceeding recommended throughput), and interval occupancy evaluation (`OccupancyInterval`, `OccupancyEvaluationResult`).
- **Global Staff Roster Locking Foundation:** `OperationalStaffRoster` is center-scoped (`unique(center_id, user_id)`), while `StaffAssignment` overlap detection is global by external `userId`. Repository provides `findByUserIdForUpdateOrderByIdAsc(userId)` with `PESSIMISTIC_WRITE` covering all roster rows for the user in deterministic ascending ID order. Safe claim: MF-02 has a deterministic global pessimistic-lock repository foundation for cross-center staff scheduling. Live scheduling concurrency is `NOT_YET_PROVEN` as live scheduling does not use this contract yet.
- **Future M3 Concurrency Obligation:** Dedicated MySQL/Testcontainers concurrency proof for concurrent roster registration versus future staff scheduling is required in Milestone 3 before the PR #102 scheduling guard is replaced (`REQUIRED_IN_M3`).
- **Scheduling Guard Preserved:** PR #102 fail-closed guard remains in effect; `DRAFT -> SCHEDULED` is still rejected with HTTP 409 Conflict until Milestone 3 readiness integration.

### 9.12 Center Operational Resource & Staff Roster REST API — RESOLVED (PR #108)
- **Exposed 8 REST routes under `/api/v1/centers/{centerId}`:** 4 operational resource endpoints and 4 operational staff roster endpoints.
- **OperationalResourceStatus Canonical Correction:** Authoritative enum contains `ACTIVE`, `MAINTENANCE`, `INACTIVE`. `MAINTENANCE` was the canonical Section 6.6 value missing from M1 and was added in M2A.
- **Pessimistic Update Locking:** Update paths for resources and roster entries execute under pessimistic row locks (`findByIdForUpdate`).
- **Duplicate Protection:** Database uniqueness constraints `unique(center_id, code)` and `unique(center_id, user_id)` are enforced. Uniqueness race exceptions map narrowly to HTTP 409 Conflict.
- **Preserved History:** Existing assignment rows (`SessionResourceAssignment`) and staff snapshot records (`StaffAssignment`) are preserved during registry updates.
- **Nested Ownership:** Mismatches between path `centerId` and resource/roster parent center normalize to HTTP 404 Not Found.

### 9.13 Session Operational Assignments REST API & MySQL Concurrency Assurance — RESOLVED (PR #110)
- **Exposed 8 REST routes under `/api/v1/sessions/{sessionId}`:** 4 session resource assignment endpoints (`POST`, `GET` list, `GET` by ID, `DELETE`) and 4 session staff assignment endpoints (`POST`, `GET` list, `GET` by ID, `DELETE`).
- **DRAFT-Only Mutation Invariant:** Assignment creation and deletion are strictly constrained to `DRAFT` sessions; non-DRAFT mutation attempts reject with `IllegalStateException` (HTTP 409 Conflict). Read operations remain lifecycle-independent.
- **Interval and Qualification Bounds:** Assignment time intervals `[startAt, endAt)` are strictly bounded within session operating windows, validated with half-open collision checks, and require `ACTIVE` resources/staff roster entries belonging to the same center with matching `StaffRole` qualifications.
- **MySQL REPEATABLE READ Snapshot Resolution:** Concurrency testing on real MySQL 8.4.3 exposed that initial non-locking entity reads establish MVCC consistent-read snapshots that prevent unblocked transactions from seeing newly committed overlapping assignments. Resolved by introducing `findOverlappingAssignmentsForUpdate` with `@Lock(LockModeType.PESSIMISTIC_WRITE)` in `StaffAssignmentRepository`, ensuring a current-read check occurs immediately after acquiring global user roster locks without introducing target-first deadlock risk.
- **MySQL Assurance Suite Growth:** Added `SessionOperationalAssignmentMySqlConcurrencyIntegrationTests` with 2 tests verifying serialization of concurrent overlapping assignments on real MySQL 8.4.3 InnoDB (suite total: 6 / 6 PASS).

## 10. SUSPENDED Planning Policy

Project policy S3 is implemented: **NEW PLANNING FROZEN, REMEDIATION ALLOWED**.

## 11. Remaining Campaign Lifecycle Work

Still unresolved or unimplemented:
- `-> COMPLETED`: `SPECIFICATION_PARTIAL`. Needs authoritative operational algorithm for "service work closed".
- `-> CANCELLED`: `MF03_BLOCKED`. Requires truthful MF-03 impact integration without silent appointment mutation.
- `COMPLETED/CANCELLED -> ARCHIVED`: `USER_DATA_REQUIRED`. Retention duration/policy missing.
- DRAFT campaign / child session scheduling: `IMPLEMENTATION_DEFINED / unresolved policy`.

## 12. MF-02 Ownership Boundaries

- MF-01 owns screening questions, questionnaire versions, eligibility rules, submissions, answers, and assessments.
- MF-02 owns campaigns, center planning, operating calendars, recurrence, sessions, resources/capacity, schedule changes, and operational utilization.
- MF-03 owns appointments, waitlists, rebooking, and check-in.
- MF-04 owns batches, stock quantities, allocations, and stock movements.
- MF-05 owns vaccination administration records and certificates.

MF-02 may read aggregate impact/readiness information through ports. It must not silently mutate MF-03 appointment/waitlist history.

## 13. Immediate Engineering Order

Recommended next direction (classified as **RECOMMENDATION_ONLY**; implementation not started):
1. synchronize maintained documentation to PR #110 (Milestone 2B) — represented by this step;
2. `NEXT_ENGINEERING_FOCUS`: **MF02_M2C_SESSION_CAPACITY_PLAN_REST_API** (Milestone 2C: Session Capacity Plan REST);
3. `SESSION_OPERATIONAL_READINESS_AND_GUARD_REPLACEMENT` (Milestone 3: readiness evaluation engine, PR #102 guard replacement, and MySQL staff-roster concurrency certification);
4. `MF02_MF03_SLOT_CAPACITY_INTEGRATION` (Milestone 4: `SlotCapacityPort` production adapter and slot generation);
5. complete MF-02/MF-03 capacity-impact integration (`SessionImpactReader` adapter, `SlotCapacityPort` adapter);
6. analyze and resolve remaining campaign lifecycle gaps (completion algorithm, cancellation impact integration, archive retention policy);
7. evaluate/decompose `VaccinationSessionService` maintainability only if justified (`DEFER`);
8. add security/RBAC;
9. add Flyway migrations;
10. implement MF-02 frontend and frontend tests;
11. final end-to-end / Definition-of-Done evidence.

## 14. Historical Milestone Summary

| PR | Milestone |
|---|---|
| #12 | Campaign/dose/target/questionnaire foundations |
| #16 | VaccinationCenter foundation |
| #20 | OperatingHours |
| #24 | CenterClosure |
| #26 | CampaignCenterAssignment |
| #28 | SessionRecurrenceRule |
| #30 | Center scheduling timezone / mutation locking prerequisite |
| #32 | VaccinationSession domain/persistence |
| #34 | Validated one-off session creation |
| #36 | Recurrence occurrence generation |
| #38 | SessionChangeEvent / impact port / session row locking |
| #40 | Individual session change workflows |
| #42 | Series-wide preview |
| #44 | Atomic series-wide confirmation |
| #46 | Session-change regression hardening |
| #48 | Full-series cancellation |
| #50 | Campaign/assignment reverse-impact protection |
| #52 | Center/hours/closure reverse-impact protection |
| #53 | VaccinationSession lifecycle state-machine hardening |
| #55 | Campaign publication |
| #58 | Deterministic reverse-impact test hardening |
| #61 | Campaign lifecycle state-integrity hardening |
| #65 | Campaign suspension |
| #67 | Terminal-state planning guard |
| #69 | SUSPENDED planning freeze |
| #70 | Documentation synchronization after PR #69 |
| #71 | Campaign `DRAFT -> SCHEDULED` approval |
| #72 | Markdown-only canonical authority synchronization |
| #73 | Campaign questionnaire publication invariant hardening |
| #74 | Evidence synchronization after PR #73 |
| #75 | Series preview calculator decomposition refactor |
| #77 | Evidence synchronization after PR #75 |
| #78 | Deterministic reverse-impact concurrency test hardening |
| #79 | Evidence synchronization after PR #78 |
| #80 | Campaign `PUBLISHED -> ACTIVE` activation |
| #81 | Evidence synchronization after PR #80 |
| #82 | Campaign `SUSPENDED -> PUBLISHED / ACTIVE` resumption |
| #83 | Evidence synchronization after PR #82 |
| #84 | Representative MySQL/Testcontainers concurrency assurance |
| #85 | Documentation synchronization after PR #84 |
| #86 | Campaign Management REST API vertical slice (9 endpoints) |
| #87 | Documentation synchronization after PR #86 |
| #88 | Vaccination Center Management REST API vertical slice (14 endpoints) |
| #89 | Documentation synchronization after PR #88 |
| #90 | Vaccination Session Core REST API vertical slice (6 endpoints; COMPLETE_WITH_OPERATIONAL_SCHEDULING_GUARD) |
| #91 | Documentation synchronization after PR #90 |
| #92 | Vaccination Session Change Control REST API vertical slice (7 endpoints; COMPLETE_WITH_MF03_RUNTIME_CONSTRAINT) |
| #93 | Documentation synchronization after PR #92 |
| #97 | Pre-Recurrence Correctness and Safety Remediation (F-01, F-02, F-03, F-04, MySQL assurance) |
| #98 | Documentation synchronization after PR #97 |
| #100 | Vaccination Session Recurrence / Series REST API Milestone 5 (9 endpoints; COMPLETE_WITH_MF03_RUNTIME_CONSTRAINT) |
| #101 | Documentation synchronization after PR #100 |
| #102 | Vaccination Session Operational Prerequisite Guard (COMPLETE) |
| #103 | Documentation synchronization after PR #102 |
| #104 | Campaign Configuration REST API (12 endpoints; COMPLETE_WITH_SEEDED_MASTER_DATA) |
| #105 | Documentation synchronization after PR #104 |
| #106 | Session Operational Core Model and Calculation Engine (Milestone 1; COMPLETE) |
| #107 | Documentation synchronization after PR #106 |
| #108 | Center Operational Resource and Staff Roster REST API (Milestone 2A; 8 endpoints; COMPLETE) |
| #109 | Documentation synchronization after PR #108 |
| #110 | Session Operational Assignments REST API (Milestone 2B; 8 endpoints; COMPLETE) |

Key full merge checkpoints:
- PR #55: `9a470da982f1510213163b3b909a02bd0f85973b`
- PR #61: `d5dff09c6d0ed643cb4537abff64056b7d20656c`
- PR #65: `228cdd6f0f6d72b8e776ab5ca1cf9af30819adc3`
- PR #67: `296c763b48bac4ab837dafeb67cc9788b071912d`
- PR #69: `545075aaedda338a2c16e7d97803704b55fef951`
- PR #70: `c3bf7c2fb03513d8540a78dcc4ab5d93f322f468`
- PR #71: `6d52449db4f29099f3a979e824a7efcae8f4ea54`
- PR #72: `5fe33208d11a21ced5c6c1c520dfe01c13858bf0`
- PR #73: `d3fbb3eb95333d1da0b3e5c33283a1bcf81635e9`
- PR #74: `b8289635054a6f15d8c611e18e86a0e7a0a64687`
- PR #75: `cc13082ad94701475b69bc53b699d897486ff7fc`
- PR #77: `6b8bf9c765aaa577b6c29521539e42acb29f70f9`
- PR #78: `20520c03bdd53329a7fe647a7355beed58ed552e`
- PR #79: `3d21d38777eff40104b46a11773ceeded6d8dece`
- PR #80: `852802ff0d533fdb013c64b4b10de395efd40557`
- PR #81: `ff3d832745708e5d563c63059820e1e157767a60`
- PR #82: `5dd7034df64951f061dc27c5596d17a3cb440061`
- PR #83: `633e6a94585a9549718e16b038ae27e6646347f0`
- PR #84: `34aa7666dc2d37b1b71faf25a7f51cfba10ee26a`
- PR #85: `9d40ddbd55139a28707c3bdb68fb6a222b5b1c28`
- PR #86: `a9a71114738ce8aed6c1f0cff8a8f42d955a033c`
- PR #87: `37574bb02e157971c3ca2250d4953834cfe6d132`
- PR #88: `faf455ff6f07d096258e7cd977765e6e9e25b10f`
- PR #89: `4eaa8cd31c04aedf23f3887f25de27ec924165b8`
- PR #90: `9cc7a606260e24e243090f70f5e957b0544bfa3b`
- PR #91: `1531ad9f89e3dc75b174cc3deb3a2204e2712a52`
- PR #92: `6ff63bdd04f34d3bd334d011fdf55dee8010a140`
- PR #93: `1b90e2ee4567502a5d90d72af28eda4998f554b7`
- PR #97: `e83e54bff1f2c682fbdb6d760856bde993faf162`
- PR #98: `6be1c7f78328c58c02b274db774936d951ef9fc0`
- PR #100: `684a4d6bffbe1d1b21f10ad9fa141b62fb23625a`
- PR #101: `046f475f13bb1e7c734492ae58f3e3bb3eb7f9ff`
- PR #102: `616636762687e5c2497db8e0227ee8692311703c`
- PR #103: `d8d7c3afcea889e9e65b6c188afbc23ef7bde548`
- PR #104: `6454a8faf2fe7a511f324deb5227c34d6197d81c`
- PR #105: `c8d5b452ee88ec97988874e211ffb26ea321b3e1`
- PR #106: `888f9d4b53d5206ff9fdfaadcedaf86092ab62f1`
- PR #107: `e63bb76ac3dbb08b5009976233bf4255a059a712`
- PR #108: `6edcaca67af0dca9d4e0a76bd69b8f180b6e7b85`
- PR #109: `94af45a27636deb4e057c94846086da018da8d9e`
- PR #110: `1c7a09c772018eaf56481aa77ff38688587879fc`

## 15. Status Discipline

Never claim:
- MF-02 complete;
- Sprint 1 complete;
- US-13 complete;
- production API/security/migrations/frontend complete (Campaign REST API, Center REST API, Session Core REST API, Session Change Control REST API, Pre-Recurrence Remediation, Recurrence/Series REST API, Session Scheduling Guard, Campaign Configuration REST API, Session Operational Core Model/Calculation Engine, Center Operational Resource & Staff Roster REST API, and Session Operational Assignments REST API are integrated; security, Flyway, frontend, Capacity Plan REST, and Operational Readiness remain pending);
- MySQL concurrency fully certified across all endpoints;
- 566, 575, 599, 616, 645, 680, 724, 763, 790, 799, 835, 882, 926, 928, 953, 998, or 1041 as the current backend baseline;
- 1086 as the functional H2 baseline (1086 represents logical combined coverage across separate validation paths: 1080 H2 + 6 MySQL).

Current status remains:
- Sprint 1: **IN PROGRESS**
- MF-02: **IN PROGRESS**
- US-13: **IN PROGRESS**
- Campaign Management REST API Milestone 1: **COMPLETE**
- Vaccination Center Management REST API Milestone 2: **COMPLETE**
- Vaccination Session Core REST API Milestone 3: **COMPLETE_WITH_OPERATIONAL_SCHEDULING_GUARD**
- Vaccination Session Change Control REST API Milestone 4: **COMPLETE_WITH_MF03_RUNTIME_CONSTRAINT**
- Pre-Recurrence Correctness and Safety Remediation: **COMPLETE**
- Vaccination Session Recurrence / Series REST API Milestone 5: **COMPLETE_WITH_MF03_RUNTIME_CONSTRAINT**
- Session Operational Prerequisite Guard: **COMPLETE**
- Campaign Configuration REST API: **COMPLETE_WITH_SEEDED_MASTER_DATA**
- Session Operational Core Model (Milestone 1): **COMPLETE**
- Session Operational Calculation Engine (Milestone 1): **COMPLETE**
- Center Operational Resources & Staff Roster REST API (Milestone 2A): **COMPLETE**
- Session Resource & Staff Assignment REST API (Milestone 2B): **COMPLETE**
- Session Capacity Plan REST API (Milestone 2C): **PENDING**
- Session Operational Configuration REST (Milestone 2 Overall): **IN PROGRESS**
- Global staff roster lock foundation: **COMPLETE**
- Live staff scheduling concurrency: **NOT_YET_PROVEN**
- Roster registration vs scheduling MySQL proof: **REQUIRED_IN_M3**
- Session Operational Readiness (Milestone 3): **PENDING_M3**
- Session Scheduling Guard Replacement (Milestone 3): **PENDING_M3**
- MF02/MF03 Slot Capacity Integration (Milestone 4): **PENDING_M4**
- current verified backend baseline: **1080 functional H2 tests plus 6 MySQL Testcontainers assurance tests** across separate validation paths.
- MF02_DOCUMENTATION_M2B_SYNC: **LOCAL_CANDIDATE_READY**

## 16. Startup Procedure for the Next AI Session

1. Read canonical Markdown `3.3-AI / 2026-08-14-R9`.
2. Fetch live `origin/develop`.
3. Verify whether `1c7a09c772018eaf56481aa77ff38688587879fc` remains an ancestor/current checkpoint.
4. Run/inspect current backend tests before changing behavior.
5. Read this handoff and Sprint evidence.
6. Review `MF02_Completion_and_Hardening_Plan.md`.
7. Do not rebuild completed foundations.
8. Resolve known consistency/hardening issues before increasing lifecycle scope.

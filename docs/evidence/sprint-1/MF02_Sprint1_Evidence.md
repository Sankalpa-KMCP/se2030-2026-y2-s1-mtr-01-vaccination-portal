# MF-02 Sprint 1 Evidence Record

**Project:** Web-Based Vaccination Portal (`WBVP-SE2030`)
**Function:** MF-02 — Vaccination Campaign, Center and Session Management
**Canonical authority:** `3.3-AI / 2026-08-14-R9`
**Source authority:** canonical Markdown only
**Sprint:** Sprint 1 — **IN PROGRESS**
**MF-02:** **IN PROGRESS**
**US-13:** **IN PROGRESS**
**Evidence synchronized through implementation:** PR #108
**Latest verified implementation merge:** `6edcaca67af0dca9d4e0a76bd69b8f180b6e7b85`
**Last updated:** 2026-08-21

> This is an implementation/verification evidence record. It is not the semantic requirements source of truth. Historical test counts are checkpoint-scoped and must not be presented as the current baseline.

## 1. Current Verification Snapshot

### PR #108 — `feat(mf02): expose center operational resources and staff roster rest api`

- reviewed source: `5dc5e2be5f30d211bf075afd9323141822c63290`
- candidate development base: `e63bb76ac3dbb08b5009976233bf4255a059a712`
- actual merge Parent 1: `e63bb76ac3dbb08b5009976233bf4255a059a712`
- actual merge Parent 2: `5dc5e2be5f30d211bf075afd9323141822c63290`
- merge: `6edcaca67af0dca9d4e0a76bd69b8f180b6e7b85`
- merged at: `2026-08-21T04:21:37Z`
- merge method: normal merge commit (two parents: `e63bb76ac3dbb08b5009976233bf4255a059a712` and `5dc5e2be5f30d211bf075afd9323141822c63290`)
- reviewed candidate preserved: YES
- integration drift: 0
- Backend CI: PASS (1041 H2 verify in combined merge context + 4 Testcontainers MySQL verify on `mysql:8.4.3`, 0 failures, 0 errors, 0 skipped, `BUILD SUCCESS` in 2m37s; Run 32446414058, Job 96666733750)
- Frontend CI: PASS (14s; Run 32446414084, Job 96666733813)
- production code changed: 15 files (10 new, 5 modified)
- test code changed: 6 files (4 new, 2 modified)
- total files: 21 (+1783 / -59 lines)
- functional H2 test executions: **1041 candidate-local / 1041 combined merge-context** (0 failures, 0 errors, 0 skipped)
- MySQL Testcontainers assurance test executions: **4** (across 3 classes on `mysql:8.4.3`; 0 failures, 0 errors, 0 skipped; existing suite, no new M2A MySQL test added). The existing MySQL/InnoDB concurrency assurance suite remains green with the M2A persistence and REST additions.
- logical combined executions: **1045 candidate / 1045 combined merge-context** (across separate validation paths)
- immediate parent baseline: **998 functional H2** (from PR #106 / PR #107)
- net-new candidate executions: **+43 H2 tests** across 6 test files (+14 `CenterOperationalResourceControllerTests`, +19 `CenterStaffRosterControllerTests`, +3 `OperationalConcurrencyTranslationUnitTests`, +2 `OperationalResourcePersistenceIntegrationTests`, -2 net / +23 additional assertion paths `OperationalResourceServiceIntegrationTests`, +7 `OperationalStaffRosterServiceIntegrationTests`)
- focused operational resource & roster regression executions: **50 / 50 PASS** candidate + full regression suite 1041 / 1041 PASS
- endpoints integrated under `/api/v1/centers/{centerId}` (8):
  1. `POST /api/v1/centers/{centerId}/resources` (Create center operational resource; 201 Created + Location header `/api/v1/centers/{centerId}/resources/{id}`)
  2. `GET /api/v1/centers/{centerId}/resources` (List center operational resources with optional `?resourceType=` filter; returns all statuses `ACTIVE`, `MAINTENANCE`, `INACTIVE` by default; 200 OK)
  3. `GET /api/v1/centers/{centerId}/resources/{resourceId}` (Get center operational resource by ID; 200 OK)
  4. `PUT /api/v1/centers/{centerId}/resources/{resourceId}` (Update operational resource name/status with pessimistic locking; 200 OK)
  5. `POST /api/v1/centers/{centerId}/staff-roster` (Register staff member to center roster; 201 Created + Location header `/api/v1/centers/{centerId}/staff-roster/{id}`)
  6. `GET /api/v1/centers/{centerId}/staff-roster` (List center staff roster with optional `?role=` and `?status=` filters; returns all roles and statuses by default; 200 OK)
  7. `GET /api/v1/centers/{centerId}/staff-roster/{rosterId}` (Get center staff roster entry by ID; 200 OK)
  8. `PUT /api/v1/centers/{centerId}/staff-roster/{rosterId}` (Update staff roster role/status with pessimistic locking; 200 OK)
- total MF-02 routes growth: 57 -> **65** ($9 \text{ Existing Campaign} + 12 \text{ Campaign Configuration} [21 \text{ Campaign-family}] + 14 \text{ Center Existing} + 8 \text{ Center Operational [4 Resources + 4 Staff Roster]} [22 \text{ Center-family}] + 13 \text{ Session} + 9 \text{ Recurrence} = 65$)
- behavior: Public REST exposure of center operational resources and staff roster configuration with pessimistic row locking on updates, unique constraint translation on creates, and strict nested 404 normalization.
- OperationalResourceStatus Canonical Correction: Enum contains `ACTIVE`, `MAINTENANCE`, `INACTIVE`. `MAINTENANCE` was the canonical Section 6.6 value missing from M1 and was added in M2A. No schema migration required (`EnumType.STRING` column).
- StaffRole: Remains `VACCINATOR`, `CHECK_IN_STAFF`, `SUPERVISOR` (3 canonical values).
- Concurrency & Integrity:
  - Updates use pessimistic locking on entity rows (`findByIdForUpdate`).
  - Database uniqueness constraints `unique(center_id, code)` and `unique(center_id, user_id)` remain final authority.
  - Uniqueness constraint violations on concurrent creates/registrations map narrowly to HTTP 409 Conflict.
  - Existing assignment rows (`SessionResourceAssignment`) and staff snapshot records (`StaffAssignment`) are preserved during registry updates.
- Scheduling Guard State: PR #102 fail-closed guard remains `COMPLETE`. `DRAFT -> SCHEDULED` still fails closed with 409 Conflict.

### Preceding Milestone: PR #106 — `feat(mf02): implement session operational core model and calculation engine`

- reviewed source: `46de335ec2654c89417366d65ca439c3ff1c5501` (original M1 commit `e91269927355e3adcaba11eec75e45728d1889d7`, cross-center staff-lock correction `46de335ec2654c89417366d65ca439c3ff1c5501`)
- candidate development base: `c8d5b452ee88ec97988874e211ffb26ea321b3e1`
- actual merge Parent 1: `c8d5b452ee88ec97988874e211ffb26ea321b3e1`
- actual merge Parent 2: `46de335ec2654c89417366d65ca439c3ff1c5501`
- merge: `888f9d4b53d5206ff9fdfaadcedaf86092ab62f1`
- merged at: `2026-08-20T09:58:52Z`
- merge method: normal merge commit (two parents: `c8d5b452ee88ec97988874e211ffb26ea321b3e1` and `46de335ec2654c89417366d65ca439c3ff1c5501`)
- reviewed candidate preserved: YES
- integration drift: 0
- Backend CI: PASS (998 H2 verify in combined merge context + 4 Testcontainers MySQL verify on `mysql:8.4.3`, 0 failures, 0 errors, 0 skipped, `BUILD SUCCESS` in 2m28s / H2 suite 1m25s; Run 32356332347, Job 96386194220)
- Frontend CI: PASS (14s; Run 32356332549, Job 96386194726)
- production code changed: 19 new files (0 modified)
- test code changed: 8 new files (0 modified)
- total files: 27 (+3009 / -0 lines)
- functional H2 test executions: **998 candidate-local / 998 combined merge-context** (0 failures, 0 errors, 0 skipped)
- MySQL Testcontainers assurance test executions: **4** (across 3 classes on `mysql:8.4.3`; 0 failures, 0 errors, 0 skipped; existing suite, no new M1 MySQL test added). The existing MySQL/InnoDB concurrency assurance suite remains green with the M1 persistence additions.
- logical combined executions: **1002 candidate / 1002 combined merge-context** (across separate validation paths)
- immediate parent baseline: **953 functional H2** (from PR #104 / PR #105)
- net-new candidate executions: **+45 H2 tests** across 8 new test files (+1 `OperationalResourceModelTests`, +1 `OperationalResourcePersistenceIntegrationTests`, +1 `OperationalResourceServiceIntegrationTests`, +1 `OperationalStaffRosterPersistenceIntegrationTests`, +1 `StaffAssignmentPersistenceIntegrationTests`, +1 `SessionResourceAssignmentPersistenceIntegrationTests`, +1 `SessionCapacityPlanPersistenceIntegrationTests`, +1 `SessionCapacityCalculationServiceTests`)
- focused operational core regression executions: **45 / 45 PASS** candidate + full regression suite 998 / 998 PASS
- endpoints integrated: 0 new routes added (total MF-02 routes remain **57**)
- behavior: Implementation of session operational core model (resources, staff roster, session resource/staff assignments, capacity plans) and calculation engine (throughput, effective station count, approved capacity, occupancy intervals).
- Capacity Semantics:
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
- Scheduling Guard: PR #102 fail-closed guard remains `COMPLETE`. `DRAFT -> SCHEDULED` still fails closed with 409 Conflict.

### Preceding Milestone: PR #104 — `feat(mf02): expose campaign configuration rest api`

- reviewed source: `eb41c2210fa8fdce80a8524746386b9c341ff2ec`
- candidate development base: `d8d7c3afcea889e9e65b6c188afbc23ef7bde548`
- actual merge Parent 1: `d8d7c3afcea889e9e65b6c188afbc23ef7bde548`
- actual merge Parent 2: `eb41c2210fa8fdce80a8524746386b9c341ff2ec`
- merge: `6454a8faf2fe7a511f324deb5227c34d6197d81c`
- merged at: `2026-08-20T06:45:46Z`
- merge method: normal merge commit (two parents: `d8d7c3afcea889e9e65b6c188afbc23ef7bde548` and `eb41c2210fa8fdce80a8524746386b9c341ff2ec`)
- reviewed candidate preserved: YES
- integration drift: 0
- Backend CI: PASS (953 H2 verify in combined merge context + 4 Testcontainers MySQL verify on `mysql:8.4.3`, 0 failures, 0 errors, 0 skipped, `BUILD SUCCESS` in 2m39s)
- Frontend CI: PASS (13s)
- production code changed: 18 files (13 new, 5 modified)
- test code changed: 3 files (3 new)
- total files: 21 (+1505 / -21 lines)
- functional H2 test executions: **953 candidate-local / 953 combined merge-context** (101 locally verified Surefire XML reports; 0 failures, 0 errors, 0 skipped)
- MySQL Testcontainers assurance test executions: **4** (across 3 classes on `mysql:8.4.3`; 0 failures, 0 errors, 0 skipped; existing suite, no new MySQL test added)
- logical combined executions: **957 candidate / 957 combined merge-context** (across separate validation paths)
- immediate parent baseline: **928 functional H2** (from PR #102 / PR #103)
- net-new candidate executions: **+25 H2 tests** (+8 `CampaignConfigurationDtoMapperTests`, +16 `CampaignConfigurationControllerTests`, +1 `CampaignConfigurationWorkflowIntegrationTests`)
- focused configuration workflow regression executions: **25 / 25 PASS** candidate + full regression suite 953 / 953 PASS
- endpoints integrated under `/api/v1/campaigns/{id}` (12):
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
- total MF-02 routes growth: 45 -> **57** ($9 \text{ Existing Campaign} + 12 \text{ Campaign Configuration} [21 \text{ Campaign-family}] + 14 \text{ Center} + 13 \text{ Session} + 9 \text{ Recurrence} = 57$)
- behavior: Public REST exposure of existing domain services `CampaignDoseProgrammeService`, `CampaignQuestionnaireAssignmentService`, `CampaignCenterAssignmentService`, and `CampaignLocationTargetService` via thin controller `CampaignConfigurationController` and pure DTO mapper `CampaignConfigurationDtoMapper`.
- audit finding resolution: Resolves confirmed P1 audit finding MF02-003 (Campaign Configuration API Blocker).
- master data boundary: `VaccineType` and `TargetGroup` remain seeded reference data without public administration REST routes (`PUBLIC_WORKFLOW_CLOSED_WITH_SEEDED_MASTER_DATA`).
- nested ownership & 404 normalization: `CampaignCenterAssignmentService` enforces nested campaign ownership on mutations (`PUT`, `end-date`, `cancel`), throwing `ResourceNotFoundException` (HTTP 404) on cross-campaign assignment mismatches. Genuine missing resources normalize to 404, while invalid business arguments remain 400 and lifecycle/overlap conflicts remain 409.
- deterministic ordering: Enforced at repository query level across all configuration history lists.
- cross-module boundary: MF-01 questionnaire publication lifecycle remains strictly behind `QuestionnaireVersionPublicationPort`; 0 direct MF-01 repository access. 0 MF-03 implementation added.

### Preceding Milestone: PR #102 — `fix(mf02): guard session scheduling until operational readiness`

- reviewed source: `d7663abeb39dd76861e817cbf750b9de7d5d70d5`
- candidate development base: `046f475f13bb1e7c734492ae58f3e3bb3eb7f9ff`
- actual merge Parent 1: `046f475f13bb1e7c734492ae58f3e3bb3eb7f9ff`
- actual merge Parent 2: `d7663abeb39dd76861e817cbf750b9de7d5d70d5`
- merge: `616636762687e5c2497db8e0227ee8692311703c`
- merged at: `2026-08-20T05:57:32Z`
- merge method: normal merge commit (two parents: `046f475f13bb1e7c734492ae58f3e3bb3eb7f9ff` and `d7663abeb39dd76861e817cbf750b9de7d5d70d5`)
- reviewed candidate preserved: YES
- integration drift: 0
- Backend CI: PASS (928 H2 verify in combined merge context + 4 Testcontainers MySQL verify)
- Frontend CI: PASS
- production code changed: 1 file (`VaccinationSessionService.java` modified)
- test code changed: 4 files (`SessionLifecycleHardeningIntegrationTests.java`, `VaccinationSessionConcurrencyIntegrationTests.java`, `SessionControllerTests.java`, `VaccinationSessionServiceQueryTests.java` modified)
- total files: 5 (+113 / -37 lines)
- functional H2 test executions: **928 candidate-local / 928 combined merge-context** (98 locally verified XML reports; 0 failures, 0 errors, 0 skipped)
- MySQL Testcontainers assurance test executions: **4** (across 3 classes on `mysql:8.4.3`; 0 failures, 0 errors, 0 skipped; existing suite, no new MySQL test added)
- logical combined executions: **932 candidate / 932 combined merge-context** (across separate validation paths)
- immediate parent baseline: **926 functional H2** (from PR #100 / PR #101)
- net-new candidate executions: **+2 H2 tests** (+1 `SessionLifecycleHardeningIntegrationTests`, +1 `VaccinationSessionConcurrencyIntegrationTests`)
- focused scheduling guard regression executions: **129 / 129 PASS** across session lifecycle, query, controller, and concurrency test suites
- endpoints integrated: 0 new routes added/removed (total MF-02 routes remained **45** at PR #102)
- behavior: The public route `POST /api/v1/sessions/{id}/schedule` is retained. An otherwise-valid DRAFT schedule attempt throws `IllegalStateException` -> `HTTP 409 Conflict` with message `"Session scheduling is unavailable until operational resource and capacity readiness can be validated"`. Persisted session lifecycle status remains `DRAFT`.
- audit finding resolution: Addresses confirmed P1 audit finding MF02-001 (Scheduling). Canonical R9 Section E.6 mandates that `DRAFT -> SCHEDULED` requires validated campaign-center assignment, operating hours, resources, capacity, and conflicts. Because authoritative models for physical resources, staff allocation, and capacity plans do not yet exist, scheduling is intentionally fail-closed rather than inventing unvalidated state transitions or production bypasses.
- completion status: Session completion remains classified as `SPECIFICATION_PARTIAL` (not a confirmed code defect; PR #102 made zero changes to `completeSession()`, `VaccinationSession.complete()`, or the completion endpoint).
- runtime constraint distinction: The scheduling operational-readiness guard is strictly distinct from the existing `SessionImpactReader` runtime constraint (which safely guards SCHEDULED change workflows).
- zero production bypass: No test flags, profiles, backdoor setters, or fake readiness exist in production code. Historical SCHEDULED sessions constructed via test fixtures continue to support downstream lifecycle operations (`start`, `complete`, `cancel`, `reschedule`, `transfer`).

### Preceding Milestone: PR #100 — `feat(mf02): implement recurrence rule and series rest api`

- reviewed source: `f5c385ae7d43a17e60a2ee7647fe96f7cfa0446c`
- candidate development base: `8f58ae232323daa456aaafcffa7ee2384795b226`
- actual merge Parent 1: `8f58ae232323daa456aaafcffa7ee2384795b226`
- actual merge Parent 2: `f5c385ae7d43a17e60a2ee7647fe96f7cfa0446c`
- merge: `684a4d6bffbe1d1b21f10ad9fa141b62fb23625a`
- merged at: `2026-08-19T12:20:35Z`
- merge method: normal merge commit (two parents: `8f58ae232323daa456aaafcffa7ee2384795b226` and `f5c385ae7d43a17e60a2ee7647fe96f7cfa0446c`)
- reviewed candidate preserved: YES
- integration drift: 0
- Backend CI: PASS (926 H2 verify in combined merge context + 4 Testcontainers MySQL verify)
- Frontend CI: PASS
- production code changed: 16 files (14 new, 2 modified)
- test code changed: 3 files (3 new, 0 modified existing)
- total files: 19 (+1987 / -0 lines)
- functional H2 test executions: **926 candidate-local / 926 combined merge-context** (98 locally verified XML reports; 0 failures, 0 errors, 0 skipped)
- MySQL Testcontainers assurance test executions: **4** (across 3 classes on `mysql:8.4.3`; 0 failures, 0 errors, 0 skipped)
- logical combined executions: **930 candidate / 930 combined merge-context** (across separate validation paths)
- immediate parent baseline: **882 functional H2** (from base `8f58ae232323daa456aaafcffa7ee2384795b226`; 835 PR #97 base + 47 unrelated tests landed on develop: 5 MF-01 + 42 MF-03)
- net-new candidate executions: **+44 H2 tests** (+24 `RecurrenceRuleControllerTests`, +8 `RecurrenceRuleDtoMapperTests`, +12 `SessionRecurrenceRuleServiceQueryTests`)
- focused recurrence/REST regression executions: **139 / 139 PASS** ($44 \text{ new} + 95 \text{ existing recurrence/series regression}$)
- endpoints integrated under `/api/v1/recurrence-rules` (9): `createRule`, `getRuleById`, `getRules`, `replaceRule`, `generateOccurrences`, `previewSeriesChange`, `confirmSeriesChange`, `previewSeriesCancellation`, `confirmSeriesCancellation`. Qualified as `COMPLETE_WITH_MF03_RUNTIME_CONSTRAINT`.

### Preceding Milestone: PR #97 — `fix(mf02): harden pre-recurrence correctness and safety`

- reviewed source: `ff60fb065bba498a8da05f41c1d3e1e56791c3c6`
- candidate development base: `1b90e2ee4567502a5d90d72af28eda4998f554b7`
- actual merge Parent 1: `fffca9ab7cc3dab0f2aef0a18f8190d49189ab69`
- merge: `e83e54bff1f2c682fbdb6d760856bde993faf162`
- merged at: `2026-08-19T10:16:36Z`
- merge method: normal merge commit (two parents: `fffca9ab7cc3dab0f2aef0a18f8190d49189ab69` and `ff60fb065bba498a8da05f41c1d3e1e56791c3c6`)
- reviewed candidate preserved: YES
- integration drift: 0
- Backend CI: PASS (835 H2 verify in combined merge context + 4 Testcontainers MySQL verify)
- Frontend CI: PASS
- production code changed: 7 files (7 modified)
- test code changed: 10 files (1 new MySQL test, 9 modified existing)
- total files: 17 (+384 / -55 lines)
- functional H2 test executions: **799 candidate-local / 835 combined merge-context** (81 local XML reports; 0 failures, 0 errors, 0 skipped)
- MySQL Testcontainers assurance test executions: **4** (across 3 classes; 0 failures, 0 errors, 0 skipped)
- logical combined executions: **803 candidate / 839 combined merge-context** (across separate validation paths)
- previous clean baseline: **790 functional H2 + 3 MySQL** (from PR #92 / PR #93)
- net-new executions: **+9 H2 tests** (+1 Session lifecycle early-start rejection, +1 recurrence-rule horizon model test, +1 series-preview horizon rejection, +2 occurrence-generation tests, +1 Campaign equal-date validation, +1 recurrence-rule missing-resource test, +1 series-change preview missing-rule test, +1 series-change confirmation missing-rule test) + **1 MySQL Testcontainers concurrency test** (`SessionSeriesMySqlConcurrencyIntegrationTests`)
- focused remediation regression executions: **143 / 143 PASS**
- endpoints integrated: 0 new REST endpoints (prerequisite correctness and safety remediation)
- remediation details: F-01 (Early session start prevention), F-02 (366-day recurrence safety horizon), F-03 (Campaign service-date consistency), F-04 (Recurrence/series missing-resource HTTP 404 normalization), MySQL series concurrency assurance (`SessionSeriesMySqlConcurrencyIntegrationTests`).

### Preceding Milestone: PR #92 — `feat(mf02): add session change control REST API`

- reviewed source: `ccf5c3d58035d35e56d316207848d58388446547`
- base: `1531ad9f89e3dc75b174cc3deb3a2204e2712a52`
- merge: `6ff63bdd04f34d3bd334d011fdf55dee8010a140`
- merged at: `2026-08-17T14:19:17Z`
- merge method: normal merge commit (two parents: `1531ad9f89e3dc75b174cc3deb3a2204e2712a52` and `ccf5c3d58035d35e56d316207848d58388446547`)
- reviewed candidate preserved: YES
- Backend CI: PASS (790 H2 verify + 3 Testcontainers MySQL verify)
- Frontend CI: PASS
- functional H2 test executions: **790** (81 XML reports; 0 failures, 0 errors, 0 skipped)
- MySQL Testcontainers assurance test executions: **3** (2 XML reports; 0 failures, 0 errors, 0 skipped)
- logical combined executions: **793** (across separate validation paths)
- previous clean baseline: **763 functional H2 + 3 MySQL** (from PR #90 / PR #91)
- net-new executions: **+27 H2 tests** (+20 `SessionControllerChangeTests`, +3 `SessionDtoMapperTests`, +4 `VaccinationSessionServiceQueryTests`)
- endpoints integrated under `/api/v1/sessions` (7): `cancellation-preview`, `cancel`, `reschedule-preview`, `reschedule`, `transfer-preview`, `transfer`, `change-events`. Qualified as `COMPLETE_WITH_MF03_RUNTIME_CONSTRAINT`.

### Preceding Milestone: PR #90 — `feat(mf02): implement vaccination session core and lifecycle rest api`

- reviewed source: `3324ab4b2a84960dea946c531115b5aee829ad61`
- base: `4eaa8cd31c04aedf23f3887f25de27ec924165b8`
- merge: `9cc7a606260e24e243090f70f5e957b0544bfa3b`
- merged at: `2026-08-17T08:36:32Z`
- merge method: normal merge commit (two parents: `4eaa8cd31c04aedf23f3887f25de27ec924165b8` and `3324ab4b2a84960dea946c531115b5aee829ad61`)
- reviewed candidate preserved: YES
- Backend CI: PASS (763 H2 verify + 3 Testcontainers MySQL verify)
- Frontend CI: PASS
- functional H2 test executions: **763** (80 XML reports; 0 failures, 0 errors, 0 skipped)
- MySQL Testcontainers assurance test executions: **3** (2 XML reports; 0 failures, 0 errors, 0 skipped)
- logical combined executions: **766**
- previous clean baseline: **724 functional H2 + 3 MySQL** (from PR #88 / PR #89)
- net-new executions: **+39 H2 tests** (+23 `SessionControllerTests`, +3 `SessionDtoMapperTests`, +13 `VaccinationSessionServiceQueryTests`)
- endpoints integrated under `/api/v1/sessions` (6): `createSession`, `getSessionById`, `getSessions`, `scheduleSession`, `startSession`, `completeSession`. Qualified as `COMPLETE_WITH_OPERATIONAL_SCHEDULING_GUARD via PR #102`.

### Preceding Milestone: PR #88 — `feat(mf02): add vaccination center management REST API`

- reviewed source: `263c26bc9d9a7b51bd81e28d326a90b004c548f0`
- base: `37574bb02e157971c3ca2250d4953834cfe6d132`
- merge: `faf455ff6f07d096258e7cd977765e6e9e25b10f`
- merged at: `2026-08-16T16:46:40Z`
- merge method: normal merge commit (two parents: `37574bb02e157971c3ca2250d4953834cfe6d132` and `263c26bc9d9a7b51bd81e28d326a90b004c548f0`)
- reviewed candidate preserved: YES
- Backend CI: PASS (724 H2 verify + 3 Testcontainers MySQL verify)
- Frontend CI: PASS
- functional H2 test executions: **724** (77 XML reports; 0 failures, 0 errors, 0 skipped)
- MySQL Testcontainers assurance test executions: **3** (2 XML reports; 0 failures, 0 errors, 0 skipped)
- logical combined executions: **727**
- previous clean baseline: **680 functional H2 + 3 MySQL** (from PR #86)
- net-new executions: **+44 H2 tests** (+33 `CenterControllerTests`, +4 `CenterDtoMapperTests`, +7 `VaccinationCenterServiceQueryTests`)
- endpoints integrated under `/api/v1/centers` (14): Center Master & Status (6), Operating Hours (4), Center Closures (4).

### Preceding Milestone: PR #86 — `feat(mf02): add campaign management REST API`

- reviewed source: `0fab4afa983252e927dda01278193a584ad9a0d8`
- merge: `a9a71114738ce8aed6c1f0cff8a8f42d955a033c`
- base: `9d40ddbd55139a28707c3bdb68fb6a222b5b1c28`
- functional H2 test executions: **680** (76 XML reports; 0 failures, 0 errors, 0 skipped)
- MySQL Testcontainers assurance test executions: **3** (2 XML reports; 0 failures, 0 errors, 0 skipped)
- logical combined executions: **683**
- net-new executions: **+35 H2 tests** (+23 `CampaignControllerTests`, +4 `CampaignDtoMapperTests`, +8 `VaccinationCampaignServiceQueryTests`)
- endpoints integrated under `/api/v1/campaigns` (9): `createCampaign`, `getCampaignById`, `getCampaigns`, `getCampaignLifecycleEvents`, `scheduleCampaign`, `publishCampaign`, `activateCampaign`, `suspendCampaign`, `resumeCampaign`.

### Preceding Milestone: PR #84 — `test(mf02): add MySQL concurrency assurance`

- reviewed source: `ded72c17d8bb4aaf222d6e2a96b44fa93208e880`
- merge: `34aa7666dc2d37b1b71faf25a7f51cfba10ee26a`
- base: `633e6a94585a9549718e16b038ae27e6646347f0`
- functional H2 baseline at PR #84: **645 tests** across 71 Surefire XML reports
- MySQL Testcontainers assurance test executions: **3 tests** across 2 XML reports (648 logical executions across separate validation paths)
- container environment: pinned `mysql:8.4.3` image, MySQL 8.4.3 InnoDB, `REPEATABLE_READ` isolation.

## 2. Test-Baseline Reconciliation

| Checkpoint | Authoritative executable tests | Notes |
|---|---:|---|
| PR #61 lifecycle integrity | 523 | historical |
| PR #65 suspension | 554 | historical |
| PR #67 terminal planning guard | 560 | clean-build reconciled |
| PR #69 SUSPENDED planning freeze | 569 | +9 from clean PR #67 baseline |
| PR #71 campaign scheduling approval | **591** | +22 from PR #69 baseline |
| PR #73 questionnaire invariant hardening | **591** | +0 net-new (assertion hardening & fixture alignment) |
| PR #75 series preview calculator decomposition | **599** | +8 from PR #73 baseline (`SessionSeriesPreviewCalculatorTests`) |
| PR #78 deterministic concurrency hardening | **599** | +0 net-new (concurrency test determinism hardening) |
| PR #80 campaign activation (PUBLISHED -> ACTIVE) | **616** | +17 from PR #78 baseline (+2 model, +14 integration, +1 concurrency) |
| PR #82 campaign resumption (SUSPENDED -> PUBLISHED / ACTIVE) | **645** | +29 from PR #80 baseline (+4 model, +2 event model, +22 integration, +1 concurrency) |
| PR #84 representative MySQL concurrency assurance | **645 functional H2 + 3 MySQL** | +0 H2, +3 MySQL Testcontainers assurance (**648 logical executions**) |
| PR #86 campaign management REST API (9 endpoints) | **680 functional H2 + 3 MySQL** | +35 H2 (+23 controller, +4 mapper, +8 service query), +3 MySQL Testcontainers assurance (**683 logical executions**) |
| PR #88 vaccination center management REST API (14 endpoints) | **724 functional H2 + 3 MySQL** | +44 H2 (+33 controller, +4 mapper, +7 service query), +3 MySQL Testcontainers assurance (**727 logical executions**) |
| PR #90 vaccination session core REST API (6 endpoints) | **763 functional H2 + 3 MySQL** | +39 H2 (+23 controller, +3 mapper, +13 service query), +3 MySQL Testcontainers assurance (**766 logical executions**) |
| PR #92 vaccination session change control REST API (7 endpoints; COMPLETE_WITH_MF03_RUNTIME_CONSTRAINT) | **790 functional H2 + 3 MySQL** | +27 H2 (+20 controller, +3 mapper, +4 service query), +3 MySQL Testcontainers assurance (**793 logical executions across separate validation paths**) |
| PR #97 pre-recurrence correctness/safety remediation | **799 functional H2 (835 combined PR context) + 4 MySQL** | +9 MF-02 H2 (+1 session start, +1 rule model, +1 series preview, +2 occurrence gen, +1 campaign date, +1 rule service, +2 series service), +1 MySQL Testcontainers assurance (**803 logical candidate executions / 839 logical combined merge executions**) |
| PR #100 recurrence and series REST API (9 endpoints; COMPLETE_WITH_MF03_RUNTIME_CONSTRAINT) | **926 functional H2 + 4 MySQL** | +44 MF-02 H2 (+24 controller, +8 mapper, +12 service query), +4 MySQL Testcontainers assurance (**930 logical executions**; 882 immediate parent baseline + 44 candidate net-new) |
| PR #102 session operational prerequisite guard (COMPLETE) | **928 functional H2 + 4 MySQL** | +2 MF-02 H2 (+1 lifecycle hardening, +1 concurrency guard), +4 MySQL Testcontainers assurance (**932 logical executions**; 926 immediate parent baseline + 2 candidate net-new) |
| PR #104 campaign configuration REST API (12 endpoints; COMPLETE_WITH_SEEDED_MASTER_DATA) | **953 functional H2 + 4 MySQL** | +25 MF-02 H2 (+8 mapper, +16 controller, +1 workflow), +4 MySQL Testcontainers assurance (**957 logical executions**; 928 immediate parent baseline + 25 candidate net-new) |
| PR #106 session operational core model and calculation engine (Milestone 1; COMPLETE) | **998 functional H2 + 4 MySQL** | +45 MF-02 H2 across 8 test files, +4 MySQL Testcontainers assurance (**1002 logical executions**; 953 immediate parent baseline + 45 candidate net-new) |
| PR #108 center operational resource and staff roster REST API (Milestone 2A; 8 endpoints; COMPLETE) | **1041 functional H2 + 4 MySQL** | +43 MF-02 H2 across 6 test files, +4 MySQL Testcontainers assurance (**1045 logical executions**; 998 immediate parent baseline + 43 candidate net-new) |

The value **575** was a non-authoritative arithmetic result derived from a superseded figure and must not be used as verified evidence.

## 3. Sprint Goal and Current Outcome

### Sprint goal
Build the MF-02 backend foundations required to create, validate, schedule, and safely change campaigns, centers, recurrence definitions, concrete vaccination sessions, and operational resources/capacity plans without violating configuration history or silently mutating MF-03 appointment/waitlist data.

### Current outcome
The Sprint has delivered substantial backend foundations, REST APIs (65 endpoints across campaigns, campaign configuration, centers, center operational resources/staff roster, sessions, and recurrence rules), lifecycle/change-control behavior, the Session Operational Core Model & Calculation Engine (PR #106), and the Center Operational Resource & Staff Roster REST API (PR #108; Milestone 2A), but Sprint 1 remains open because session resource/staff assignment REST (Milestone 2B), session capacity plan REST (Milestone 2C), operational readiness integration (Milestone 3), slot capacity integration (Milestone 4), remaining lifecycle/integration work, security, Flyway, and frontend remain pending.

## 4. Completed MF-02 Evidence

### Campaign / configuration foundations
Implemented and integrated:
- VaccinationCampaign
- VaccineType (seeded reference data)
- TargetGroup (seeded reference data)
- CampaignDoseProgramme
- CampaignLocationTarget
- CampaignQuestionnaireAssignment
- future-safe effective-history behavior
- campaign service-date reverse-impact protection (aligned in PR #97 to require `endDate > startDate` per `MF02-BR-01`)

### Center foundations
Implemented and integrated:
- VaccinationCenter
- explicit scheduling timezone
- OperatingHours
- CenterClosure
- CampaignCenterAssignment
- center deactivation reverse-impact protection
- hours/closure reverse-impact protection

### Center operational resources & staff roster (Milestone 2A — PR #108)
Implemented and integrated under `/api/v1/centers/{centerId}` (PR #108): 8 REST endpoints (4 operational resource routes and 4 operational staff roster routes). Includes canonical correction of `OperationalResourceStatus` (`ACTIVE`, `MAINTENANCE`, `INACTIVE`), pessimistic row locking on updates, and narrow duplicate key race translation to HTTP 409 Conflict.

### Recurrence / concrete session
Implemented and integrated:
- SessionRecurrenceRule (hardened in PR #97 with 366-day inclusive safety horizon)
- VaccinationSession (hardened in PR #97 with Clock-based early-start rejection; scheduling fail-closed in PR #102)
- schedule/timezone/provenance invariants
- authoritative `VaccinationSessionValidator`
- one-off DRAFT creation
- recurrence occurrence generation (bounded by 366-day safety horizon)
- deterministic occurrence keys
- idempotent retries
- DST gap/overlap handling
- generator compatibility with individual exceptions

### Material session changes
Implemented and integrated:
- SessionChangeEvent
- SessionScheduleSnapshot
- SessionImpactReader / SessionImpactSummary port
- impact preview
- SHA-256 stale-preview fingerprint
- individual reschedule
- center transfer
- individual cancellation
- series preview/diff (with 366-day horizon guard)
- series confirmation
- full-series cancellation
- preserved individual exceptions
- deterministic locking and rollback behavior

### Session operational core model & calculation engine (Milestone 1 — PR #106)
Implemented and integrated:
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

### Pre-Recurrence Correctness and Safety Remediation (PR #97)
Implemented and integrated:
- **F-01:** Clock-based early session start rejection in `VaccinationSessionService.startSession` (`!now.isBefore(session.getStartAt())`).
- **F-02:** 366-day inclusive recurrence safety horizon in `SessionRecurrenceRule`, `SessionSeriesPreviewCalculator`, and `SessionOccurrenceGenerationService`.
- **F-03:** Campaign service-date alignment (`endDate > startDate`) in `VaccinationCampaignService`.
- **F-04:** Missing persistent recurrence/series resource normalization to `ResourceNotFoundException` (HTTP 404).
- **MySQL Assurance:** Live MySQL Testcontainers concurrency test for series confirmation vs occurrence generation (`SessionSeriesMySqlConcurrencyIntegrationTests`).

### Campaign REST API (Milestone 1)
Implemented and integrated under `/api/v1/campaigns` (PR #86): 9 REST endpoints.

### Center REST API (Milestone 2)
Implemented and integrated under `/api/v1/centers` (PR #88): 14 REST endpoints.

### Session Core REST API (Milestone 3)
Implemented and integrated under `/api/v1/sessions` (PR #90): 6 REST endpoints (`COMPLETE_WITH_OPERATIONAL_SCHEDULING_GUARD via PR #102`).

### Session Change Control REST API (Milestone 4)
Implemented and integrated under `/api/v1/sessions` (PR #92): 7 REST endpoints (`COMPLETE_WITH_MF03_RUNTIME_CONSTRAINT`).

### Recurrence and Series REST API (Milestone 5)
Implemented and integrated under `/api/v1/recurrence-rules` (PR #100): 9 REST endpoints (`COMPLETE_WITH_MF03_RUNTIME_CONSTRAINT`).

### Session Operational Prerequisite Guard (PR #102)
Implemented and integrated in `VaccinationSessionService.scheduleSession` (PR #102): fails closed with HTTP 409 Conflict while operational resource, staff, and capacity models remain unbuilt.

### Campaign Configuration REST API (PR #104)
Implemented and integrated under `/api/v1/campaigns/{id}` (PR #104): 12 REST endpoints covering dose programmes, questionnaire assignments, campaign-center assignments, and location targets (`COMPLETE_WITH_SEEDED_MASTER_DATA`).

### Session Operational Core Model & Calculation Engine (PR #106)
Implemented and integrated operational domain models, repositories, pure calculation engine, and cross-center staff roster pessimistic lock foundation (PR #106; Milestone 1 COMPLETE).

### Center Operational Resources & Staff Roster REST API (PR #108)
Implemented and integrated under `/api/v1/centers/{centerId}` (PR #108): 8 REST endpoints (4 operational resource routes and 4 operational staff roster routes; Milestone 2A COMPLETE).

## 5. PR #71 — Campaign `DRAFT -> SCHEDULED` Evidence
Details preserved: DRAFT only -> SCHEDULED under campaign pessimistic write lock, full validation of prerequisites.

## 6. PR #73 — Campaign Questionnaire Invariant Hardening Evidence
Details preserved: Defensively enforces `MF02-BR-02` revalidating `QuestionnaireVersionPublicationPort.isPublished(...)` under campaign lock.

## 7. PR #75 — Series Preview Calculator Extraction Refactor Evidence
Details preserved: Extraction of deterministic series preview calculation and SHA-256 fingerprint token generation into `SessionSeriesPreviewCalculator`.

## 8. PR #78 — Reverse-Impact Concurrency Determinism Hardening Evidence
Details preserved: Elimination of `Thread.sleep(300)` via deterministic latch coordination.

## 9. PR #80 — Campaign Activation (PUBLISHED -> ACTIVE) Evidence
Details preserved: Authoritative campaign activation per Canonical R9 Appendix E.5.

## 10. PR #82 — Campaign Resumption (SUSPENDED -> PUBLISHED / ACTIVE) Evidence
Details preserved: Authoritative campaign resumption per Canonical R9 Appendix E.5.

## 11. PR #84 — Representative MySQL Concurrency Assurance Evidence
Details preserved: Containerized MySQL 8.4.3 InnoDB integration tests via Testcontainers under profile `-Pmysql-concurrency`.

## 12. PR #97 — Pre-Recurrence Correctness and Safety Remediation Evidence
Details preserved: F-01 (Early session start prevention), F-02 (366-day recurrence safety horizon), F-03 (Campaign service date consistency), F-04 (Missing resource HTTP 404 normalization), MySQL series concurrency assurance (`SessionSeriesMySqlConcurrencyIntegrationTests`).

## 13. PR #100 — Vaccination Session Recurrence / Series REST API Evidence
Details preserved: 9 REST routes under `/api/v1/recurrence-rules`, `cancelRule()` internal only, DTO immutability, `COMPLETE_WITH_MF03_RUNTIME_CONSTRAINT`.

## 14. PR #102 — Session Operational Prerequisite Guard Evidence
Details preserved: Resolves confirmed P1 audit finding MF02-001 (Scheduling). `POST /api/v1/sessions/{id}/schedule` fails closed with HTTP 409 Conflict while operational resource, staff, and capacity models remain unbuilt.

## 15. PR #104 — Campaign Configuration REST API Evidence

### Behavior and Implementation Contracts
PR #104 resolves confirmed P1 audit finding MF02-003 (Campaign Configuration API Blocker) by exposing existing domain services through 12 REST endpoints:

- **Dose Programme Configuration:**
  - `POST /api/v1/campaigns/{id}/dose-programmes` -> creates initial dose programme (version 1, 201 Created + Location header).
  - `POST /api/v1/campaigns/{id}/dose-programmes/versions` -> creates version N+1 and supersedes active version (201 Created + Location header).
  - `GET /api/v1/campaigns/{id}/dose-programmes` -> returns complete version history ordered deterministically by `versionNumber DESC`.
- **Questionnaire Assignment Configuration:**
  - `POST /api/v1/campaigns/{id}/questionnaire-assignments` -> assigns published questionnaire version (201 Created + Location header).
  - `GET /api/v1/campaigns/{id}/questionnaire-assignments` -> returns assignment history ordered deterministically by `activeFrom DESC, id DESC`.
  - MF-01 Boundary: Revalidates publication status through `QuestionnaireVersionPublicationPort`. Zero direct MF-01 repository access.
- **Campaign-Center Assignment Configuration:**
  - `POST /api/v1/campaigns/{id}/center-assignments` -> creates center authorization (201 Created + item Location header).
  - `GET /api/v1/campaigns/{id}/center-assignments` -> lists assignments for campaign ordered by `activeFrom ASC, id ASC`.
  - `PUT /api/v1/campaigns/{id}/center-assignments/{assignmentId}` -> updates mutable future assignment (200 OK).
  - `POST /api/v1/campaigns/{id}/center-assignments/{assignmentId}/end-date` -> ends currently effective assignment early (200 OK).
  - `POST /api/v1/campaigns/{id}/center-assignments/{assignmentId}/cancel` -> cancels mutable future assignment (200 OK).
  - Nested Ownership Enforcement: All mutations verify `assignment.getCampaign().getId().equals(campaignId)`, throwing `ResourceNotFoundException` (HTTP 404) if mismatched.
- **Geographic Location Target Configuration:**
  - `POST /api/v1/campaigns/{id}/location-targets` -> adds location target (201 Created + Location header).
  - `GET /api/v1/campaigns/{id}/location-targets` -> lists location targets with optional `?type=` filter, ordered deterministically (`targetType ASC, targetCode ASC, id ASC` or `targetCode ASC, id ASC`).
  - Geographic Targeting Only: Location targets remain visibility/service-area metadata and are NOT an artificial campaign scheduling prerequisite.
- **Master Data Boundary:**
  `VaccineType` and `TargetGroup` remain seeded reference data without public administration routes (`PUBLIC_WORKFLOW_CLOSED_WITH_SEEDED_MASTER_DATA`).

### Test Impact
- **Immediate parent baseline:** **928 functional H2 tests** (at `d8d7c3afcea889e9e65b6c188afbc23ef7bde548`).
- **Candidate-local functional H2 baseline:** **953 / 953 PASS** across 101 locally verified Surefire XML reports (+25 net-new tests).
- **Combined PR #104 Backend CI:** **953 / 953 PASS** (0 failures, 0 errors, 0 skipped, `BUILD SUCCESS` in 2m39s).
- **MySQL CI:** **4 / 4 PASS** across 3 classes on `mysql:8.4.3`. PR #104 added no new MySQL tests.
- **Frontend CI:** PASS (13s).

## 16. PR #106 — Session Operational Core Model and Calculation Engine Evidence

### Behavior and Implementation Contracts
PR #106 implements the core domain foundation and calculation engine for operational resources, staff rosters, assignments, and capacity plans (Milestone 1):

- **Operational Resources:**
  - `OperationalResource`: Center-owned physical resource (`centerId`, `name`, `resourceType`, `status`, `notes`).
  - `ResourceType`: Enum (`STATION`, `EQUIPMENT`, `FACILITY`, `OTHER`).
  - `OperationalResourceStatus`: Enum (`ACTIVE`, `MAINTENANCE`, `DECOMMISSIONED`).
  - `OperationalResourceService`: Domain service for resource creation, status updates, and center-scoped queries.
  - `SessionResourceAssignment`: Links sessions to allocated operational resources with validation against double-booking across active sessions.
- **Operational Staff Roster & Assignments:**
  - `OperationalStaffRoster`: Center-scoped staff qualification roster (`centerId`, `userId`, `staffRole`, `status`, `notes`) with unique constraint `unique(center_id, user_id)`.
  - `StaffRole`: Enum (`VACCINATOR`, `ADMINISTRATOR`, `COORDINATOR`, `OBSERVER`, `SUPPORT`).
  - `OperationalStaffStatus`: Enum (`ACTIVE`, `INACTIVE`, `SUSPENDED`).
  - `OperationalStaffRosterService`: Roster management, status transitions, and cross-center qualification queries.
  - `StaffAssignment`: Session-scoped staff assignments with global overlap protection by external `userId` across concurrent active sessions.
  - Global Staff Lock Foundation: `OperationalStaffRosterRepository` provides `findByUserIdForUpdateOrderByIdAsc(userId)` with `PESSIMISTIC_WRITE` covering all roster rows for the user in deterministic ID order.
- **Session Capacity Plan & Pure Calculation Engine:**
  - `SessionCapacityPlan`: Immutable/revisioned session capacity record (`sessionId`, `effectiveStationCount`, `assignedVaccinatorCount`, `availableStationCount`, `fullyEquippedStationCount`, `expectedServiceMinutes`, `sessionServiceCeiling`, `recommendedThroughput`, `approvedCapacity`, `overrideReason`, `approvedBy`, `calculatedAt`).
  - `SessionCapacityCalculationService`: Pure calculation service computing recommended throughput, effective station count, approved capacity bounds, and interval occupancy evaluations.
  - Value Objects: `OccupancyInterval` (time slice with capacity and count), `OccupancyEvaluationResult` (aggregation of peak utilization and overflow flags).
- **Capacity Semantics:**
  - `recommendedThroughput = effectiveStationCount * floor(durationMinutes / expectedServiceMinutes)`
  - `effectiveStationCount = min(assignedVaccinatorCount, availableStationCount, fullyEquippedStationCount)` where `fullyEquippedStationCount` is an explicit scalar snapshot/input.
  - `approvedCapacity`: Operationally approved booking capacity. If `sessionServiceCeiling != null`, then `approvedCapacity <= sessionServiceCeiling`.
  - `overrideReason`: Mandatory when `approvedCapacity > recommendedThroughput`. The hard `sessionServiceCeiling` cannot be overridden.
  - `sessionServiceCeiling`: Configured on `VaccinationCenter`, nullable, snapshotted into `SessionCapacityPlan`; NOT calculated from throughput; NOT calculated from availableStationCount.
  - `approvedBy`: String audit actor metadata (authenticated RBAC is not implemented).
- **Staff Concurrency & Safe Claim:**
  - Safe claim: MF-02 has a deterministic global pessimistic-lock repository foundation for cross-center staff scheduling. Live scheduling concurrency is `NOT_YET_PROVEN` as live scheduling does not use this contract yet.
  - Future M3 obligation: Concurrent roster registration vs future staff scheduling must receive dedicated MySQL/Testcontainers concurrency proof in Milestone 3 before the PR #102 scheduling guard is replaced (`REQUIRED_IN_M3`).
- **Scheduling Guard State:**
  - PR #102 temporary fail-closed scheduling guard remains `COMPLETE`. `DRAFT -> SCHEDULED` still fails closed with HTTP 409 Conflict. These foundations are NOT wired into `DRAFT -> SCHEDULED` yet.

### Test Impact
- **Immediate parent baseline:** **953 functional H2 tests** (at `c8d5b452ee88ec97988874e211ffb26ea321b3e1`).
- **Candidate-local functional H2 baseline:** **998 / 998 PASS** across locally verified Surefire XML reports (+45 net-new tests across 8 test files).
- **Combined PR #106 Backend CI:** **998 / 998 PASS** (0 failures, 0 errors, 0 skipped, `BUILD SUCCESS` in 2m28s / H2 suite 1m25s; Run 32356332347, Job 96386194220).
- **MySQL CI:** **4 / 4 PASS** across 3 classes on `mysql:8.4.3`. PR #106 added no new MySQL tests. The existing MySQL/InnoDB concurrency assurance suite remains green with the M1 persistence additions.
- **Frontend CI:** PASS (14s; Run 32356332549, Job 96386194726).

## 17. PR #108 — Center Operational Resource and Staff Roster REST API Evidence

### Behavior and Implementation Contracts
PR #108 exposes center operational resources and center operational staff roster configuration via REST (Milestone 2A):

- **Center Operational Resources:**
  - `POST /api/v1/centers/{centerId}/resources` -> creates operational resource (status defaults `ACTIVE`, 201 Created + Location header).
  - `GET /api/v1/centers/{centerId}/resources` -> lists operational resources for center (returns `ACTIVE`, `MAINTENANCE`, `INACTIVE` by default; optional `?resourceType=` filter).
  - `GET /api/v1/centers/{centerId}/resources/{resourceId}` -> returns resource by ID (200 OK).
  - `PUT /api/v1/centers/{centerId}/resources/{resourceId}` -> updates mutable fields `name` and `status` under pessimistic row lock (`findByIdForUpdate`, 200 OK). `code` and `resourceType` are immutable.
  - Nested Center Ownership: Operations verify resource belongs to `centerId`, throwing `ResourceNotFoundException` (HTTP 404) on mismatch.
  - Uniqueness & Conflict: `unique(center_id, code)` enforced; duplicate code returns HTTP 409 Conflict.
- **Center Operational Staff Roster:**
  - `POST /api/v1/centers/{centerId}/staff-roster` -> registers staff user to center roster (status defaults `ACTIVE`, 201 Created + Location header).
  - `GET /api/v1/centers/{centerId}/staff-roster` -> lists roster for center (returns all roles and statuses by default; optional `?role=` and `?status=` filters).
  - `GET /api/v1/centers/{centerId}/staff-roster/{rosterId}` -> returns roster entry by ID (200 OK).
  - `PUT /api/v1/centers/{centerId}/staff-roster/{rosterId}` -> updates mutable fields `staffRole` and `status` under pessimistic row lock (`findByIdForUpdate`, 200 OK). `userId` is immutable.
  - Nested Center Ownership: Operations verify roster entry belongs to `centerId`, throwing `ResourceNotFoundException` (HTTP 404) on mismatch.
  - Uniqueness & Conflict: `unique(center_id, user_id)` enforced; duplicate user at same center returns HTTP 409 Conflict.
- **OperationalResourceStatus Canonical Correction:**
  - Authoritative enum contains `ACTIVE`, `MAINTENANCE`, `INACTIVE`.
  - `MAINTENANCE` was the canonical Section 6.6 value missing from M1 and was added in M2A.
  - No database schema migration required (`EnumType.STRING` column).
- **StaffRole:**
  - Remains `VACCINATOR`, `CHECK_IN_STAFF`, `SUPERVISOR` (3 canonical values).
- **Preserved History:**
  - Resource status changes and roster status changes do not cascade-delete or corrupt existing assignment rows (`SessionResourceAssignment`) or staff assignment snapshots (`StaffAssignment`).
- **Scheduling Guard Preserved:**
  - PR #102 fail-closed guard remains in effect; `DRAFT -> SCHEDULED` is still rejected with HTTP 409 Conflict until Milestone 3 readiness integration.

### Test Impact
- **Immediate parent baseline:** **998 functional H2 tests** (at `e63bb76ac3dbb08b5009976233bf4255a059a712`).
- **Candidate-local functional H2 baseline:** **1041 / 1041 PASS** across locally verified Surefire XML reports (+43 net-new tests across 6 test files).
- **Combined PR #108 Backend CI:** **1041 / 1041 PASS** (0 failures, 0 errors, 0 skipped, `BUILD SUCCESS` in 2m37s; Run 32446414058, Job 96666733750).
- **MySQL CI:** **4 / 4 PASS** across 3 classes on `mysql:8.4.3`. PR #108 added no new MySQL tests. The existing MySQL/InnoDB concurrency assurance suite remains green with the M2A persistence and REST additions.
- **Frontend CI:** PASS (14s; Run 32446414084, Job 96666733813).

## 18. Concurrency / Transaction Evidence

Current high-risk workflows use transactional boundaries and explicit pessimistic locking where needed.

Representative established lock orders:
- campaign-owned configuration: Campaign
- center-owned calendar writes: Center
- recurrence generation: Campaign -> Center -> Rule
- same-center session change: Campaign -> Center -> Session
- center transfer: Campaign -> Center(min) -> Center(max) -> Session
- series confirmation: Campaign -> Center -> Rule -> Sessions sorted by ID
- campaign scheduling: Campaign
- campaign activation: Campaign
- campaign resumption: Campaign
- session scheduling guard: Campaign -> Session
- resource row update: `OperationalResource` via `findByIdForUpdate`
- staff roster row update: `OperationalStaffRoster` via `findByIdForUpdate`
- staff roster cross-center lock query: `findByUserIdForUpdateOrderByIdAsc(userId)` (PESSIMISTIC_WRITE across all roster rows for the user in ascending ID order)

### Test Database Architecture and Concurrency Assurance
- **Fast Functional In-Memory Suite:** Standard backend tests run against H2 in MySQL compatibility mode (1041 tests).
- **Representative Production-Equivalent MySQL Assurance (PR #84 & PR #97):** Dedicated MySQL Testcontainers test suite executed in CI under the `-Pmysql-concurrency` profile against a pinned `mysql:8.4.3` InnoDB container image. Validates single-row duplicate-command race serialization (`CampaignLifecycleMySqlConcurrencyIntegrationTests`), parent/child reverse-impact locking (`CampaignReverseImpactMySqlConcurrencyIntegrationTests`), and series confirmation vs occurrence generation (`SessionSeriesMySqlConcurrencyIntegrationTests`).
- **Scope Boundary:** *Representative single-row lifecycle, parent/child reverse-impact, and series concurrency pessimistic-locking behavior is validated against real MySQL/InnoDB in CI; exact low-level JDBC blocking timing is not instrumented, and non-concurrency domain paths remain verified by the fast H2 suite.* The existing MySQL/InnoDB concurrency assurance suite remains green with the M2A persistence and REST additions. Dedicated MySQL concurrency proof for concurrent roster registration versus future staff scheduling is required in Milestone 3.

## 19. Historical Milestone Chronology

| PR | Scope |
|---|---|
| #12 | Campaign, dose-programme, location-target and questionnaire-assignment foundations |
| #16 | VaccinationCenter foundation |
| #20 | OperatingHours |
| #24 | CenterClosure |
| #26 | CampaignCenterAssignment |
| #28 | SessionRecurrenceRule |
| #30 | Center scheduling timezone / authoritative mutation prerequisite |
| #32 | VaccinationSession domain/persistence |
| #34 | Authoritative write-time validation and one-off DRAFT session creation |
| #36 | Recurrence occurrence generation |
| #38 | Session-change audit / impact port / row locking |
| #40 | Individual material session changes |
| #42 | Series-wide preview |
| #44 | Atomic series-wide confirmation |
| #46 | Session-change regression hardening |
| #48 | Full-series cancellation |
| #50 | Campaign / assignment reverse-impact protection |
| #52 | Center / hours / closure reverse-impact protection |
| #53 | VaccinationSession lifecycle state-machine hardening |
| #55 | Campaign publication |
| #58 | Deterministic reverse-impact test hardening |
| #59 | Evidence synchronization after deterministic hardening |
| #61 | Campaign lifecycle state-integrity hardening |
| #64 | Lifecycle documentation synchronization |
| #65 | Campaign suspension |
| #66 | Suspension documentation synchronization |
| #67 | Terminal-state planning guard |
| #68 | Terminal planning-guard documentation synchronization |
| #69 | SUSPENDED new-planning freeze |
| #70 | SUSPENDED planning-freeze documentation synchronization |
| #71 | Campaign DRAFT -> SCHEDULED approval |
| #72 | Markdown-only canonical authority synchronization |
| #73 | Campaign questionnaire publication invariant hardening |
| #74 | Evidence synchronization after PR #73 |
| #75 | Series preview calculator decomposition refactor |
| #76 | Screening module (MF-01) foundation (inter-module production publication-port provider) |
| #77 | Evidence synchronization after PR #75 |
| #78 | Deterministic reverse-impact concurrency test hardening |
| #79 | Evidence synchronization after PR #78 |
| #80 | Campaign `PUBLISHED -> ACTIVE` activation |
| #81 | Evidence synchronization after PR #80 |
| #82 | Campaign `SUSPENDED -> PUBLISHED / ACTIVE` resumption |
| #83 | Evidence synchronization after PR #82 |
| #84 | Representative MySQL/Testcontainers concurrency assurance |
| #85 | Evidence synchronization after PR #84 |
| #86 | Campaign Management REST API vertical slice (9 endpoints) |
| #87 | Evidence synchronization after PR #86 |
| #88 | Vaccination Center Management REST API vertical slice (14 endpoints) |
| #89 | Evidence synchronization after PR #88 |
| #90 | Vaccination Session Core REST API vertical slice (6 endpoints; COMPLETE_WITH_OPERATIONAL_SCHEDULING_GUARD) |
| #91 | Evidence synchronization after PR #90 |
| #92 | Vaccination Session Change Control REST API vertical slice (7 endpoints; COMPLETE_WITH_MF03_RUNTIME_CONSTRAINT) |
| #93 | Evidence synchronization after PR #92 |
| #97 | Pre-Recurrence Correctness and Safety Remediation (F-01, F-02, F-03, F-04, MySQL assurance) |
| #98 | Evidence synchronization after PR #97 |
| #100 | Vaccination Session Recurrence / Series REST API Milestone 5 (9 endpoints; COMPLETE_WITH_MF03_RUNTIME_CONSTRAINT) |
| #101 | Documentation synchronization after PR #100 |
| #102 | Vaccination Session Operational Prerequisite Guard (COMPLETE) |
| #103 | Documentation synchronization after PR #102 |
| #104 | Campaign Configuration REST API (12 endpoints; COMPLETE_WITH_SEEDED_MASTER_DATA) |
| #105 | Documentation synchronization after PR #104 |
| #106 | Session Operational Core Model and Calculation Engine (Milestone 1; COMPLETE) |
| #107 | Documentation synchronization after PR #106 |
| #108 | Center Operational Resource and Staff Roster REST API (Milestone 2A; 8 endpoints; COMPLETE) |

### Key merge checkpoints
- PR #55: `9a470da982f1510213163b3b909a02bd0f85973b`
- PR #61: `d5dff09c6d0ed643cb4537abff64056b7d20656c`
- PR #64: `8a83da13dffa93413c40df080328828104eb0214`
- PR #65: `228cdd6f0f6d72b8e776ab5ca1cf9af30819adc3`
- PR #66: `a0548359bee67ed87801b0683c8c58c89daa9f5e`
- PR #67: `296c763b48bac4ab837dafeb67cc9788b071912d`
- PR #68: `6b3e9df3cf52aba53933e7671eaab098851f496a`
- PR #69: `545075aaedda338a2c16e7d97803704b55fef951`
- PR #70: `c3bf7c2fb03513d8540a78dcc4ab5d93f322f468`
- PR #71: `6d52449db4f29099f3a979e824a7efcae8f4ea54`
- PR #72: `5fe33208d11a21ced5c6c1c520dfe01c13858bf0`
- PR #73: `d3fbb3eb95333d1da0b3e5c33283a1bcf81635e9`
- PR #74: `b8289635054a6f15d8c611e18e86a0e7a0a64687`
- PR #75: `cc13082ad94701475b69bc53b699d897486ff7fc`
- PR #76: `56493de3f03b2909403d1fb9ff7dbfec4ad07618`
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

## 20. Carried-Forward Findings / Risks

### Domain consistency — RESOLVED (PR #73)
Normal questionnaire assignment creation requires a published version, and PR #73 defensively hardened `scheduleCampaign(...)` to revalidate published status through `QuestionnaireVersionPublicationPort`.

### Pre-Recurrence Correctness and Safety Remediation — RESOLVED (PR #97)
Prerequisite engineering review findings resolved: early session start rejection (F-01), 366-day recurrence safety horizon (F-02), campaign service date consistency (F-03), missing-resource HTTP 404 normalization (F-04), and MySQL series concurrency assurance.

### Recurrence and Series REST API Milestone 5 — RESOLVED (PR #100)
Exposed 9 public REST routes under `/api/v1/recurrence-rules` matching Prompt 115 frozen design. Preserved `cancelRule()` as internal only, while exposing impact-aware preview/confirm series cancellation. Qualified as `COMPLETE_WITH_MF03_RUNTIME_CONSTRAINT`.

### Session Operational Prerequisite Guard — RESOLVED (PR #102)
Resolved confirmed P1 audit finding MF02-001 (Scheduling). `POST /api/v1/sessions/{id}/schedule` fails closed with HTTP 409 Conflict while physical resource, staff assignment, and capacity planning models are absent, preserving `DRAFT` persistence state.

### Campaign Configuration REST API — RESOLVED (PR #104)
Resolved confirmed P1 audit finding MF02-003 (Campaign Configuration API Blocker). Exposed 12 public REST routes under `/api/v1/campaigns/{id}` covering dose programmes, questionnaire assignments, campaign-center assignments, and location targets with deterministic ordering and nested ownership checks. Qualified as `PUBLIC_WORKFLOW_CLOSED_WITH_SEEDED_MASTER_DATA`.

### Session Operational Core Model and Calculation Engine — RESOLVED (PR #106)
- **Operational Core Models:** Implemented entities, repositories, and domain services for center-owned physical resources (`OperationalResource`), center operational staff roster (`OperationalStaffRoster`), session resource assignments (`SessionResourceAssignment`), session staff assignments (`StaffAssignment`), and session capacity plans (`SessionCapacityPlan`).
- **Calculation Engine:** Implemented pure calculations for recommended throughput (`recommendedThroughput = effectiveStationCount * floor(durationMinutes / expectedServiceMinutes)`), effective station count (`min(assignedVaccinatorCount, availableStationCount, fullyEquippedStationCount)` where `fullyEquippedStationCount` is an explicit scalar snapshot/input), approved capacity (`approvedCapacity <= sessionServiceCeiling` when ceiling configured; overrideReason required if exceeding recommended throughput), and interval occupancy evaluation (`OccupancyInterval`, `OccupancyEvaluationResult`).
- **Global Staff Roster Locking Foundation:** `OperationalStaffRoster` is center-scoped (`unique(center_id, user_id)`), while `StaffAssignment` overlap detection is global by external `userId`. Repository provides `findByUserIdForUpdateOrderByIdAsc(userId)` with `PESSIMISTIC_WRITE` covering all roster rows for the user in deterministic ascending ID order. Safe claim: MF-02 has a deterministic global pessimistic-lock repository foundation for cross-center staff scheduling. Live scheduling concurrency is `NOT_YET_PROVEN` as live scheduling does not use this contract yet.
- **Future M3 Concurrency Obligation:** Dedicated MySQL/Testcontainers concurrency proof for concurrent roster registration versus future staff scheduling is required in Milestone 3 before the PR #102 scheduling guard is replaced (`REQUIRED_IN_M3`).
- **Scheduling Guard Preserved:** PR #102 fail-closed guard remains in effect; `DRAFT -> SCHEDULED` is still rejected with HTTP 409 Conflict until Milestone 3 readiness integration.

### Center Operational Resource & Staff Roster REST API — RESOLVED (PR #108)
- **Exposed 8 REST routes under `/api/v1/centers/{centerId}`:** 4 operational resource endpoints and 4 operational staff roster endpoints.
- **OperationalResourceStatus Canonical Correction:** Authoritative enum contains `ACTIVE`, `MAINTENANCE`, `INACTIVE`. `MAINTENANCE` was the canonical Section 6.6 value missing from M1 and was added in M2A.
- **Pessimistic Update Locking:** Update paths for resources and roster entries execute under pessimistic row locks (`findByIdForUpdate`).
- **Duplicate Protection:** Database uniqueness constraints `unique(center_id, code)` and `unique(center_id, user_id)` are enforced. Uniqueness race exceptions map narrowly to HTTP 409 Conflict.
- **Preserved History:** Existing assignment rows (`SessionResourceAssignment`) and staff snapshot records (`StaffAssignment`) are preserved during registry updates.
- **Nested Ownership:** Mismatches between path `centerId` and resource/roster parent center normalize to HTTP 404 Not Found.

### MF-02 / MF-03 Capacity & Impact Integration — PARTIAL_P1 (Shared Dependency)
`SessionImpactReader` production adapter is MF-03-owned (currently missing; SCHEDULED changes fail closed with 409). `SlotCapacityPort` production adapter is expected to be MF-02-owned after an authoritative slot/capacity model is built.

### Session Completion Policy — SPECIFICATION_PARTIAL
Canonical mentions attendance and operational-close checks but does not define a complete decision algorithm. Not a confirmed code defect; completion implementation preserved without changes.

### Maintainability — PARTIALLY RESOLVED (PR #75)
- `SessionSeriesChangeService` was successfully decomposed via PR #75, reducing its size from ~1,104 lines to ~694 lines.
- `VaccinationSessionService` remains ~869 lines and is a candidate for future maintainability review if justified (`DEFER`).

### Optional critical ports
Production-facing adapters for questionnaire publication (provided by MF-01 `QuestionnaireVersionService` in PR #76) and MF-03 session impact are still optional-injection boundaries in the current code. Production configuration should ultimately fail fast when required adapters are absent.

### Concurrency test quality — RESOLVED (PR #78)
PR #78 eliminated the remaining `Thread.sleep(300)` timing delay from `CampaignAssignmentReverseImpactConcurrencyIntegrationTests.java` and established deterministic latch coordination with explicit winner/state assertions. Zero `Thread.sleep` calls remain across the backend test suite.

### Representative MySQL Testcontainers concurrency assurance — RESOLVED (PR #84 & PR #97)
PR #84 and PR #97 established real MySQL 8.4.3 InnoDB integration tests via Testcontainers:
- Validated single-row duplicate-command race serialization (`CampaignLifecycleMySqlConcurrencyIntegrationTests`).
- Validated parent/child reverse-impact pessimistic-locking serialization (`CampaignReverseImpactMySqlConcurrencyIntegrationTests`).
- Validated concurrent series confirmation vs occurrence generation (`SessionSeriesMySqlConcurrencyIntegrationTests`).
- Automated in GitHub Actions CI under dedicated `-Pmysql-concurrency` profile step.
- *Scope boundary:* Representative single-row lifecycle, parent/child reverse-impact, and series concurrency pessimistic-locking behavior is validated against real MySQL/InnoDB in CI; exact low-level JDBC blocking timing is not instrumented, and non-concurrency domain paths remain verified by the fast H2 suite. The existing MySQL/InnoDB concurrency assurance suite remains green with the M2A persistence and REST additions. Dedicated MySQL concurrency proof for concurrent roster registration versus future staff scheduling is required in Milestone 3.

### Audit deletion
Lifecycle/change event repositories inherit normal delete APIs, so “append-only” is not deletion-proof persistence.

## 21. Remaining Sprint / Function Work

Still not complete:
- Center Operational Resources & Staff Roster REST API (Milestone 2A: COMPLETE)
- Session Resource & Staff Assignment REST API (Milestone 2B: PENDING_M2B)
- Session Capacity Plan REST API (Milestone 2C: PENDING_M2C)
- Session Operational Configuration REST API (Milestone 2 Overall: IN_PROGRESS)
- Session Operational Readiness & Guard Replacement (Milestone 3: PENDING_M3)
- MF02/MF03 Slot Capacity Integration (Milestone 4: PENDING_M4)
- design and implementation of remaining campaign lifecycle transitions: completion (`SPECIFICATION_PARTIAL`), campaign cancellation (`MF03_BLOCKED`), archival (`USER_DATA_REQUIRED`)
- evaluation/decomposition of `VaccinationSessionService` maintainability only if justified (`DEFER`)
- archival retention value (`USER_DATA_REQUIRED`)
- DRAFT parent-campaign / child-session scheduling policy (`IMPLEMENTATION_DEFINED / unresolved policy`)
- MF-01 production adapter/FK
- MF-03 production impact adapter
- transactional outbox
- Campaign date update REST API
- Spring Security/RBAC
- OpenAPI
- Flyway migrations
- representative production MySQL concurrency assurance (RESOLVED in PR #84 & PR #97; broad production database certification deferred)
- MF-02 frontend
- frontend tests
- final end-to-end / Definition-of-Done evidence

## 22. Sprint Status

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
- Session Resource & Staff Assignment REST API (Milestone 2B): **PENDING**
- Session Capacity Plan REST API (Milestone 2C): **PENDING**
- Session Operational Configuration REST (Milestone 2 Overall): **IN PROGRESS**
- Global staff roster lock foundation: **COMPLETE**
- Live staff scheduling concurrency: **NOT_YET_PROVEN**
- Roster registration vs scheduling MySQL proof: **REQUIRED_IN_M3**
- Session Operational Readiness (Milestone 3): **PENDING_M3**
- Session Scheduling Guard Replacement (Milestone 3): **PENDING_M3**
- MF02/MF03 Slot Capacity Integration (Milestone 4): **PENDING_M4**
- current verified backend baseline: **1041 functional H2 tests plus 4 MySQL Testcontainers assurance tests** across separate validation paths.
- MF02_DOCUMENTATION_M2A_SYNC: **LOCAL_CANDIDATE_READY**

Do not infer completion from the size of the backend test suite.

## 23. Evidence Maintenance Rules

1. Canonical Markdown is the sole semantic authority.
2. Live Git/code/tests control implementation truth.
3. A documentation-only merge must not be mislabeled as a feature implementation checkpoint.
4. Historical test counts must remain tied to their historical checkpoints.
5. Clean-build counts supersede non-clean/transient counts.
6. Future evidence updates should append concise milestone entries rather than embedding controller transcripts.
7. Before claiming “current,” fetch live `origin/develop`.

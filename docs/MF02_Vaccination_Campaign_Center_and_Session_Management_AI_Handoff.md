# MF-02 — Vaccination Campaign, Center and Session Management
## AI Engineering Handoff

**Project:** Web-Based Vaccination Portal (`WBVP-SE2030`)
**Function:** MF-02 — Vaccination Campaign, Center and Session Management
**Canonical authority:** `3.3-AI / 2026-08-14-R9`
**Canonical source model:** Markdown-only; the canonical Markdown file is the sole normative semantic source.
**Sprint:** Sprint 1 — **IN PROGRESS**
**MF-02:** **IN PROGRESS**
**US-13:** **IN PROGRESS**
**Last synchronized implementation checkpoint:** PR #102
**Reviewed PR #102 candidate:** `d7663abeb39dd76861e817cbf750b9de7d5d70d5`
**PR #102 candidate development base:** `046f475f13bb1e7c734492ae58f3e3bb3eb7f9ff`
**PR #102 actual merge Parent 1:** `046f475f13bb1e7c734492ae58f3e3bb3eb7f9ff`
**PR #102 merge:** `616636762687e5c2497db8e0227ee8692311703c`
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
- **Candidate-local functional H2 baseline:** **928 functional H2 tests** across 98 locally verified Surefire XML reports (0 failures, 0 errors, 0 skipped; +2 over immediate parent baseline 926).
- **Fresh PR #102 merge-context Backend CI:** **928 functional H2 tests** across the combined tree context (0 failures, 0 errors, 0 skipped, `BUILD SUCCESS`).
- **Authoritative fresh MySQL assurance:** **4 MySQL/Testcontainers assurance tests** across 3 classes on `mysql:8.4.3` (0 failures, 0 errors, 0 skipped; existing suite, no new MySQL test added).
- **Combined logical executions:** **932 logical executions** (928 H2 + 4 MySQL) across separate validation paths.
- Clean Maven test/verify PASS; Backend CI PASS; Frontend CI PASS.
- **PR #102 net-new executions:** +2 H2 tests (+1 `SessionLifecycleHardeningIntegrationTests`, +1 `VaccinationSessionConcurrencyIntegrationTests`).
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

Future agents must fetch live `origin/develop` before treating `616636762687e5c2497db8e0227ee8692311703c` as the repository tip.

## 2. Current Milestone

### Milestone reached

**MF-02 SESSION OPERATIONAL PREREQUISITE GUARD (PR #102; COMPLETE), RECURRENCE AND SERIES REST API MILESTONE 5 (PR #100; 9 ENDPOINTS; COMPLETE_WITH_MF03_RUNTIME_CONSTRAINT), PRE-RECURRENCE CORRECTNESS AND SAFETY REMEDIATION (PR #97; F-01, F-02, F-03, F-04, SESSION SERIES MYSQL CONCURRENCY ASSURANCE), VACCINATION SESSION CHANGE CONTROL REST API (7 ENDPOINTS; COMPLETE_WITH_MF03_RUNTIME_CONSTRAINT), VACCINATION SESSION CORE REST API (6 ENDPOINTS; COMPLETE_WITH_OPERATIONAL_SCHEDULING_GUARD), VACCINATION CENTER MANAGEMENT REST API (14 ENDPOINTS), CAMPAIGN MANAGEMENT REST API (9 ENDPOINTS), MYSQL/TESTCONTAINERS CONCURRENCY ASSURANCE, CAMPAIGN RESUMPTION, CAMPAIGN ACTIVATION, SCHEDULING APPROVAL, AND SESSION-CHANGE FOUNDATIONS ARE INTEGRATED.**

This does **not** mean MF-02 is complete.

The current checkpoint includes the Session Operational Prerequisite Guard (PR #102), the Vaccination Session Recurrence / Series REST API Milestone 5 (PR #100), the Pre-Recurrence Correctness and Safety Remediation (PR #97), the Vaccination Session Change Control REST API vertical slice (PR #92), the Vaccination Session Core REST API vertical slice (PR #90), the Vaccination Center Management REST API vertical slice (PR #88), the Campaign Management REST API vertical slice (PR #86), representative production-equivalent MySQL/Testcontainers concurrency assurance (PR #84, hardened in PR #97), authoritative campaign resumption `SUSPENDED -> PUBLISHED / ACTIVE` (PR #82), authoritative campaign activation `PUBLISHED -> ACTIVE` (PR #80), deterministic reverse-impact concurrency test hardening (PR #78), the behavior-preserving extraction of `SessionSeriesPreviewCalculator` from `SessionSeriesChangeService` (PR #75), defensive questionnaire publication invariant revalidation (PR #73), and campaign scheduling approval (PR #71) on top of the already integrated campaign, center, recurrence, session, reverse-impact, publication, suspension, terminal-planning, and SUSPENDED-planning foundations.

Total MF-02 REST routes currently integrated: **45** ($9 \text{ Campaign} + 14 \text{ Center} + 13 \text{ Session [6 Core + 7 Change]} + 9 \text{ Recurrence/Series} = 45$). Total Session routes: **13** ($6 + 7 = 13$). Total Recurrence/Series routes: **9**.

### Current verified integrations

1. **PR #102 — `fix(mf02): guard session scheduling until operational readiness`**
   - reviewed source: `d7663abeb39dd76861e817cbf750b9de7d5d70d5`
   - candidate development base: `046f475f13bb1e7c734492ae58f3e3bb3eb7f9ff`
   - actual merge Parent 1: `046f475f13bb1e7c734492ae58f3e3bb3eb7f9ff`
   - actual merge Parent 2: `d7663abeb39dd76861e817cbf750b9de7d5d70d5`
   - merge: `616636762687e5c2497db8e0227ee8692311703c`
   - merged at: `2026-08-20T05:57:32Z`
   - merge method: normal merge commit (two parents: `046f475f13bb1e7c734492ae58f3e3bb3eb7f9ff` and `d7663abeb39dd76861e817cbf750b9de7d5d70d5`)
   - integration drift: 0
   - Backend CI: PASS (928 H2 executions in combined merge context, 0 failures, 0 errors, 0 skipped; 4 MySQL Testcontainers executions on `mysql:8.4.3`, 0 failures, 0 errors, 0 skipped)
   - Frontend CI: PASS
   - authoritative backend baseline: **928 functional H2 tests** (98 locally verified XML reports; +2 over immediate parent baseline 926) and **928 combined merge-context H2 tests** in CI + **4 MySQL assurance tests** across 3 classes
   - net-new executions: **+2 H2 tests** (+1 `SessionLifecycleHardeningIntegrationTests`, +1 `VaccinationSessionConcurrencyIntegrationTests`)
   - scope: 5 files (1 modified production, 4 modified tests; +113 / -37 lines)
   - behavior: The public route `POST /api/v1/sessions/{id}/schedule` remains available. An otherwise-valid DRAFT session schedule attempt now fails closed with `IllegalStateException` -> `HTTP 409 Conflict` and message `"Session scheduling is unavailable until operational resource and capacity readiness can be validated"`. The persisted session remains `DRAFT`.
   - audit finding resolution: Addresses confirmed P1 audit finding MF02-001 (Scheduling). Canonical R9 Section E.6 mandates that `DRAFT -> SCHEDULED` requires validated campaign-center assignment, operating hours, resources, capacity, and conflicts. Because authoritative models for physical resources, staff allocation, and capacity plans do not yet exist, scheduling is intentionally fail-closed rather than inventing unvalidated state transitions or production bypasses.
   - completion status: Session completion remains classified as `SPECIFICATION_PARTIAL` (not a confirmed code defect; PR #102 made zero changes to `completeSession()`, `VaccinationSession.complete()`, or the completion endpoint).
   - distinction from runtime constraints: The scheduling operational-readiness guard is strictly distinct from the existing `SessionImpactReader` runtime constraint (which safely guards SCHEDULED change workflows).
   - zero production bypass: No test flags, profiles, backdoor setters, or fake readiness exist in production code. Historical SCHEDULED sessions constructed via test fixtures continue to support downstream lifecycle operations (`start`, `complete`, `cancel`, `reschedule`, `transfer`).

2. **PR #100 — `feat(mf02): implement recurrence rule and series rest api`**
   - reviewed source: `f5c385ae7d43a17e60a2ee7647fe96f7cfa0446c`
   - merge: `684a4d6bffbe1d1b21f10ad9fa141b62fb23625a`
   - integrated endpoints under `/api/v1/recurrence-rules` (9): `createRule`, `getRuleById`, `getRules`, `replaceRule`, `generateOccurrences`, `previewSeriesChange`, `confirmSeriesChange`, `previewSeriesCancellation`, `confirmSeriesCancellation`. Qualified as `COMPLETE_WITH_MF03_RUNTIME_CONSTRAINT`.

3. **PR #97 — `fix(mf02): harden pre-recurrence correctness and safety`**
   - reviewed source: `ff60fb065bba498a8da05f41c1d3e1e56791c3c6`
   - merge: `e83e54bff1f2c682fbdb6d760856bde993faf162`
   - remediation contents: F-01 (Early session start prevention), F-02 (366-day recurrence safety horizon), F-03 (Campaign service-date consistency), F-04 (Recurrence/series missing-resource HTTP 404 normalization), MySQL series concurrency assurance (`SessionSeriesMySqlConcurrencyIntegrationTests`).

4. **PR #92 — `feat(mf02): add session change control REST API`**
   - reviewed source: `ccf5c3d58035d35e56d316207848d58388446547`
   - merge: `6ff63bdd04f34d3bd334d011fdf55dee8010a140`
   - integrated endpoints under `/api/v1/sessions` (7): `cancellation-preview`, `cancel`, `reschedule-preview`, `reschedule`, `transfer-preview`, `transfer`, `change-events`. Qualified as `COMPLETE_WITH_MF03_RUNTIME_CONSTRAINT`.

5. **PR #90 — `feat(mf02): implement vaccination session core and lifecycle rest api`**
   - reviewed source: `3324ab4b2a84960dea946c531115b5aee829ad61`
   - merge: `9cc7a606260e24e243090f70f5e957b0544bfa3b`
   - integrated endpoints under `/api/v1/sessions` (6): `createSession`, `getSessionById`, `getSessions`, `scheduleSession`, `startSession`, `completeSession`. Qualified as `COMPLETE_WITH_OPERATIONAL_SCHEDULING_GUARD`.

6. **PR #88 — `feat(mf02): add vaccination center management REST API`**
   - merge: `faf455ff6f07d096258e7cd977765e6e9e25b10f`
   - integrated endpoints under `/api/v1/centers` (14): Center Master (6), Operating Hours (4), Center Closures (4).

7. **PR #86 — `feat(mf02): add campaign management REST API`**
   - merge: `a9a71114738ce8aed6c1f0cff8a8f42d955a033c`
   - integrated endpoints under `/api/v1/campaigns` (9): Campaign Master & Lifecycle transitions.

## 3. Implemented MF-02 Backend Scope

The repository contains substantial implemented MF-02 backend behavior:

### Campaign configuration
- `VaccineType`
- `TargetGroup`
- `VaccinationCampaign`
- `CampaignDoseProgramme`
- `CampaignLocationTarget`
- `CampaignQuestionnaireAssignment`
- effective-history validation and mutation protection
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
- `CampaignCenterAssignment`
- center/hours/closure reverse-impact protection
- center and campaign pessimistic serialization where required

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

### Session lifecycle
The current session lifecycle foundation includes controlled DRAFT/SCHEDULED/IN_PROGRESS/COMPLETED/CANCELLED behavior, prevents start attempts prior to `startAt`, and guards DRAFT -> SCHEDULED transitions with HTTP 409 Conflict until operational readiness can be evaluated.

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

Critical write services are transactional. Read-only preview operations use read-only transactions where appropriate.

## 6. Current Test / Verification State

### Distinct Verification Checkpoints

1. **Historical Checkpoints:**
   - Pre-Remediation Checkpoint (Base `1b90e2ee...`): **790 functional H2 tests** across 81 XML reports + **3 MySQL assurance tests** (793 logical executions).
   - PR #97 Remediation Candidate Checkpoint: **799 functional H2 tests** (81 local XML reports).
   - PR #97 Combined Merge-Context Checkpoint: **835 functional H2 tests** executed in CI (incorporating 36 intervening MF-01 screening tests).
   - Milestone 5 Immediate Parent Baseline (`8f58ae23...`): **882 functional H2 tests** (835 PR #97 base + 47 unrelated tests landed on develop: 5 MF-01 + 42 MF-03).
   - Milestone 5 Verified Backend Baseline (PR #100 / Merge `684a4d6...`): **926 functional H2 tests** (+44 net-new tests over parent baseline 882) + **4 MySQL assurance tests**.

2. **Session Scheduling Guard Verified Backend Baseline (PR #102 / Merge `6166367...`):**
   - **Candidate-local functional H2 baseline:** **928 functional H2 tests** across 98 locally verified Surefire XML reports (0 failures, 0 errors, 0 skipped; +2 net-new tests over immediate parent baseline 926).
   - **Fresh PR #102 merge-context Backend CI:** **928 functional H2 tests** (0 failures, 0 errors, 0 skipped, `BUILD SUCCESS`).
   - **Net-new candidate executions (+2 H2 tests):**
     - +1 Lifecycle hardening test verifying operational readiness guard and conflict behavior (`SessionLifecycleHardeningIntegrationTests`)
     - +1 Concurrent scheduling guard fail-closed test (`VaccinationSessionConcurrencyIntegrationTests`)
   - **Focused scheduling guard regression:** **129 / 129 PASS** across session lifecycle, query, controller, and concurrency test suites.

### Authoritative MySQL Concurrency Assurance Checkpoint

Fresh GitHub Actions CI executed **4 tests across 3 classes** under `-Pmysql-concurrency` on `mysql:8.4.3`:
- `CampaignLifecycleMySqlConcurrencyIntegrationTests`: 2 tests (0 failures, 0 errors, 0 skipped)
- `CampaignReverseImpactMySqlConcurrencyIntegrationTests`: 1 test (0 failures, 0 errors, 0 skipped)
- `SessionSeriesMySqlConcurrencyIntegrationTests`: 1 test (0 failures, 0 errors, 0 skipped)
- **Total:** **4 / 4 PASS** (0 failures, 0 errors, 0 skipped).
- *Supported Claim:* Fresh MySQL/Testcontainers assurance passed on `mysql:8.4.3` including the Session series concurrency test (`SessionSeriesMySqlConcurrencyIntegrationTests`). PR #102 added no new MySQL-specific tests.

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

Do not resurrect 566, 575, 599, 616, 645, 680, 724, 763, 790, 799, 835, 882, or 926 as authoritative current evidence (they are historical baselines).

## 7. Architecture / Maintainability Audit — Current Repository Snapshot

The current repository maintains a **layered modular monolith**.

Verified component sizes after PR #102:
- `RecurrenceRuleController.java`: ~174 lines
- `RecurrenceRuleDtoMapper.java`: ~160 lines
- `SessionSeriesChangeService.java`: ~694 lines
- `SessionSeriesPreviewCalculator.java`: ~456 lines
- `VaccinationSessionService.java`: ~869 lines
- `VaccinationCampaignService.java`: ~428 lines
- `OperatingHoursService.java`: ~488 lines
- `CenterClosureService.java`: ~424 lines
- `VaccinationCenterService.java`: ~245 lines
- `CenterController.java`: ~228 lines
- `CampaignController.java`: ~184 lines
- `SessionController.java`: ~188 lines
- `SessionDtoMapper.java`: ~98 lines

Current MF-02 production Java files under the campaign module: 109.
Current MF-02 test Java files in the campaign test package: 85.

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
- Campaign Configuration REST API (12 proposed endpoints; CONFIRMED_P1) — **PENDING**
- Authoritative Session Operational Model (TimeSlot, physical resources, staff allocations, capacity plan, throughput) — **PENDING**
- Blocked lifecycle endpoints (`complete`, `cancel`, `archive`) (pending specification/integration)
- Spring Security/RBAC/authentication enforcement (pending)
- OpenAPI documentation (pending)
- Flyway business migrations (pending)
- representative production MySQL Testcontainers assurance (RESOLVED in PR #84 & PR #97; broad production database certification deferred)
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

### 9.5 Representative MySQL Testcontainers concurrency assurance — RESOLVED (PR #84 & PR #97)
- Containerized MySQL 8.4.3 InnoDB integration tests via Testcontainers under profile `-Pmysql-concurrency`.
- PR #84 validated single-row lifecycle duplicate-command race serialization and parent/child reverse-impact locking.
- PR #97 added `SessionSeriesMySqlConcurrencyIntegrationTests` validating concurrent series confirmation versus occurrence generation.
- Total suite in CI: 4 tests across 3 classes on `mysql:8.4.3`.

### 9.6 Append-only audit limitation
Lifecycle/change repositories inherit normal repository delete operations. Domain/API convention rather than deletion-proof database enforcement.

### 9.7 Pre-Recurrence Correctness and Safety Remediation — RESOLVED (PR #97)
Prerequisite engineering review findings resolved: early session start rejection (F-01), 366-day recurrence safety horizon (F-02), campaign service date consistency (F-03), missing-resource HTTP 404 normalization (F-04), and MySQL series concurrency assurance.

### 9.8 Recurrence and Series REST API Milestone 5 — RESOLVED (PR #100)
Exposed 9 public REST routes under `/api/v1/recurrence-rules` matching Prompt 115 frozen design. Preserved `cancelRule()` as internal only, while exposing impact-aware preview/confirm series cancellation. Qualified as `COMPLETE_WITH_MF03_RUNTIME_CONSTRAINT`.

### 9.9 Session Operational Prerequisite Guard — RESOLVED (PR #102)
Resolved confirmed P1 audit finding MF02-001 (Scheduling). `POST /api/v1/sessions/{id}/schedule` fails closed with HTTP 409 Conflict while physical resource, staff assignment, and capacity planning models are absent, preserving `DRAFT` persistence state.

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
1. synchronize maintained documentation to PR #102 — represented by this step;
2. `NEXT_ENGINEERING_MILESTONE`: **CAMPAIGN_CONFIGURATION_REST_MILESTONE_PENDING_DESIGN_CONFIRMATION** (Confirmed P1 audit finding MF02-003: expose existing domain services `CampaignDoseProgrammeService`, `CampaignQuestionnaireAssignmentService`, `CampaignCenterAssignmentService`, `CampaignLocationTargetService` via 12 proposed REST routes; potential future total: 57 routes);
3. design and integrate authoritative session operational capacity/resource model (TimeSlot, physical resources, staff allocation, capacity plan, throughput calculation) to unlock genuine DRAFT -> SCHEDULED state advancement;
4. complete MF-02/MF-03 capacity-impact integration (`SessionImpactReader` adapter, `SlotCapacityPort` adapter);
5. analyze and resolve remaining campaign lifecycle gaps (completion algorithm, cancellation impact integration, archive retention policy);
6. evaluate/decompose `VaccinationSessionService` maintainability only if justified;
7. add security/RBAC;
8. add Flyway migrations;
9. implement MF-02 frontend and frontend tests;
10. final end-to-end / Definition-of-Done evidence.

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

## 15. Status Discipline

Never claim:
- MF-02 complete;
- Sprint 1 complete;
- US-13 complete;
- production API/security/migrations/frontend complete (Campaign REST API, Center REST API, Session Core REST API, Session Change Control REST API, Pre-Recurrence Remediation, Recurrence/Series REST API, and Session Scheduling Guard are integrated; security, Flyway, frontend, and campaign configuration REST remain pending);
- MySQL concurrency fully certified across all endpoints;
- 566, 575, 599, 616, 645, 680, 724, 763, 790, 799, 835, 882, or 926 as the current backend baseline;
- 932 as the functional H2 baseline (932 represents logical combined coverage across separate validation paths: 928 H2 + 4 MySQL).

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
- current verified backend baseline: **928 functional H2 tests plus 4 MySQL Testcontainers assurance tests** across separate validation paths.

## 16. Startup Procedure for the Next AI Session

1. Read canonical Markdown `3.3-AI / 2026-08-14-R9`.
2. Fetch live `origin/develop`.
3. Verify whether `616636762687e5c2497db8e0227ee8692311703c` remains an ancestor/current checkpoint.
4. Run/inspect current backend tests before changing behavior.
5. Read this handoff and Sprint evidence.
6. Review `MF02_Completion_and_Hardening_Plan.md`.
7. Do not rebuild completed foundations.
8. Resolve known consistency/hardening issues before increasing lifecycle scope.

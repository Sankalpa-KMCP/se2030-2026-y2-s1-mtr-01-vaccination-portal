# MF-02 Sprint 1 Evidence Record

**Project:** Web-Based Vaccination Portal (`WBVP-SE2030`)
**Function:** MF-02 — Vaccination Campaign, Center and Session Management
**Canonical authority:** `3.3-AI / 2026-08-14-R9`
**Source authority:** canonical Markdown only
**Sprint:** Sprint 1 — **IN PROGRESS**
**MF-02:** **IN PROGRESS**
**US-13:** **IN PROGRESS**
**Evidence synchronized through implementation:** PR #100
**Latest verified implementation merge:** `684a4d6bffbe1d1b21f10ad9fa141b62fb23625a`
**Last updated:** 2026-08-19

> This is an implementation/verification evidence record. It is not the semantic requirements source of truth. Historical test counts are checkpoint-scoped and must not be presented as the current baseline.

## 1. Current Verification Snapshot

### PR #100 — `feat(mf02): implement recurrence rule and series rest api`

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
- endpoints integrated under `/api/v1/recurrence-rules` (9):
  1. `POST /api/v1/recurrence-rules` (Create recurrence rule; 201 Created + `Location: /api/v1/recurrence-rules/{id}`)
  2. `GET /api/v1/recurrence-rules/{id}` (Get recurrence rule by ID; 200 OK)
  3. `GET /api/v1/recurrence-rules` (List rules with optional `campaignId`, `centerId`, `status` filters, logical AND semantics, ordered by `startDate` ASC, `startTime` ASC, `id` ASC; 200 OK)
  4. `PUT /api/v1/recurrence-rules/{id}` (Full replacement of mutable future recurrence definition before occurrences exist; 200 OK)
  5. `POST /api/v1/recurrence-rules/{id}/generate` (Idempotent occurrence generation for full rule span or optional window; 200 OK)
  6. `POST /api/v1/recurrence-rules/{id}/change-preview` (Preview series-wide change against proposed snapshot; 200 OK)
  7. `POST /api/v1/recurrence-rules/{id}/change` (Confirm series-wide change with preview token, reason, actorId; 200 OK)
  8. `POST /api/v1/recurrence-rules/{id}/cancellation-preview` (Preview full series cancellation impact; 200 OK)
  9. `POST /api/v1/recurrence-rules/{id}/cancel` (Confirm full series cancellation with preview token, reason, actorId; 200 OK)
- architecture: strict transport adapter pattern (`RecurrenceRuleController` invokes domain services `SessionRecurrenceRuleService`, `SessionOccurrenceGenerationService`, `SessionSeriesChangeService`; 0 repository dependencies in controller; 0 controller transactions; pure `RecurrenceRuleDtoMapper` handles DTO transformations and reuses `SessionDtoMapper`; existing `@RestControllerAdvice` `GlobalCampaignExceptionHandler` provides structured RFC-aligned `ApiErrorResponse`).
- cancellation semantics: low-level `SessionRecurrenceRuleService.cancelRule()` remains strictly **INTERNAL** (not exposed via REST). Public recurrence cancellation uses the impact-aware series preview/confirmation workflow (`/cancellation-preview` -> `/cancel` invoking `confirmSeriesCancellation`).
- audit routes: no rule-level audit route was added; individual session change events remain queryable via existing `GET /api/v1/sessions/{id}/change-events`.
- runtime constraint & external adapter status: `SessionImpactReader` production adapter is **MISSING** (owned by MF-03). DRAFT recurrence/series operations operate fully with 0/0 impact without the adapter. SCHEDULED impact-aware series operations safely require the adapter and fail fast with `IllegalStateException` -> `HTTP 409 Conflict`. Milestone is qualified as `COMPLETE_WITH_MF03_RUNTIME_CONSTRAINT`.
- DTO immutability: existing `SessionSummaryResponse` and `SessionResponse` were reused completely unchanged (0 diff). `occurrenceKey` and `generationKey` are exposed strictly as read-only transport outputs.
- error & exception taxonomy:
  - 400 Bad Request: `@Valid` payload validation failure, parameter type mismatch, malformed JSON, general `IllegalArgumentException` (including recurrence horizon > 366 days, invalid weekdays, inverted date/time intervals)
  - 404 Not Found: `ResourceNotFoundException` (missing recurrence rule, campaign, or center)
  - 409 Conflict: `IllegalStateException` (stale/invalid preview token, concurrent state conflict, or missing `SessionImpactReader` for SCHEDULED changes)
  - 500 Internal Server Error: unexpected unhandled server exception
- locking & validation preservation: PR #100 preserved existing authoritative locking (`Campaign -> Center -> Rule` for generation; `Campaign -> Center -> Rule -> Sessions sorted by ID ASC` for series changes) with post-lock entity refresh and transactional integrity.
- Thread.sleep audit: **0** occurrences remain under `backend/src/test`.

### Preceding Milestone: PR #97 — `fix(mf02): harden pre-recurrence correctness and safety`

- reviewed source: `ff60fb065bba498a8da05f41c1d3e1e56791c3c6`
- candidate development base: `1b90e2ee4567502a5d90d72af28eda4998f554b7`
- actual merge Parent 1: `fffca9ab7cc3dab0f2aef0a18f8190d49189ab69` (develop advanced safely through unrelated MF-01 screening PR #94 and PR #96 prior to merge)
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
- production code changed: 11 files (8 new, 3 modified)
- test code changed: 3 files (1 new, 2 modified)
- total files: 14 (+1318 / -14 lines)
- functional H2 test executions: **790** (81 XML reports; 0 failures, 0 errors, 0 skipped)
- MySQL Testcontainers assurance test executions: **3** (2 XML reports; 0 failures, 0 errors, 0 skipped)
- logical combined executions: **793** (across separate validation paths)
- previous clean baseline: **763 functional H2 + 3 MySQL** (from PR #90 / PR #91)
- net-new executions: **+27 H2 tests** (+20 `SessionControllerChangeTests`, +3 `SessionDtoMapperTests`, +4 `VaccinationSessionServiceQueryTests`)
- focused regression executions: **84 / 84 PASS** (23 `SessionControllerTests` + 20 `SessionControllerChangeTests` + 6 `SessionDtoMapperTests` + 17 `VaccinationSessionServiceQueryTests` + 18 `VaccinationSessionChangeIntegrationTests`)
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
- production code changed: 7 files (5 new, 2 modified)
- test code changed: 3 files (3 new, 0 modified existing)
- total files: 10
- functional H2 test executions: **763** (80 XML reports; 0 failures, 0 errors, 0 skipped)
- MySQL Testcontainers assurance test executions: **3** (2 XML reports; 0 failures, 0 errors, 0 skipped)
- logical combined executions: **766**
- previous clean baseline: **724 functional H2 + 3 MySQL** (from PR #88 / PR #89)
- net-new executions: **+39 H2 tests** (+23 `SessionControllerTests`, +3 `SessionDtoMapperTests`, +13 `VaccinationSessionServiceQueryTests`)
- endpoints integrated under `/api/v1/sessions` (6): `createSession`, `getSessionById`, `getSessions`, `scheduleSession`, `startSession`, `completeSession`.

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

The value **575** was a non-authoritative arithmetic result derived from a superseded figure and must not be used as verified evidence.

## 3. Sprint Goal and Current Outcome

### Sprint goal
Build the MF-02 backend foundations required to create, validate, schedule, and safely change campaigns, centers, recurrence definitions, and concrete vaccination sessions without violating configuration history or silently mutating MF-03 appointment/waitlist data.

### Current outcome
The Sprint has delivered substantial backend foundations, REST APIs (45 endpoints across campaigns, centers, sessions, and recurrence rules), and lifecycle/change-control behavior, but Sprint 1 remains open because production-layer and remaining lifecycle/integration work is incomplete.

## 4. Completed MF-02 Evidence

### Campaign / configuration foundations
Implemented and integrated:
- VaccinationCampaign
- VaccineType
- TargetGroup
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

### Recurrence / concrete session
Implemented and integrated:
- SessionRecurrenceRule (hardened in PR #97 with 366-day inclusive safety horizon)
- VaccinationSession (hardened in PR #97 with Clock-based early-start rejection)
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
Implemented and integrated under `/api/v1/sessions` (PR #90): 6 REST endpoints.

### Session Change Control REST API (Milestone 4)
Implemented and integrated under `/api/v1/sessions` (PR #92): 7 REST endpoints (`COMPLETE_WITH_MF03_RUNTIME_CONSTRAINT`).

### Recurrence and Series REST API (Milestone 5)
Implemented and integrated under `/api/v1/recurrence-rules` (PR #100): 9 REST endpoints (`COMPLETE_WITH_MF03_RUNTIME_CONSTRAINT`).

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

### Behavior and Implementation Contracts
PR #100 implements and integrates the final Prompt 115 frozen REST API design for Vaccination Session Recurrence / Series Management under `/api/v1/recurrence-rules`:

- **Route Surface (9 endpoints):**
  1. `POST /api/v1/recurrence-rules` (201 Created + `Location: /api/v1/recurrence-rules/{id}`)
  2. `GET /api/v1/recurrence-rules/{id}` (200 OK, returns 404 for missing rule)
  3. `GET /api/v1/recurrence-rules` (200 OK, optional filters: `campaignId`, `centerId`, `status`, deterministic ordering: `startDate ASC, startTime ASC, id ASC`)
  4. `PUT /api/v1/recurrence-rules/{id}` (200 OK, full replacement of mutable future definition before occurrences exist)
  5. `POST /api/v1/recurrence-rules/{id}/generate` (200 OK, supports full rule span or optional requested window bounds)
  6. `POST /api/v1/recurrence-rules/{id}/change-preview` (200 OK, returns proposed diff and SHA-256 preview token)
  7. `POST /api/v1/recurrence-rules/{id}/change` (200 OK, confirms series-wide change with preview token, reason, actorId)
  8. `POST /api/v1/recurrence-rules/{id}/cancellation-preview` (200 OK, no body, calculates impact across future occurrences)
  9. `POST /api/v1/recurrence-rules/{id}/cancel` (200 OK, confirms full series cancellation with preview token, reason, actorId)

- **Cancellation Guard:**
  Low-level `SessionRecurrenceRuleService.cancelRule()` remains strictly **INTERNAL** and is not exposed as a public endpoint. Public recurrence cancellation invokes `SessionSeriesChangeService.confirmSeriesCancellation(...)`, enforcing preview token validation, impact calculation, reason/actor audit recording, and atomic session state updates.

- **DTO and Identifier Rules:**
  Existing `SessionSummaryResponse` and `SessionResponse` DTOs remain completely unchanged (0 diff). `occurrenceKey` and `generationKey` are exposed strictly as read-only transport outputs.

- **Runtime Constraint:**
  `SessionImpactReader` production adapter is **MISSING** (owned by MF-03). DRAFT recurrence/series operations function completely with 0/0 impact without the adapter. SCHEDULED impact-aware series operations safely require the adapter and fail fast with `IllegalStateException` -> `HTTP 409 Conflict`.

### Test Impact
- **Immediate parent baseline:** **882 functional H2 tests** (at `8f58ae232323daa456aaafcffa7ee2384795b226`).
- **Candidate-local functional H2 baseline:** **926 / 926 PASS** across 98 locally verified XML reports (+44 net-new tests: 24 controller, 8 mapper, 12 query).
- **Combined PR #100 Backend CI:** **926 / 926 PASS** (0 failures, 0 errors, 0 skipped, `BUILD SUCCESS`).
- **Focused recurrence/REST regression:** **139 / 139 PASS** ($44 \text{ new} + 95 \text{ existing recurrence/series regression}$).
- **MySQL CI:** **4 / 4 PASS** across 3 classes on `mysql:8.4.3` (`SessionSeriesMySqlConcurrencyIntegrationTests` PASS).
- **Frontend CI:** PASS.

## 14. Concurrency / Transaction Evidence

Current high-risk workflows use transactional boundaries and explicit pessimistic locking where needed.

Representative established lock orders:
- campaign-owned configuration: Campaign
- center-owned calendar writes: Center
- occurrence generation: Campaign -> Center -> Rule
- same-center session change: Campaign -> Center -> Session
- center transfer: Campaign -> Center(min) -> Center(max) -> Session
- series confirmation: Campaign -> Center -> Rule -> Sessions sorted by ID
- campaign scheduling: Campaign
- campaign activation: Campaign
- campaign resumption: Campaign

### Test Database Architecture and Concurrency Assurance
- **Fast Functional In-Memory Suite:** Standard backend tests run against H2 in MySQL compatibility mode (926 tests, 98 locally verified XML reports).
- **Representative Production-Equivalent MySQL Assurance (PR #84 & PR #97):** Dedicated MySQL Testcontainers test suite executed in CI under the `-Pmysql-concurrency` profile against a pinned `mysql:8.4.3` InnoDB container image. Validates single-row duplicate-command race serialization (`CampaignLifecycleMySqlConcurrencyIntegrationTests`), parent/child reverse-impact locking (`CampaignReverseImpactMySqlConcurrencyIntegrationTests`), and series confirmation vs occurrence generation (`SessionSeriesMySqlConcurrencyIntegrationTests`).
- **Scope Boundary:** *Representative single-row lifecycle, parent/child reverse-impact, and series concurrency pessimistic-locking behavior is validated against real MySQL/InnoDB in CI; exact low-level JDBC blocking timing is not instrumented, and non-concurrency domain paths remain verified by the fast H2 suite.*

## 15. Historical Milestone Chronology

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
| #89 | Documentation synchronization after PR #88 |
| #90 | Vaccination Session Core REST API vertical slice (6 endpoints) |
| #91 | Documentation synchronization after PR #90 |
| #92 | Vaccination Session Change Control REST API vertical slice (7 endpoints; COMPLETE_WITH_MF03_RUNTIME_CONSTRAINT) |
| #93 | Documentation synchronization after PR #92 |
| #97 | Pre-Recurrence Correctness and Safety Remediation (F-01, F-02, F-03, F-04, MySQL assurance) |
| #98 | Documentation synchronization after PR #97 |
| #100 | Vaccination Session Recurrence / Series REST API Milestone 5 (9 endpoints; COMPLETE_WITH_MF03_RUNTIME_CONSTRAINT) |

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

## 16. Carried-Forward Findings / Risks

### Domain consistency — RESOLVED (PR #73)
Normal questionnaire assignment creation requires a published version, and PR #73 defensively hardened `scheduleCampaign(...)` to revalidate published status through `QuestionnaireVersionPublicationPort`.

### Pre-Recurrence Correctness and Safety Remediation — RESOLVED (PR #97)
Prerequisite engineering review findings resolved: early session start rejection (F-01), 366-day recurrence safety horizon (F-02), campaign service date consistency (F-03), missing-resource HTTP 404 normalization (F-04), and MySQL series concurrency assurance.

### Recurrence and Series REST API Milestone 5 — RESOLVED (PR #100)
Exposed 9 public REST routes under `/api/v1/recurrence-rules` matching Prompt 115 frozen design. Preserved `cancelRule()` as internal only, while exposing impact-aware preview/confirm series cancellation. Qualified as `COMPLETE_WITH_MF03_RUNTIME_CONSTRAINT`.

### Maintainability — PARTIALLY RESOLVED (PR #75)
- `SessionSeriesChangeService` was successfully decomposed via PR #75, reducing its size from ~1,104 lines to ~694 lines.
- `VaccinationSessionService` remains ~824 lines and is a candidate for future maintainability review if justified (`DEFER`).

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
- *Scope boundary:* Representative single-row lifecycle, parent/child reverse-impact, and series concurrency pessimistic-locking behavior is validated against real MySQL/InnoDB in CI; exact low-level JDBC blocking timing is not instrumented, and non-concurrency domain paths remain verified by the fast H2 suite.

### Audit deletion
Lifecycle/change event repositories inherit normal delete APIs, so “append-only” is not deletion-proof persistence.

## 17. Remaining Sprint / Function Work

Still not complete:
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

## 18. Sprint Status

Current status remains:
- Sprint 1: **IN PROGRESS**
- MF-02: **IN PROGRESS**
- US-13: **IN PROGRESS**
- Campaign Management REST API Milestone 1: **COMPLETE**
- Vaccination Center Management REST API Milestone 2: **COMPLETE**
- Vaccination Session Core REST API Milestone 3: **COMPLETE**
- Vaccination Session Change Control REST API Milestone 4: **COMPLETE_WITH_MF03_RUNTIME_CONSTRAINT**
- Pre-Recurrence Correctness and Safety Remediation: **COMPLETE**
- Vaccination Session Recurrence / Series REST API Milestone 5: **COMPLETE_WITH_MF03_RUNTIME_CONSTRAINT**
- current verified backend baseline: **926 functional H2 tests plus 4 MySQL Testcontainers assurance tests** across separate validation paths.

Do not infer completion from the size of the backend test suite.

## 19. Evidence Maintenance Rules

1. Canonical Markdown is the sole semantic authority.
2. Live Git/code/tests control implementation truth.
3. A documentation-only merge must not be mislabeled as a feature implementation checkpoint.
4. Historical test counts must remain tied to their historical checkpoints.
5. Clean-build counts supersede non-clean/transient counts.
6. Future evidence updates should append concise milestone entries rather than embedding controller transcripts.
7. Before claiming “current,” fetch live `origin/develop`.

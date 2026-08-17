# MF-02 Sprint 1 Evidence Record

**Project:** Web-Based Vaccination Portal (`WBVP-SE2030`)
**Function:** MF-02 — Vaccination Campaign, Center and Session Management
**Canonical authority:** `3.3-AI / 2026-08-14-R9`
**Source authority:** canonical Markdown only
**Sprint:** Sprint 1 — **IN PROGRESS**
**MF-02:** **IN PROGRESS**
**US-13:** **IN PROGRESS**
**Evidence synchronized through implementation:** PR #90
**Latest verified implementation merge:** `9cc7a606260e24e243090f70f5e957b0544bfa3b`
**Last updated:** 2026-08-17

> This is an implementation/verification evidence record. It is not the semantic requirements source of truth. Historical test counts are checkpoint-scoped and must not be presented as the current baseline.

## 1. Current Verification Snapshot

### PR #90 — `feat(mf02): implement vaccination session core and lifecycle rest api`

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
- endpoints integrated under `/api/v1/sessions` (6):
  1. `POST /api/v1/sessions` (Create one-off DRAFT session; 201 Created)
  2. `GET /api/v1/sessions/{id}` (Get session details; 200 OK)
  3. `GET /api/v1/sessions` (List sessions with optional `campaignId`, `centerId`, `status` filters, logical AND semantics, ordered by `startAt` ASC, `id` ASC; 200 OK)
  4. `POST /api/v1/sessions/{id}/schedule` (Transition DRAFT -> SCHEDULED; 200 OK)
  5. `POST /api/v1/sessions/{id}/start` (Transition SCHEDULED -> IN_PROGRESS; 200 OK)
  6. `POST /api/v1/sessions/{id}/complete` (Transition IN_PROGRESS -> COMPLETED; 200 OK)
- architecture: strict transport adapter pattern (`SessionController` invokes domain service `VaccinationSessionService`; 0 repository dependencies in controller; 0 controller transactions; service layer owns `@Transactional` persistence/locking; pure `SessionDtoMapper` handles DTO conversion; existing `@RestControllerAdvice` `GlobalCampaignExceptionHandler` provides structured RFC-aligned `ApiErrorResponse`).
- transport timestamps & timezone: transport uses `Instant startAt`, `Instant endAt`, `Instant bookingCloseAt`. Center entity remains authoritative for scheduling timezone; response includes `scheduleTimezone`. No client-supplied timezone field in `CreateSessionRequest`.
- error & exception taxonomy:
  - 400 Bad Request: `@Valid` payload validation failure, parameter type mismatch, malformed JSON, general `IllegalArgumentException`
  - 404 Not Found: `ResourceNotFoundException` (missing campaign on creation, missing center on creation, missing session on GET / schedule / start / complete)
  - 409 Conflict: `IllegalStateException` (invalid lifecycle state transition or business invariant conflict)
  - 500 Internal Server Error: unexpected unhandled server exception
- query implementation: database-side filtering via `findWithFilters` with optional `campaignId`, `centerId`, `status` using logical `AND` semantics and deterministic `ORDER BY s.startAt ASC, s.id ASC`. Returns 200 `[]` when empty (unpaged).
- locking & validation preservation: PR #90 preserved and exposed existing authoritative locking (`findByIdForUpdate` with post-lock `entityManager.refresh` for creation [Campaign -> Center], schedule [Campaign -> Session], start [Session], complete [Session]) and existing service validation (operating hours, closures, intervals, campaign status).
- note: *The exact reviewed candidate passed CI before zero-drift merge; no additional local post-merge rerun was performed.*
- Thread.sleep audit: **0** occurrences remain under `backend/src/test`.

### Preceding Milestone: PR #88 — `feat(mf02): add vaccination center management REST API`

- reviewed source: `263c26bc9d9a7b51bd81e28d326a90b004c548f0`
- base: `37574bb02e157971c3ca2250d4953834cfe6d132`
- merge: `faf455ff6f07d096258e7cd977765e6e9e25b10f`
- merged at: `2026-08-16T16:46:40Z`
- merge method: normal merge commit (two parents: `37574bb02e157971c3ca2250d4953834cfe6d132` and `263c26bc9d9a7b51bd81e28d326a90b004c548f0`)
- reviewed candidate preserved: YES
- Backend CI: PASS (724 H2 verify + 3 Testcontainers MySQL verify)
- Frontend CI: PASS
- production code changed: 17 files (13 new, 4 modified)
- test code changed: 4 files (3 new, 1 modified fixture fix)
- total files: 21
- functional H2 test executions: **724** (77 XML reports; 0 failures, 0 errors, 0 skipped)
- MySQL Testcontainers assurance test executions: **3** (2 XML reports; 0 failures, 0 errors, 0 skipped)
- logical combined executions: **727**
- previous clean baseline: **680 functional H2 + 3 MySQL** (from PR #86)
- net-new executions: **+44 H2 tests** (+33 `CenterControllerTests`, +4 `CenterDtoMapperTests`, +7 `VaccinationCenterServiceQueryTests`)
- endpoints integrated under `/api/v1/centers` (14):
  - Center Master & Status (6):
    1. `POST /api/v1/centers` (Create center; 201 Created)
    2. `GET /api/v1/centers/{id}` (Get center details; 200 OK)
    3. `GET /api/v1/centers` (List centers with optional `status` filter, ordered by `code` ASC; 200 OK)
    4. `PUT /api/v1/centers/{id}` (Update center details; 200 OK)
    5. `POST /api/v1/centers/{id}/activate` (Activate center; 200 OK)
    6. `POST /api/v1/centers/{id}/deactivate` (Deactivate center with reverse-impact check; 200 OK)
  - Operating Hours (4):
    7. `POST /api/v1/centers/{id}/operating-hours` (Add operating hours; 201 Created)
    8. `GET /api/v1/centers/{id}/operating-hours` (List operating hours for center; 200 OK)
    9. `PUT /api/v1/centers/{centerId}/operating-hours/{hoursId}` (Update operating hours; 200 OK)
    10. `POST /api/v1/centers/{centerId}/operating-hours/{hoursId}/end-date` (End-date operating hours; 200 OK)
  - Center Closures (4):
    11. `POST /api/v1/centers/{id}/closures` (Add center closure; 201 Created)
    12. `GET /api/v1/centers/{id}/closures` (List closures for center; 200 OK)
    13. `PUT /api/v1/centers/{centerId}/closures/{closureId}` (Update future closure; 200 OK)
    14. `POST /api/v1/centers/{centerId}/closures/{closureId}/cancel` (Cancel future closure; 200 OK)
- architecture: strict transport adapter pattern (`CenterController` invokes domain services `VaccinationCenterService`, `OperatingHoursService`, and `CenterClosureService`; 0 repository dependencies in controller; 0 controller transactions; service layer owns `@Transactional` persistence/locking; pure `CenterDtoMapper` handles DTO conversion; existing `@RestControllerAdvice` `GlobalCampaignExceptionHandler` provides structured RFC-aligned `ApiErrorResponse`).
- nested parent-child ownership integrity: nested child mutations (`/centers/{centerId}/operating-hours/{hoursId}`, `/centers/{centerId}/closures/{closureId}`) validate both parent center and child IDs; mismatch throws `ResourceNotFoundException` -> 404 Not Found, preventing cross-center mutation.
- error & exception taxonomy:
  - 400 Bad Request: `@Valid` payload validation failure, parameter type mismatch, malformed JSON, general `IllegalArgumentException` (including normal duplicate center code detected by service precheck)
  - 404 Not Found: `ResourceNotFoundException` (missing Center, OperatingHours, CenterClosure, or nested parent-child mismatch)
  - 409 Conflict: `IllegalStateException` (business invariant conflict, active shift/session collision)
  - 500 Internal Server Error: unexpected unhandled server exception
- duplicate center code note: *Normal duplicate center code is rejected with 400 Bad Request via service precheck. Concurrent duplicate code race is protected by database unique constraint backstop (`CONCURRENT_RACE_DB_BACKSTOP_UNSPECIALIZED`).*
- locking preservation: *PR #88 preserved 100% of underlying `findByIdForUpdate` pessimistic locking, post-lock `entityManager.refresh`, and transactional reverse-impact check invariants.*
- test fixture cleanup note: *`VaccinationCenterConcurrencyIntegrationTests` fixture cleanup was adjusted to delete dependent child entities before centers to avoid foreign key cleanup constraints in H2 (`JUSTIFIED_TEST_FIX`; 0 assertion/concurrency proof changes).*
- note: *The exact reviewed candidate passed CI before zero-drift merge; no additional local post-merge rerun was performed.*
- Thread.sleep audit: **0** occurrences remain under `backend/src/test`.

### Preceding Milestone: PR #86 — `feat(mf02): add campaign management REST API`

- reviewed source: `0fab4afa983252e927dda01278193a584ad9a0d8` (corrective commit resolving missing-campaign 404 lifecycle contract from initial candidate `fef02226ed180a3dc2a9b3195dbd7b598c230e80`)
- base: `9d40ddbd55139a28707c3bdb68fb6a222b5b1c28`
- merge: `a9a71114738ce8aed6c1f0cff8a8f42d955a033c`
- merged at: `2026-08-16T10:46:45Z`
- merge method: normal merge commit (two parents: `9d40ddbd55139a28707c3bdb68fb6a222b5b1c28` and `0fab4afa983252e927dda01278193a584ad9a0d8`)
- reviewed candidate preserved: YES
- Backend CI: PASS (standard H2 verify + dedicated Testcontainers MySQL verify)
- Frontend CI: PASS
- production code changed: 14 files (13 new, 1 modified)
- test code changed: 3 files (3 new)
- total files: 17
- functional H2 test executions: **680** (76 XML reports; 0 failures, 0 errors, 0 skipped)
- MySQL Testcontainers assurance test executions: **3** (2 XML reports; 0 failures, 0 errors, 0 skipped)
- logical combined executions: **683**
- previous clean baseline: **645 functional H2 + 3 MySQL** (from PR #84)
- net-new executions: **+35 H2 tests** (+23 `CampaignControllerTests`, +4 `CampaignDtoMapperTests`, +8 `VaccinationCampaignServiceQueryTests`)
- endpoints integrated under `/api/v1/campaigns` (9):
  1. `POST /api/v1/campaigns` (Create campaign; 201 Created)
  2. `GET /api/v1/campaigns/{id}` (Get campaign details; 200 OK)
  3. `GET /api/v1/campaigns` (List campaigns with optional `status` filter; 200 OK)
  4. `GET /api/v1/campaigns/{id}/lifecycle-events` (Get chronological lifecycle audit history; 200 OK)
  5. `POST /api/v1/campaigns/{id}/schedule` (Transition DRAFT -> SCHEDULED; 200 OK)
  6. `POST /api/v1/campaigns/{id}/publish` (Transition DRAFT/SCHEDULED -> PUBLISHED; 200 OK)
  7. `POST /api/v1/campaigns/{id}/activate` (Transition PUBLISHED -> ACTIVE; 200 OK)
  8. `POST /api/v1/campaigns/{id}/suspend` (Transition to SUSPENDED with mandatory reason and actorId; 200 OK)
  9. `POST /api/v1/campaigns/{id}/resume` (Resume to PUBLISHED or ACTIVE with mandatory reason and actorId; 200 OK)
- architecture: strict transport adapter pattern (`CampaignController` invokes `VaccinationCampaignService`; 0 repository dependencies in controller; 0 controller transactions; service layer owns `@Transactional` persistence/locking; pure `CampaignDtoMapper` handles DTO conversion; `@RestControllerAdvice` `GlobalCampaignExceptionHandler` provides structured RFC-aligned `ApiErrorResponse`).
- error & exception taxonomy:
  - 400 Bad Request: `@Valid` payload validation failure, parameter type mismatch, malformed JSON, general `IllegalArgumentException`
  - 404 Not Found: `ResourceNotFoundException` (missing campaign ID, missing `VaccineType`, missing `TargetGroup`)
  - 409 Conflict: `IllegalStateException` (invalid lifecycle state transition or business invariant conflict)
  - 500 Internal Server Error: unexpected unhandled server exception
- actorId audit boundary: *`actorId` in suspend/resume requests is caller-supplied audit metadata recorded on `CampaignLifecycleEvent`; it is NOT authenticated authorization identity. Spring Security / RBAC enforcement is deferred to a dedicated security milestone.*
- locking preservation: *PR #86 preserved 100% of underlying `findByIdForUpdate` pessimistic locking, post-lock `entityManager.refresh`, and transactional state-machine invariants.*
- note: *The exact reviewed candidate passed CI before zero-drift merge; no additional local post-merge rerun was performed.*
- Thread.sleep audit: **0** occurrences remain under `backend/src/test`.

### Preceding Milestone: PR #84 — `test(mf02): add MySQL concurrency assurance`

- reviewed source: `ded72c17d8bb4aaf222d6e2a96b44fa93208e880` (corrective commit for assertion mismatch from initial candidate `3343817c964ba6fd837a4b609a6bdbb63037fd51`)
- base: `633e6a94585a9549718e16b038ae27e6646347f0`
- merge: `34aa7666dc2d37b1b71faf25a7f51cfba10ee26a`
- merged at: `2026-08-16T07:55:53Z`
- merge method: normal merge commit (two parents: `633e6a94585a9549718e16b038ae27e6646347f0` and `ded72c17d8bb4aaf222d6e2a96b44fa93208e880`)
- reviewed candidate preserved: YES
- Backend CI: PASS (standard H2 verify + dedicated Testcontainers MySQL verify)
- Frontend CI: PASS
- production code changed: **0**
- functional H2 test executions: **645** (71 XML reports; 0 failures, 0 errors, 0 skipped)
- MySQL Testcontainers assurance test executions: **3** (2 XML reports; 0 failures, 0 errors, 0 skipped)
- logical combined executions: **648**
- previous clean baseline: **645** (from PR #82 / PR #83)
- net-new executions: **+0 H2**, **+3 MySQL Testcontainers** (+2 `CampaignLifecycleMySqlConcurrencyIntegrationTests`, +1 `CampaignReverseImpactMySqlConcurrencyIntegrationTests`)
- container environment: pinned `mysql:8.4.3` image, MySQL 8.4.3 InnoDB, `REPEATABLE_READ` transaction isolation, MySQL Connector/J driver, singleton container via `AbstractMySqlTestContainersIntegrationTest`, dynamic datasource via `@DynamicPropertySource`, runtime product assertion via `assertConnectedToRealMySql()`.
- CI execution: dual-step verification in `.github/workflows/backend-ci.yml` invoking `./mvnw verify` (H2 default) then `./mvnw verify -Pmysql-concurrency` with tag `@Tag("mysql-testcontainers")`.
- validated locking topologies:
  - **Topology A (Single-Row Command Race Serialization):** Verified duplicate-command race serialization on MySQL InnoDB for campaign resumption (`simultaneousResumptionSerializesToOneTransitionOnMySql`) and activation (`simultaneousActivationSerializesToOneTransitionOnMySql`). Exactly 1 worker transitions state; competing worker receives `IllegalStateException` and observes post-lock entity state; audit event and final status are strictly validated.
  - **Topology B (Parent/Child Reverse-Impact Serialization):** Verified cross-entity lock contention on MySQL InnoDB (`concurrentSessionCreationAndCampaignDateShorteningSerializeOnMySql`). Campaign `PESSIMISTIC_WRITE` lock held during service date shortening blocks competing session creation attempt; date shortening succeeds, conflicting session creation fails validation, 0 invalid sessions persist in MySQL.
- exact assurance claim: *Representative single-row lifecycle and parent/child reverse-impact pessimistic-locking behavior is validated against real MySQL/InnoDB in CI; exact low-level JDBC blocking timing is not instrumented, and non-concurrency domain paths remain verified by the fast H2 suite.*
- Thread.sleep audit: **0** occurrences remain under `backend/src/test`.

### Preceding Feature Milestone: PR #82 — `feat(mf02): add campaign resumption lifecycle transition`

- reviewed source: `cec16fcb99ae7064ee0804ea2f1cd4578aeb89e4`
- base: `ff3d832745708e5d563c63059820e1e157767a60`
- merge: `5dd7034df64951f061dc27c5596d17a3cb440061`
- merged at: `2026-08-15T19:54:11Z`
- merge method: normal merge commit
- reviewed candidate preserved: YES
- Backend CI: PASS
- Frontend CI: PASS
- clean Maven test: PASS
- clean Maven verify: PASS
- total tests: **645**
- failures: 0
- errors: 0
- skipped: 0
- Surefire XML reports: **71**
- Surefire-summed executions: **645**
- previous clean baseline: **616** (from PR #80 / PR #81)
- net-new executions: **+29** (+4 `VaccinationCampaignModelTests`, +2 `CampaignLifecycleEventModelTests`, +22 `CampaignResumptionIntegrationTests`, +1 `CampaignResumptionConcurrencyIntegrationTests`)
- focused lifecycle regression: **110 / 110 PASS** (0 failures, 0 errors, 0 skipped across 12 test classes)
- Thread.sleep audit: **0** occurrences remain under `backend/src/test`
- behavior: implements canonical `SUSPENDED -> PUBLISHED` (when `today < serviceStartDate`) and `SUSPENDED -> ACTIVE` (when `serviceStartDate <= today <= serviceEndDate`) campaign resumption per Canonical R9 Appendix E.5 under campaign pessimistic write lock (`findByIdForUpdate`) and post-lock `entityManager.refresh`. Requires mandatory non-blank reason (max 500 chars) and actor ID; atomically records append-only `CampaignLifecycleEvent` with change type `RESUMED`. Fails deterministically with `IllegalStateException` when `today > serviceEndDate` without mutating campaign state or creating events. Serializes concurrent resumption requests deterministically without predetermining worker winner identity.
- actor note: *Canonical identifies Campaign Officer / Administrator as authorized actors; enforcement remains an API/security-layer responsibility outside this domain/service change.*
- atomicity note: *Campaign state update and lifecycle event persistence are atomic within a single Spring `@Transactional` service boundary.*

### Preceding Feature Milestone: PR #80 — `feat(mf02): add campaign activation lifecycle transition`

- reviewed source: `fdf6f84938326968347cac2c01d3db6ea72938c4`
- base: `3d21d38777eff40104b46a11773ceeded6d8dece`
- merge: `852802ff0d533fdb013c64b4b10de395efd40557`
- merged at: `2026-08-15T18:20:13Z`
- merge method: normal merge commit
- reviewed candidate preserved: YES
- Backend CI: PASS
- Frontend CI: PASS
- clean Maven test: PASS
- clean Maven verify: PASS
- total tests: **616**
- failures: 0
- errors: 0
- skipped: 0
- Surefire XML reports: 69
- Surefire-summed executions: **616**
- previous clean baseline: **599** (from PR #78 / PR #79)
- net-new executions: **+17** (+2 `VaccinationCampaignModelTests`, +14 `CampaignActivationIntegrationTests`, +1 `CampaignActivationConcurrencyIntegrationTests`)
- focused lifecycle regression: **70 / 70 PASS** (0 failures, 0 errors, 0 skipped)
- Thread.sleep audit: **0** occurrences remain under `backend/src/test`
- behavior: implements canonical `PUBLISHED -> ACTIVE` campaign activation per Canonical R9 Appendix E.5 under campaign pessimistic write lock (`findByIdForUpdate`) and post-lock `entityManager.refresh`. Requires UTC application Clock date to satisfy inclusive service interval `serviceStartDate <= today <= serviceEndDate`. Rejects all non-PUBLISHED statuses and out-of-boundary dates while preserving persisted state. Serializes concurrent activation requests deterministically without predetermining worker winner identity.
- actor note: *Canonical identifies System or Campaign Officer as authorized actors; enforcement remains an API/security-layer responsibility outside this domain/service implementation.*

### Preceding Test-Hardening Milestone: PR #78 — `test(mf02): harden reverse-impact concurrency determinism`

- reviewed source: `2e43665ff0b88027fd82b3577bf73d8357a1ba46`
- base: `6b8bf9c765aaa577b6c29521539e42acb29f70f9`
- merge: `20520c03bdd53329a7fe647a7355beed58ed552e`
- merged at: `2026-08-15T15:51:34Z`
- merge method: normal merge commit
- reviewed candidate preserved: YES
- Backend CI: PASS
- Frontend CI: PASS
- clean Maven test: PASS
- clean Maven verify: PASS
- total tests: **599**
- failures: 0
- errors: 0
- skipped: 0
- Surefire XML reports: 67
- Surefire-summed executions: **599**
- previous clean baseline: **599** (from PR #75)
- net-new executions: **+0** (concurrency test determinism hardening of existing test method)
- target test: PASS (`CampaignAssignmentReverseImpactConcurrencyIntegrationTests`)
- repeated target executions: **20 / 20 PASS** (0 failures, 0 timeouts/hangs)
- MF-02 concurrency regression: **11 suites / 30 tests PASS**
- Thread.sleep audit: **0** occurrences remain under `backend/src/test`
- behavior: eliminated the final `Thread.sleep(300)` timing delay in `CampaignAssignmentReverseImpactConcurrencyIntegrationTests` and replaced it with deterministic latch coordination (`startLatch`, `lockAcquiredLatch`, `competingAttemptLatch` with 5s timeout guards). Replaced ambiguous XOR check with explicit winner and database state assertions.
- note: *Deterministic transaction ordering and serialization are proven; exact JDBC blocking timing is not directly instrumented.*

### Preceding Refactor Milestone: PR #75 — `refactor(mf02): extract series preview calculator`

- reviewed source: `df0852da3be62503be5dc7036cca3039cbc0c3ed`
- base: `b8289635054a6f15d8c611e18e86a0e7a0a64687`
- merge: `cc13082ad94701475b69bc53b699d897486ff7fc`
- merged at: `2026-08-15T08:30:41Z`
- merge method: normal merge commit
- reviewed candidate preserved: YES
- Backend CI: PASS
- Frontend CI: PASS
- clean Maven test: PASS
- clean Maven verify: PASS
- total tests: **599**
- failures: 0
- errors: 0
- skipped: 0
- Surefire XML reports: 67
- Surefire-summed executions: **599**
- previous clean baseline: **591** (from PR #71 / PR #73)
- net-new executions: **+8** (from `SessionSeriesPreviewCalculatorTests`)
- behavior: behavior-preserving maintainability extraction of recurrence diffing, occurrence classification, DST conversion, and SHA-256 fingerprint token generation from `SessionSeriesChangeService` into `SessionSeriesPreviewCalculator`.

### Preceding Hardening Milestone: PR #73 — `fix(mf02): enforce published questionnaire when scheduling`

- reviewed source: `75cd1bb34a0ec6bed32e66387bfa54bca01b4b31`
- base: `5fe33208d11a21ced5c6c1c520dfe01c13858bf0`
- merge: `d3fbb3eb95333d1da0b3e5c33283a1bcf81635e9`
- merged at: `2026-08-15T06:30:15Z`
- merge method: normal merge commit
- reviewed candidate preserved: YES
- Backend CI: PASS
- Frontend CI: PASS
- clean Maven test: PASS
- clean Maven verify: PASS
- total tests: **591**
- failures: 0
- errors: 0
- skipped: 0
- Surefire XML reports: 66
- Surefire-summed executions: **591**
- previous clean baseline: **591** (from PR #71)
- net-new executions: **0** (behavior/assertion hardening within existing 591 suite)

### Preceding Feature Milestone: PR #71 — `feat(mf02): add campaign scheduling approval`

- reviewed source: `422fce3768646310448c3c82efec60c654afa0c7`
- base: `c3bf7c2fb03513d8540a78dcc4ab5d93f322f468`
- total tests: **591** (+22 from PR #69 clean baseline of 569)

## 2. Test-Baseline Reconciliation

| Checkpoint | Authoritative executable tests | Notes |
|---|---:|---|
| PR #61 lifecycle integrity | 523 | historical |
| PR #65 suspension | 554 | historical |
| PR #67 terminal planning guard | 560 | clean-build reconciled; earlier 566 reporting superseded |
| PR #69 SUSPENDED planning freeze | 569 | +9 from clean PR #67 baseline |
| PR #71 campaign scheduling approval | **591** | +22 from PR #69/current pre-PR71 baseline |
| PR #73 questionnaire invariant hardening | **591** | +0 net-new (assertion hardening & fixture alignment) |
| PR #75 series preview calculator decomposition | **599** | +8 from PR #73 baseline (source: `SessionSeriesPreviewCalculatorTests`) |
| PR #78 deterministic concurrency hardening | **599** | +0 net-new (concurrency test determinism hardening) |
| PR #80 campaign activation (PUBLISHED -> ACTIVE) | **616** | +17 from PR #78/PR #79 baseline (+2 model, +14 integration, +1 concurrency) |
| PR #82 campaign resumption (SUSPENDED -> PUBLISHED / ACTIVE) | **645** | +29 from PR #80/PR #81 baseline (+4 model, +2 event model, +22 integration, +1 concurrency) |
| PR #84 representative MySQL concurrency assurance | **645 functional H2 + 3 MySQL** | +0 H2, +3 MySQL Testcontainers assurance (**648 logical executions**) |
| PR #86 campaign management REST API (9 endpoints) | **680 functional H2 + 3 MySQL** | +35 H2 (+23 controller, +4 mapper, +8 service query), +3 MySQL Testcontainers assurance (**683 logical executions**) |
| PR #88 vaccination center management REST API (14 endpoints) | **724 functional H2 + 3 MySQL** | +44 H2 (+33 controller, +4 mapper, +7 service query), +3 MySQL Testcontainers assurance (**727 logical executions**) |
| PR #90 vaccination session core REST API (6 endpoints) | **763 functional H2 + 3 MySQL** | +39 H2 (+23 controller, +3 mapper, +13 service query), +3 MySQL Testcontainers assurance (**766 logical executions**) |

The value **575** was a non-authoritative arithmetic result derived from the superseded 566 figure and must not be used as verified evidence.

## 3. Sprint Goal and Current Outcome

### Sprint goal
Build the MF-02 backend foundations required to create, validate, schedule, and safely change campaigns, centers, recurrence definitions, and concrete vaccination sessions without violating configuration history or silently mutating MF-03 appointment/waitlist data.

### Current outcome
The Sprint has delivered substantial backend foundations and lifecycle/change-control behavior, but Sprint 1 remains open because production-layer and remaining lifecycle/integration work is incomplete.

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
- campaign service-date reverse-impact protection

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
- SessionRecurrenceRule
- VaccinationSession
- schedule/timezone/provenance invariants
- authoritative `VaccinationSessionValidator`
- one-off DRAFT creation
- recurrence occurrence generation
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
- series preview/diff
- series confirmation
- full-series cancellation
- preserved individual exceptions
- deterministic locking and rollback behavior

### Campaign lifecycle / lifecycle integrity
Implemented and integrated:
- controlled campaign creation in DRAFT
- generic campaign status setter removed
- publication from DRAFT/SCHEDULED
- suspension from PUBLISHED/ACTIVE
- terminal-state session-planning guard
- SUSPENDED planning freeze
- DRAFT -> SCHEDULED approval
- PUBLISHED -> ACTIVE campaign activation
- SUSPENDED -> PUBLISHED / ACTIVE campaign resumption
- representative MySQL/Testcontainers concurrency assurance (single-row command locking + parent/child reverse-impact locking)

### Campaign REST API (Milestone 1)
Implemented and integrated under `/api/v1/campaigns` (PR #86):
- 9 REST endpoints: `createCampaign`, `getCampaignById`, `getCampaigns` (status filter), `getCampaignLifecycleEvents`, `scheduleCampaign`, `publishCampaign`, `activateCampaign`, `suspendCampaign`, `resumeCampaign`
- Strict DTO request/response contracts and pure `CampaignDtoMapper`
- `@RestControllerAdvice` (`GlobalCampaignExceptionHandler`) with structured `ApiErrorResponse`
- Typed `ResourceNotFoundException -> 404` error mapping across all resource lookup and lifecycle endpoints
- Preservation of underlying `@Transactional` boundaries, `findByIdForUpdate` pessimistic locking, and `entityManager.refresh`
- Unauthenticated caller-supplied audit metadata semantics for `actorId`

### Center REST API (Milestone 2)
Implemented and integrated under `/api/v1/centers` (PR #88):
- 14 REST endpoints:
  - Center Master & Status (6): `POST /api/v1/centers`, `GET /api/v1/centers/{id}`, `GET /api/v1/centers` (status filter, ordered by `code` ASC), `PUT /api/v1/centers/{id}`, `POST /api/v1/centers/{id}/activate`, `POST /api/v1/centers/{id}/deactivate`
  - Operating Hours (4): `POST /api/v1/centers/{id}/operating-hours`, `GET /api/v1/centers/{id}/operating-hours`, `PUT /api/v1/centers/{centerId}/operating-hours/{hoursId}`, `POST /api/v1/centers/{centerId}/operating-hours/{hoursId}/end-date`
  - Center Closures (4): `POST /api/v1/centers/{id}/closures`, `GET /api/v1/centers/{id}/closures`, `PUT /api/v1/centers/{centerId}/closures/{closureId}`, `POST /api/v1/centers/{centerId}/closures/{closureId}/cancel`
- Strict DTO request/response contracts and pure `CenterDtoMapper`
- Reused `@RestControllerAdvice` (`GlobalCampaignExceptionHandler`) with structured `ApiErrorResponse`
- Typed `ResourceNotFoundException -> 404` error mapping for missing entities and nested parent-child ownership mismatches
- Strict parent-child ownership verification on all child mutations (`/centers/{centerId}/operating-hours/{hoursId}`, `/centers/{centerId}/closures/{closureId}`)
- Preservation of underlying `@Transactional` boundaries, parent `findByIdForUpdate` pessimistic locking, and `entityManager.refresh`
- Service precheck rejection of normal duplicate center code (400 Bad Request); DB unique constraint backstop for concurrent duplicate race

### Session Core REST API (Milestone 3)
Implemented and integrated under `/api/v1/sessions` (PR #90):
- 6 REST endpoints: `createSession`, `getSessionById`, `getSessions` (optional `campaignId`, `centerId`, `status` filters), `scheduleSession`, `startSession`, `completeSession`
- Strict DTO request/response contracts (`CreateSessionRequest`, `SessionResponse`, `SessionSummaryResponse`) and pure `SessionDtoMapper`
- Reused `@RestControllerAdvice` (`GlobalCampaignExceptionHandler`) with structured `ApiErrorResponse`
- Typed `ResourceNotFoundException -> 404` error mapping across missing campaign/center on creation and missing session on GET/lifecycle transitions
- Database-side filtering with optional parameters, logical `AND` semantics, and deterministic `ORDER BY startAt ASC, id ASC`
- Preservation of underlying `@Transactional` boundaries, `findByIdForUpdate` pessimistic locking, `entityManager.refresh`, and domain schedule validation
- Transport timestamps modeled as `Instant` with Center entity retaining timezone authority and exposing `scheduleTimezone` in response

## 5. PR #71 — Campaign `DRAFT -> SCHEDULED` Evidence

### Behavior
PR #71 adds controlled internal campaign approval:
- DRAFT only -> SCHEDULED
- Campaign pessimistic write lock
- authoritative post-lock refresh
- all prerequisites validated before transition

### Date rule
`serviceStartDate` must be strictly later than `LocalDate.now(clock)`.

Repository application Clock is UTC, making the rule deterministic against UTC civil date.

### Required campaign configuration
- valid campaign date / booking-window invariants
- ACTIVE VaccineType
- ACTIVE TargetGroup
- exactly one effective CampaignDoseProgramme at service start
- exactly one effective CampaignQuestionnaireAssignment at service start

### Center policy P2
A campaign can be scheduled only when:
- at least one ACTIVE CampaignCenterAssignment overlaps the campaign service period;
- every overlapping ACTIVE assignment references an ACTIVE center.

Non-overlapping assignments do not block. Full-period coverage, OperatingHours, CenterClosure, recurrence rules, and concrete sessions are not prerequisites for campaign scheduling approval.

### Publication boundary
In PR #71, the scheduling command did not independently require the linked questionnaire version publication-port check, relying on assignment creation validation. This was subsequently hardened in PR #73 to defensively revalidate publication status at scheduling time.

### Tests added
PR #71 adds 22 executions covering:
- DRAFT -> SCHEDULED domain transition
- non-DRAFT rejection
- same-day/past/future boundaries
- inactive vaccine type / target group
- missing/ambiguous dose programme
- missing questionnaire assignment
- zero/cancelled/non-overlapping center assignments
- inclusive/half-open overlap behavior
- inactive/mixed centers
- non-overlapping inactive center not blocking
- concurrent scheduling serialization

## 6. PR #73 — Campaign Questionnaire Invariant Hardening Evidence

### Invariant and Behavior
PR #73 defensively enforces the canonical invariant (`MF02-BR-02`, Section 11 Data Model) that an effective `CampaignQuestionnaireAssignment` must reference a `PUBLISHED` MF-01 `QuestionnaireVersion` at the moment `scheduleCampaign(...)` is evaluated.

- Revalidates `QuestionnaireVersionPublicationPort.isPublished(questionnaireVersionId)` under the pessimistic campaign lock before status mutation.
- Throws `IllegalStateException` on un-published questionnaire versions.
- Preserves campaign status in `DRAFT` upon validation failure.
- Aligned `CampaignSchedulingIntegrationTests.unpublishedQuestionnaireVersionRejectsScheduling` to assert deterministic rejection and state preservation.
- Configured `CampaignSchedulingConcurrencyIntegrationTests` fixture with a valid published questionnaire port.

### Test Impact
- Executable tests: **591** (net-new: 0; transformed contradictory test into invariant rejection test).
- Failures: 0, Errors: 0, Skipped: 0.
- Maven `test`: PASS, Maven `verify`: PASS.

## 7. PR #75 — Series Preview Calculator Extraction Refactor Evidence

### Behavior and Decomposition
PR #75 delivered a behavior-preserving maintainability refactoring of the oversized `SessionSeriesChangeService` (~1,104 lines -> ~694 lines) by extracting deterministic series preview calculation and SHA-256 fingerprint token generation into `SessionSeriesPreviewCalculator` (~454 lines).

- **`SessionSeriesChangeService` retains:** All `@Transactional` boundaries, 5 Spring Data JPA repositories, `EntityManager`, hierarchical pessimistic locking (`Campaign -> Center -> Rule -> Sessions ASC`), post-lock `refresh(...)`, override-history resolution via `findOverriddenSessionIds`, stale-preview verification, mutations, and audit persistence.
- **`SessionSeriesPreviewCalculator` owns:** Recurrence diffing, occurrence classification (`RETAINED`, `REMOVED`, `NEW`, `INELIGIBLE`, `PRESERVED_EXCEPTION`), DST conversion, deterministic SHA-256 series preview token generation, schedule validation delegation, and conditional `SessionImpactReader` evaluation.
- **Calculator properties:** 0 JPA repositories, 0 `EntityManager` references, 0 `@Transactional` annotations. It is repository-free and transaction-free, while conditionally calls `SessionImpactReader` on demand to preserve lazy impact evaluation semantics (`testMissingImpactReaderAllowedWhenScheduledRetainedOccurrencesUnchanged`).

### Test Impact
- Executable tests: **599** (+8 net-new tests from `SessionSeriesPreviewCalculatorTests`).
- Failures: 0, Errors: 0, Skipped: 0.
- Surefire reports: 67 XML reports.
- Maven `test`: PASS, Maven `verify`: PASS (BUILD SUCCESS).
- Backend CI: PASS, Frontend CI: PASS.

## 8. PR #78 — Reverse-Impact Concurrency Determinism Hardening Evidence

### Behavior and Latch Synchronization
PR #78 eliminated the final `Thread.sleep(300)` timing delay in `CampaignAssignmentReverseImpactConcurrencyIntegrationTests.java` and established deterministic latch coordination:
- **`startLatch` (count: 1):** Synchronizes worker thread startup across Thread 1 and Thread 2.
- **`lockAcquiredLatch` (count: 1):** Signaled by Task A (Thread 1) after acquiring the `PESSIMISTIC_WRITE` row lock on the campaign via `campaignRepository.findByIdForUpdate(campaignId)`.
- **`competingAttemptLatch` (count: 1):** Signaled by Task B (Thread 2) inside its transaction immediately prior to calling `sessionService.createOneOffDraftSession(...)`.
- **Two-way handshake:** Task A remains inside its open transaction holding the database row lock while awaiting `competingAttemptLatch`.
- **5-second failure guards:** All latch awaits enforce 5-second failure bounds, eliminating test hangs and deadlocks.
- **Explicit winner assertions:** Replaced ambiguous XOR outcome check with explicit assertions:
  - `assertTrue(shortenSuccess.get(), "Campaign service date shortening must succeed");`
  - `assertFalse(createSuccess.get(), "Conflicting session creation outside shortened range must fail");`
  - `assertEquals(LocalDate.of(2026, 6, 15), persistedCampaign.getServiceStartDate());`
  - `assertEquals(0, sessionRepository.count(), "No invalid session outside shortened campaign service dates should be persisted");`

*Note on accepted review interpretation:* Deterministic transaction ordering and serialization are proven; exact JDBC blocking timing is not directly instrumented.

### Test Impact
- Executable tests: **599** (net-new: 0; hardening of existing test method).
- Failures: 0, Errors: 0, Skipped: 0.
- Surefire reports: 67 XML reports.
- Target test: PASS (`CampaignAssignmentReverseImpactConcurrencyIntegrationTests`).
- Repeated target runs: **20 / 20 PASS** (0 failures, 0 timeouts/hangs).
- Concurrency regression: **11 suites / 30 tests PASS** (100%).
- Maven `test`: PASS, Maven `verify`: PASS (BUILD SUCCESS).
- Backend CI: PASS, Frontend CI: PASS.
- Thread.sleep audit: **0** occurrences remain under `backend/src/test`.

## 9. PR #80 — Campaign Activation (PUBLISHED -> ACTIVE) Evidence

### Behavior and Contract
PR #80 implements authoritative campaign activation according to Canonical R9 Appendix E.5:
- **Domain method:** `VaccinationCampaign.activate()` transitions status from `PUBLISHED` to `ACTIVE`; all other source statuses fail with `IllegalStateException`.
- **Service method:** `VaccinationCampaignService.activateCampaign(Long campaignId)` acquires campaign pessimistic write lock (`findByIdForUpdate`), refreshes authoritative state (`entityManager.refresh`), validates source status `PUBLISHED`, derives `today` from injected UTC `Clock`, and validates the inclusive service date interval `serviceStartDate <= today <= serviceEndDate`.
- **State integrity:** Failed activation attempts leave the database state unmutated in `PUBLISHED`.
- **Concurrency serialization:** Concurrent activation requests are serialized via row locking. Exactly one contender transitions `PUBLISHED -> ACTIVE`, while competing contenders observe `ACTIVE` and fail with `IllegalStateException: Cannot activate campaign in ACTIVE status`. Final persisted status is `ACTIVE`. Winner identity is not predetermined.
- **Child sessions / dependencies:** Zero child session mutations and zero new cross-module dependencies.
- **Actor boundary:** *Canonical identifies System or Campaign Officer as authorized actors; enforcement remains an API/security-layer responsibility outside this domain/service implementation.*

### Test Impact
- Executable tests: **616** (+17 net-new tests: +2 `VaccinationCampaignModelTests`, +14 `CampaignActivationIntegrationTests`, +1 `CampaignActivationConcurrencyIntegrationTests`).
- Failures: 0, Errors: 0, Skipped: 0.
- Surefire reports: **69 XML reports** (67 prior + 2 new test classes).
- Focused lifecycle regression: **70 / 70 PASS** (0 failures, 0 errors, 0 skipped).
- Maven `test`: PASS, Maven `verify`: PASS (BUILD SUCCESS).
- Backend CI: PASS, Frontend CI: PASS.
- Thread.sleep audit: **0** occurrences remain under `backend/src/test`.

## 10. PR #82 — Campaign Resumption (SUSPENDED -> PUBLISHED / ACTIVE) Evidence

### Behavior and Contract
PR #82 implements authoritative campaign resumption according to Canonical R9 Appendix E.5:
- **Domain methods:** `VaccinationCampaign.resumeToPublished()` and `VaccinationCampaign.resumeToActive()` transition status from `SUSPENDED` to `PUBLISHED` or `ACTIVE` respectively; all other source statuses fail with `IllegalStateException`.
- **Service method:** `VaccinationCampaignService.resumeCampaign(Long campaignId, String reason, String actorId)`:
  - Validates non-null/non-blank reason (max 500 chars) and actorId (max 64 chars);
  - Acquires campaign pessimistic write lock via `campaignRepository.findByIdForUpdate(campaignId)`;
  - Refreshes authoritative entity state via `entityManager.refresh(campaign)`;
  - Requires source status `SUSPENDED` (rejects all other 7 statuses with `IllegalStateException`);
  - Derives `today` from injected application `Clock` (`LocalDate.now(clock)`);
  - Evaluates date-driven target:
    - `today < serviceStartDate` -> transitions `SUSPENDED -> PUBLISHED` ("Resume before service start");
    - `serviceStartDate <= today <= serviceEndDate` -> transitions `SUSPENDED -> ACTIVE` ("Resume during service period");
    - `today > serviceEndDate` -> fails deterministically with `IllegalStateException` ("Campaign service end date has already passed"), preserving `SUSPENDED` status with zero lifecycle events;
  - Persists updated campaign via `campaignRepository.save(campaign)`;
  - Creates and persists append-only `CampaignLifecycleEvent` recording `changeType == RESUMED`, `previousStatus == SUSPENDED`, `newStatus`, `reason`, `actorId`, and `createdAt` from `clock.instant()`;
  - Returns the managed `VaccinationCampaign` entity.
- **Transactional atomicity:** Campaign status transition and `CampaignLifecycleEvent` persistence are executed within the same Spring `@Transactional` service boundary. Failure in event creation/persistence rolls back campaign update.
- **Concurrency serialization:** Concurrent resumption requests are serialized via row locking. Exactly one contender transitions the status and persists a `RESUMED` event, while competing contenders observe the non-`SUSPENDED` state and fail with `IllegalStateException`. Final persisted status matches the date-derived target; exactly one `RESUMED` event persists. Winner identity is not predetermined.
- **Child sessions / dependencies:** Zero child session or appointment modifications.
- **Actor boundary:** *Canonical identifies Campaign Officer / Administrator as authorized actors. Actor ID is captured as audit data; authorization enforcement remains an API/security-layer responsibility outside this domain/service change.*

### Test Impact
- Executable tests: **645** (+29 net-new tests: +4 `VaccinationCampaignModelTests`, +2 `CampaignLifecycleEventModelTests`, +22 `CampaignResumptionIntegrationTests`, +1 `CampaignResumptionConcurrencyIntegrationTests`).
- Failures: 0, Errors: 0, Skipped: 0.
- Surefire reports: **71 XML reports** (69 prior + 2 new test classes).
- Focused lifecycle regression: **110 / 110 PASS** (0 failures, 0 errors, 0 skipped across 12 test classes).
- Maven `test`: PASS, Maven `verify`: PASS (BUILD SUCCESS).
- Backend CI: PASS, Frontend CI: PASS.
- Thread.sleep audit: **0** occurrences remain under `backend/src/test`.

## 11. Concurrency / Transaction Evidence

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

Separate-transaction concurrency regression tests exist for real race conditions.

### Test Database Architecture and Concurrency Assurance
- **Fast Functional In-Memory Suite:** Standard backend tests run against H2 in MySQL compatibility mode (763 tests, 80 XML reports). This provides fast structural and domain validation.
- **Representative Production-Equivalent MySQL Assurance (PR #84):** Dedicated MySQL Testcontainers test suite executed in CI under the `-Pmysql-concurrency` profile against a pinned `mysql:8.4.3` InnoDB container image. Validates single-row duplicate-command race serialization (`CampaignLifecycleMySqlConcurrencyIntegrationTests`) and parent/child reverse-impact locking (`CampaignReverseImpactMySqlConcurrencyIntegrationTests`).
- **Scope Boundary:** *Representative single-row lifecycle and parent/child reverse-impact pessimistic-locking behavior is validated against real MySQL/InnoDB in CI; exact low-level JDBC blocking timing is not instrumented, and non-concurrency domain paths remain verified by the fast H2 suite.*

## 12. Historical Milestone Chronology

This table preserves the engineering sequence without retaining stale controller transcripts.

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

## 13. Carried-Forward Findings / Risks

### Domain consistency — RESOLVED (PR #73)
Normal questionnaire assignment creation requires a published version, and PR #73 defensively hardened `scheduleCampaign(...)` to revalidate published status through `QuestionnaireVersionPublicationPort`, replacing the bypassed fixture with an active rejection test.

### Maintainability — PARTIALLY RESOLVED (PR #75)
- `SessionSeriesChangeService` was successfully decomposed via PR #75, reducing its size from ~1,104 lines to ~694 lines.
- `VaccinationSessionService` remains ~824 lines and is a candidate for future maintainability review if justified.

### Optional critical ports
Production-facing adapters for questionnaire publication (provided by MF-01 `QuestionnaireVersionService` in PR #76) and MF-03 session impact are still optional-injection boundaries in the current code. Production configuration should ultimately fail fast when required adapters are absent.

### Concurrency test quality — RESOLVED (PR #78)
PR #78 eliminated the remaining `Thread.sleep(300)` timing delay from `CampaignAssignmentReverseImpactConcurrencyIntegrationTests.java` and established deterministic latch coordination with explicit winner/state assertions. Zero `Thread.sleep` calls remain across the backend test suite.

### Representative MySQL Testcontainers concurrency assurance — RESOLVED (PR #84)
PR #84 closed the representative database locking assurance gap by introducing containerized MySQL 8.4.3 InnoDB integration tests via Testcontainers:
- Validated single-row duplicate-command race serialization against MySQL InnoDB for campaign resumption and activation (`CampaignLifecycleMySqlConcurrencyIntegrationTests`).
- Validated parent/child reverse-impact pessimistic-locking serialization against MySQL InnoDB for campaign service-date shortening versus concurrent conflicting session creation (`CampaignReverseImpactMySqlConcurrencyIntegrationTests`).
- Automated in GitHub Actions CI under dedicated `-Pmysql-concurrency` profile step.
- *Scope boundary:* Representative single-row lifecycle and parent/child reverse-impact pessimistic-locking behavior is validated against real MySQL/InnoDB in CI; exact low-level JDBC blocking timing is not instrumented, and non-concurrency domain paths remain verified by the fast H2 suite.

### Audit deletion
Lifecycle/change event repositories inherit normal delete APIs, so “append-only” is not deletion-proof persistence.

## 14. Remaining Sprint / Function Work

Still not complete:
- design and implementation of remaining campaign lifecycle transitions: completion (`SPECIFICATION_PARTIAL`), campaign cancellation (`MF03_BLOCKED`), archival (`USER_DATA_REQUIRED`)
- design and implementation of Vaccination Session Change Control REST API (cancellation, reschedule, transfer, preview/confirm, change-event history)
- design and implementation of Vaccination Session Recurrence / Series REST API (recurrence rule REST, occurrence generation, series preview/confirm, series cancellation)
- evaluation/decomposition of `VaccinationSessionService` maintainability only if justified
- archival retention value (`USER_DATA_REQUIRED`)
- DRAFT parent-campaign / child-session scheduling policy (`IMPLEMENTATION_DEFINED / unresolved policy`)
- MF-01 production adapter/FK
- MF-03 production impact adapter
- transactional outbox
- Campaign date update REST API
- Spring Security/RBAC
- OpenAPI
- Flyway migrations
- representative production MySQL concurrency assurance (RESOLVED in PR #84; broad production database certification deferred)
- MF-02 frontend
- frontend tests
- final end-to-end / Definition-of-Done evidence

## 15. Sprint Status

Current status remains:
- Sprint 1: **IN PROGRESS**
- MF-02: **IN PROGRESS**
- US-13: **IN PROGRESS**
- Campaign Management REST API Milestone 1: **COMPLETE**
- Vaccination Center Management REST API Milestone 2: **COMPLETE**
- Vaccination Session Core REST API Milestone 3: **COMPLETE**
- Vaccination Session Change Control REST API: **PENDING**
- Vaccination Session Recurrence / Series REST API: **PENDING**

Do not infer completion from the size of the backend test suite.

## 16. Evidence Maintenance Rules

1. Canonical Markdown is the sole semantic authority.
2. Live Git/code/tests control implementation truth.
3. A documentation-only merge must not be mislabeled as a feature implementation checkpoint.
4. Historical test counts must remain tied to their historical checkpoints.
5. Clean-build counts supersede non-clean/transient counts.
6. Future evidence updates should append concise milestone entries rather than embedding controller transcripts.
7. Before claiming “current,” fetch live `origin/develop`.

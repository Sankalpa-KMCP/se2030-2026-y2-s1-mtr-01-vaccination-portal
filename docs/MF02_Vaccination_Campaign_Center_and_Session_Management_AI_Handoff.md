# MF-02 — Vaccination Campaign, Center and Session Management
## AI Engineering Handoff

**Project:** Web-Based Vaccination Portal (`WBVP-SE2030`)
**Function:** MF-02 — Vaccination Campaign, Center and Session Management
**Canonical authority:** `3.3-AI / 2026-08-14-R9`
**Canonical source model:** Markdown-only; the canonical Markdown file is the sole normative semantic source.
**Sprint:** Sprint 1 — **IN PROGRESS**
**MF-02:** **IN PROGRESS**
**US-13:** **IN PROGRESS**
**Last synchronized implementation checkpoint:** PR #92
**Reviewed PR #92 source:** `ccf5c3d58035d35e56d316207848d58388446547`
**PR #92 base:** `1531ad9f89e3dc75b174cc3deb3a2204e2712a52`
**PR #92 merge:** `6ff63bdd04f34d3bd334d011fdf55dee8010a140`
**Preceding documentation synchronization milestone:** PR #91 (`debc5666f2284991507df089ea49d0dd1310114a` / merge `1531ad9f89e3dc75b174cc3deb3a2204e2712a52`)
**Preceding session core REST milestone:** PR #90 (`3324ab4b2a84960dea946c531115b5aee829ad61` / merge `9cc7a606260e24e243090f70f5e957b0544bfa3b`)
**Preceding documentation synchronization milestone:** PR #89 (`77c9fbeb75e268cadf519fcb1fa297822712f7bb` / merge `4eaa8cd31c04aedf23f3887f25de27ec924165b8`)
**Preceding center REST milestone:** PR #88 (`263c26bc9d9a7b51bd81e28d326a90b004c548f0` / merge `faf455ff6f07d096258e7cd977765e6e9e25b10f`)
**Preceding documentation synchronization milestone:** PR #87 (`9371e0e14f07670175674b79ec98978096136158` / merge `37574bb02e157971c3ca2250d4953834cfe6d132`)
**Preceding campaign REST milestone:** PR #86 (`0fab4afa983252e927dda01278193a584ad9a0d8` / merge `a9a71114738ce8aed6c1f0cff8a8f42d955a033c`)
**Preceding documentation synchronization milestone:** PR #85 (`ac860a9d271daa2ecf4211571e44a93a2f2622f4` / merge `9d40ddbd55139a28707c3bdb68fb6a222b5b1c28`)
**Preceding MySQL assurance milestone:** PR #84 (`ded72c17d8bb4aaf222d6e2a96b44fa93208e880` / merge `34aa7666dc2d37b1b71faf25a7f51cfba10ee26a`)
**Preceding documentation synchronization milestone:** PR #83 (`5753f03855800f24e176b91d4efefab34177b472` / merge `633e6a94585a9549718e16b038ae27e6646347f0`)
**Preceding resumption milestone:** PR #82 (`cec16fcb99ae7064ee0804ea2f1cd4578aeb89e4` / merge `5dd7034df64951f061dc27c5596d17a3cb440061`)
**Preceding documentation synchronization milestone:** PR #81 (`7200790d4cdad0af5600317b0476870c48fd0df0` / merge `ff3d832745708e5d563c63059820e1e157767a60`)
**Preceding activation milestone:** PR #80 (`fdf6f84938326968347cac2c01d3db6ea72938c4` / merge `852802ff0d533fdb013c64b4b10de395efd40557`)
**Preceding test-hardening milestone:** PR #78 (`2e43665ff0b88027fd82b3577bf73d8357a1ba46`)
**Current verified backend baseline:** **790 functional H2 tests** (81 XML reports) plus **3 MySQL/Testcontainers assurance tests** (2 XML reports); producing **793 logical executions** across the two validation paths. Clean Maven test/verify PASS; Backend CI PASS; Frontend CI PASS.
**PR #92 net-new executions:** +27 H2 tests (+20 `SessionControllerChangeTests`, +3 `SessionDtoMapperTests`, +4 `VaccinationSessionServiceQueryTests`).
**Thread.sleep in tests:** 0 occurrences across `backend/src/test`.

> This file is a maintained engineering snapshot. Canonical Markdown controls semantics; live Git/code/tests control implementation truth. This handoff must not be used to override canonical rules.

## 1. Authority Order

Use this order when facts conflict:

1. `docs/Web_Based_Vaccination_Portal_AI_Canonical_Context_v3.3.md` — semantic/business source of truth.
2. Live `origin/develop`, merged PRs, production code, tests, and CI — implementation truth.
3. `docs/MF02_Vaccination_Campaign_Center_and_Session_Management_AI_Handoff.md` — current MF-02 engineering state.
4. `docs/evidence/sprint-1/MF02_Sprint1_Evidence.md` — implementation/verification chronology.
5. `PROJECT_CONTEXT.md` — orientation summary only.
6. `MF02_Completion_and_Hardening_Plan.md` — planned work, not evidence of implementation.

Future agents must fetch live `origin/develop` before treating `6ff63bdd04f34d3bd334d011fdf55dee8010a140` as the repository tip.

## 2. Current Milestone

### Milestone reached

**MF-02 VACCINATION SESSION CHANGE CONTROL REST API (7 ENDPOINTS; COMPLETE_WITH_MF03_RUNTIME_CONSTRAINT), VACCINATION SESSION CORE REST API (6 ENDPOINTS), VACCINATION CENTER MANAGEMENT REST API (14 ENDPOINTS), CAMPAIGN MANAGEMENT REST API (9 ENDPOINTS), MYSQL/TESTCONTAINERS CONCURRENCY ASSURANCE, CAMPAIGN RESUMPTION, CAMPAIGN ACTIVATION, SCHEDULING APPROVAL, AND SESSION-CHANGE FOUNDATIONS ARE INTEGRATED.**

This does **not** mean MF-02 is complete.

The current checkpoint includes the Vaccination Session Change Control REST API vertical slice (PR #92), the Vaccination Session Core REST API vertical slice (PR #90), the Vaccination Center Management REST API vertical slice (PR #88), the Campaign Management REST API vertical slice (PR #86), representative production-equivalent MySQL/Testcontainers concurrency assurance (PR #84), authoritative campaign resumption `SUSPENDED -> PUBLISHED / ACTIVE` (PR #82), authoritative campaign activation `PUBLISHED -> ACTIVE` (PR #80), deterministic reverse-impact concurrency test hardening (PR #78), the behavior-preserving extraction of `SessionSeriesPreviewCalculator` from `SessionSeriesChangeService` (PR #75), defensive questionnaire publication invariant revalidation (PR #73), and campaign scheduling approval (PR #71) on top of the already integrated campaign, center, recurrence, session, reverse-impact, publication, suspension, terminal-planning, and SUSPENDED-planning foundations.

Total MF-02 REST routes currently integrated: **36** ($9 + 14 + 6 + 7 = 36$). Total Session routes: **13** ($6 + 7 = 13$).

### Current verified integrations

1. **PR #92 — `feat(mf02): add session change control REST API`**
   - reviewed source: `ccf5c3d58035d35e56d316207848d58388446547`
   - merge: `6ff63bdd04f34d3bd334d011fdf55dee8010a140`
   - base: `1531ad9f89e3dc75b174cc3deb3a2204e2712a52`
   - merge method: normal merge commit (two parents: `1531ad9f89e3dc75b174cc3deb3a2204e2712a52` and `ccf5c3d58035d35e56d316207848d58388446547`)
   - Backend CI: PASS (790 H2 executions, 0 failures, 0 errors, 0 skipped; 3 MySQL Testcontainers executions, 0 failures, 0 errors, 0 skipped)
   - Frontend CI: PASS
   - authoritative backend baseline: **790 functional H2 tests** across 81 Surefire XML reports + **3 MySQL assurance tests** (793 logical executions across separate validation paths)
   - net-new executions: **+27 H2 tests** (+20 `SessionControllerChangeTests`, +3 `SessionDtoMapperTests`, +4 `VaccinationSessionServiceQueryTests`)
   - focused regression: **84 executions** (23 `SessionControllerTests`, 20 `SessionControllerChangeTests`, 6 `SessionDtoMapperTests`, 17 `VaccinationSessionServiceQueryTests`, 18 `VaccinationSessionChangeIntegrationTests`)
   - scope: 14 files (8 new production, 3 modified production, 1 new test, 2 modified tests)
   - integrated endpoints under `/api/v1/sessions` (7):
     1. `POST /api/v1/sessions/{id}/cancellation-preview` (Preview cancellation; 200 OK)
     2. `POST /api/v1/sessions/{id}/cancel` (Confirm cancellation; 200 OK)
     3. `POST /api/v1/sessions/{id}/reschedule-preview` (Preview same-center reschedule; 200 OK)
     4. `POST /api/v1/sessions/{id}/reschedule` (Confirm same-center reschedule; 200 OK)
     5. `POST /api/v1/sessions/{id}/transfer-preview` (Preview center transfer; 200 OK)
     6. `POST /api/v1/sessions/{id}/transfer` (Confirm center transfer; 200 OK)
     7. `GET /api/v1/sessions/{id}/change-events` (List chronological change-event audit history; 200 OK)
   - architecture: strict transport adapter pattern (`SessionController` invokes domain service `VaccinationSessionService`; 0 repository dependencies in controller; 0 controller transactions; service layer owns `@Transactional` persistence/locking; pure `SessionDtoMapper` handles DTO conversion; existing `@RestControllerAdvice` `GlobalCampaignExceptionHandler` provides structured RFC-aligned `ApiErrorResponse`).
   - runtime constraint & external adapter status: `SessionImpactReader` production adapter is **MISSING** (owned by MF-03). DRAFT session changes operate fully with 0/0 impact without the adapter. SCHEDULED impact-aware changes safely require the adapter and fail fast with `IllegalStateException` -> `HTTP 409 Conflict`. Milestone is qualified as `COMPLETE_WITH_MF03_RUNTIME_CONSTRAINT`.
   - preview token contract: opaque client-facing token backed by deterministic SHA-256 fingerprint; confirm-time locks, entity refresh, and revalidation precede token comparison; stale token produces `IllegalStateException` -> `HTTP 409 Conflict`.
   - timezone contract: Center entity remains authoritative for scheduling timezone; transfer updates `scheduleTimezone` to the target center; no client-supplied timezone override.
   - audit metadata: `reason` (required, non-blank, max 500 chars), `actorId` (required, non-blank, max 64 chars; classified as `CALLER_SUPPLIED_UNAUTHENTICATED_AUDIT_METADATA`).
   - error & exception taxonomy:
     - 400 Bad Request: `@Valid` payload validation failure, parameter type mismatch, malformed JSON, general `IllegalArgumentException` (including invalid lifecycle status for change operations: `CANCELLED`, `IN_PROGRESS`, `COMPLETED`)
     - 404 Not Found: `ResourceNotFoundException` (missing session on preview/confirm/history, missing target center on transfer preview/confirm)
     - 409 Conflict: `IllegalStateException` (stale/invalid preview token, concurrent state conflict, or missing `SessionImpactReader` for SCHEDULED changes)
     - 500 Internal Server Error: unexpected unhandled server exception
   - locking & validation preservation: PR #92 preserved and exposed existing authoritative locking (cancellation: `Campaign -> Center -> Session`; reschedule: `Campaign -> Center -> Session`; transfer: `Campaign -> min(c1,c2) -> max(c1,c2) -> Session`) under `findByIdForUpdate` with post-lock `entityManager.refresh` and transactional reverse-impact check invariants.
   - audit atomicity: session mutation and `SessionChangeEvent` persistence occur within the same service `@Transactional` boundary; preview operations are `@Transactional(readOnly = true)` with zero side effects.
   - note: *The exact reviewed candidate passed CI before zero-drift merge; no additional local post-merge rerun was performed.*
   - Thread.sleep audit: **0** occurrences remain under `backend/src/test`.

2. **PR #90 — `feat(mf02): implement vaccination session core and lifecycle rest api`**
   - reviewed source: `3324ab4b2a84960dea946c531115b5aee829ad61`
   - merge: `9cc7a606260e24e243090f70f5e957b0544bfa3b`
   - base: `4eaa8cd31c04aedf23f3887f25de27ec924165b8`
   - merge method: normal merge commit (two parents: `4eaa8cd31c04aedf23f3887f25de27ec924165b8` and `3324ab4b2a84960dea946c531115b5aee829ad61`)
   - Backend CI: PASS (763 H2 executions, 0 failures, 0 errors, 0 skipped; 3 MySQL Testcontainers executions, 0 failures, 0 errors, 0 skipped)
   - Frontend CI: PASS
   - authoritative backend baseline: **763 functional H2 tests** across 80 Surefire XML reports + **3 MySQL assurance tests** (766 logical executions across the two validation paths)
   - net-new executions: **+39 H2 tests** (+23 `SessionControllerTests`, +3 `SessionDtoMapperTests`, +13 `VaccinationSessionServiceQueryTests`)
   - scope: 10 files (7 production [5 new, 2 modified], 3 test [3 new, 0 modified existing])
   - integrated endpoints under `/api/v1/sessions` (6):
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

2. **PR #88 — `feat(mf02): add vaccination center management REST API`**
   - reviewed source: `263c26bc9d9a7b51bd81e28d326a90b004c548f0`
   - merge: `faf455ff6f07d096258e7cd977765e6e9e25b10f`
   - base: `37574bb02e157971c3ca2250d4953834cfe6d132`
   - merge method: normal merge commit (two parents: `37574bb02e157971c3ca2250d4953834cfe6d132` and `263c26bc9d9a7b51bd81e28d326a90b004c548f0`)
   - Backend CI: PASS (724 H2 executions, 0 failures, 0 errors, 0 skipped; 3 MySQL Testcontainers executions, 0 failures, 0 errors, 0 skipped)
   - Frontend CI: PASS
   - authoritative backend baseline: **724 functional H2 tests** across 77 Surefire XML reports + **3 MySQL assurance tests** (727 logical executions across the two validation paths)
   - net-new executions: **+44 H2 tests** (+33 `CenterControllerTests`, +4 `CenterDtoMapperTests`, +7 `VaccinationCenterServiceQueryTests`)
   - scope: 21 files (17 production [13 new, 4 modified], 4 test [3 new, 1 modified fixture fix])
   - integrated endpoints under `/api/v1/centers` (14):
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
   - Thread.sleep occurrences under `backend/src/test`: **0**

2. **PR #86 — `feat(mf02): add campaign management REST API`**
   - reviewed source: `0fab4afa983252e927dda01278193a584ad9a0d8` (corrective commit resolving missing-campaign 404 lifecycle contract from initial candidate `fef02226ed180a3dc2a9b3195dbd7b598c230e80`)
   - merge: `a9a71114738ce8aed6c1f0cff8a8f42d955a033c`
   - base: `9d40ddbd55139a28707c3bdb68fb6a222b5b1c28`
   - merge method: normal merge commit (two parents: `9d40ddb` and `0fab4af`)
   - Backend CI: PASS (680 H2 executions, 0 failures, 0 errors, 0 skipped; 3 MySQL Testcontainers executions, 0 failures, 0 errors, 0 skipped)
   - Frontend CI: PASS
   - authoritative backend baseline: **680 functional H2 tests** across 76 Surefire XML reports + **3 MySQL assurance tests** (683 logical executions)
   - net-new executions: **+35 H2 tests** (+23 `CampaignControllerTests`, +4 `CampaignDtoMapperTests`, +8 `VaccinationCampaignServiceQueryTests`)
   - scope: 17 files (14 production, 3 test)
   - integrated endpoints under `/api/v1/campaigns` (9):
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
   - Thread.sleep occurrences under `backend/src/test`: **0**

2. **PR #84 — `test(mf02): add MySQL concurrency assurance`**
   - reviewed source: `ded72c17d8bb4aaf222d6e2a96b44fa93208e880` (corrective commit resolving assertion message mismatch from original candidate `3343817c964ba6fd837a4b609a6bdbb63037fd51`)
   - merge: `34aa7666dc2d37b1b71faf25a7f51cfba10ee26a`
   - base: `633e6a94585a9549718e16b038ae27e6646347f0`
   - merge method: normal merge commit (two parents: `633e6a9` and `ded72c1`)
   - Backend CI: PASS
   - Frontend CI: PASS
   - production code changed: 0
   - functional H2 baseline: **645 tests** across 71 Surefire XML reports (0 failures, 0 errors, 0 skipped)
   - MySQL/Testcontainers assurance executions: **3 tests** across 2 MySQL-specific XML reports (0 failures, 0 errors, 0 skipped)
   - logical combined coverage: **648 executions** across the two validation paths
   - test infrastructure & container: Testcontainers 1.20.4 (`junit-jupiter`, `mysql` test scope), shared static singleton container (`AbstractMySqlTestContainersIntegrationTest`), dynamic property injection via `@DynamicPropertySource`, runtime product assertion via `assertConnectedToRealMySql()`. Pinned test image: `mysql:8.4.3`.
   - CI workflow: dual verification in `.github/workflows/backend-ci.yml` running standard `./mvnw verify` (H2 suite) followed by `./mvnw verify -Pmysql-concurrency` (MySQL assurance suite).
   - validated topologies:
     - **Topology A (Single-Row Command Locking):** Verified duplicate-command race serialization against MySQL InnoDB for campaign resumption (`simultaneousResumptionSerializesToOneTransitionOnMySql`) and activation (`simultaneousActivationSerializesToOneTransitionOnMySql`). Exactly 1 contender succeeds, competing contender observes post-lock entity state and receives `IllegalStateException`; audit event and final status are strictly validated.
     - **Topology B (Parent/Child Reverse-Impact Locking):** Verified cross-entity lock contention against MySQL InnoDB (`concurrentSessionCreationAndCampaignDateShorteningSerializeOnMySql`). Campaign `PESSIMISTIC_WRITE` lock held during service date shortening blocks competing session creation attempt; date shortening succeeds, conflicting session creation fails validation, 0 invalid sessions persist in MySQL.
   - note: *Representative single-row lifecycle and parent/child reverse-impact pessimistic-locking behavior is validated against real MySQL/InnoDB in CI; exact low-level JDBC blocking timing is not instrumented, and non-concurrency domain paths remain verified by the fast H2 suite.*
   - Thread.sleep occurrences under `backend/src/test`: **0**

2. **PR #82 — `feat(mf02): add campaign resumption lifecycle transition`**
   - reviewed source: `cec16fcb99ae7064ee0804ea2f1cd4578aeb89e4`
   - merge: `5dd7034df64951f061dc27c5596d17a3cb440061`
   - base: `ff3d832745708e5d563c63059820e1e157767a60`
   - merge method: normal merge commit
   - Backend CI: PASS
   - Frontend CI: PASS
   - authoritative backend baseline: **645**
   - net-new executions: **+29** (+4 `VaccinationCampaignModelTests`, +2 `CampaignLifecycleEventModelTests`, +22 `CampaignResumptionIntegrationTests`, +1 `CampaignResumptionConcurrencyIntegrationTests`)
   - behavior: implements canonical `SUSPENDED -> PUBLISHED` (when `today < serviceStartDate`) and `SUSPENDED -> ACTIVE` (when `serviceStartDate <= today <= serviceEndDate`) campaign resumption per Canonical R9 Appendix E.5 under campaign pessimistic write lock (`findByIdForUpdate`) and post-lock `entityManager.refresh`. Date decisions derived from injected application `Clock`. Requires mandatory non-blank reason (max 500 chars) and actor ID; atomically records append-only `CampaignLifecycleEvent` with change type `RESUMED`. Fails deterministically with `IllegalStateException` when `today > serviceEndDate` without mutating campaign state or creating events. Serializes concurrent resumption requests deterministically without predetermining worker winner identity.
   - actor note: *Canonical identifies Campaign Officer / Administrator as authorized actors; enforcement remains an API/security-layer responsibility outside this domain/service change.*
   - atomicity note: *Campaign state update and lifecycle event persistence are atomic within a single Spring `@Transactional` service boundary.*
   - Thread.sleep occurrences under `backend/src/test`: **0**

2. **PR #80 — `feat(mf02): add campaign activation lifecycle transition`**
   - reviewed source: `fdf6f84938326968347cac2c01d3db6ea72938c4`
   - merge: `852802ff0d533fdb013c64b4b10de395efd40557`
   - base: `3d21d38777eff40104b46a11773ceeded6d8dece`
   - merge method: normal merge commit
   - Backend CI: PASS
   - Frontend CI: PASS
   - authoritative backend baseline: **616**
   - net-new executions: **+17** (+2 `VaccinationCampaignModelTests`, +14 `CampaignActivationIntegrationTests`, +1 `CampaignActivationConcurrencyIntegrationTests`)
   - behavior: implements canonical `PUBLISHED -> ACTIVE` campaign activation per Canonical R9 Appendix E.5 under campaign pessimistic write lock (`findByIdForUpdate`) and post-lock `entityManager.refresh`. Requires UTC application Clock date to satisfy inclusive service interval `serviceStartDate <= today <= serviceEndDate`. Rejects all non-PUBLISHED statuses and out-of-boundary dates while preserving persisted state. Serializes concurrent activation requests deterministically without predetermining worker winner identity.
   - actor note: *Canonical identifies System or Campaign Officer as authorized actors; enforcement remains an API/security-layer responsibility outside this domain/service implementation.*
   - Thread.sleep occurrences under `backend/src/test`: **0**

2. **PR #78 — `test(mf02): harden reverse-impact concurrency determinism`**
   - reviewed source: `2e43665ff0b88027fd82b3577bf73d8357a1ba46`
   - merge: `20520c03bdd53329a7fe647a7355beed58ed552e`
   - base: `6b8bf9c765aaa577b6c29521539e42acb29f70f9`
   - merge method: normal merge commit
   - Backend CI: PASS
   - Frontend CI: PASS
   - authoritative backend baseline: **599**
   - net-new executions: **+0** (test hardening within existing test method)
   - behavior: eliminated the final `Thread.sleep(300)` timing delay in `CampaignAssignmentReverseImpactConcurrencyIntegrationTests` and replaced it with deterministic latch coordination (`startLatch`, `lockAcquiredLatch`, `competingAttemptLatch` with 5s timeout guards). Replaced ambiguous XOR check with explicit winner and database state assertions.
   - note: *Deterministic transaction ordering and serialization are proven; exact JDBC blocking timing is not directly instrumented.*
   - Thread.sleep occurrences under `backend/src/test`: **0**

2. **PR #75 — `refactor(mf02): extract series preview calculator`**
   - reviewed source: `df0852da3be62503be5dc7036cca3039cbc0c3ed`
   - merge: `cc13082ad94701475b69bc53b699d897486ff7fc`
   - base: `b8289635054a6f15d8c611e18e86a0e7a0a64687`
   - merge method: normal merge commit
   - Backend CI: PASS
   - Frontend CI: PASS
   - authoritative backend baseline: **599**
   - net-new executions: **+8** (from `SessionSeriesPreviewCalculatorTests`)
   - behavior: behavior-preserving maintainability extraction of recurrence diffing, occurrence classification, DST conversion, and SHA-256 fingerprint token generation from `SessionSeriesChangeService` into `SessionSeriesPreviewCalculator`.

3. **PR #73 — `fix(mf02): enforce published questionnaire when scheduling`**
   - reviewed source: `75cd1bb34a0ec6bed32e66387bfa54bca01b4b31`
   - merge: `d3fbb3eb95333d1da0b3e5c33283a1bcf81635e9`
   - base: `5fe33208d11a21ced5c6c1c520dfe01c13858bf0`
   - merge method: normal merge commit
   - Backend CI: PASS
   - Frontend CI: PASS
   - authoritative backend baseline: **591**
   - net-new executions: **0** (hardened invariant assertions and aligned fixtures)

4. **PR #71 — `feat(mf02): add campaign scheduling approval`**
   - reviewed source: `422fce3768646310448c3c82efec60c654afa0c7`
   - merge: `6d52449db4f29099f3a979e824a7efcae8f4ea54`
   - base: `c3bf7c2fb03513d8540a78dcc4ab5d93f322f468`
   - merge method: normal merge commit
   - Backend CI: PASS
   - Frontend CI: PASS
   - authoritative candidate/backend baseline: **591**
   - previous clean baseline: **569**
   - net-new executions: **22**

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
- `SessionRecurrenceRule`
- `VaccinationSession`
- authoritative session schedule validator
- one-off DRAFT creation
- recurrence occurrence generation
- deterministic occurrence identity
- retry/idempotency behavior
- DST-aware conversion
- individual reschedule / center transfer / cancellation
- series-wide preview / confirmation
- full-series cancellation
- append-only-style `SessionChangeEvent`
- stale-preview SHA-256 fingerprints
- terminal-state planning guard
- SUSPENDED new-planning freeze

### Session lifecycle
The current session lifecycle foundation includes controlled DRAFT/SCHEDULED/IN_PROGRESS/COMPLETED/CANCELLED behavior and prevents the previously broad arbitrary transition surface.

## 4. Campaign Lifecycle Contracts

### 4.1 Campaign `DRAFT -> SCHEDULED` Contract — PR #71

PR #71 implements internal campaign scheduling approval.

#### Source and target
- source: `DRAFT` only
- target: `SCHEDULED`

#### Authoritative service command
`VaccinationCampaignService.scheduleCampaign(Long campaignId)`

The service:
1. validates ID;
2. takes the Campaign pessimistic write lock;
3. refreshes authoritative Campaign state;
4. requires source state `DRAFT`;
5. validates existing campaign date consistency;
6. requires `serviceStartDate` to be strictly after `LocalDate.now(clock)`;
7. requires ACTIVE `VaccineType`;
8. requires ACTIVE `TargetGroup`;
9. requires exactly one effective `CampaignDoseProgramme` at `serviceStartDate`;
10. requires exactly one effective `CampaignQuestionnaireAssignment` at `serviceStartDate`;
11. requires the assigned MF-01 `QuestionnaireVersion` to be in `PUBLISHED` status via `QuestionnaireVersionPublicationPort.isPublished(...)` (hardened in PR #73);
12. requires at least one relevant overlapping ACTIVE `CampaignCenterAssignment`;
13. requires every relevant overlapping assignment to reference an ACTIVE center;
14. invokes the domain `schedule()` transition only after all checks pass.

The application `Clock` is `Clock.systemUTC()`, so the current-date comparison is based on the UTC civil date.

#### Center validity policy
For scheduling approval, relevant assignments are ACTIVE assignments whose half-open assignment period overlaps the inclusive campaign service range.

- zero relevant assignments: reject;
- relevant assignment to INACTIVE center: reject;
- any relevant overlapping assignment to INACTIVE center: reject;
- non-overlapping assignments: do not block;
- full-period center coverage is not required;
- OperatingHours and CenterClosure are not campaign-scheduling prerequisites.

#### Questionnaire publication invariant (PR #73 hardening)
Under canonical rule `MF02-BR-02`, a valid questionnaire assignment must reference a published questionnaire version. PR #73 hardened `scheduleCampaign(...)` to defensively revalidate `publicationPort.isPublished(questionnaireVersionId)` under the campaign lock before status mutation, matching `publishCampaign(...)`. If un-published, scheduling fails deterministically with `IllegalStateException` and the campaign remains in `DRAFT`.

### 4.2 Campaign `PUBLISHED -> ACTIVE` Activation Contract — PR #80

PR #80 implements authoritative campaign activation according to Canonical R9 Appendix E.5.

#### Source and target
- source: `PUBLISHED` only
- target: `ACTIVE`

#### Authoritative service command
`VaccinationCampaignService.activateCampaign(Long campaignId)`

The service:
1. validates campaign ID;
2. acquires Campaign pessimistic write lock via `campaignRepository.findByIdForUpdate(campaignId)`;
3. refreshes authoritative entity state via `entityManager.refresh(campaign)`;
4. requires authoritative source status `PUBLISHED` (rejects all other statuses with `IllegalStateException`);
5. derives `today` from injected application `Clock` (`LocalDate.now(clock)`);
6. enforces inclusive service interval date rule:
   `serviceStartDate <= today <= serviceEndDate`
   - `today < serviceStartDate`: rejected with `IllegalStateException` ("has not been reached");
   - `today == serviceStartDate`: allowed;
   - `serviceStartDate < today < serviceEndDate`: allowed;
   - `today == serviceEndDate`: allowed;
   - `today > serviceEndDate`: rejected with `IllegalStateException` ("has already passed");
7. invokes domain transition `campaign.activate()`;
8. persists and returns managed entity via `campaignRepository.save(campaign)`.

#### Integrity and Concurrency
- Non-idempotent: subsequent activation attempts on `ACTIVE` campaigns throw `IllegalStateException`.
- Persistence safety: failed activation attempts leave the database state uncorrupted in `PUBLISHED`.
- Concurrency serialization: simultaneous activation attempts race for the row lock; exactly one contender transitions `PUBLISHED -> ACTIVE`, while competing contenders observe the refreshed `ACTIVE` state and fail with `IllegalStateException: Cannot activate campaign in ACTIVE status`. Final persisted status is `ACTIVE`. Winner identity is not predetermined.
- Child session decoupling: campaign activation does not modify child session statuses or bookings.
- Actor boundary: *Canonical identifies System or Campaign Officer as authorized actors; enforcement remains an API/security-layer responsibility outside this domain/service implementation.*

### 4.3 Campaign `SUSPENDED -> PUBLISHED / ACTIVE` Resumption Contract — PR #82

PR #82 implements authoritative campaign resumption according to Canonical R9 Appendix E.5.

#### Source and target
- source: `SUSPENDED` only
- target: derived from authoritative application date:
  - `PUBLISHED` when `today < serviceStartDate` ("Resume before service start")
  - `ACTIVE` when `serviceStartDate <= today <= serviceEndDate` ("Resume during service period")

#### Authoritative service command
`VaccinationCampaignService.resumeCampaign(Long campaignId, String reason, String actorId)`

The service:
1. validates input parameters: `reason` must be non-null and non-blank (max 500 characters); `actorId` must be non-null and non-blank (max 64 characters);
2. acquires Campaign pessimistic write lock via `campaignRepository.findByIdForUpdate(campaignId)`;
3. refreshes authoritative entity state via `entityManager.refresh(campaign)`;
4. requires authoritative source status `SUSPENDED` (rejects all 7 other statuses with `IllegalStateException`);
5. derives `today` from injected application `Clock` (`LocalDate.now(clock)`);
6. verifies service dates and evaluates date-driven target:
   - `today.isAfter(serviceEndDate)`: rejected with `IllegalStateException` ("Campaign service end date has already passed"), leaving campaign state unchanged in `SUSPENDED` and generating zero lifecycle events;
   - `today.isBefore(serviceStartDate)`: invokes domain transition `campaign.resumeToPublished()`;
   - `serviceStartDate <= today <= serviceEndDate`: invokes domain transition `campaign.resumeToActive()`;
7. persists updated campaign state via `campaignRepository.save(campaign)`;
8. creates and persists append-only `CampaignLifecycleEvent` with `changeType == RESUMED`, `previousStatus == SUSPENDED`, `newStatus`, `reason`, `actorId`, and `createdAt` from `clock.instant()`;
9. returns the managed `VaccinationCampaign` entity.

#### Integrity, Concurrency, and Atomicity
- Non-idempotent: subsequent resumption attempts against a resumed campaign throw `IllegalStateException` because the status is no longer `SUSPENDED`.
- Transactional atomicity: campaign status update and `CampaignLifecycleEvent` persistence are executed within the same Spring `@Transactional` service boundary. If event creation or persistence fails, the entire transaction rolls back.
- Persistence safety: failed resumption attempts leave database state uncorrupted in `SUSPENDED` with zero `CampaignLifecycleEvent` records created.
- Concurrency serialization: simultaneous resumption attempts race for the row lock; exactly one contender transitions the status and persists a `RESUMED` event, while competing contenders observe the non-`SUSPENDED` state and fail with `IllegalStateException`. Final persisted status matches the date-derived target; exactly one `RESUMED` event persists. Winner identity is not predetermined.
- Child session decoupling: campaign resumption does not modify child session statuses or bookings.
- Actor boundary: *Canonical identifies Campaign Officer / Administrator as authorized actors. Actor ID is captured as audit data; authorization enforcement remains an API/security-layer responsibility outside this domain/service change.*

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

Critical write services are transactional. Read-only preview operations use read-only transactions where appropriate.

Do not add pessimistic locks merely for style. Add them only for a real invariant/race, and preserve the established order to avoid inversion.

## 6. Current Test / Verification State

### Authoritative current backend baseline
**763 functional H2 tests** (80 XML reports) plus **3 MySQL/Testcontainers assurance tests** (2 XML reports); producing **766 logical executions** across the two validation paths.

- failures: 0
- errors: 0
- skipped: 0
- clean Maven test (H2 default): PASS (763 executions)
- clean Maven verify (H2 default): PASS (763 executions)
- clean Maven verify (`-Pmysql-concurrency` profile): PASS (3 executions)
- Surefire reports at PR #90 candidate: 80 H2 XML files + 2 MySQL XML files
- Surefire-summed executions: 763 (H2) / 3 (MySQL)
- Backend CI: PASS (H2 step + MySQL Testcontainers step)
- Frontend CI: PASS

### Baseline reconciliation history
- PR #67 backend code checkpoint: **560**
- PR #69: **569**
- PR #71: **591**
- PR #73: **591**
- PR #75: **599** (+8 from `SessionSeriesPreviewCalculatorTests`)
- PR #78: **599** (+0 net-new; test determinism hardening)
- PR #80: **616** (+17 net-new: +2 model tests, +14 activation integration tests, +1 activation concurrency test)
- PR #82: **645** (+29 net-new: +4 model tests, +2 lifecycle-event model tests, +22 resumption integration tests, +1 resumption concurrency test)
- PR #84: **645 functional H2** + **3 MySQL assurance** (**648 logical executions**; +0 H2, +3 MySQL Testcontainers concurrency tests)
- PR #86: **680 functional H2** + **3 MySQL assurance** (**683 logical executions**; +35 H2 [+23 `CampaignControllerTests`, +4 `CampaignDtoMapperTests`, +8 `VaccinationCampaignServiceQueryTests`])
- PR #88: **724 functional H2** + **3 MySQL assurance** (**727 logical executions**; +44 H2 [+33 `CenterControllerTests`, +4 `CenterDtoMapperTests`, +7 `VaccinationCenterServiceQueryTests`])
- PR #90: **763 functional H2** + **3 MySQL assurance** (**766 logical executions**; +39 H2 [+23 `SessionControllerTests`, +3 `SessionDtoMapperTests`, +13 `VaccinationSessionServiceQueryTests`])

Do not resurrect 566, 575, 599, 616, 645, 680, or 724 as authoritative current evidence (they are historical baselines). Do not report 766 as the functional backend baseline (766 represents logical combined coverage across the two separate verification paths).

### Test architecture
The default backend test suite continues to run against in-memory H2 in MySQL compatibility mode, verifying all 763 domain, service, and integration tests rapidly.

PR #84 introduced a dedicated, production-equivalent MySQL concurrency assurance suite:
- Pinned container image: `mysql:8.4.3`
- Database: MySQL 8.4.3 InnoDB with `REPEATABLE_READ` isolation and MySQL Connector/J driver
- Container lifecycle: Singleton container managed via `AbstractMySqlTestContainersIntegrationTest` with dynamic Spring datasource configuration (`@DynamicPropertySource`)
- Tagging & execution: Test classes are tagged `@Tag("mysql-testcontainers")`, excluded from standard Surefire test runs, and executed in CI via Maven profile `mysql-concurrency` (`-Pmysql-concurrency`)
- Scope: Validates representative locking topologies (single-row lifecycle duplicate-command race serialization and cross-entity parent/child reverse-impact locking) against real MySQL/InnoDB.

## 7. Architecture / Maintainability Audit — Current Repository Snapshot

The current repository is not broadly obsolete. The dominant issue is maintainability from feature accumulation.

Verified sizes after PR #75 decomposition and PR #90 REST slice:

- `SessionSeriesChangeService.java`: ~694 lines (reduced from ~1,104 lines)
- `SessionSeriesPreviewCalculator.java`: ~454 lines (new calculation collaborator)
- `VaccinationSessionService.java`: ~824 lines
- `VaccinationCampaignService.java`: ~428 lines
- `OperatingHoursService.java`: ~488 lines
- `CenterClosureService.java`: ~424 lines
- `VaccinationCenterService.java`: ~245 lines
- `CenterController.java`: ~228 lines
- `CampaignController.java`: ~184 lines
- `SessionController.java`: ~85 lines
- `SessionDtoMapper.java`: ~58 lines

Current MF-02 production Java files under the campaign module: 95 (including controller, DTO, and mapper packages).
Current MF-02 test Java files in the campaign test package: 81.

### Architectural verdict
Keep the current **layered modular monolith**. Do not perform a wholesale architecture rewrite.

### Decomposition of `SessionSeriesChangeService` (PR #75)
PR #75 successfully performed the first behavior-preserving extraction:
- `SessionSeriesChangeService` retains all `@Transactional` boundaries, 5 Spring Data JPA repositories, `EntityManager`, hierarchical locking (`Campaign -> Center -> Rule -> Sessions ASC`), post-lock `refresh(...)`, override-history resolution, stale-token comparison, mutations, and audit persistence.
- `SessionSeriesPreviewCalculator` owns recurrence diffing, occurrence classification (`RETAINED`, `REMOVED`, `NEW`, `INELIGIBLE`, `PRESERVED_EXCEPTION`), DST conversion, deterministic SHA-256 series preview token generation, candidate schedule validation delegation, and conditional `SessionImpactReader` evaluation.
- `SessionSeriesPreviewCalculator` has 0 JPA repositories, 0 `EntityManager` references, and 0 transaction annotations. It is repository-free and transaction-free, but conditionally calls `SessionImpactReader` on demand to preserve lazy impact evaluation.

Remaining bounded maintainability targets:
1. Evaluation/focused decomposition of `VaccinationSessionService` (~824 lines) if future evidence justifies it
2. Duplicated preview/impact/fingerprint/event-construction helpers across session services

Refactoring must remain behavior-preserving and separated from new lifecycle implementation.

## 8. Production-Layer Gaps

The following are not complete in the uploaded repository snapshot:

- MF-02 Campaign REST API (9 endpoints) — **RESOLVED in PR #86**
- MF-02 VaccinationCenter REST API (14 endpoints) — **RESOLVED in PR #88**
- MF-02 VaccinationSession Core REST API (6 endpoints) — **RESOLVED in PR #90**
- MF-02 VaccinationSession Change Control REST API (7 endpoints) — **RESOLVED in PR #92 (COMPLETE_WITH_MF03_RUNTIME_CONSTRAINT)**
- VaccinationSession Recurrence / Series REST API (recurrence rule REST, occurrence generation, series preview/confirm, series cancellation) — (pending)
- Campaign date update REST API (pending)
- Blocked lifecycle endpoints (`complete`, `cancel`, `archive`) (pending specification/integration)
- Spring Security/RBAC/authentication enforcement (pending)
- OpenAPI documentation (pending)
- Flyway business migrations (pending)
- representative production MySQL Testcontainers assurance (RESOLVED in PR #84; broad production database certification deferred)
- MF-02 frontend business screens (pending)
- frontend automated tests (pending)
- production MF-03 impact adapter (pending)
- reliable transactional outbox delivery (pending)
- final MF-01 production adapter/FK integration (note: PR #76 introduced the production bean `QuestionnaireVersionService` implementing `QuestionnaireVersionPublicationPort`, providing the publication-port bean)

Placeholder directories must not be reported as implemented features.

## 9. Known Issues / Risks to Resolve Before More Lifecycle Growth

### 9.1 Campaign questionnaire-version invariant — RESOLVED (PR #73)

Canonical semantics state that MF-02 links a campaign to a **published** questionnaire version (`MF02-BR-02`). Normal `CampaignQuestionnaireAssignmentService` creation validates publication through `QuestionnaireVersionPublicationPort`.

Previously in PR #71, scheduling did not re-verify publication status and an integration test fixture directly persisted an unpublished assignment.

**Resolution in PR #73:**
- `VaccinationCampaignService.scheduleCampaign(...)` defensively revalidates `publicationPort.isPublished(questionnaireVersionId)` under the campaign lock before status mutation, matching `publishCampaign(...)`.
- If un-published, scheduling throws `IllegalStateException` and the campaign remains `DRAFT`.
- The contradictory test fixture was replaced with `unpublishedQuestionnaireVersionRejectsScheduling` to assert deterministic rejection.
- Concurrency test fixtures were configured with canonically valid published questionnaire versions.
- Module boundaries remain intact (MF-01 owns publication status; MF-02 queries `QuestionnaireVersionPublicationPort`).

### 9.2 Oversized orchestration services — PARTIALLY RESOLVED (PR #75)
- `SessionSeriesChangeService` was successfully decomposed via PR #75, reducing its size from ~1,104 lines to ~694 lines by extracting `SessionSeriesPreviewCalculator`.
- `VaccinationSessionService` remains ~809 lines and is a candidate for future maintainability review if justified.

### 9.3 Optional critical integration ports
The snapshot uses optional injection for:
- `QuestionnaireVersionPublicationPort` (production implementation provided by MF-01 `QuestionnaireVersionService` in PR #76)
- `SessionImpactReader` (MF-03 port; production adapter still pending)

Development/test flexibility is useful, but production-required integrations should ultimately fail fast through explicit production configuration/adapters.

### 9.4 Timing-based concurrency test — RESOLVED (PR #78)
PR #78 eliminated the remaining `Thread.sleep(300)` timing delay in `CampaignAssignmentReverseImpactConcurrencyIntegrationTests.java` and replaced it with deterministic latch coordination (`startLatch`, `lockAcquiredLatch`, `competingAttemptLatch` with 5s failure timeout guards) and explicit winner/state assertions. Zero `Thread.sleep` calls remain under `backend/src/test`.

*Note:* Deterministic transaction ordering and serialization are proven; exact JDBC blocking timing is not directly instrumented.

### 9.5 Representative MySQL Testcontainers concurrency assurance — RESOLVED (PR #84)
PR #84 closed the representative database locking assurance gap by introducing containerized MySQL 8.4.3 InnoDB integration tests via Testcontainers:
- Validated single-row duplicate-command race serialization against MySQL InnoDB for campaign resumption and activation (`CampaignLifecycleMySqlConcurrencyIntegrationTests`).
- Validated parent/child reverse-impact pessimistic-locking serialization against MySQL InnoDB for campaign service-date shortening versus concurrent conflicting session creation (`CampaignReverseImpactMySqlConcurrencyIntegrationTests`).
- Automated in GitHub Actions CI under dedicated `-Pmysql-concurrency` profile step.
- *Scope boundary:* Representative single-row lifecycle and parent/child reverse-impact pessimistic-locking behavior is validated against real MySQL/InnoDB in CI; exact low-level JDBC blocking timing is not instrumented, and non-concurrency domain paths remain verified by the fast H2 suite.

### 9.6 Append-only audit limitation
Lifecycle/change repositories inherit normal repository delete operations. Current append-only behavior is a domain/API convention rather than deletion-proof database enforcement.

## 10. SUSPENDED Planning Policy

Project policy S3 is implemented:

**NEW PLANNING FROZEN, REMEDIATION ALLOWED**

While Campaign is `SUSPENDED`, block:
- recurrence-rule creation;
- future recurrence definition update;
- one-off DRAFT creation;
- recurrence occurrence generation;
- DRAFT session progression to SCHEDULED.

Remediation remains governed by existing rules:
- shorten recurrence;
- cancel recurrence;
- individual reschedule/transfer;
- individual cancellation;
- series corrective change;
- full-series cancellation.

This policy is a project/product decision consistent with canonical suspension semantics; it is not an explicit canonical administrative-planning matrix.

## 11. Remaining Campaign Lifecycle Work

Still unresolved or unimplemented:

### `PUBLISHED -> ACTIVE` — RESOLVED (PR #80)
Authoritative campaign activation implemented per Canonical R9 Appendix E.5 under campaign row lock (`findByIdForUpdate`), `entityManager.refresh`, and UTC Clock inclusive date validation (`serviceStartDate <= today <= serviceEndDate`).

### `SUSPENDED -> PUBLISHED / ACTIVE` — RESOLVED (PR #82)
Authoritative campaign resumption implemented per Canonical R9 Appendix E.5 under campaign row lock (`findByIdForUpdate`), `entityManager.refresh`, mandatory reason/actor audit parameters, transactional `CampaignLifecycleEvent` (change type `RESUMED`), and UTC Clock date-driven target resolution (`today < serviceStartDate` -> `PUBLISHED`; `serviceStartDate <= today <= serviceEndDate` -> `ACTIVE`; `today > serviceEndDate` -> `IllegalStateException`).

### `-> COMPLETED`
Status: `SPECIFICATION_PARTIAL`. Needs an authoritative operational algorithm for "service work closed" before implementation.

### `-> CANCELLED`
Status: `MF03_BLOCKED`. Requires reason, dependency/impact policy, truthful MF-03 impact integration, and no silent appointment mutation (production `SessionImpactReader` adapter remains absent).

### `COMPLETED/CANCELLED -> ARCHIVED`
Status: `USER_DATA_REQUIRED`. Retention duration/policy remains missing and requires authoritative user/domain specification.

### DRAFT campaign / child session scheduling
Status: `IMPLEMENTATION_DEFINED / unresolved policy`. The separate policy question of whether a child DRAFT session may become SCHEDULED while its parent Campaign remains DRAFT is still unresolved in canonical specification.

## 12. MF-02 Ownership Boundaries

Do not move responsibilities across modules:

- MF-01 owns screening questions, questionnaire versions, eligibility rules, submissions, answers, and eligibility assessments.
- MF-02 owns campaigns, center planning, operating calendars, recurrence, sessions, resources/capacity, schedule changes, and operational utilization.
- MF-03 owns appointments, waitlists, rebooking, and check-in.
- MF-04 owns batches, stock quantities, allocations, and stock movements.
- MF-05 owns vaccination administration records and certificates.

MF-02 may read aggregate impact/readiness information through ports. It must not silently mutate MF-03 appointment/waitlist history.

## 13. Immediate Engineering Order

Do **not** automatically start another lifecycle transition without proper requirements alignment.

Recommended order:
1. synchronize maintained documentation to PR #92 — represented by this step;
2. design and discovery for Vaccination Session Recurrence / Series REST API vertical slice (recurrence rule REST, occurrence generation, series preview/confirm, series cancellation);
3. implement Recurrence / Series REST API;
4. analyze and resolve remaining campaign lifecycle gaps (completion algorithm, cancellation impact integration, archive retention policy);
5. evaluate/decompose `VaccinationSessionService` maintainability only if justified;
6. complete MF-03 production impact adapter and outbox boundary;
7. add security/RBAC;
8. add Flyway migrations;
9. implement MF-02 frontend and frontend tests;
10. final end-to-end / Definition-of-Done evidence.

## 14. Historical Milestone Summary

This compact chronology replaces stale controller transcripts. Detailed Git history remains the authoritative provenance.

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
| #90 | Vaccination Session Core REST API vertical slice (6 endpoints) |
| #91 | Documentation synchronization after PR #90 |
| #92 | Vaccination Session Change Control REST API vertical slice (7 endpoints; COMPLETE_WITH_MF03_RUNTIME_CONSTRAINT) |

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

## 15. Status Discipline

Never claim:
- MF-02 complete;
- Sprint 1 complete;
- US-13 complete;
- production API/security/migrations/frontend complete (Campaign REST API, Center REST API, Session Core REST API, and Session Change Control REST API are integrated; Recurrence/Series REST API, security, Flyway, and frontend remain pending);
- MySQL concurrency fully certified across all endpoints;
- 566, 575, 599, 616, 645, 680, 724, or 763 as the current backend baseline, or 793 as the functional H2 baseline (793 is logical combined coverage: 790 H2 + 3 MySQL assurance across separate validation paths).

Current status remains:

- Sprint 1: **IN PROGRESS**
- MF-02: **IN PROGRESS**
- US-13: **IN PROGRESS**
- Campaign Management REST API Milestone 1: **COMPLETE**
- Vaccination Center Management REST API Milestone 2: **COMPLETE**
- Vaccination Session Core REST API Milestone 3: **COMPLETE**
- Vaccination Session Change Control REST API Milestone 4: **COMPLETE_WITH_MF03_RUNTIME_CONSTRAINT**
- Vaccination Session Recurrence / Series REST API: **PENDING**
- current verified backend baseline: **790 functional H2 tests (plus 3 MySQL assurance tests; 793 logical executions across separate validation paths)**

## 16. Startup Procedure for the Next AI Session

1. Read canonical Markdown `3.3-AI / 2026-08-14-R9`.
2. Fetch live `origin/develop`.
3. Verify whether `6ff63bdd04f34d3bd334d011fdf55dee8010a140` remains an ancestor/current checkpoint.
4. Run/inspect current backend tests before changing behavior.
5. Read this handoff and Sprint evidence.
6. Review `MF02_Completion_and_Hardening_Plan.md`.
7. Do not rebuild completed foundations.
8. Resolve known consistency/hardening issues before increasing lifecycle scope.

# MF-02 Sprint 1 Evidence Record

**Project:** Web-Based Vaccination Portal (`WBVP-SE2030`)
**Function:** MF-02 — Vaccination Campaign, Center and Session Management
**Canonical version:** `3.3-AI`
**Canonical revision:** `2026-08-08-R8`
**Sprint:** Sprint 1
**Working window:** Weeks 1–3
**Last updated:** 2026-08-11
**Latest implementation integration:** merged into `develop` via PR #24 (`CenterClosure` backend foundation)
**Latest implementation develop checkpoint:** `e70940b7021778ae35dd2976ac5df6b649a28584`
**Latest completed evidence integration recorded:** PR #22 (`docs(mf02): stabilize evidence metadata`) at `fe101bb2adfc1215146793a97cd80fdc3176622c`
**Evidence record prepared from develop checkpoint:** `e70940b7021778ae35dd2976ac5df6b649a28584`
**OperatingHours feature branch HEAD:** `4fe94df8df7c07ade3c22a39165aff3da1e8795a`
**Canonical documentation sync checkpoint (PR #17):** `bd22cb973bad83c8456897805bc2df95b65593d0`
**Historical evidence integration checkpoints:** PR #18 (`c120ee8530c055a6769b68777fd1342abeca193b`), PR #19 (`8cdb1125809770671325a4f47fc80fd5c48b4ddc`), PR #21 (`433dbe1dd919b93d082aa31d3592519b37a75206`)
**OperatingHours backend integration checkpoint (PR #20):** `1cad899971c8b18804af31bfb079a24285f7d16d`
**OperatingHours source branch:** `feature/mf02-operating-hours`
**Pre-closure hardening integration checkpoint (PR #23):** `5a5b3e7d285b50b7645a1c32b57f79b753b2baff`
**CenterClosure integration checkpoint (PR #24):** `e70940b7021778ae35dd2976ac5df6b649a28584`
**CenterClosure source branch final pointer:** `d6c04718639d2356f9f4800730d9d79bed5494bf`

> Living evidence record. Update it as Sprint 1 progresses and finalize it at the Sprint Review/Retrospective. This file records implementation and integration evidence; the canonical Markdown remains the requirements source of truth.
>
> **Metadata Stability Policy:** Metadata-only evidence synchronization commits/PRs are recorded as evidence checkpoints and are not recursively treated as new MF-02 implementation checkpoints. Implementation checkpoints remain strictly tied to actual domain/feature implementation integrations.

## Sprint Goal

Build the MF-02 campaign and center-planning foundations needed for a valid vaccination campaign, including campaign lifecycle, dose-programme configuration, administrative service-area targeting, questionnaire linkage, center assignment, center calendar/closure foundations, and recurrence foundations.

Expected integrated Sprint 1 outcome: a valid campaign can be linked to a published questionnaire, has approved service locations and booking/service boundaries, and has usable center-planning foundations.

## Sprint Status

**We are still in Sprint 1.**

The campaign foundation, dose-programme foundation, administrative location targeting, MF-02 questionnaire-assignment side, vaccination-center master foundation, pre-closure hardening, **OperatingHours**, and the **CenterClosure backend foundation** are implemented, tested, CI-verified, and merged into `develop`.

The canonical R8 human-readable DOCX synchronization has also been merged into `develop` through PR #17.

Sprint 1 is **not complete yet** because the remaining MF-02 Sprint 1 foundations still include:

* `CampaignCenterAssignment` implementation (architecture gate complete)
* `SessionRecurrenceRule` foundation
* remaining campaign publication prerequisite/transition integration
* API/controller work where required
* Flyway business migrations
* frontend/integration/evidence work required by the Definition of Done
* final MF-01 `QuestionnaireVersion` adapter/FK integration

`CampaignCenterAssignment` is the next approved MF-02 domain slice. Its architecture gate is complete with verdict **CAMPAIGNCENTERASSIGNMENT PLAN READY — SAFE TO IMPLEMENT**, but implementation has **not started**.

## Current Verification Snapshot

### Overall `develop` checkpoint after PR #24 CenterClosure integration

```text
Latest verified origin/develop:
e70940b7021778ae35dd2976ac5df6b649a28584

Latest develop merge:
PR #24
e70940b7021778ae35dd2976ac5df6b649a28584

Latest MF-02 implementation checkpoint:
PR #24
e70940b7021778ae35dd2976ac5df6b649a28584

PR #24 status:
MERGED

Backend CI:
SUCCESS

Frontend CI:
SUCCESS
```

PR #23 hardened the shared MF-02 temporal, mutation, and locking foundation. PR #24 then integrated `CenterClosure` with retained lifecycle history, exact half-open overlap behavior, parent-center serialization, and focused separate-transaction coverage. The prior PR #20 OperatingHours checkpoint remains historical evidence below.

### Historical OperatingHours implementation / integration checkpoint (PR #20)

```text
Backend implementation develop checkpoint:
1cad899971c8b18804af31bfb079a24285f7d16d

Merge evidence:
1cad899 — Merge pull request #20 from Sankalpa-KMCP/feature/mf02-operating-hours

Feature branch:
feature/mf02-operating-hours

Latest verified feature-branch pointer:
4fe94df8df7c07ade3c22a39165aff3da1e8795a

Feature branch local/remote:
synchronized

Feature branch ahead of develop at the implementation verification checkpoint:
0 commits (merged)

Working tree at verification:
clean
```

All 7 OperatingHours commits recorded below are present locally, remotely, and in `develop`.

### Backend verification

```text
./mvnw test:
BUILD SUCCESS
194 tests
0 failures
0 errors
0 skipped

./mvnw verify:
BUILD SUCCESS
JAR packaged successfully
```

### Canonical DOCX synchronization checkpoint (PR #17)

The corrected R8 canonical DOCX was committed and pushed on a dedicated documentation branch and merged into `develop`.

```text
Documentation branch:
docs/sync-canonical-master-r8

Documentation branch HEAD:
16d4193cf2b16f6cc8a19f778b48b16d59e633d6

Documentation commits:
8a26fe6 — docs: add latest synced version of vaccination portal AI canonical documentation
16d4193 — docs: restore canonical master filename

Pull request:
PR #17

Base:
develop

Head:
docs/sync-canonical-master-r8

Merge commit:
bd22cb973bad83c8456897805bc2df95b65593d0

PR status:
MERGED

Canonical repository path:
docs/Web_Based_Vaccination_Portal_AI_Canonical_Master_v3.3.docx

Backend CI:
SUCCESS

Frontend CI:
SUCCESS
```

### Evidence integration checkpoints (PR #18, PR #19, & PR #21)

```text
PR #18 merge commit:
c120ee8530c055a6769b68777fd1342abeca193b

PR #19 merge commit:
8cdb1125809770671325a4f47fc80fd5c48b4ddc

PR #21 merge commit:
433dbe1dd919b93d082aa31d3592519b37a75206

PR #21 branch:
docs/mf02-sprint1-operating-hours-evidence

PR #21 status:
MERGED

Backend CI:
SUCCESS

Frontend CI:
SUCCESS
```

### Final hardening audit verdict

**HARDENING COMPLETE — SAFE TO CONTINUE**

The final audit reported:

* canonical alignment: PASS
* module boundaries: PASS
* effective-dated history: PASS
* concurrency hardening: PASS
* validation/exceptions: PASS
* JPA/database: PASS WITH NOTES, no blocking issue
* test quality: PASS
* backend README: PASS
* blockers: none
* fixes required before continuing: none
* safe to start the next domain slice: yes

## Planned Lab 02 Stories

| Story   | Priority | Intent                                                            | Current status                                                                                                                                                |
| ------- | -------- | ----------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `US-09` | High     | Create/manage campaign lifecycle and target group                 | In progress — backend foundation implemented; later publication workflow, API/UI, and Definition-of-Done evidence pending                                      |
| `US-10` | High     | Link campaign to one published questionnaire version              | In progress — MF-02 foundation implemented and hardened; final MF-01 adapter/FK integration pending                                                            |
| `US-11` | High     | Define campaign dose programme                                    | In progress — versioning and hardening implemented; later API/UI and integration evidence pending                                                              |
| `US-12` | High     | Assign vaccination centers to a campaign                          | In progress — architecture plan complete; implementation **NOT STARTED**                                                                                        |
| `US-13` | High     | Configure center operating hours, closures and recurring sessions | In progress — OperatingHours and CenterClosure backend foundations complete; recurrence **NOT STARTED**; later session/API/UI/migration work pending           |
| `US-27` | High     | Define campaign service areas and booking periods                 | In progress — backend foundation implemented; later API/UI evidence pending                                                                                    |

## Completed Work

### 1. Campaign domain foundation

Implemented:

* `CampaignStatus`
* `VaccineType`
* `TargetGroup`
* `VaccinationCampaign`
* campaign JPA repositories
* persistence integration tests
* isolated H2 test-schema lifecycle configuration

Covered behavior:

* exact campaign lifecycle status model: `DRAFT`, `SCHEDULED`, `PUBLISHED`, `ACTIVE`, `SUSPENDED`, `COMPLETED`, `CANCELLED`, `ARCHIVED`
* unique vaccine-type, target-group and campaign codes
* service end date strictly after service start date
* booking close after booking open
* `PUBLISHED` / `ACTIVE` intake behavior inside the booking window
* MF-02 ownership of shared `VaccineType`

### 2. Campaign dose programme

Implemented:

* `DoseCategory` with canonical values `PRIMARY` and `BOOSTER`
* `CampaignDoseProgramme`
* `CampaignDoseProgrammeRepository`
* `CampaignDoseProgrammeService`
* domain and persistence/versioning tests

Covered behavior:

* version-number validation
* permitted-dose-number validation
* minimum-interval validation
* previous-dose-evidence flag
* positive expected-service time with default of 10 minutes
* effective-date ranges
* unique campaign/version combination
* non-overlapping effective programme periods
* historical version preservation
* explicit null-date guards
* MF02-BR-18 future-only replacement protection using an injected application `Clock`
* parent-campaign pessimistic write locking for serialized configuration changes
* safe initial creation and replacement behavior under the locked transaction boundary

### 3. Administrative service-area targeting

Implemented, tested and committed:

* `LocationTargetType`
* `CampaignLocationTarget`
* `CampaignLocationTargetRepository`
* `CampaignLocationTargetService`
* unit and integration tests

Covered behavior:

* exact target types: `PROVINCE`, `DISTRICT`, `HEALTH_REGION`, `CENTER`
* unique `(campaign_id, target_type, target_code)`
* exact type/code matching only
* no geographic hierarchy inference
* `Locale.ROOT` code normalization
* atomic batch target addition
* no normal hard-delete workflow
* separation from MF-01 medical eligibility
* separation from later `CampaignCenterAssignment`

### 4. Campaign questionnaire assignment

Implemented, tested and hardened on the MF-02 side:

* `QuestionnaireVersionPublicationPort`
* `CampaignQuestionnaireAssignment`
* `CampaignQuestionnaireAssignmentRepository`
* `CampaignQuestionnaireAssignmentService`
* domain and persistence/versioning tests

Covered behavior:

* scalar `questionnaire_version_id` temporary cross-module boundary
* no duplicate MF-01 `QuestionnaireVersion` entity inside MF-02
* publication validation through an explicit MF-01 integration port
* no production `always true` publication bypass
* effective-date/history preservation
* non-overlapping effective questionnaire assignments
* MF02-BR-17 future-only replacement protection using the application `Clock`
* campaign-row pessimistic write locking before child-history decisions
* serialized zero-assignment and replacement flows per campaign
* no hard-delete workflow
* date-aware effective-assignment queries
* correct distinction between an open-ended assignment and an assignment effective on a specific date
* rejection of invalid/unpublished replacement attempts before history mutation

Pending MF-01 integration:

* real MF-01 `QuestionnaireVersion` entity/table
* production implementation/adapter for `QuestionnaireVersionPublicationPort`
* final database foreign key from `campaign_questionnaire_assignments.questionnaire_version_id`
* end-to-end historical `ScreeningSubmission` version-retention test

### 5. Vaccination center foundation

Implemented, tested and committed:

* `CenterStatus`
* `VaccinationCenter`
* `VaccinationCenterRepository`
* `VaccinationCenterService`
* model and persistence/lifecycle tests

Covered behavior:

* exact center statuses: `ACTIVE`, `INACTIVE`
* unique normalized center code
* center name, address and contact master data
* `safeConcurrentOccupancy` as a required positive physical-safety limit
* optional positive `sessionServiceCeiling`
* center activation/deactivation without hard deletion
* stable center-code policy through updates
* separation of long-term center status from temporary `CenterClosure` records
* no campaign assignment, operating-hours, session, inventory or eligibility logic embedded in the center master

### 6. Backend hardening and correctness pass

Completed after the initial center foundation:

* added deterministic application `java.time.Clock`
* added explicit null-date guards for dose-programme effective dates
* enforced MF02-BR-18 future-only dose-programme replacements
* enforced MF02-BR-17 future-only questionnaire-assignment replacements
* added `VaccinationCampaignRepository.findByIdForUpdate(...)` using `PESSIMISTIC_WRITE`
* serialized dose-programme read-check-modify-insert flows by parent campaign
* serialized questionnaire-assignment read-check-modify-insert flows by parent campaign
* verified the locking query under the H2 test profile
* corrected misleading rollback-test terminology
* clarified MF02-BR-02 Javadocs so configuration non-overlap is distinct from the later exact-one publication prerequisite
* updated and corrected `backend/README.md`
* completed final backend audit with no blockers or fix-before-continuing findings

### 7. Canonical documentation synchronization (PR #17)

Completed after the backend hardening checkpoint:

* reviewed the canonical Markdown and human-readable DOCX for release synchronization;
* kept the authoritative canonical Markdown unchanged as the requirements source of truth;
* synchronized the canonical master DOCX to `3.3-AI` / `2026-08-08-R8` (`docs/Web_Based_Vaccination_Portal_AI_Canonical_Master_v3.3.docx`);
* merged PR #17 into `develop` with merge commit `bd22cb973bad83c8456897805bc2df95b65593d0`;
* verified Backend CI `SUCCESS` and Frontend CI `SUCCESS`.

### 8. Sprint 1 evidence checkpoints (PR #18, PR #19, & PR #21)

Completed after canonical documentation synchronization:

* merged PR #18 (`c120ee8`) recording evidence up to PR #17;
* merged PR #19 (`8cdb112`) recording evidence up to PR #18;
* merged PR #21 (`433dbe1`) recording evidence up to PR #20;
* verified Backend CI `SUCCESS` and Frontend CI `SUCCESS` across all evidence integration PRs.

### 9. OperatingHours domain, persistence, and service foundation (PR #20)

Implemented, tested, hardened, and merged into `develop`:

* `OperatingHours` domain entity
* `OperatingHoursRepository` with center, weekday, effective-date, and scalar center-ID queries
* `VaccinationCenterRepository.findByIdForUpdate(...)` pessimistic write locking
* `OperatingHoursService` managing creation, updates, end-dating, deterministic sorting, and conflict checking
* 19 domain unit tests (`OperatingHoursModelTests`)
* 14 persistence integration tests (`OperatingHoursPersistenceIntegrationTests`)
* 22 service integration tests (`OperatingHoursServiceIntegrationTests`)
* total 55 OperatingHours-specific tests (152 total backend tests passing)

Covered behavior:

* 1 center + 1 weekday + 1 positive-duration opening/closing interval + 1 effective date period
* mandatory center relation, mandatory `DayOfWeek`, mandatory `effectiveFrom`, nullable `effectiveTo`
* inclusive effective date boundaries `[effectiveFrom, effectiveTo]`
* positive same-day shift duration `opening.isBefore(closing)` (overnight and zero-duration shifts rejected)
* half-open time interval interpretation `[opening, closing)`
* complete mutation exception-safety across all constructors and setters via candidate pre-validation (`OperatingHours.validateParameters`)
* split shifts and adjacent shifts touching at boundaries allowed (e.g. `[08:00, 12:00)` and `[12:00, 16:00)`)
* multidimensional overlap conflict enforcement: same center, same weekday, overlapping inclusive dates, overlapping half-open times rejected
* exact duplicate rejection
* identical times in disjoint date spans permitted
* parent `VaccinationCenter` write serialization using `VaccinationCenterRepository.findByIdForUpdate`
* safe lock ordering for update/end-date operations via scalar `findCenterIdById` to eliminate stale-managed-entity race windows
* shortening-only end-date/deactivation workflow (extensions must use `updateOperatingHours` to undergo conflict validation)
* deterministic Monday→Sunday Java calendar sorting using `DayOfWeek.getValue()`
* configuration permitted for both `ACTIVE` and `INACTIVE` centers
* zero hard-delete workflow

### 10. Pre-closure backend hardening (PR #23)

Implemented, reviewed, tested, and merged into `develop` at `5a5b3e7d285b50b7645a1c32b57f79b753b2baff`:

* `0cc1ad9` — `refactor(mf02): harden campaign domain foundations`
* `3683a0d` — `test(mf02): prove serialized mutation invariants`
* `d3efd0e` — `docs(mf02): correct questionnaire history boundary docs`

This integration established the current MF-02 technical baseline:

* absolute/audit and campaign booking-window timestamps use UTC-safe `Instant` values and the application UTC `Clock`;
* `CampaignDoseProgramme` and `CampaignQuestionnaireAssignment` use half-open effective history;
* `OperatingHours` deliberately retains inclusive calendar-date boundaries;
* entity mutation surfaces and unsafe custom entity equality/hash behavior were reduced;
* dose/questionnaire services and repositories and location-target batch handling were simplified around cohesive domain operations;
* genuine separate-transaction locking tests demonstrate serialized sibling-history decisions structurally under H2/MySQL mode.

Validation at this checkpoint was 149 backend tests with zero failures, errors, or skips, Maven `test` and `verify` success, and Backend CI and Frontend CI success. Production MySQL concurrency behavior remains a later environment-specific validation item; the H2 evidence is not represented as MySQL proof.

The narrow documentation correction `d3efd0e` is classified separately from the implementation/hardening commits. The PR #23 merge commit is an integration checkpoint, not an implementation commit. Hosted independent GitHub approval evidence was not recorded in the controller evidence available for this synchronization; this is a neutral process-provenance note and does not change the merged technical state.

### 11. CenterClosure backend foundation (PR #24)

`CenterClosure` is **IMPLEMENTED, REVIEWED, TESTED, CI-VERIFIED, AND MERGED** as a backend foundation. PR #24 merged into `develop` at `e70940b7021778ae35dd2976ac5df6b649a28584`; the preserved feature branch pointer is `d6c04718639d2356f9f4800730d9d79bed5494bf`.

Implementation and test commits:

* `8d70359` — `feat(mf02): add center closure domain and persistence`
* `f1bd663` — `feat(mf02): add center closure management service`
* `cf47f725` — `test(mf02): cover center closure concurrency invariants`
* `d6c0471` — `test(mf02): isolate center closure concurrency fixtures` (test-only CI isolation correction)

Covered behavior:

* mandatory lazy ownership by `VaccinationCenter`, without remove cascades;
* absolute UTC-safe `Instant startAt` / `endAt` with half-open `[startAt, endAt)` semantics;
* positive intervals only; partial-day, overnight, and multi-day intervals are supported;
* mandatory trimmed, nonblank reason of at most 255 characters;
* lifecycle status exactly `ACTIVE` / `CANCELLED`;
* ACTIVE closures for one center cannot overlap; exact adjacency is allowed;
* CANCELLED records do not block a later ACTIVE interval;
* creation may record future, ongoing, or historical intervals;
* normal update and cancellation are permitted only before the existing closure starts;
* cancellation is terminal, retains the row, and preserves the interval and original reason;
* configuration is independent of `CenterStatus` and `OperatingHours`;
* no service hard-delete workflow;
* same-center create/update/cancel decisions serialize through the parent center pessimistic write lock;
* a read-side active-overlap helper is available for later atomic `VaccinationSession` integration, which must share the same center lock.

Verification comprised 45 CenterClosure-specific tests: 15 model, 6 persistence integration, 21 service integration, and 3 concurrency integration tests. The full backend contained 194 tests with zero failures, errors, or skips. Maven `test` and `verify`, Backend CI, and Frontend CI all succeeded. The concurrency suite uses separate service-proxy transactions and provides structural H2/MySQL-mode evidence for overlapping creates, create-versus-update, and different-center independence; production MySQL proof remains deferred.

The independent implementation review verdict was **ACCEPT WITH NON-BLOCKING NOTES**. Canonical, domain, persistence, lifecycle, interval/overlap, concurrency, maintainability, test-quality, and data-integrity gates passed. Deferred notes concerned future Flyway index tuning, possible overlap-query optimization only if profiling warrants it, optional additional persisted audit-transition assertions, and production MySQL concurrency validation; none changed the approved implementation contract.

Hosted independent GitHub approval evidence was not recorded in the controller evidence available for PR #24. The owner-authorized integration and successful technical gates are recorded without fabricating an approval event.

### 12. CampaignCenterAssignment architecture gate

The architecture gate is complete with verdict **CAMPAIGNCENTERASSIGNMENT PLAN READY — SAFE TO IMPLEMENT**. No `CampaignCenterAssignment` production or test code has been started.

The approved implementation contract, recorded here as planned rather than implemented, is:

* operational campaign-to-center authorization, distinct from citizen-facing `CampaignLocationTarget`;
* `LocalDate activeFrom` and nullable exclusive `activeTo`, using half-open `[activeFrom, activeTo)` semantics;
* minimal planned status model `ACTIVE` / `CANCELLED`; ordinary expiry at `activeTo` does not create an `ENDED` status;
* no overlapping non-cancelled periods for the same campaign-center pair; adjacency is allowed;
* ownership is immutable; future period updates, prospective ending, and future cancellation use explicit workflows;
* historical rows are retained and no normal hard-delete service is planned;
* campaign and center lifecycle statuses do not gate foundation configuration;
* assignment periods may extend beyond campaign service dates; later session validation combines both boundaries;
* an open-ended assignment must be explicitly end-dated before a later replacement becomes non-overlapping;
* cross-row conflict decisions serialize on the campaign parent row;
* a later session helper must establish assignment effectiveness for the session's complete calendar-date span.

## Important Canonical Distinctions Preserved

### MF02-BR-02

During draft/configuration:

* effective histories must not overlap;
* zero configuration may temporarily exist while setup is incomplete;
* therefore at most one dose programme / questionnaire assignment may be effective for a campaign/date.

At campaign publication:

* exactly one effective dose-programme version is required;
* exactly one effective assignment referencing a `PUBLISHED` questionnaire version is required.

### MF02-BR-17 and MF02-BR-18

* questionnaire-link replacements are future-safe and do not rewrite existing submission history;
* dose-programme replacements cannot be backdated through the normal replacement workflow;
* historical records are retained with bounded effective periods.

### OperatingHours vs CenterClosure

* `OperatingHours` defines normal weekly recurring opening/closing shifts across effective calendar spans.
* `CenterClosure` represents temporary exception dates/intervals where the facility is unavailable, overriding normal operating hours without mutating base schedule records.
* `CenterStatus` represents long-term master facility status (`ACTIVE` vs `INACTIVE`), not temporary closures.

## Not Yet Completed

* `CampaignCenterAssignment`
* `SessionRecurrenceRule` foundation
* `VaccinationSession` and `TimeSlot` foundations
* session validation under `MF02-BR-05`
* closure/capacity validation under `MF02-BR-06`
* full campaign publication transition/prerequisite validation workflow
* Sprint 1 frontend work
* MF-02 REST controller / DTO / mapper layer where applicable
* Flyway business migrations for implemented MF-02 tables
* OpenAPI updates where applicable
* Spring Security/RBAC integration for MF-02 endpoints
* reference-aware lifecycle enforcement once sessions exist
* final MF-01 questionnaire-version adapter/FK integration

## Blockers / Dependencies

No blocker currently prevents continuing MF-02 Sprint 1 domain implementation.

Known dependency:

* MF-01 has not yet provided the final authoritative `QuestionnaireVersion` integration required by MF-02.
* MF-02 can continue using the explicit `QuestionnaireVersionPublicationPort` boundary.

## Important Decisions

* Keep campaign, session, booking, center and resource status models separate.
* MF-02 owns `VaccineType`; MF-04 references it rather than duplicating it.
* `DoseCategory` is limited to canonical `PRIMARY` and `BOOSTER`.
* Dose-programme history is effective-dated/versioned rather than overwritten.
* `CampaignLocationTarget` controls administrative visibility/service area, not medical eligibility.
* Location matching is exact type/code matching; no administrative hierarchy is inferred.
* MF-02 location targets expose no normal hard-delete workflow.
* MF-01 owns `QuestionnaireVersion`; MF-02 owns `CampaignQuestionnaireAssignment`.
* `QuestionnaireVersionPublicationPort` is the explicit cross-module contract until MF-01 provides the production adapter.
* Effective-dated configuration changes for dose programmes, questionnaire assignments, and center operating hours serialize on the parent entity row using `PESSIMISTIC_WRITE`.
* `OperatingHours` mutators pre-validate all candidate parameters prior to state assignment, guaranteeing exception atomicity.
* Operating hours conflict detection evaluates multidimensional overlap across center, weekday, inclusive dates, and half-open time intervals.
* `CenterStatus` represents long-term `ACTIVE` / `INACTIVE`; temporary unavailability belongs to retained `CenterClosure` records.
* `CenterClosure` uses absolute half-open intervals, explicit cancellation, and parent-center locking without coupling to `OperatingHours`.
* Planned `CampaignCenterAssignment` uses half-open `LocalDate` periods and campaign-row serialization; this plan is not implementation evidence.
* `safeConcurrentOccupancy` and `sessionServiceCeiling` are distinct center constraints.
* Use small, focused logical Git commits rather than large feature commits.

## Commits Recorded So Far

### A. Campaign / targeting / questionnaire checkpoint — 27 commits

#### Campaign foundation

* `3212129` — `test(mf02): configure isolated test schema lifecycle`
* `844ba27` — `feat(mf02): add campaign lifecycle status model`
* `292494b` — `feat(mf02): add vaccine type master entity`
* `1944f88` — `feat(mf02): add target group domain entity`
* `c9027fd` — `feat(mf02): add vaccination campaign domain model`
* `5813adf` — `feat(mf02): add campaign persistence repositories`
* `3bc70c2` — `test(mf02): add campaign persistence coverage`

#### Campaign dose programme

* `9e0b4a1` — `feat(mf02): add dose category model`
* `e799b43` — `test(mf02): cover dose category model`
* `f807ce4` — `feat(mf02): add campaign dose programme domain model`
* `9095012` — `test(mf02): cover dose programme domain validation`
* `2a8e925` — `feat(mf02): add dose programme persistence repository`
* `c14a5c3` — `feat(mf02): enforce effective dose programme versioning`
* `8473409` — `test(mf02): cover dose programme persistence and versioning`

#### Administrative service-area targeting

* `fbc6b67` — `feat(mf02): add location target type model`
* `6361ed7` — `test(mf02): cover location target type model`
* `cc8da25` — `feat(mf02): add campaign location target domain model`
* `f902e5d` — `test(mf02): cover campaign location target validation`
* `ec169ad` — `feat(mf02): add location target persistence repository`
* `b2c9888` — `feat(mf02): add administrative service area targeting`
* `73a3246` — `test(mf02): cover location target persistence and matching`

#### Campaign questionnaire assignment

* `c6c2ae3` — `feat(mf02): add questionnaire publication integration port`
* `08354de` — `feat(mf02): add campaign questionnaire assignment domain model`
* `554bd9c` — `test(mf02): cover questionnaire assignment domain validation`
* `a7bac6a` — `feat(mf02): add questionnaire assignment persistence repository`
* `71ec4fe` — `feat(mf02): enforce effective questionnaire assignment versioning`
* `f34ecb2` — `test(mf02): cover questionnaire assignment persistence and versioning`

This 27-commit checkpoint was merged into `develop` through PR #12 (merge commit `eb4a4d9`).

### B. Vaccination-center and hardening checkpoint — 22 commits after PR #12

#### Vaccination center

* `336301f` — `feat(mf02): add center status model`
* `2a01e91` — `test(mf02): cover center status model`
* `c6d9e91` — `feat(mf02): add vaccination center domain model`
* `db9d004` — `test(mf02): cover vaccination center validation`
* `e7e3d5b` — `feat(mf02): add vaccination center persistence repository`
* `0739418` — `feat(mf02): add vaccination center management service`
* `d4441e9` — `test(mf02): cover vaccination center persistence and lifecycle`

#### Hardening / correctness

* `089e453` — `fix(mf02): guard dose programme effective dates`
* `150f6b4` — `test(mf02): cover null dose programme effective dates`
* `4b7ead6` — `feat(backend): provide application clock`
* `9aac0a1` — `fix(mf02): prevent retroactive dose programme changes`
* `fd5235d` — `test(mf02): cover future-only dose programme changes`
* `c3aac3a` — `fix(mf02): prevent retroactive questionnaire assignment changes`
* `2b41129` — `test(mf02): cover future-only questionnaire assignment changes`
* `e1358a3` — `feat(mf02): add locked campaign lookup`
* `c1047f7` — `test(mf02): verify locked campaign lookup`
* `4cc3339` — `fix(mf02): serialize dose programme configuration changes`
* `f7121ac` — `fix(mf02): serialize questionnaire assignment changes`
* `4d33729` — `test(mf02): clarify rejected questionnaire assignment safety`
* `9452e9c` — `docs(mf02): clarify effective configuration invariants`
* `154824c` — `docs(mf02): clarify publication prerequisite boundaries`
* `2655866` — `docs(backend): update current implementation status`

This 22-commit checkpoint was merged into `develop` through PR #16 (merge commit `201768e`).

### C. OperatingHours backend foundation — 7 commits after PR #19

#### OperatingHours domain foundation

* `e4a56dc` — `feat(mf02): add operating hours domain model`
* `7240684` — `test(mf02): cover operating hours domain validation`
* `0bc876a` — `fix(mf02): preserve operating hours state on invalid updates`

#### OperatingHours persistence layer

* `76460a4` — `feat(mf02): add operating hours persistence`
* `a80d13c` — `test(mf02): cover operating hours persistence`

#### OperatingHours service layer & conflict enforcement

* `1d0ecef` — `feat(mf02): add operating hours management service`
* `4fe94df` — `test(mf02): cover operating hours service behavior`

This 7-commit checkpoint was merged into `develop` through PR #20 (merge commit `1cad899`).

### D. Pre-closure hardening — 3 commits after PR #22

* `0cc1ad9` — `refactor(mf02): harden campaign domain foundations`
* `3683a0d` — `test(mf02): prove serialized mutation invariants`
* `d3efd0e` — `docs(mf02): correct questionnaire history boundary docs`

These commits were merged through PR #23 at `5a5b3e7d285b50b7645a1c32b57f79b753b2baff`.

### E. CenterClosure backend foundation — 4 commits after PR #23

* `8d70359` — `feat(mf02): add center closure domain and persistence`
* `f1bd663` — `feat(mf02): add center closure management service`
* `cf47f725` — `test(mf02): cover center closure concurrency invariants`
* `d6c0471` — `test(mf02): isolate center closure concurrency fixtures`

These commits were merged through PR #24 at `e70940b7021778ae35dd2976ac5df6b649a28584`.

### Commit accounting

* earlier campaign/targeting/questionnaire checkpoint: **27 commits**
* center/hardening checkpoint after PR #12: **22 commits**
* OperatingHours backend foundation checkpoint after PR #19: **7 commits**
* pre-closure hardening after PR #22: **3 commits**
* CenterClosure backend foundation after PR #23: **4 commits**
* cumulative feature/hardening history listed across these five checkpoints: **63 commits**
* PR #12 merge checkpoint: **1 merge commit (`eb4a4d9`)**
* PR #16 merge checkpoint: **1 merge commit (`201768e`)**
* canonical documentation synchronization: **2 documentation commits (`8a26fe6`, `16d4193`) plus PR #17 merge commit `bd22cb9`**
* Sprint 1 evidence record integrations:
  - PR #18 evidence: **2 evidence commits (`c0e6f80`, `735a046`) plus merge commit `c120ee8`**
  - PR #19 evidence: **1 evidence commit (`1afa39d`) plus merge commit `8cdb112`**
  - PR #21 evidence: **1 evidence commit (`787b941`) plus merge commit `433dbe1`**
* OperatingHours integration checkpoint: **PR #20 merge commit `1cad899`**
* metadata stability checkpoint: **PR #22 merge commit `fe101bb`**; it remains evidence metadata rather than a new implementation checkpoint
* pre-closure hardening checkpoint: **PR #23 merge commit `5a5b3e7`**
* CenterClosure integration checkpoint: **PR #24 merge commit `e70940b`**

## Pull Requests / Remote State

### MF-02 implementation integration (PR #12 & PR #16)

* PR #12 merged 27 campaign/targeting/questionnaire commits into `develop` (`eb4a4d9`).
* PR #16 merged 22 center/hardening commits into `develop` (`201768e`).

### Canonical documentation integration (PR #17)

* PR #17 merged canonical DOCX synchronization into `develop` (`bd22cb9`).
* Backend CI: `SUCCESS`, Frontend CI: `SUCCESS`.

### Sprint 1 evidence integrations (PR #18, PR #19, & PR #21)

* PR #18 merged evidence record update into `develop` (`c120ee8`).
* PR #19 merged evidence record update into `develop` (`8cdb112`).
* PR #21 merged evidence record update into `develop` (`433dbe1`).
* Backend CI: `SUCCESS`, Frontend CI: `SUCCESS`.

### OperatingHours backend foundation integration (PR #20)

Confirmed state:

* feature branch: `feature/mf02-operating-hours`;
* feature branch local/remote HEAD was verified at `4fe94df8df7c07ade3c22a39165aff3da1e8795a`;
* the branch contained exactly 7 OperatingHours commits above the prior `develop` baseline (`8cdb112`);
* PR #20 was created from `feature/mf02-operating-hours` to `develop`;
* PR #20 changed exactly 7 intended backend foundation files;
* Backend CI: `SUCCESS` (`pass`, 44s);
* Frontend CI: `SUCCESS` (`pass`, 11s);
* PR #20 was merged successfully into `develop` using a normal merge commit;
* PR #20 merge commit: `1cad899971c8b18804af31bfb079a24285f7d16d`;
* latest verified `origin/develop` after PR #20: `1cad899971c8b18804af31bfb079a24285f7d16d`;
* all 7 feature commits are verified direct ancestors of `origin/develop`;
* remote branch `origin/feature/mf02-operating-hours` remains preserved.

### Metadata stabilization (PR #22)

PR #22 established the overall evidence checkpoint `fe101bb2adfc1215146793a97cd80fdc3176622c`. Under the metadata stability policy, it is not recursively treated as a new domain implementation checkpoint.

### Pre-closure hardening integration (PR #23)

Confirmed state:

* merge commit and resulting `develop` checkpoint: `5a5b3e7d285b50b7645a1c32b57f79b753b2baff`;
* three branch commits were preserved: `0cc1ad9`, `3683a0d`, and `d3efd0e`;
* 149 backend tests passed with zero failures, errors, or skips;
* Maven `test` and `verify`: `SUCCESS`;
* Backend CI: `SUCCESS`;
* Frontend CI: `SUCCESS`.

### CenterClosure backend foundation integration (PR #24)

Confirmed state:

* source branch: `feature/mf02-center-closure`;
* final source pointer: `d6c04718639d2356f9f4800730d9d79bed5494bf`;
* PR #24 contained the four intended commits `8d70359`, `f1bd663`, `cf47f725`, and `d6c0471`;
* PR #24 changed only the four CenterClosure production types and four focused test classes;
* PR #24 merged using the normal merge-commit workflow;
* merge commit and resulting `develop` checkpoint: `e70940b7021778ae35dd2976ac5df6b649a28584`;
* all four feature commits are ancestors of `origin/develop`;
* Backend CI: `SUCCESS`;
* Frontend CI: `SUCCESS`;
* the remote feature branch was preserved at the final source pointer.

## Testing Evidence

### Latest final backend verification

```text
./mvnw test
Tests run: 194
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS

./mvnw verify
BUILD SUCCESS
JAR packaged successfully
```

### Recorded test milestones

* campaign foundation: **20** tests passing
* campaign dose programme: **36** tests passing
* campaign location target: **50** tests passing
* campaign questionnaire assignment: **69** tests passing
* vaccination center foundation: **88** tests passing
* dose-programme null-date hardening: **90** tests passing
* future-only dose-programme hardening: **93** tests passing
* future-only questionnaire-assignment hardening: **96** tests passing
* locked campaign lookup / hardening state: **97** tests passing
* OperatingHours domain validation: **114** tests passing
* OperatingHours mutation-safety fix: **116** tests passing
* OperatingHours persistence foundation: **129** tests passing
* OperatingHours service & conflict enforcement: **152** tests passing
* pre-closure hardening and genuine serialized-mutation coverage: **149** tests passing after removal/consolidation of low-value tests
* CenterClosure domain, persistence, service, lifecycle, and concurrency foundation: **194** tests passing (**45** CenterClosure-specific: 15 model, 6 persistence, 21 service, 3 concurrency)

### CI Evidence

* PR #17 (Documentation Sync): Backend CI `SUCCESS`, Frontend CI `SUCCESS`
* PR #18 (Evidence Checkpoint): Backend CI `SUCCESS`, Frontend CI `SUCCESS`
* PR #19 (Final Evidence Checkpoint): Backend CI `SUCCESS`, Frontend CI `SUCCESS`
* PR #20 (OperatingHours Foundation): Backend CI `SUCCESS`, Frontend CI `SUCCESS`
* PR #21 (Sprint 1 Evidence Checkpoint): Backend CI `SUCCESS`, Frontend CI `SUCCESS`
* PR #22 (Evidence Metadata Stability): Backend CI `SUCCESS`, Frontend CI `SUCCESS`
* PR #23 (Pre-closure Hardening): Backend CI `SUCCESS`, Frontend CI `SUCCESS`
* PR #24 (CenterClosure Foundation): Backend CI `SUCCESS`, Frontend CI `SUCCESS`

## Current Repository State

### Latest overall `develop` state

```text
origin/develop:
e70940b7021778ae35dd2976ac5df6b649a28584

Latest develop merge:
PR #24
feature/mf02-center-closure -> develop

PR #24 merge:
e70940b7021778ae35dd2976ac5df6b649a28584

Latest implementation checkpoint:
PR #24 (e70940b7021778ae35dd2976ac5df6b649a28584)

Evidence file:
docs/evidence/sprint-1/MF02_Sprint1_Evidence.md

Canonical DOCX:
docs/Web_Based_Vaccination_Portal_AI_Canonical_Master_v3.3.docx

Canonical Markdown change in PR #24:
none
```

### Backend implementation checkpoint

```text
Backend implementation checkpoint:
e70940b7021778ae35dd2976ac5df6b649a28584

feature/mf02-center-closure:
d6c04718639d2356f9f4800730d9d79bed5494bf

feature branch local/remote:
synchronized

CenterClosure merge:
PR #24
e70940b7021778ae35dd2976ac5df6b649a28584
```

Latest backend verification remains:

```text
194 tests passing
0 failures
0 errors
0 skipped
BUILD SUCCESS

./mvnw verify:
BUILD SUCCESS
JAR packaged successfully
```

## Evidence Still to Capture

Already captured:

* campaign, dose programme, targeting, questionnaire assignment checkpoints
* vaccination center master and hardening checkpoints (PR #16)
* canonical documentation synchronization (PR #17)
* Sprint 1 evidence integration checkpoints (PR #18, PR #19, PR #21)
* OperatingHours backend foundation (PR #20, merge commit `1cad899`, 152 passing tests)
* pre-closure hardening (PR #23, merge commit `5a5b3e7`, 149 passing tests)
* CenterClosure backend foundation (PR #24, merge commit `e70940b`, 45 component tests and 194 total passing tests)
* Backend CI & Frontend CI results through PR #24

Still to capture:

* CI result(s) for later MF-02 implementation/integration changes
* `CampaignCenterAssignment` commit/test evidence
* `SessionRecurrenceRule` foundation commit/test evidence
* Flyway migration evidence
* API request/response evidence
* frontend lint/build results when frontend work begins
* UI screenshots when available
* integration demonstration evidence
* final MF-01 questionnaire-version adapter/FK integration evidence

## Sprint Review

*To complete formally at the end of Sprint 1.*

### Demonstrated / evidence available so far

* campaign domain foundation
* dose-programme versioning foundation
* administrative service-area targeting foundation
* MF-02 side of campaign-to-published-questionnaire assignment/versioning
* vaccination-center master/lifecycle foundation
* OperatingHours domain, persistence, and service foundation with multidimensional conflict enforcement
* CenterClosure domain, persistence, lifecycle, overlap, and separate-transaction concurrency foundation
* future-only effective configuration protection
* campaign-row and center-row pessimistic locking for critical effective-dated configuration changes
* 194 passing backend tests with successful Maven verify packaging
* R8 canonical DOCX synchronization merged through PR #17
* successful Backend CI and Frontend CI through PR #24
* canonical Markdown integrity preserved

### Not completed / likely carry-forward candidates if Sprint 1 closes before completion

* `CampaignCenterAssignment`
* `SessionRecurrenceRule` foundation
* publication workflow/API/UI/migration/integration evidence as applicable

## Sprint Retrospective

### Worked well

* Small logical commits kept implementation history reviewable.
* Canonical ownership/boundary checks were performed before each MF-02 slice.
* Tests expanded continuously as real behavior was introduced (194 total tests).
* Candidate pre-validation (`OperatingHours.validateParameters`) and scalar parent center lock ordering ensured mutation exception-safety and clean concurrency protection.
* Dedicated documentation and evidence branches preserved clean separation from domain implementation history.
* Post-merge ancestor verification ensured all feature commits are preserved directly in `develop`.
* GitHub Actions CI consistently verified backend and frontend builds across all merged PRs.

### Improve next sprint / remaining Sprint 1 work

* Capture remote PR/CI evidence closer to each integration checkpoint.
* Keep implementation evidence synchronized immediately after a branch/PR transition.
* Add Flyway/API/UI evidence as those layers are implemented instead of postponing all evidence collection to the end.
* Keep evidence updates as derivatives of the existing living record rather than replacing the record with shortened reconstructions.

## Exact Next Work

The `CenterClosure` backend foundation is implemented, reviewed, tested, CI-verified, and merged through PR #24 at checkpoint `e70940b7021778ae35dd2976ac5df6b649a28584`. Sprint 1 and US-13 remain in progress because recurrence, later session integration, and Definition-of-Done layers remain pending.

When MF-02 feature development resumes:

1. Inspect the canonical MF-02 requirements, MF-02 handoff, current repository code and relevant existing tests before planning the next slice.
2. Implement the approved `CampaignCenterAssignment` backend foundation; its architecture gate is complete but no code exists yet.
3. Review, test, and integrate `CampaignCenterAssignment` through a controlled feature PR.
4. Plan and implement the `SessionRecurrenceRule` foundation.
5. Later add `VaccinationSession` enforcement combining campaign dates, assignment effectiveness, `OperatingHours`, `CenterClosure`, and other session constraints.
6. Complete remaining publication checks, Flyway migrations, API/UI/security integration, MF-01 adapter/FK work, production MySQL concurrency validation, and Definition-of-Done evidence in separately approved slices.

**Current stopping point:** `origin/develop` is `e70940b7021778ae35dd2976ac5df6b649a28584`; `CenterClosure` is complete as a backend foundation; `CampaignCenterAssignment` is architecture-ready and **NOT STARTED**; no recurrence or session implementation has begun.

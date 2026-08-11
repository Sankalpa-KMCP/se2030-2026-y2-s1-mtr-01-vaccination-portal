# MF-02 Sprint 1 Evidence Record

**Project:** Web-Based Vaccination Portal (`WBVP-SE2030`)
**Function:** MF-02 — Vaccination Campaign, Center and Session Management
**Canonical version:** `3.3-AI`
**Canonical revision:** `2026-08-08-R8`
**Sprint:** Sprint 1
**Working window:** Weeks 1–3
**Last updated:** 2026-08-11
**Latest implementation integration:** merged into `develop` via PR #40 (Individual `VaccinationSession` change workflows: preview, SHA-256 stale token protection, reschedule, center transfer, cancellation, locking, recurrence provenance preservation, generator compatibility, and `SessionChangeEvent` creation)
**Latest implementation develop checkpoint:** `b72fc6bdcff8b8e6d1817cfa35271edca90bb118`
**Latest completed evidence integration recorded:** PR #39 (`docs(mf02): sync evidence after session change foundation`) at `f6d8c180b8bdf06886ad2647e5469c210ab5d971`
**Evidence record prepared from implementation checkpoint:** `b72fc6bdcff8b8e6d1817cfa35271edca90bb118`
**OperatingHours feature branch HEAD:** `4fe94df8df7c07ade3c22a39165aff3da1e8795a`
**Canonical documentation sync checkpoint (PR #17):** `bd22cb973bad83c8456897805bc2df95b65593d0`
**Historical evidence integration checkpoints:** PR #18 (`c120ee8530c055a6769b68777fd1342abeca193b`), PR #19 (`8cdb1125809770671325a4f47fc80fd5c48b4ddc`), PR #21 (`433dbe1dd919b93d082aa31d3592519b37a75206`), PR #25 (`7b9398b48008e627fc18e5d7a0f864686a69b588`), PR #27 (`04b53da01a80be85873b3619f9aac2b6f54003b6`), PR #29 (`7b7896b0f714742aa6dc8b47bdc2fe2d1c2c2ebf`), PR #31 (`b5b674fec2136a6633fff54b4784cd41f721f929`), PR #33 (`f7360fb8d195c192a0396ca75e86f9825f9f4986`), PR #35 (`f0dcff3c6109b55ae1777937988d63389532e5e8`), PR #37 (`ce540402fd845458fcbaba35b850e5cf74f6131c`), PR #39 (`f6d8c180b8bdf06886ad2647e5469c210ab5d971`)
**OperatingHours backend integration checkpoint (PR #20):** `1cad899971c8b18804af31bfb079a24285f7d16d`
**OperatingHours source branch:** `feature/mf02-operating-hours`
**Pre-closure hardening integration checkpoint (PR #23):** `5a5b3e7d285b50b7645a1c32b57f79b753b2baff`
**CenterClosure integration checkpoint (PR #24):** `e70940b7021778ae35dd2976ac5df6b649a28584`
**CenterClosure source branch final pointer:** `d6c04718639d2356f9f4800730d9d79bed5494bf`
**CampaignCenterAssignment integration checkpoint (PR #26):** `f9c2bbaa8f9f0c13a338ea910961820b34e88d9e`
**CampaignCenterAssignment source branch final pointer:** `5a3d5c23900c28293574d4552cc3a883c09a13fb`
**CampaignCenterAssignment documentation checkpoint (PR #27):** `04b53da01a80be85873b3619f9aac2b6f54003b6`
**SessionRecurrenceRule integration checkpoint (PR #28):** `0dd05a5dce856bbaff6924c54fcf3c395920b076`
**SessionRecurrenceRule source branch final pointer:** `d7409db1d621ba2995162859b3923ba061992861`
**SessionRecurrenceRule documentation checkpoint (PR #29):** `7b7896b0f714742aa6dc8b47bdc2fe2d1c2c2ebf`
**Center scheduling-timezone prerequisite integration checkpoint (PR #30):** `bab77b7a6dc421d622153d422a7d589d3057ae7a`
**Center scheduling-timezone reviewed source pointer:** `d70c67b2ac48fe01f8d5f58491995bd19aa4b833`
**Center scheduling-timezone documentation checkpoint (PR #31):** `b5b674fec2136a6633fff54b4784cd41f721f929`
**VaccinationSession domain foundation integration checkpoint (PR #32):** `b175e8e1dc07cb73e7c32479defa89f5c112402d`
**VaccinationSession domain foundation reviewed source pointer:** `e906ac8e800597b61a9fcea3eb0f3c05796b8d30`
**VaccinationSession domain foundation documentation checkpoint (PR #33):** `f7360fb8d195c192a0396ca75e86f9825f9f4986`
**VaccinationSession one-off validation integration checkpoint (PR #34):** `f0a8880831d27387b16f548a86e849ee2892b599`
**VaccinationSession one-off validation reviewed source pointer:** `59da64445ba98482237dba01d8e2436de12678b6`
**VaccinationSession recurrence occurrence generation integration checkpoint (PR #36):** `c54f59011543cda49a234710a857cdfe48d4b459`
**VaccinationSession recurrence occurrence generation certified source pointer:** `96450eb715e3dbc48547fe1235faa74598b6d009`
**VaccinationSession recurrence occurrence generation actual base:** `f0dcff3c6109b55ae1777937988d63389532e5e8`
**Session change audit foundation integration checkpoint (PR #38):** `b407679f95b6ec791d23bff070f2756dbbaa06b2`
**Session change audit foundation reviewed source pointer:** `aa5b99320a609f412d832e40b9644e95addee039`
**Session change audit foundation actual base:** `ce540402fd845458fcbaba35b850e5cf74f6131c`
**Individual session changes integration checkpoint (PR #40):** `b72fc6bdcff8b8e6d1817cfa35271edca90bb118`
**Individual session changes reviewed source pointer:** `2fa748903a3d630d84fbbf527e3bf73ddb8ffc3e`
**Individual session changes actual base:** `f6d8c180b8bdf06886ad2647e5469c210ab5d971`

> Living evidence record. Update it as Sprint 1 progresses and finalize it at the Sprint Review/Retrospective. This file records implementation and integration evidence; the canonical Markdown remains the requirements source of truth.
>
> **Metadata Stability Policy:** Metadata-only evidence synchronization commits/PRs are recorded as evidence checkpoints and are not recursively treated as new MF-02 implementation checkpoints. Implementation checkpoints remain strictly tied to actual domain/feature implementation integrations.
>
> This update is prepared from PR #40 implementation checkpoint `b72fc6bdcff8b8e6d1817cfa35271edca90bb118`. The merge commit that later integrates this documentation is a separate evidence-metadata checkpoint recorded by Git history and the integration report; it must not be relabeled as the PR #40 implementation SHA. Full Git object identities were re-verified against live Git/GitHub before documentation synchronization.

## Sprint Goal

Build the MF-02 campaign and center-planning foundations needed for a valid vaccination campaign, including campaign lifecycle, dose-programme configuration, administrative service-area targeting, questionnaire linkage, center assignment, center calendar/closure foundations, recurrence foundations, concrete session foundations, session change audit foundations, and individual session change workflows.

Expected integrated Sprint 1 outcome: a valid campaign can be linked to a published questionnaire, has approved service locations and booking/service boundaries, and has usable center-planning, concrete session, change audit, and individual change workflows.

## Sprint Status

**We are still in Sprint 1.**

The campaign foundation, dose-programme foundation, administrative location targeting, MF-02 questionnaire-assignment side, vaccination-center master foundation, pre-closure hardening, **OperatingHours**, **CenterClosure**, **CampaignCenterAssignment**, **SessionRecurrenceRule**, **VaccinationCenter** scheduling-timezone prerequisite, **VaccinationSession** domain and repository foundation, **VaccinationSession** authoritative write-time validation / atomic one-off DRAFT creation, **SessionRecurrenceRule** occurrence generation and idempotent materialization, **SessionChangeEvent** audit model / **SessionImpactReader** integration port / **VaccinationSession** row locking foundation, and **individual VaccinationSession change workflows** (preview, stale token protection, reschedule, center transfer, cancellation, locking, and audit) are implemented, reviewed, certified, tested, CI-verified, and merged into `develop`.

The canonical R8 human-readable DOCX synchronization has also been merged into `develop` through PR #17.

Sprint 1 is **not complete yet** because the remaining MF-02 Sprint 1 foundations still include:

* remaining campaign publication prerequisite/transition integration
* `VaccinationSession` series-wide schedule change orchestration
* recurrence-rule orchestration for materialized series and series exception handling
* MF-03 production impact reader adapter
* outbox pattern for cross-module change events
* `VaccinationSession` lifecycle transition services beyond initial DRAFT
* reverse configuration-impact workflows (later modifications to campaigns, centers, assignments, operating hours, or closures retroactively impacting existing sessions)
* REST/API/DTO/mapper, OpenAPI, security/RBAC, and frontend work where required
* Flyway business migrations
* final MF-01 `QuestionnaireVersion` adapter/FK integration
* final Sprint integration and Definition-of-Done evidence

**MF-02 INDIVIDUAL SESSION CHANGES INTEGRATED.** This milestone covers the current backend-foundation scope for Campaign, DoseProgramme, LocationTarget, QuestionnaireAssignment, VaccinationCenter, OperatingHours, CenterClosure, CampaignCenterAssignment, SessionRecurrenceRule, VaccinationSession domain/persistence, authoritative write-time validation / one-off DRAFT creation, recurrence occurrence generation, session change audit / locking foundation, and individual session change workflows. It does not mean MF-02, US-13, or Sprint 1 is complete.

## Current Verification Snapshot

### Overall `develop` implementation checkpoint after PR #40 Individual session changes integration

```text
Verified origin/develop at Individual session changes merge:
b72fc6bdcff8b8e6d1817cfa35271edca90bb118

Latest develop merge:
PR #40
b72fc6bdcff8b8e6d1817cfa35271edca90bb118

Latest MF-02 implementation checkpoint:
PR #40
b72fc6bdcff8b8e6d1817cfa35271edca90bb118

Reviewed feature source:
2fa748903a3d630d84fbbf527e3bf73ddb8ffc3e

Actual base:
f6d8c180b8bdf06886ad2647e5469c210ab5d971

PR #40 status:
MERGED

Backend CI:
SUCCESS (pre-merge 45s run 31507118413, post-merge 1m3s run 31507250317)

Frontend CI:
SUCCESS (pre-merge 15s run 31507118170, post-merge 18s run 31507250249)

Backend tests:
405 tests passing (26 net-new tests: 5 model, 3 preview record, 3 token hashing, 13 workflow integration, 2 concurrency)
```

PR #24 integrated `CenterClosure`; PR #26 integrated `CampaignCenterAssignment`; PR #28 integrated `SessionRecurrenceRule`; PR #30 integrated `VaccinationCenter` scheduling timezone; PR #32 integrated `VaccinationSession` domain and repository foundation; PR #34 integrated `VaccinationSession` authoritative write-time validation and atomic one-off DRAFT creation; PR #36 integrated `SessionRecurrenceRule` occurrence generation and idempotent materialization; PR #38 integrated the `SessionChangeEvent` audit model, `SessionImpactReader` integration port, and `VaccinationSession` pessimistic row locking foundation; and PR #40 integrated individual `VaccinationSession` change workflows (preview, stale token, reschedule, center transfer, cancellation, locking, and audit). The prior checkpoints remain historical evidence below.

### Historical Session change audit foundation implementation / integration checkpoint (PR #38)

```text
Verified origin/develop at Session change audit foundation merge:
b407679f95b6ec791d23bff070f2756dbbaa06b2

Latest develop merge:
PR #38
b407679f95b6ec791d23bff070f2756dbbaa06b2

Reviewed feature source:
aa5b99320a609f412d832e40b9644e95addee039

Actual base:
ce540402fd845458fcbaba35b850e5cf74f6131c

Backend CI:
SUCCESS (pre-merge run 31503826501, post-merge run 31503969086)

Frontend CI:
SUCCESS (pre-merge run 31503826389, post-merge run 31503969033)

Backend tests:
379 tests passing (25 net-new tests: 13 model, 9 persistence integration, 3 impact summary)
```


```text
Backend implementation develop checkpoint:
c54f59011543cda49a234710a857cdfe48d4b459

Merge evidence:
c54f590 — Merge pull request #36 from Sankalpa-KMCP/feature/mf02-session-occurrence-generation

Feature branch:
feature/mf02-session-occurrence-generation

Certified feature source:
96450eb715e3dbc48547fe1235faa74598b6d009

Actual base:
f0dcff3c6109b55ae1777937988d63389532e5e8

PR #36 status:
MERGED

Backend CI:
SUCCESS (pre-merge 49s, post-merge run 31485025553)

Frontend CI:
SUCCESS (pre-merge 12s, post-merge run 31485025485)

Backend tests at checkpoint:
354 tests passing (25 net-new tests: 19 generation integration, 2 concurrency, 4 mutation protection)
```

### Historical VaccinationSession one-off validation implementation / integration checkpoint (PR #34)

```text
Backend implementation develop checkpoint:
f0a8880831d27387b16f548a86e849ee2892b599

Merge evidence:
f0a8880 — Merge pull request #34 from Sankalpa-KMCP/feature/mf02-vaccination-session-one-off-validation

Feature branch:
feature/mf02-vaccination-session-one-off-validation

Reviewed feature source:
59da64445ba98482237dba01d8e2436de12678b6

PR #34 status:
MERGED

Backend CI:
SUCCESS

Frontend CI:
SUCCESS

Backend tests at checkpoint:
329 tests passing
```

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
379 tests
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

### Evidence integration checkpoints (PR #18, PR #19, PR #21, PR #25, PR #27, PR #29, PR #31, PR #33, PR #35, & PR #37)

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

PR #25 merge commit:
7b9398b48008e627fc18e5d7a0f864686a69b588

PR #27 merge commit:
04b53da01a80be85873b3619f9aac2b6f54003b6

PR #29 merge commit:
7b7896b0f714742aa6dc8b47bdc2fe2d1c2c2ebf

PR #31 merge commit:
b5b674fec2136a6633fff54b4784cd41f721f929

PR #33 merge commit:
f7360fb8d195c192a0396ca75e86f9825f9f4986

PR #33 branch:
docs/mf02-sync-after-vaccination-session-foundation

PR #33 status:
MERGED

PR #35 merge commit:
f0dcff3c6109b55ae1777937988d63389532e5e8

PR #35 branch:
docs/mf02-sync-after-validated-one-off-session

PR #35 status:
MERGED

PR #37 merge commit:
ce540402fd845458fcbaba35b850e5cf74f6131c

PR #37 branch:
docs/mf02-sync-after-recurrence-generation

PR #37 status:
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
| `US-12` | High     | Assign vaccination centers to a campaign                          | In progress — backend foundation implemented, reviewed, tested, CI-verified, and merged; Flyway, API/security/UI, session integration, and final DoD evidence pending |
| `US-13` | High     | Configure center operating hours, closures and recurring sessions | **IN PROGRESS** — VaccinationCenter, OperatingHours, CenterClosure, SessionRecurrenceRule, VaccinationSession domain/persistence foundations, authoritative write-time validation / atomic one-off DRAFT creation, recurrence occurrence generation, and session change audit / locking foundation complete; individual session impact preview, stale-preview protection, reschedule, center transfer, cancellation workflows, series-wide change orchestration, MF-03 adapter, lifecycle transition services beyond DRAFT, reverse configuration impact, API/UI/migration, and final DoD work pending |
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
* booking end date strictly after booking start date
* booking end date on or before service end date
* all seven mandatory entity fields validated on save
* immutable fields protected against in-place reassignment
* valid state transitions strictly enforced
* terminal states block further transitions
* persistence integration verifies database-level schema constraints
* distinct test suites use unique entity codes to prevent cross-suite contamination

### 2. Campaign dose programme

Implemented:

* `CampaignDoseProgramme`
* dose-programme JPA repository
* dose-programme service
* comprehensive domain, service, and concurrency integration tests

Covered behavior:

* immutable dose sequence number (1-based)
* non-negative minimum interval days; zero for dose 1
* positive recommended interval days strictly greater than minimum interval
* active effective periods `[activeFrom, activeTo)`
* prospective-only modification of active dose programmes
* campaign-level optimistic and pessimistic locking
* multi-version history retention per dose number
* overlap prevention for active dose-programme versions

### 3. Campaign administrative location targeting

Implemented:

* `CampaignLocationTarget`
* location-target JPA repository
* location-target service
* domain and service integration tests

Covered behavior:

* target location code, name, and administrative level
* postal code pattern matching
* active/inactive status lifecycle
* campaign-level scoping and overlap prevention

### 4. Campaign questionnaire assignment

Implemented:

* `CampaignQuestionnaireAssignment`
* questionnaire-assignment JPA repository
* questionnaire-assignment service
* domain, service, and lifecycle integration tests

Covered behavior:

* questionnaire version reference validation
* effective period `[activeFrom, activeTo)`
* future-only modification of active assignments
* campaign publication prerequisite validation

### 5. Vaccination center master foundation

Implemented:

* `VaccinationCenter`
* `CenterStatus`
* center JPA repository
* center service
* domain, service, validation, and concurrency integration tests

Covered behavior:

* center code, name, address, contact phone, operating status
* daily capacity, concurrent lane capacity
* active/inactive status transitions
* optimistic locking and concurrency control

### 6. OperatingHours backend foundation

Implemented:

* `OperatingHours`
* `OperatingHoursRepository`
* `OperatingHoursService`
* domain, validation, persistence, service, and concurrency integration tests

Covered behavior:

* weekday opening/closing shift configuration
* inclusive effective calendar-date boundaries
* multi-shift support with overlap prevention
* candidate pre-validation and exception-safe mutation
* center-row serialization for conflicting writes

### 7. CenterClosure backend foundation

Implemented:

* `CenterClosure`
* `CenterClosureStatus`
* `CenterClosureRepository`
* `CenterClosureService`
* domain, validation, persistence, service, and concurrency integration tests

Covered behavior:

* mandatory VaccinationCenter ownership
* UTC-safe `Instant startAt` / `endAt` with half-open `[startAt, endAt)` semantics
* status exactly `ACTIVE` / `CANCELLED`
* same-center ACTIVE overlap prevention with adjacency allowed
* future-only normal update/cancellation, terminal cancellation, and retained history
* center-row pessimistic serialization
* read-side overlap helper for session validation

### 8. CampaignCenterAssignment backend foundation

Implemented:

* `CampaignCenterAssignment`
* `CampaignCenterAssignmentStatus`
* `CampaignCenterAssignmentRepository`
* `CampaignCenterAssignmentService`
* domain, validation, persistence, service, and concurrency integration tests

Covered behavior:

* operational authorization for center to serve campaign
* half-open calendar period `[activeFrom, activeTo)`
* same campaign-center ACTIVE overlap prevention
* campaign-row pessimistic serialization with authoritative refresh

### 9. SessionRecurrenceRule backend foundation

Implemented:

* `SessionRecurrenceRule`
* `SessionRecurrenceRuleStatus`
* `SessionRecurrenceRuleRepository`
* `SessionRecurrenceRuleService`
* domain, validation, persistence, service, and worker integration tests

Covered behavior:

* bounded weekly recurrence definition
* immutable campaign/center relationships
* element-collection `Set<DayOfWeek>`
* inclusive `startDate`/`endDate`, same-local-day `[startTime, endTime)`
* normalized `ZoneId` string, immutable `generationKey`
* prospective modification and cancellation

### 10. VaccinationSession domain and repository foundation (PR #32)

Implemented:

* `VaccinationSession` entity
* `SessionLifecycleStatus` enum: `DRAFT`, `SCHEDULED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`
* `SessionBookingStatus` enum: `NOT_OPEN`, `OPEN`, `FULL`, `CLOSED`, `SUSPENDED`
* `VaccinationSessionRepository` with `findByOccurrenceKey`
* composite database indexes and unique constraint `uk_session_occurrence_key`

Covered behavior:

* mandatory immutable campaign ownership, mandatory center hosting relationship
* all-or-none recurrence provenance invariant
* normalized `ZoneId` `scheduleTimezone` snapshot
* half-open `[startAt, endAt)` interval validation with `bookingCloseAt <= startAt`
* initial lifecycle strictly `DRAFT`

### 11. VaccinationSession authoritative write-time validation and atomic one-off DRAFT creation (PR #34)

Implemented:

* `VaccinationSessionValidator`
* `VaccinationSessionService` with `createOneOffDraftSession`, `getSessionsForCampaign`, `getSessionsForCenter`
* `VaccinationSessionRepository` queries `findByCampaignId` and `findByCenterId`
* 35 integration tests across validation, service, and concurrency test suites

Covered behavior:

* **Candidate Invariants:** Rejects null timestamps, `startAt >= endAt`, and `bookingCloseAt > startAt`.
* **Center State:** Requires center `status == CenterStatus.ACTIVE`.
* **Scheduling Timezone Projection:** Resolves center `ZoneId` to project `startAt` and `endAt` into local calendar dates (`firstTouchedDate`, `lastTouchedDate = endAt.minusNanos(1)`).
* **Campaign Containment:** Ensures all touched calendar dates fall within `[campaign.startDate, campaign.endDate]`.
* **CampaignCenterAssignment Coverage:** Requires an active assignment covering the full span `[firstTouchedDate, lastTouchedDate + 1)`.
* **OperatingHours Contiguous Coverage:** For each touched date, validates that the session interval is fully covered by contiguous active OperatingHours shifts on that day of week. OperatingHours entities are explicitly refreshed prior to evaluation to guard against stale first-level cache state.
* **CenterClosure Overlap Exclusion:** Ensures the session interval `[startAt, endAt)` does not overlap any active `CenterClosure` on `[closure.startAt, closure.endAt)`. Exact boundary adjacencies are permitted.
* **No Generic Same-Center Overlap Prohibition:** Multiple concurrent or overlapping sessions may exist at the same center (AC-9).
* **Deterministic Write Locking:** `campaignRepository.findByIdForUpdate(campaignId)` followed by `centerRepository.findByIdForUpdate(centerId)`.
* **Authoritative Reload:** `EntityManager.refresh()` on both campaign and center post-lock.
* **One-Off DRAFT Construction:** Timezone inherited from center, recurrence provenance null (`recurrenceRule = null`, `occurrenceKey = null`, `originOccurrenceDate = null`), initial lifecycle `DRAFT`.
* **Write-Time Validity Boundary:** Guarantees serialized write-time validation at creation instant; reverse-impact workflows remain explicitly deferred.

### 13. Individual VaccinationSession change workflows (PR #40)

Implemented:

* `SessionChangePreview` immutable record encapsulating session ID, change type, old snapshot, proposed snapshot, impact counts, and preview token
* `VaccinationSessionService` preview methods (`previewReschedule`, `previewCenterTransfer`, `previewRescheduleOrTransfer`, `previewCancellation`)
* `VaccinationSessionService` confirm methods (`confirmReschedule`, `confirmCenterTransfer`, `confirmRescheduleOrTransfer`, `confirmCancellation`)
* `VaccinationSessionService.generatePreviewToken` cryptographic SHA-256 fingerprint generator
* `VaccinationSession` domain mutation methods (`reschedule`, `cancel`, `transitionLifecycleStatus`)
* `SessionOccurrenceGenerationService` center transfer occurrence recognition
* 26 net-new tests across model, preview record, deterministic token hashing, workflow integration, and concurrency suites

Covered behavior:

* **Read-only Preview Operations:** `previewReschedule`, `previewCenterTransfer`, `previewRescheduleOrTransfer`, and `previewCancellation` compute old/new snapshots, evaluate cross-module impact counts, generate preview tokens, and run validation without mutating state.
* **Reschedule & Center Transfer:** `confirmReschedule`, `confirmCenterTransfer`, and `confirmRescheduleOrTransfer` revalidate schedule bounds, active hosting center status, campaign service boundaries, operational assignment coverage, operating hours contiguous coverage, and closure exclusions via `VaccinationSessionValidator`. On center transfer, updates session center and changes `scheduleTimezone` to the target center's timezone.
* **Booking Close Policy:** Preserves existing `bookingCloseAt` if `<= newStartAt`; otherwise resets `bookingCloseAt` to `newStartAt` (or validates explicit new timestamp).
* **Cancellation:** `confirmCancellation` and `session.cancel()` transition `lifecycleStatus` to `CANCELLED`, retain session row, preserve recurrence provenance, validate non-blank `reason` (max 500) and `actorId` (max 64), and create append-only `SessionChangeEvent` with `CANCELLED` change type. MF-03 records are not modified.
* **Cryptographic Stale-Preview Protection:** Computes deterministic SHA-256 fingerprint from `sessionId|oldCenterId|oldTz|oldStart|oldEnd|oldClose|oldStatus|newCenterId|newTz|newStart|newEnd|newClose|newStatus|changeType|apptCount|waitlistCount`. At confirmation, write locks are acquired, entities refreshed, schedule revalidated, and token recomputed; mismatches reject with `IllegalStateException` prior to session mutation.
* **Deterministic Hierarchical Parent Locking:** Same-center change locks `campaign → center → session`; center transfer locks `campaign → center(min(oldId, newId)) → center(max(oldId, newId)) → session`. Order is fully compatible with existing `campaign → center → rule` generation locking; zero lock inversion.
* **Cross-Module Impact Policy:** `DRAFT` sessions evaluate impact counts as known zero. `SCHEDULED` sessions strictly require real `SessionImpactReader` counts and reject if unavailable. No fake zero-count production adapter exists.
* **Occurrence Isolation & Generator Compatibility:** Individual overrides preserve `recurrenceRule`, `occurrenceKey`, and `originOccurrenceDate`. Sibling occurrences remain on original schedules. `SessionOccurrenceGenerationService` recognizes individually modified/transferred/cancelled occurrences by `occurrenceKey` as already materialized without reset or duplicate generation. True provenance corruption remains strictly rejected.
* **Controller Decision Recorded:** Individually edited occurrences must be preserved during later series changes and shown as exceptions.
* **Deferred Scope:** Series-wide schedule change orchestration, recurrence-rule orchestration for materialized series, series exception handling, MF-03 production adapter, outbox pattern, reverse configuration-impact workflows, lifecycle transitions beyond initial DRAFT, migrations, and API/security/UI remain NOT IMPLEMENTED.

### PR #40 Integration Facts

Confirmed state:

* source branch: `feature/mf02-individual-session-changes`;
* reviewed and preserved source pointer: `2fa748903a3d630d84fbbf527e3bf73ddb8ffc3e`;
* PR #40 contained exactly one reviewed implementation commit;
* PR #40 changed exactly 10 files (4 production, 6 test; 1,725 additions, 10 deletions):
  * `VaccinationSession.java`
  * `SessionChangePreview.java`
  * `SessionOccurrenceGenerationService.java`
  * `VaccinationSessionService.java`
  * `Mf02ConcurrencyIntegrationTests.java`
  * `SessionChangePreviewTests.java`
  * `SessionChangeTokenTests.java`
  * `SessionOccurrenceGenerationConcurrencyIntegrationTests.java`
  * `VaccinationSessionChangeIntegrationTests.java`
  * `VaccinationSessionModelTests.java`
* no canonical, evidence, migration, API, frontend, or package-movement changes entered the implementation PR;
* PR #40 merged through the normal merge-commit workflow at `b72fc6bdcff8b8e6d1817cfa35271edca90bb118`;
* merge parents: `f6d8c180b8bdf06886ad2647e5469c210ab5d971` and `2fa748903a3d630d84fbbf527e3bf73ddb8ffc3e`;
* the reviewed source commit is an ancestor of `develop` and its tree matches the merge result;
* Backend CI and Frontend CI succeeded before merge and again on the merged checkpoint;
* no CI correction commit was required, and the remote feature branch remains preserved.

```text
./mvnw test
Tests run: 405
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

## Testing Evidence

### Latest final backend verification

```text
./mvnw test
Tests run: 405
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
* CampaignCenterAssignment domain, persistence, service, lifecycle, and concurrency foundation: **242** tests passing (**48** CampaignCenterAssignment-specific: 8 model, 9 persistence, 27 service, 4 concurrency)
* SessionRecurrenceRule domain, persistence, service, lifecycle, and authoritative-mutation foundation: **272** tests passing (**30** SessionRecurrenceRule-specific: 8 model, 7 persistence, 14 service, 1 concurrency)
* VaccinationCenter scheduling-timezone and authoritative-mutation prerequisite: **276** tests passing (**21** focused center tests in the implementation/review gate)
* VaccinationSession domain and repository foundation: **290** tests passing (**14** VaccinationSession-specific: 8 model, 6 persistence)
* VaccinationSession authoritative write-time validation and atomic one-off DRAFT creation: **329** tests passing (**53** VaccinationSession-focused: 8 model, 6 persistence, 25 validation, 7 service, 3 concurrency)
* SessionRecurrenceRule occurrence generation and idempotent materialization: **354** tests passing (**25** net-new tests: 19 generation integration, 2 concurrency, 4 mutation protection)
* Session change audit foundation, SessionImpactReader port, and VaccinationSession row locking: **379** tests passing (**25** net-new tests: 13 model, 9 persistence, 3 impact summary)
* Individual VaccinationSession change workflows: **405** tests passing (**26** net-new tests: 5 model, 3 preview record, 3 token hashing, 13 workflow integration, 2 concurrency)

### CI Evidence

* PR #17 (Documentation Sync): Backend CI `SUCCESS`, Frontend CI `SUCCESS`
* PR #18 (Evidence Checkpoint): Backend CI `SUCCESS`, Frontend CI `SUCCESS`
* PR #19 (Final Evidence Checkpoint): Backend CI `SUCCESS`, Frontend CI `SUCCESS`
* PR #20 (OperatingHours Foundation): Backend CI `SUCCESS`, Frontend CI `SUCCESS`
* PR #21 (Sprint 1 Evidence Checkpoint): Backend CI `SUCCESS`, Frontend CI `SUCCESS`
* PR #22 (Evidence Metadata Stability): Backend CI `SUCCESS`, Frontend CI `SUCCESS`
* PR #23 (Pre-closure Hardening): Backend CI `SUCCESS`, Frontend CI `SUCCESS`
* PR #24 (CenterClosure Foundation): Backend CI `SUCCESS`, Frontend CI `SUCCESS`
* PR #26 (CampaignCenterAssignment Foundation): Backend CI `SUCCESS`, Frontend CI `SUCCESS` before merge and after merge
* PR #28 (SessionRecurrenceRule Foundation): Backend CI `SUCCESS`, Frontend CI `SUCCESS` before merge and after merge
* PR #30 (Center Scheduling Timezone): Backend CI `SUCCESS`, Frontend CI `SUCCESS` before merge and after merge
* PR #31 (Center Scheduling Timezone Evidence): Backend CI `SUCCESS`, Frontend CI `SUCCESS` before merge and after merge
* PR #32 (VaccinationSession Foundation): Backend CI `SUCCESS`, Frontend CI `SUCCESS` before merge and after merge
* PR #33 (VaccinationSession Foundation Evidence): Backend CI `SUCCESS`, Frontend CI `SUCCESS` before merge and after merge
* PR #34 (VaccinationSession One-Off Validation & Creation): Backend CI `SUCCESS`, Frontend CI `SUCCESS` before merge and after merge
* PR #35 (VaccinationSession One-Off Validation Evidence): Backend CI `SUCCESS`, Frontend CI `SUCCESS` before merge and after merge
* PR #36 (SessionRecurrenceRule Occurrence Generation): Backend CI `SUCCESS`, Frontend CI `SUCCESS` before merge and after merge
* PR #37 (SessionRecurrenceRule Occurrence Generation Evidence): Backend CI `SUCCESS`, Frontend CI `SUCCESS` before merge and after merge
* PR #38 (Session Change Audit Foundation & Row Locking): Backend CI `SUCCESS`, Frontend CI `SUCCESS` before merge and after merge
* PR #39 (Session Change Audit Foundation Evidence): Backend CI `SUCCESS`, Frontend CI `SUCCESS` before merge and after merge
* PR #40 (Individual VaccinationSession Changes): Backend CI `SUCCESS`, Frontend CI `SUCCESS` before merge and after merge

## Current Repository State

### Latest overall `develop` state

```text
origin/develop implementation checkpoint used to prepare this evidence update:
b72fc6bdcff8b8e6d1817cfa35271edca90bb118

Latest develop merge:
PR #40
feature/mf02-individual-session-changes -> develop

PR #40 merge:
b72fc6bdcff8b8e6d1817cfa35271edca90bb118

Latest implementation checkpoint:
PR #40 (b72fc6bdcff8b8e6d1817cfa35271edca90bb118)

Evidence file:
docs/evidence/sprint-1/MF02_Sprint1_Evidence.md

Canonical DOCX:
docs/Web_Based_Vaccination_Portal_AI_Canonical_Master_v3.3.docx

Canonical Markdown change in PR #40:
none
```

### Backend implementation checkpoint (PR #40 Individual session changes integration)

```text
Backend implementation checkpoint:
b72fc6bdcff8b8e6d1817cfa35271edca90bb118

feature/mf02-individual-session-changes:
2fa748903a3d630d84fbbf527e3bf73ddb8ffc3e

feature branch local/remote:
synchronized

Individual session changes merge:
PR #40
b72fc6bdcff8b8e6d1817cfa35271edca90bb118
```

Latest backend verification:

```text
405 tests passing
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
* Sprint 1 evidence integration checkpoints (PR #18, PR #19, PR #21, PR #25, PR #27, PR #29, PR #31, PR #33, PR #35, PR #37, PR #39)
* OperatingHours backend foundation (PR #20, merge commit `1cad899`, 152 passing tests)
* pre-closure hardening (PR #23, merge commit `5a5b3e7`, 149 passing tests)
* CenterClosure backend foundation (PR #24, merge commit `e70940b`, 45 component tests and 194 total passing tests)
* CampaignCenterAssignment backend foundation (PR #26, merge commit `f9c2bba`, 48 component tests and 242 total passing tests)
* SessionRecurrenceRule backend foundation (PR #28, merge commit `0dd05a5`, 30 component tests and 272 total passing tests)
* VaccinationCenter scheduling-timezone prerequisite (PR #30, merge commit `bab77b7`, reviewed source `d70c67b`, 21 focused center tests and 276 total passing tests)
* VaccinationSession domain and repository foundation (PR #32, merge commit `b175e8e`, reviewed source `e906ac8`, 14 focused tests and 290 total passing tests)
* VaccinationSession authoritative write-time validation and atomic one-off DRAFT creation (PR #34, merge commit `f0a8880`, reviewed source `59da644`, 35 focused tests, 53 focused suite total, and 329 total passing tests)
* SessionRecurrenceRule occurrence generation and idempotent materialization (PR #36, merge commit `c54f590`, certified source `96450eb`, 25 net-new tests and 354 total passing tests)
* SessionChangeEvent audit foundation, SessionImpactReader port, and VaccinationSession row locking (PR #38, merge commit `b407679`, reviewed source `aa5b993`, 25 net-new tests and 379 total passing tests)
* Individual VaccinationSession change workflows (PR #40, merge commit `b72fc6b`, reviewed source `2fa7489`, 26 net-new tests and 405 total passing tests)
* Backend CI & Frontend CI results through PR #40, including the merged implementation checkpoint

Still to capture:

* series-wide schedule change preview and confirmation orchestration evidence
* recurrence-rule orchestration for materialized series and series exception handling evidence
* MF-03 production impact reader adapter evidence
* outbox pattern cross-module event evidence
* session lifecycle transition management evidence beyond initial DRAFT
* reverse configuration-impact workflow evidence (modifications to existing campaigns/centers/assignments/hours/closures)
* CI result(s) for later MF-02 implementation/integration changes
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
* CampaignCenterAssignment operational authorization, lifecycle, effective-period, and separate-transaction concurrency foundation
* SessionRecurrenceRule bounded weekly definition, timezone/lifecycle, generation identity, element-collection, and authoritative-mutation foundation
* VaccinationCenter explicit normalized scheduling timezone and authoritative mutation locking/refresh prerequisite
* VaccinationSession domain entity, separate canonical lifecycle and booking-projection enums, all-or-none recurrence provenance, occurrence-key uniqueness, normalized timezone snapshot, half-open interval validation, booking-close ordering invariant, and repository lookup
* VaccinationSession shared authoritative write-time validator (`VaccinationSessionValidator`), enforcing candidate bounds, center ACTIVE state, local timezone projection, campaign service dates, active assignment coverage, operating hours contiguous coverage with entity refresh, and closure exclusion
* VaccinationSession deterministic parent write locking (`campaign -> center`), post-lock authoritative refresh, and atomic one-off DRAFT creation (`VaccinationSessionService.createOneOffDraftSession`)
* SessionRecurrenceRule occurrence generation and idempotent materialization (`SessionOccurrenceGenerationService`), including deterministic date enumeration, controller decision `bookingCloseAt = startAt`, normalized rule/center timezone compatibility, explicit DST resolution, stable occurrence identity, global lock ordering (`campaign → center → rule`), validator reuse, all-or-nothing batch persistence, and same-rule concurrency serialization
* interim recurrence mutation protection (`SessionRecurrenceRuleService`), preventing definition updates and shortening past existing occurrences while preserving rule cancellation
* SessionChangeEvent append-only audit model, SessionScheduleSnapshot immutable embeddable, SessionChangeType enum, and deterministic history repository ordering (`SessionChangeEventRepository.findBySessionIdOrderByCreatedAtAscIdAsc`)
* SessionImpactReader interface and SessionImpactSummary value record defining the MF-02 -> MF-03 cross-module integration port boundary
* VaccinationSession pessimistic write row locking query (`VaccinationSessionRepository.findByIdForUpdate`)
* Individual VaccinationSession change workflows (`SessionChangePreview`, `VaccinationSessionService` preview/confirm for reschedule, center transfer, and cancellation, SHA-256 stale token protection, hierarchical write locking `campaign → center(s) → session`, booking close policy, and atomic `SessionChangeEvent` audit creation)
* Recurrence occurrence isolation and generator compatibility for individual overrides (`SessionOccurrenceGenerationService` recognizing modified occurrences as already materialized without reset)
* separate-transaction concurrency tests demonstrating deactivation race rejection, deadlock-free same-center concurrent creations, closure write races, concurrent same-rule recurrence generation, same-session concurrent change serialization, and bidirectional center transfer locking
* future-only effective configuration protection
* campaign-row, center-row, and session-row pessimistic locking for critical effective-dated configuration and session changes
* 405 passing backend tests with successful Maven verify packaging
* R8 canonical DOCX synchronization merged through PR #17
* successful Backend CI and Frontend CI through PR #40 and its merged checkpoint
* canonical Markdown integrity preserved

### Not completed / likely carry-forward candidates if Sprint 1 closes before completion

* VaccinationSession series-wide schedule change orchestration
* recurrence-rule orchestration for materialized series and series exception handling
* MF-03 production impact reader adapter
* outbox pattern for cross-module change events
* session lifecycle transition services beyond initial DRAFT
* reverse configuration-impact workflows
* publication workflow/API/UI/migration/integration evidence as applicable

## Sprint Retrospective

### Worked well

* Small logical commits kept implementation history reviewable.
* Canonical ownership/boundary checks were performed before each MF-02 slice.
* Tests expanded continuously as real behavior was introduced (405 total tests).
* Candidate pre-validation (`OperatingHours.validateParameters`, `VaccinationSession.validateSchedule`, `VaccinationSession.validateProvenance`, `VaccinationSessionValidator.validateSessionSchedule`, `SessionChangeEvent` invariants, `SessionChangePreview` invariants) ensured mutation exception-safety and clean concurrency protection.
* Dedicated documentation and evidence branches preserved clean separation from domain implementation history.
* Post-merge ancestor verification ensured all feature commits are preserved directly in `develop`.
* GitHub Actions CI consistently verified backend and frontend builds across all merged PRs.

### Improve next sprint / remaining Sprint 1 work

* Capture remote PR/CI evidence closer to each integration checkpoint.
* Keep implementation evidence synchronized immediately after a branch/PR transition.
* Add Flyway/API/UI evidence as those layers are implemented instead of postponing all evidence collection to the end.
* Keep evidence updates as derivatives of the existing living record rather than replacing the record with shortened reconstructions.

## Exact Next Work

The individual `VaccinationSession` change workflows are implemented, independently reviewed, certified, tested, CI-verified, and merged through PR #40 at the latest MF-02 implementation checkpoint `b72fc6bdcff8b8e6d1817cfa35271edca90bb118`, with reviewed source `2fa748903a3d630d84fbbf527e3bf73ddb8ffc3e`. OperatingHours, CenterClosure, CampaignCenterAssignment, SessionRecurrenceRule, the center-timezone prerequisite, the VaccinationSession domain foundation, one-off write-time validation, recurrence occurrence generation, session change audit / locking foundation, and individual session change workflows are complete. **MF-02 INDIVIDUAL SESSION CHANGES INTEGRATED.** Sprint 1 and US-13 remain in progress because series-wide change orchestration, recurrence-rule orchestration for materialized series, series exception handling, MF-03 adapter, lifecycle transitions beyond DRAFT, reverse configuration impact, and Definition-of-Done layers remain pending.

When MF-02 feature development resumes:

1. Perform architecture and implementation for series-wide VaccinationSession changes building on `SessionChangeEvent`, `SessionRecurrenceRule`, `VaccinationSessionValidator`, and individual occurrence exception isolation.
2. Ensure controller decisions are adhered to: individually edited occurrences must be preserved during later series changes and shown as exceptions; SCHEDULED session changes use real MF-03 impact counts; no fake zero-count MF-03 production adapter.
3. Keep outbox, reverse configuration-impact workflows, and lifecycle transition services beyond DRAFT deferred to their scoped phases.

**Current implementation stopping point:** Individual session changes merge `b72fc6bdcff8b8e6d1817cfa35271edca90bb118`; OperatingHours, CenterClosure, CampaignCenterAssignment, SessionRecurrenceRule, the center-timezone prerequisite, the VaccinationSession domain/persistence foundation, authoritative one-off write-time validation / DRAFT creation, recurrence occurrence generation, session change audit / locking foundation, and individual session change workflows are complete; series-wide session changes are **NOT STARTED**. The next controlled phase is series-wide VaccinationSession change architecture and implementation.

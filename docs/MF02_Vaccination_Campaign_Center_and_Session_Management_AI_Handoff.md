# MF-02 — Vaccination Campaign, Center and Session Management

## AI Engineering Handoff

**Project:** Web-Based Vaccination Portal (`WBVP-SE2030`)

**Function:** MF-02 — Vaccination Campaign, Center and Session Management

**Canonical authority:** `3.3-AI / 2026-08-08-R8`

**Sprint:** Sprint 1 — **IN PROGRESS**

**Current implementation checkpoint:** `c54f59011543cda49a234710a857cdfe48d4b459` (PR #36 merge)

**Latest completed backend foundation:** `SessionRecurrenceRule occurrence generation and idempotent VaccinationSession materialization`

**Next controlled implementation phase:** `VaccinationSession one-occurrence and series-change architecture gate` — **READY TO START**

> This handoff is optimized for the next AI session. The canonical Markdown is the semantic requirements source of truth; live Git history and tests are the implementation source of truth. Do not rebuild completed foundations or treat this handoff as authority to change canonical rules.
>
> The latest MF-02 implementation integration checkpoint is PR #36 merge `c54f59011543cda49a234710a857cdfe48d4b459`, with certified feature source `96450eb715e3dbc48547fe1235faa74598b6d009` and base `f0dcff3c6109b55ae1777937988d63389532e5e8`. Any later documentation PR merge containing this handoff is an overall repository metadata checkpoint; it does not replace or rename the PR #36 implementation checkpoint.

## 1. Executive Snapshot

The following MF-02 backend foundations are already implemented and integrated:

* campaign lifecycle, VaccineType, and TargetGroup;
* CampaignDoseProgramme;
* CampaignLocationTarget;
* CampaignQuestionnaireAssignment;
* VaccinationCenter;
* OperatingHours;
* CenterClosure;
* CampaignCenterAssignment;
* SessionRecurrenceRule;
* VaccinationCenter scheduling timezone and authoritative mutation locking prerequisite;
* VaccinationSession domain and repository foundation;
* VaccinationSession authoritative write-time validation and atomic one-off DRAFT creation;
* SessionRecurrenceRule occurrence generation and idempotent VaccinationSession materialization.

`CenterClosure` remains **IMPLEMENTED, REVIEWED, TESTED, CI-VERIFIED, AND MERGED**.

`CampaignCenterAssignment` remains **IMPLEMENTED, REVIEWED, TESTED, CI-VERIFIED, AND MERGED** at backend-foundation scope through PR #26.

`SessionRecurrenceRule` is **IMPLEMENTED, REVIEWED, TESTED, CI-VERIFIED, AND MERGED** at backend-foundation scope through PR #28.

The `VaccinationCenter` scheduling-timezone and mutation-locking prerequisite is **IMPLEMENTED, INDEPENDENTLY REVIEWED, TESTED, CI-VERIFIED, AND MERGED** through PR #30.

The `VaccinationSession` domain and persistence foundation is **IMPLEMENTED, INDEPENDENTLY REVIEWED, TESTED, CI-VERIFIED, AND MERGED** through PR #32.

The `VaccinationSession` authoritative write-time validation and atomic one-off DRAFT creation is **IMPLEMENTED, INDEPENDENTLY REVIEWED, TESTED, CI-VERIFIED, AND MERGED** through PR #34.

The `SessionRecurrenceRule` occurrence generation and idempotent `VaccinationSession` materialization is **IMPLEMENTED, INDEPENDENTLY REVIEWED, CERTIFIED, TESTED, CI-VERIFIED, AND MERGED** through PR #36:
* `SessionOccurrenceGenerationService` provides transactional occurrence materialization (`generateOccurrences(Long ruleId)` and `generateOccurrences(Long ruleId, LocalDate requestedStart, LocalDate requestedEnd)`).
* Candidate recurrence dates are enumerated in ascending order over the intersection of rule boundaries and requested windows, filtered strictly by configured rule weekdays.
* Controller decision applied: every generated occurrence receives `bookingCloseAt = startAt` exactly (controller implementation decision filling an unspecified generation detail; not derived from `VaccinationCampaign.bookingCloseAt`, not capped, no lead-time offset, and no recurrence rule schema change).
* Enforces strict normalized rule/center timezone equality (`rule.getTimezone().equals(center.getTimezone())`); mismatches reject before persistence.
* DST transitions are resolved explicitly via `ZoneRules.getValidOffsets`: 0 offsets (gap) reject with `IllegalArgumentException` without silent shifting; 2 offsets (overlap) deterministically select `validOffsets.get(0)` (earlier offset). Asserts `startAt < endAt`.
* Deterministic immutable occurrence identity: `occurrenceKey = <generationKey>:<originOccurrenceDate>` (47 characters, within 64-char limit), independent of mutable current timestamps.
* Global pessimistic lock ordering: acquires write locks in strict order `campaign → center → rule` with authoritative post-lock `EntityManager.refresh` on all three entities. Audit confirmed no cross-workflow lock inversion.
* Reuses `VaccinationSessionValidator.validateSessionSchedule` for candidate validation against campaign service dates, center ACTIVE status, assignment coverage, operating hours contiguous coverage, and closure exclusion. Zero duplicated validation rules.
* Idempotent & atomic: existing occurrences matching rule generation keys are retained untouched; all missing candidates in a generation call are validated and persisted atomically via `sessionRepository.saveAll()` and `flush()`. Any single validation failure rolls back all missing candidates while preserving pre-existing sessions.
* Same-rule concurrency: serialized via recurrence-rule pessimistic locking backed by database `uk_session_occurrence_key` constraint.
* Interim mutation protection in `SessionRecurrenceRuleService`: `updateFutureRule` is blocked if generated sessions exist for the rule; `shortenRuleEnd` is blocked if any generated occurrence has `originOccurrenceDate > newEndDate`; `cancelRule` remains permitted and leaves existing generated sessions intact.

Current backend validation is **354 tests** (25 net-new tests: 19 generation integration, 2 concurrency, 4 mutation protection) with zero failures, errors, or skips. Maven `test` and `verify` succeeded, the executable JAR packaged successfully, and Backend CI and Frontend CI succeeded before and after the PR #36 merge.

**MF-02 RECURRENCE OCCURRENCE GENERATION & IDEMPOTENT SESSION MATERIALIZATION INTEGRATED.** This milestone covers the current backend-foundation scope for Campaign, DoseProgramme, LocationTarget, QuestionnaireAssignment, VaccinationCenter, OperatingHours, CenterClosure, CampaignCenterAssignment, SessionRecurrenceRule, VaccinationSession domain/persistence, authoritative write-time validation / one-off DRAFT creation, and recurrence occurrence generation. It does not mean MF-02, US-13, or Sprint 1 is complete.

Sprint 1 remains in progress. Recurrence occurrence generation is complete, but individual occurrence rescheduling/cancellation workflows, series-wide schedule changes, SessionChangeEvent, reverse configuration-impact workflows, lifecycle transitions beyond initial DRAFT, migrations, API/security/UI layers, publication integration, and final Definition-of-Done evidence remain pending.

## 2. Authority and Startup Procedure

Use this source order:

1. `docs/Web_Based_Vaccination_Portal_AI_Canonical_Context_v3.3.md` for semantic requirements;
2. live `origin/develop`, merged PRs, code, and tests for implementation truth;
3. `docs/evidence/sprint-1/MF02_Sprint1_Evidence.md` for accumulated engineering evidence;
4. this handoff for current context and safe next steps.

At the start of the next session:

1. fetch `origin` and verify `origin/develop`;
2. inspect any advancement after the latest documentation integration checkpoint built from implementation merge `c54f59011543cda49a234710a857cdfe48d4b459`;
3. confirm Sprint 1 and US-13 remain in progress;
4. reuse the completed domain, validator, recurrence-generation, and service foundations;
5. perform controlled architecture/Definition-of-Ready analysis for `VaccinationSession one-occurrence and series-change architecture gate`;
6. keep reverse configuration-impact workflows, lifecycle transitions beyond DRAFT, resources/capacity, migrations, and API work outside that slice.

The recurrence foundation and occurrence-generation service supply materialized occurrences with full provenance, but individual occurrence editing and series-wide change orchestration must be planned through the architecture gate before coding.

## 3. Repository and Integration History

| Checkpoint | Integrated scope |
| --- | --- |
| PR #12 `eb4a4d9` | Campaign, dose-programme, targeting, and questionnaire-assignment foundation |
| PR #16 `201768e` | Vaccination-center foundation and initial hardening |
| PR #17 `bd22cb9` | Canonical R8 DOCX synchronization |
| PR #18 `c120ee8` | Sprint evidence checkpoint |
| PR #19 `8cdb112` | Sprint evidence checkpoint |
| PR #20 `1cad899` | OperatingHours backend foundation |
| PR #21 `433dbe1` | OperatingHours evidence checkpoint |
| PR #22 `fe101bb` | Evidence metadata stability checkpoint |
| PR #23 `5a5b3e7` | Pre-CenterClosure backend hardening |
| PR #24 `e70940b` | CenterClosure backend foundation |
| PR #25 `7b9398b` | Evidence/handoff synchronization after CenterClosure |
| PR #26 `f9c2bba` | CampaignCenterAssignment backend foundation |
| PR #27 `04b53da` | Evidence/handoff synchronization after CampaignCenterAssignment |
| PR #28 `0dd05a5` | SessionRecurrenceRule backend foundation |
| PR #29 `7b7896b` | Evidence/handoff synchronization after SessionRecurrenceRule; metadata checkpoint only |
| PR #30 `bab77b7` | VaccinationCenter scheduling-timezone and mutation-locking prerequisite |
| PR #31 `b5b674f` | Evidence/handoff synchronization after center scheduling timezone; metadata checkpoint only |
| PR #32 `b175e8e` | VaccinationSession domain and persistence foundation |
| PR #33 `f7360fb` | Evidence/handoff synchronization after VaccinationSession domain foundation; metadata checkpoint only |
| PR #34 `f0a8880` | VaccinationSession authoritative write-time validation and atomic one-off DRAFT creation |
| PR #35 `f0dcff3` | Evidence/handoff synchronization after validated one-off session; metadata checkpoint only |
| PR #36 `c54f590` | SessionRecurrenceRule occurrence generation and idempotent VaccinationSession materialization |

The current overall checkpoint is:

```text
latest MF-02 implementation integration checkpoint = c54f59011543cda49a234710a857cdfe48d4b459
certified PR #36 feature source = 96450eb715e3dbc48547fe1235faa74598b6d009
actual PR #36 base = f0dcff3c6109b55ae1777937988d63389532e5e8
```

Older SHAs and test counts in the evidence chronology are historical milestones, not the current repository state.

## 4. Architecture and Ownership Boundaries

The backend uses Spring Boot, Spring Data JPA, field-access entities, Maven, MySQL as the production target, and H2 in MySQL compatibility mode for current integration tests.

Important ownership distinctions:

* `CampaignLocationTarget` is administrative/service-area targeting.
* `CampaignCenterAssignment` is operational authorization for a center to serve a campaign.
* `OperatingHours` defines normal recurring weekly center availability.
* `CenterClosure` records temporary exception intervals without rewriting OperatingHours.
* `CenterStatus` is long-term facility lifecycle state, not temporary unavailability.
* `SessionRecurrenceRule` describes bounded weekly recurrence definitions; it is not OperatingHours or a concrete VaccinationSession.
* `VaccinationCenter.timezone` is the approved implementation-level authority for one-off local scheduling.
* `VaccinationSession` is a concrete operational appointment session interval hosted at a center for a campaign.
* `SessionBookingStatus` is a derived/read-model projection concept, not an independently mutable state.
* MF-01 owns questionnaire version publication; MF-02 owns campaign linkage and still requires the final adapter/FK integration.

Do not conflate administrative targeting, operational authorization, normal hours, closures, recurrence, or actual session validity.

## 5. Shared Temporal and Locking Conventions

Completed effective-history domains generally retain history and avoid normal hard-delete workflows.

Temporal conventions must remain explicit:

* CampaignCenterAssignment uses half-open calendar periods `[activeFrom, activeTo)`.
* CenterClosure uses half-open absolute intervals `[startAt, endAt)`.
* OperatingHours retains its implemented inclusive effective calendar-date convention.
* VaccinationSession uses half-open absolute UTC intervals `[startAt, endAt)` with `bookingCloseAt <= startAt`.
* Campaign service dates and assignment dates are independent configuration boundaries at foundation level; session validation combines them.

Locking conventions:

* campaign-owned cross-row configuration decisions serialize on the campaign row;
* center-owned cross-row scheduling decisions serialize on the center row;
* scalar parent-ID lookup may route an existing-child mutation before the parent lock;
* authoritative child state and business checks must be obtained only after the appropriate parent lock when stale first-level-cache state is possible;
* atomic session creation must use deterministic parent lock ordering (`campaign → center`) to prevent deadlocks between competing concurrent writes;
* recurrence-rule sibling creation is intentionally unconstrained; existing-rule mutation locks and refreshes that rule row directly.

### 5.1 VaccinationCenter scheduling-timezone and mutation prerequisite

PR #30 added the approved implementation architecture needed before concrete sessions:

* every center requires an explicitly supplied scheduling timezone;
* the timezone is persisted as `ZoneId.of(trimmedInput).getId()` in a non-null String column;
* no JVM, operating-system, database, browser, user-profile, country, or hard-coded regional default is consulted;
* general center detail updates cannot change the timezone, and there is no public generic setter or timezone-change service operation;
* one-off sessions snapshot the center timezone into `VaccinationSession.scheduleTimezone`, while recurrence-generated sessions inherit the rule's stored timezone.

Integration facts:

* PR #30 — `feat(mf02): add center scheduling timezone`;
* reviewed feature commit: `d70c67b2ac48fe01f8d5f58491995bd19aa4b833`;
* merge commit: `bab77b7a6dc421d622153d422a7d589d3057ae7a`.

## 6. OperatingHours — Completed Contract

OperatingHours was merged through PR #20 at `1cad899971c8b18804af31bfb079a24285f7d16d`.

The completed foundation includes:

* recurring weekday opening/closing shifts;
* inclusive effective calendar-date boundaries;
* multi-shift support with overlap prevention;
* candidate pre-validation and exception-safe mutation;
* future/current/historical configuration behavior;
* shortening-only deactivation/end workflow;
* center-row serialization for conflicting writes.

Historical validation at that checkpoint was 55 OperatingHours-specific tests and 152 total backend tests.

## 7. CenterClosure — Completed Contract

CenterClosure was merged through PR #24 at `e70940b7021778ae35dd2976ac5df6b649a28584`.

Completed behavior:

* mandatory VaccinationCenter ownership;
* UTC-safe `Instant startAt` / `endAt` with half-open `[startAt, endAt)` semantics;
* partial-day, overnight, multi-day, ongoing, historical, and future intervals;
* status exactly `ACTIVE` / `CANCELLED`;
* same-center ACTIVE overlap prevention with adjacency allowed;
* future-only normal update/cancellation, terminal cancellation, and retained history;
* independence from CenterStatus and OperatingHours configuration;
* center-row pessimistic serialization;
* read-side overlap helper for atomic VaccinationSession integration.

Historical validation at that checkpoint was 45 CenterClosure-specific tests and 194 total backend tests.

## 8. CampaignCenterAssignment — Completed Contract

### 8.1 Purpose and model

CampaignCenterAssignment records operational authorization for one center to serve one campaign during an effective calendar period. It is distinct from CampaignLocationTarget.

Implemented types and fields:

* `CampaignCenterAssignment`;
* `CampaignCenterAssignmentStatus` with exactly `ACTIVE` and `CANCELLED`;
* mandatory lazy campaign relationship;
* mandatory lazy center relationship;
* `LocalDate activeFrom`;
* nullable exclusive `LocalDate activeTo`;
* `Instant createdAt` and `Instant updatedAt`;
* immutable ownership and retained history.

### 8.2 Effective periods and lifecycle

The effective period is half-open `[activeFrom, activeTo)`:

* start is inclusive;
* end is exclusive;
* null end is open-ended;
* exact adjacency is allowed;
* zero-length and reversed finite periods are rejected;
* same campaign-center ACTIVE periods cannot overlap;
* CANCELLED rows are neither effective nor overlap blockers.

### 8.3 Persistence, locking, and authoritative reload

The campaign row is the write-serialization guard. Existing-child mutation ordering uses scalar campaign ID lookup, pessimistic campaign lock, authoritative assignment refresh via `EntityManager.refresh()`, lifecycle/period checks, and mutation.

Integration facts:

* PR #26 — `feat(mf02): add campaign center assignment backend foundation`;
* merge commit: `f9c2bbaa8f9f0c13a338ea910961820b34e88d9e`;
* 48 component tests; 242 total backend tests at that historical checkpoint.

## 9. SessionRecurrenceRule — Completed Contract

### 9.1 Purpose and model

SessionRecurrenceRule is a bounded weekly definition from which individually stored VaccinationSession occurrences may later be generated.

Implemented types and state:

* `SessionRecurrenceRule`;
* `SessionRecurrenceRuleStatus` with exactly `ACTIVE` and `CANCELLED`;
* mandatory immutable lazy campaign and center relationships;
* mutation-safe `Set<DayOfWeek>` persisted through an EAGER seven-value `@ElementCollection`;
* mandatory inclusive `LocalDate startDate` / `endDate`;
* same-local-day half-open `LocalTime` interval `[startTime, endTime)`;
* normalized Java `ZoneId` string;
* immutable backend-generated UUID `generationKey`;
* `Instant createdAt` and `updatedAt` audit fields.

### 9.2 Lifecycle and authoritative mutation

New rules are ACTIVE. Full definition update is permitted only before the rule starts; prospective end-date shortening and terminal cancellation are supported. Existing-rule writes lock and refresh the rule row directly.

Integration facts:

* PR #28 — `feat(mf02): add session recurrence rule backend foundation`;
* merge commit: `0dd05a5dce856bbaff6924c54fcf3c395920b076`;
* 30 component tests; 272 total backend tests at that historical checkpoint.

## 10. VaccinationSession — Completed Domain, Validation & One-Off Creation

### 10.1 Purpose and model

VaccinationSession represents a concrete scheduled operational interval during which vaccination appointments may be conducted at a hosting center for an authorized campaign.

Implemented types and state (PR #32 & PR #34):

* `VaccinationSession` JPA entity;
* `SessionLifecycleStatus` enum: `DRAFT`, `SCHEDULED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`;
* `SessionBookingStatus` enum: `NOT_OPEN`, `OPEN`, `FULL`, `CLOSED`, `SUSPENDED` (reserved for upcoming read-model projection; not an independently mutable persisted state);
* generated `Long id` identity;
* mandatory immutable lazy `VaccinationCampaign` ownership (`updatable = false`);
* mandatory lazy `VaccinationCenter` hosting relationship;
* optional immutable lazy `SessionRecurrenceRule` provenance (`updatable = false`);
* nullable immutable `String occurrenceKey` (length 64) with database unique constraint `uk_session_occurrence_key`;
* nullable immutable `LocalDate originOccurrenceDate`;
* mandatory normalized `ZoneId` `scheduleTimezone` snapshot (e.g. `ZoneId.of(trimmed).getId()`);
* mandatory absolute schedule `Instant startAt` and `endAt` with half-open `[startAt, endAt)` semantics (`startAt < endAt`);
* mandatory `Instant bookingCloseAt` with `bookingCloseAt <= startAt`;
* initial lifecycle status strictly initialized to `SessionLifecycleStatus.DRAFT`;
* audit fields `Instant createdAt` and `updatedAt` managed via standard `@PrePersist` and `@PreUpdate`;
* zero broad public setter surface;
* no unsafe remove cascade or orphan removal.

### 10.2 Provenance invariant

The entity strictly enforces all-or-none recurrence provenance:

* **One-off session:** `recurrenceRule == null && occurrenceKey == null && originOccurrenceDate == null` (`isGenerated() == false`); multiple one-off sessions with null occurrence keys are persistence-valid.
* **Generated occurrence:** `recurrenceRule != null && occurrenceKey != null && originOccurrenceDate != null` (`isGenerated() == true`); occurrence key is trimmed and validated (non-blank, length <= 64), and uniqueness is enforced at database level.
* Mixed partial provenance combinations are rejected with `IllegalArgumentException`.

### 10.3 Persistence & repository

`VaccinationSessionRepository` extends `JpaRepository<VaccinationSession, Long>` and provides:
* `Optional<VaccinationSession> findByOccurrenceKey(String occurrenceKey)`;
* `List<VaccinationSession> findByCampaignId(Long campaignId)`;
* `List<VaccinationSession> findByCenterId(Long centerId)`;
* `List<VaccinationSession> findByRecurrenceRuleIdOrderByOriginOccurrenceDateAsc(Long recurrenceRuleId)`;
* `boolean existsByRecurrenceRuleId(Long recurrenceRuleId)`;
* `boolean existsByRecurrenceRuleIdAndOriginOccurrenceDateAfter(Long recurrenceRuleId, LocalDate date)`.

Table `vaccination_sessions` defines composite indexes for campaign schedule, center schedule, and recurrence origin, along with the unique occurrence key constraint.

### 10.4 VaccinationSessionValidator contract

The shared `VaccinationSessionValidator` performs authoritative concrete-session schedule validation against already locked and refreshed parent entities:

* **Candidate Invariants:** Rejects null timestamps, `startAt >= endAt`, and `bookingCloseAt > startAt`.
* **Center State:** Requires center `status == CenterStatus.ACTIVE`.
* **Scheduling Timezone Projection:** Resolves the center's `ZoneId` to project `startAt` and `endAt` into local calendar dates (`firstTouchedDate` at `startAt`, `lastTouchedDate` at `endAt.minusNanos(1)`).
* **Campaign Containment:** Ensures all touched calendar dates fall within `[campaign.startDate, campaign.endDate]`.
* **CampaignCenterAssignment Coverage:** Requires an active assignment covering the full span `[firstTouchedDate, lastTouchedDate + 1)`.
* **OperatingHours Contiguous Coverage:** For each touched date, validates that the session interval is fully covered by contiguous active OperatingHours shifts on that day of week, active for that date. OperatingHours entities are explicitly refreshed prior to evaluation to guard against stale first-level cache state.
* **CenterClosure Overlap Exclusion:** Ensures the session interval `[startAt, endAt)` does not overlap any active `CenterClosure` on `[closure.startAt, closure.endAt)`. Exact boundary adjacencies (`closure.endAt <= session.startAt` or `session.endAt <= closure.startAt`) are permitted.
* **No Generic Same-Center Overlap Prohibition:** Multiple concurrent or overlapping sessions may exist at the same center (AC-9, no generic same-center overlap restriction).
* **Boundary:** The validator is a pure validation component: it does not acquire locks, does not persist entities, does not generate occurrences, and does not perform capacity or staffing checks.

### 10.5 VaccinationSessionService and One-Off DRAFT Creation

`VaccinationSessionService.createOneOffDraftSession(...)` implements atomic one-off session creation:

1. **Deterministic Locking:** Acquires pessimistic write locks in strict order: `campaignRepository.findByIdForUpdate(campaignId)` followed by `centerRepository.findByIdForUpdate(centerId)`.
2. **Authoritative Reload:** Refreshes both campaign and center via `EntityManager.refresh()` to ensure authoritative state.
3. **Write-Time Validation:** Invokes `VaccinationSessionValidator.validateSessionSchedule(...)`, which explicitly refreshes effective `OperatingHours` instances.
4. **Draft Construction & Persistence:** Constructs a one-off `VaccinationSession` in `DRAFT` status with `scheduleTimezone = center.getTimezone()`, null recurrence provenance (`recurrenceRule = null`, `occurrenceKey = null`, `originOccurrenceDate = null`), and saves it via `VaccinationSessionRepository`.

### 10.6 Write-Time Validity vs. Deferred Reverse-Impact Boundary

The system guarantees that a session cannot be created or materialized when the authoritative serialized configuration state at write time violates session rules (**authoritative write-time validation**).

This milestone does **NOT** implement reverse-impact workflows. The following reverse configuration-impact behaviors remain explicitly deferred:
* Campaign date modifications affecting existing sessions;
* Center deactivation affecting existing sessions;
* Assignment shortening or cancellation affecting existing sessions;
* OperatingHours modifications affecting existing sessions;
* CenterClosure creation or updates affecting existing sessions.

### 10.7 One-Off Concurrency and Integration Evidence

Three separate-transaction concurrency integration tests verify the runtime locking and refresh contracts:
* `creationRejectsWhenCenterConcurrentlyDeactivated`: Creation attempt serialized after center deactivation observes `INACTIVE` state and is rejected with `IllegalArgumentException`.
* `concurrentSessionCreationsForSameCenterSucceedWithoutDeadlock`: Concurrent creations for the same center succeed without deadlocks and both sessions are committed.
* `creationRacingWithOverlappingClosureRejectsWhenClosureCommitsFirst`: Session creation serialized after an overlapping closure commits is rejected. (This verifies serialized write-time validation; it does not prove closures created later automatically cancel existing sessions).

Integration facts:

* PR #34 — `feat(mf02): add validated one-off session creation`;
* reviewed feature commit: `59da64445ba98482237dba01d8e2436de12678b6`;
* merge commit: `f0a8880831d27387b16f548a86e849ee2892b599`;
* merge parents: `f7360fb8d195c192a0396ca75e86f9825f9f4986` and `59da64445ba98482237dba01d8e2436de12678b6`;
* 35 new integration tests; full backend total was 329 tests at that historical checkpoint.

### 10.8 SessionRecurrenceRule Occurrence Generation and Idempotent Materialization

`SessionOccurrenceGenerationService` implements controlled transactional materialization of concrete DRAFT `VaccinationSession` occurrences from an ACTIVE `SessionRecurrenceRule` (PR #36):

1. **Routing Discovery & Pessimistic Locking:** Discovers parent IDs via a non-locking pre-read of immutable `@JoinColumn(updatable = false)` IDs, then acquires pessimistic write locks in strict order: `campaign → center → rule`, followed by post-lock `EntityManager.refresh` on all three entities. An audit of all repository locking confirmed zero cross-workflow lock inversion.
2. **Rule Eligibility & Timezone Validation:** Requires `rule.getStatus() == SessionRecurrenceRuleStatus.ACTIVE`. Enforces strict normalized rule/center timezone equality (`rule.getTimezone().equals(center.getTimezone())`); mismatches reject immediately with `IllegalArgumentException`.
3. **Effective Range & Date Enumeration:** Computes the effective generation window intersection (`[effectiveStart, effectiveEnd]` between rule bounds and requested range). If disjoint, returns an empty result `RecurrenceGenerationResult(0, 0, List.of())`. Iterates ascending `LocalDate` values matching `rule.getWeekdays()`, producing exactly one candidate per matching date.
4. **DST-Safe Local Time Conversion:** Converts configured local `[startTime, endTime)` to UTC `Instant` using `ZoneRules.getValidOffsets(LocalDateTime)`:
   * 0 valid offsets (gap): rejects candidate and transaction with `IllegalArgumentException` (no silent forward shifting);
   * 1 valid offset: used directly;
   * 2 valid offsets (overlap): deterministically selects `validOffsets.get(0)` (earlier offset).
   * Asserts absolute `startAt.isBefore(endAt)`.
5. **Controller Booking-Close Decision:** For every generated occurrence, `bookingCloseAt = startAt` exactly. (This is a controller implementation decision filling an unspecified generation detail; it is not derived from `VaccinationCampaign.bookingCloseAt`, not capped, no lead-time offset is applied, and no recurrence rule schema field is added).
6. **Deterministic Occurrence Identity:** `occurrenceKey = <rule.generationKey>:<originOccurrenceDate>` (e.g. 47 characters for UUID + ISO date, well within the 64-character column limit). Independent of mutable start/end timestamps or center names.
7. **Validator Reuse:** Directly invokes `VaccinationSessionValidator.validateSessionSchedule(campaign, center, startAt, endAt, bookingCloseAt)` for every missing candidate occurrence.
8. **Idempotency & Atomic Batch Persistence:** Pre-existing occurrences for the rule are detected via `findByRecurrenceRuleIdOrderByOriginOccurrenceDateAsc` and retained untouched with historical provenance verified. All missing candidates are validated in memory before `sessionRepository.saveAll()` and `flush()`. Any validation failure throws before persistence, rolling back all newly generated occurrences in that invocation while preserving pre-existing sessions.
9. **Same-Rule Concurrency:** Serialized on the recurrence rule row write lock backed by the database `uk_session_occurrence_key` constraint.
10. **Interim Rule Mutation Protection:**
    * `SessionRecurrenceRuleService.updateFutureRule`: rejects definition updates with `IllegalArgumentException` if any generated session exists for the rule (`existsByRecurrenceRuleId(ruleId)`).
    * `SessionRecurrenceRuleService.shortenRuleEnd`: rejects shortening if any generated occurrence exists after the new end date (`existsByRecurrenceRuleIdAndOriginOccurrenceDateAfter(ruleId, newEndDate)`). An occurrence ON the new inclusive end date remains valid.
    * `SessionRecurrenceRuleService.cancelRule`: remains permitted with generated occurrences, marking the rule `CANCELLED`, blocking future generation, and leaving existing generated sessions untouched.

Integration facts:

* PR #36 — `feat(mf02): add recurrence occurrence generation`;
* certified feature source: `96450eb715e3dbc48547fe1235faa74598b6d009`;
* merge commit: `c54f59011543cda49a234710a857cdfe48d4b459`;
* merge parents: `f0dcff3c6109b55ae1777937988d63389532e5e8` and `96450eb715e3dbc48547fe1235faa74598b6d009`;
* 25 net-new integration tests (19 generation integration, 2 concurrency, 4 recurrence mutation protection); full backend total is **354 tests**, zero failures, zero errors, zero skipped;
* Maven `test` and `verify` succeeded and executable JAR packaged;
* Backend CI and Frontend CI succeeded before and after merge.

## 11. Story and Sprint Status

Sprint 1 remains **IN PROGRESS**.

| Story | Current status |
| --- | --- |
| US-09 | In progress at full-story scope: backend foundation implemented; publication transition/API/UI/DoD work remains |
| US-10 | In progress: MF-02 foundation hardened; final MF-01 QuestionnaireVersion adapter/FK remains |
| US-11 | Backend foundation complete; later API/UI/integration layers remain |
| US-12 | Backend foundation implemented, reviewed, tested, CI-verified, and merged; Flyway, API/security/UI, VaccinationSession integration, and final DoD evidence remain |
| US-13 | **IN PROGRESS**: VaccinationCenter, OperatingHours, CenterClosure, SessionRecurrenceRule, VaccinationSession domain/persistence foundations, authoritative write-time validation / atomic one-off DRAFT creation, and recurrence occurrence generation complete; one-occurrence edits, series-wide change orchestration, SessionChangeEvent, lifecycle transition services beyond DRAFT, reverse configuration impact, API/UI/migration, and final DoD work remain |
| US-27 | Backend foundation implemented; later API/UI integration remains |

Do not mark Sprint 1, US-12, or US-13 fully Definition-of-Done complete solely from these backend foundations.

## 12. SessionRecurrenceRule Boundary

`SessionRecurrenceRule` is complete at its current backend-foundation scope, and occurrence generation is now operational through `SessionOccurrenceGenerationService`.

Preserved boundary:

* it belongs to US-13;
* it describes recurrence of proposed sessions rather than normal center availability;
* it must not duplicate OperatingHours or CenterClosure;
* it exposes stable definition and generation-identity data for VaccinationSession generation;
* plain definition updates on rules with materialized sessions are guarded; series-wide future-session editing and one-occurrence isolation remain to be designed in the next architecture gate.

## 13. VaccinationSession Operational Boundary & Remaining Work

The domain foundation, shared validator, one-off creation service, and recurrence occurrence generation are complete, but later operational session workflows remain **NOT IMPLEMENTED**:

* `VaccinationSession` one-occurrence rescheduling / cancellation workflows;
* series-wide schedule change orchestration;
* `SessionChangeEvent` (impact preview, change reason, audit logging);
* session lifecycle transition services beyond initial `DRAFT` (`SCHEDULED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`);
* reverse configuration-impact handling (campaign/center/assignment/hours/closure edits affecting existing sessions);
* TimeSlot, staffing/resources, capacity;
* Flyway business migrations;
* REST/API/DTO/mapper, security/RBAC, OpenAPI, frontend;
* final MF-01 QuestionnaireVersion adapter/FK;
* production MySQL concurrency assurance;
* final Sprint integration and Definition-of-Done evidence.

## 14. Anti-Regression Guardrails

Do not:

* rebuild or materially redesign completed OperatingHours, CenterClosure, CampaignCenterAssignment, SessionRecurrenceRule, or VaccinationSession domain foundations without a new scoped review;
* remove CampaignCenterAssignment, VaccinationSession, or SessionOccurrenceGeneration post-lock authoritative refresh;
* conflate CampaignLocationTarget with CampaignCenterAssignment;
* treat ACTIVE status alone as effectiveness outside the applicable period;
* change half-open assignment, closure, or session schedule semantics;
* make assignment configuration depend on campaign status, center status, or campaign service-date containment;
* represent H2 concurrency tests as proof of production MySQL behavior;
* claim hosted approval evidence that GitHub does not contain;
* rebuild or broaden the completed SessionRecurrenceRule foundation without a new scoped review;
* bypass deterministic parent locking (`campaign → center → rule`) when writing or generating sessions;
* introduce a generic same-center session overlap prohibition;
* claim reverse configuration-impact protections are implemented;
* mark Sprint 1 complete prematurely.

## 15. Final Handoff State

```text
Canonical: 3.3-AI / 2026-08-08-R8
Sprint: Sprint 1 — IN PROGRESS
Latest MF-02 implementation merge: PR #36 / c54f59011543cda49a234710a857cdfe48d4b459
PR #36 certified source: 96450eb715e3dbc48547fe1235faa74598b6d009
PR #36 actual base: f0dcff3c6109b55ae1777937988d63389532e5e8
CenterClosure: IMPLEMENTED, REVIEWED, TESTED, CI-VERIFIED, AND MERGED
CampaignCenterAssignment: IMPLEMENTED, REVIEWED, TESTED, CI-VERIFIED, AND MERGED
SessionRecurrenceRule: IMPLEMENTED, REVIEWED, TESTED, CI-VERIFIED, AND MERGED
Center scheduling-timezone prerequisite: IMPLEMENTED, INDEPENDENTLY REVIEWED, TESTED, CI-VERIFIED, AND MERGED
VaccinationSession domain & persistence foundation: IMPLEMENTED, INDEPENDENTLY REVIEWED, TESTED, CI-VERIFIED, AND MERGED
VaccinationSession write-time validation & one-off creation: IMPLEMENTED, INDEPENDENTLY REVIEWED, TESTED, CI-VERIFIED, AND MERGED
VaccinationSession recurrence occurrence generation: IMPLEMENTED, INDEPENDENTLY REVIEWED, TESTED, CI-VERIFIED, AND MERGED
Package structure verdict: KEEP CURRENT STRUCTURE
Backend baseline: 354 tests; Maven test/verify SUCCESS; executable JAR packaged
Supporting backend foundations: RECURRENCE OCCURRENCE GENERATION & IDEMPOTENT SESSION MATERIALIZATION INTEGRATED
Next phase: VaccinationSession one-occurrence and series-change architecture gate
```

The next AI should refresh repository state, reuse the completed domain, validator, recurrence-generation, and service foundations, and perform the controlled architecture/Definition-of-Ready gate for `VaccinationSession one-occurrence and series-change architecture gate`. Lifecycle transition services, reverse configuration-impact workflows, migrations, API/security/UI, TimeSlot, resources, capacity, and SessionChangeEvent remain separate later work.

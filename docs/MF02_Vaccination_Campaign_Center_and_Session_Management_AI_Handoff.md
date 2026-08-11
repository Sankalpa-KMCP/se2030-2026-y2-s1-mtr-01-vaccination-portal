# MF-02 — Vaccination Campaign, Center and Session Management

## AI Engineering Handoff

**Project:** Web-Based Vaccination Portal (`WBVP-SE2030`)

**Function:** MF-02 — Vaccination Campaign, Center and Session Management

**Canonical authority:** `3.3-AI / 2026-08-08-R8`

**Sprint:** Sprint 1 — **IN PROGRESS**

**Current implementation checkpoint:** `b175e8e1dc07cb73e7c32479defa89f5c112402d` (PR #32 merge)

**Latest completed backend foundation:** `VaccinationSession domain and repository foundation`

**Next controlled implementation phase:** `VaccinationSession cross-component validation and atomic one-off DRAFT creation` — **READY TO START**

> This handoff is optimized for the next AI session. The canonical Markdown is the semantic requirements source of truth; live Git history and tests are the implementation source of truth. Do not rebuild completed foundations or treat this handoff as authority to change canonical rules.
>
> The latest MF-02 implementation integration checkpoint is PR #32 merge `b175e8e1dc07cb73e7c32479defa89f5c112402d`, with reviewed feature source `e906ac8e800597b61a9fcea3eb0f3c05796b8d30`. Any later documentation PR merge containing this handoff is an overall repository metadata checkpoint; it does not replace or rename the PR #32 implementation checkpoint.

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
* VaccinationSession domain and repository foundation.

`CenterClosure` remains **IMPLEMENTED, REVIEWED, TESTED, CI-VERIFIED, AND MERGED**.

`CampaignCenterAssignment` remains **IMPLEMENTED, REVIEWED, TESTED, CI-VERIFIED, AND MERGED** at backend-foundation scope through PR #26.

`SessionRecurrenceRule` is **IMPLEMENTED, REVIEWED, TESTED, CI-VERIFIED, AND MERGED** at backend-foundation scope through PR #28.

The `VaccinationCenter` scheduling-timezone and mutation-locking prerequisite is **IMPLEMENTED, INDEPENDENTLY REVIEWED, TESTED, CI-VERIFIED, AND MERGED** through PR #30.

The `VaccinationSession` domain and persistence foundation is **IMPLEMENTED, INDEPENDENTLY REVIEWED, TESTED, CI-VERIFIED, AND MERGED** through PR #32. It establishes the concrete stored-session entity, canonical lifecycle and booking projection enums, all-or-none recurrence provenance, occurrence-key uniqueness, normalized timezone snapshot, half-open interval validation, and repository lookup. It does not implement atomic session creation, cross-component validation, or recurrence occurrence generation.

Current backend validation is 290 tests with zero failures, errors, or skips. Maven `test` and `verify` succeeded, the executable JAR packaged successfully, and Backend CI and Frontend CI succeeded before and after the PR #32 merge.

**MF-02 VACCINATIONSESSION DOMAIN/PERSISTENCE FOUNDATION INTEGRATED.** This milestone covers the current backend-foundation scope for Campaign, DoseProgramme, LocationTarget, QuestionnaireAssignment, VaccinationCenter, OperatingHours, CenterClosure, CampaignCenterAssignment, SessionRecurrenceRule, and VaccinationSession domain/persistence. It does not mean MF-02, US-13, or Sprint 1 is complete.

Sprint 1 remains in progress. The VaccinationSession domain/repository foundation is in place, but atomic cross-component session validation, atomic one-off DRAFT creation, recurrence occurrence generation, migrations, API/security/UI layers, publication integration, and final Definition-of-Done evidence remain pending.

## 2. Authority and Startup Procedure

Use this source order:

1. `docs/Web_Based_Vaccination_Portal_AI_Canonical_Context_v3.3.md` for semantic requirements;
2. live `origin/develop`, merged PRs, code, and tests for implementation truth;
3. `docs/evidence/sprint-1/MF02_Sprint1_Evidence.md` for accumulated engineering evidence;
4. this handoff for current context and safe next steps.

At the start of the next session:

1. fetch `origin` and verify `origin/develop`;
2. inspect any advancement after the latest documentation integration checkpoint built from implementation merge `b175e8e1dc07cb73e7c32479defa89f5c112402d`;
3. confirm Sprint 1 and US-13 remain in progress;
4. reuse the completed VaccinationSession domain and repository foundation;
5. begin the controlled `VaccinationSession cross-component validation and atomic one-off DRAFT creation` slice;
6. keep recurrence occurrence generation/idempotency, lifecycle transition services, resources/capacity, migrations, and API work outside that slice.

The recurrence foundation supplies stable definition data, and the session entity supports recurrence provenance, but concrete occurrence generation remains a separate later slice.

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

The current overall checkpoint is:

```text
latest MF-02 implementation integration checkpoint = b175e8e1dc07cb73e7c32479defa89f5c112402d
reviewed PR #32 feature source = e906ac8e800597b61a9fcea3eb0f3c05796b8d30
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
* atomic session creation must use deterministic parent lock ordering (campaign → center) to prevent deadlocks between competing concurrent writes;
* recurrence-rule sibling creation is intentionally unconstrained; existing-rule mutation locks and refreshes that rule row directly.

### 5.1 VaccinationCenter scheduling-timezone and mutation prerequisite

PR #30 added the approved implementation architecture needed before concrete sessions:

* every center requires an explicitly supplied scheduling timezone;
* the timezone is persisted as `ZoneId.of(trimmedInput).getId()` in a non-null String column;
* no JVM, operating-system, database, browser, user-profile, country, or hard-coded regional default is consulted;
* general center detail updates cannot change the timezone, and there is no public generic setter or timezone-change service operation;
* future one-off sessions snapshot the center timezone into `VaccinationSession.scheduleTimezone`, while recurrence-generated sessions inherit the rule's stored timezone.

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
* read-side overlap helper for later atomic VaccinationSession integration.

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

## 10. VaccinationSession — Completed Domain & Repository Foundation

### 10.1 Purpose and model

VaccinationSession represents a concrete scheduled operational interval during which vaccination appointments may be conducted at a hosting center for an authorized campaign.

Implemented types and state (PR #32):

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

`VaccinationSessionRepository` extends `JpaRepository<VaccinationSession, Long>` and provides `Optional<VaccinationSession> findByOccurrenceKey(String occurrenceKey)`. Table `vaccination_sessions` defines composite indexes for campaign schedule, center schedule, and recurrence origin, along with the unique occurrence key constraint.

Integration facts:

* PR #32 — `feat(mf02): add vaccination session domain foundation`;
* reviewed feature commit: `e906ac8e800597b61a9fcea3eb0f3c05796b8d30`;
* merge commit: `b175e8e1dc07cb73e7c32479defa89f5c112402d`;
* merge parents: `b5b674fec2136a6633fff54b4784cd41f721f929` and `e906ac8e800597b61a9fcea3eb0f3c05796b8d30`;
* 14 focused tests (8 model, 6 persistence integration); full backend total is **290 tests**, zero failures, zero errors, zero skipped;
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
| US-13 | **IN PROGRESS**: VaccinationCenter, OperatingHours, CenterClosure, SessionRecurrenceRule, and VaccinationSession domain/persistence foundations complete; cross-component validation, atomic one-off DRAFT creation, recurrence occurrence generation, API/UI/migration, and final DoD work remain |
| US-27 | Backend foundation implemented; later API/UI integration remains |

Do not mark Sprint 1, US-12, or US-13 fully Definition-of-Done complete solely from these backend foundations.

## 12. SessionRecurrenceRule Boundary

`SessionRecurrenceRule` is complete at its current backend-foundation scope. Do not rebuild it or mistake it for generated occurrences.

Preserved boundary:

* it belongs to US-13;
* it describes recurrence of proposed sessions rather than normal center availability;
* it must not duplicate OperatingHours or CenterClosure;
* it exposes stable definition and generation-identity data for later VaccinationSession work;
* occurrence expansion, occurrence keys, one-occurrence editing, and future-series impact orchestration remain unimplemented.

Do not add session generation directly to the recurrence entity/service. The completed VaccinationSession architecture keeps occurrence generation in a later controlled slice.

## 13. VaccinationSession Operational Boundary & Remaining Work

The domain and persistence foundation is complete, but concrete operational session workflows remain **NOT IMPLEMENTED**:

* `VaccinationSessionService`;
* shared authoritative cross-component session validator (`VaccinationSessionValidator`);
* atomic one-off DRAFT session creation;
* MF02-BR-05 assignment/hours enforcement;
* MF02-BR-06 closure enforcement;
* campaign service-date containment enforcement;
* CampaignCenterAssignment active period coverage enforcement;
* center ACTIVE status enforcement;
* recurrence occurrence generation and deterministic occurrence-key derivation;
* generation idempotency and retry orchestration;
* rule/center timezone compatibility enforcement;
* deterministic parent locking (campaign → center);
* session lifecycle transition services;
* one-occurrence edits and series-wide change orchestration;
* `SessionChangeEvent`;
* TimeSlot, staffing/resources, capacity;
* Flyway business migrations;
* REST/API/DTO/mapper, security/RBAC, OpenAPI, frontend;
* final MF-01 QuestionnaireVersion adapter/FK;
* production MySQL concurrency assurance;
* final Sprint integration and Definition-of-Done evidence.

## 14. Anti-Regression Guardrails

Do not:

* rebuild or materially redesign completed OperatingHours, CenterClosure, CampaignCenterAssignment, SessionRecurrenceRule, or VaccinationSession domain foundations without a new scoped review;
* remove CampaignCenterAssignment's post-lock authoritative refresh;
* conflate CampaignLocationTarget with CampaignCenterAssignment;
* treat ACTIVE status alone as effectiveness outside the applicable period;
* change half-open assignment, closure, or session schedule semantics;
* make assignment configuration depend on campaign status, center status, or campaign service-date containment;
* represent H2 concurrency tests as proof of production MySQL behavior;
* claim hosted approval evidence that GitHub does not contain;
* rebuild or broaden the completed SessionRecurrenceRule foundation without a new scoped review;
* claim that SessionRecurrenceRule creates operationally valid sessions by itself;
* bypass deterministic parent locking (campaign → center) when writing sessions;
* expand the immediate next one-off session slice into recurrence occurrence generation;
* mark Sprint 1 complete prematurely.

## 15. Final Handoff State

```text
Canonical: 3.3-AI / 2026-08-08-R8
Sprint: Sprint 1 — IN PROGRESS
Latest MF-02 implementation merge: PR #32 / b175e8e1dc07cb73e7c32479defa89f5c112402d
PR #32 reviewed source: e906ac8e800597b61a9fcea3eb0f3c05796b8d30
CenterClosure: IMPLEMENTED, REVIEWED, TESTED, CI-VERIFIED, AND MERGED
CampaignCenterAssignment: IMPLEMENTED, REVIEWED, TESTED, CI-VERIFIED, AND MERGED
SessionRecurrenceRule: IMPLEMENTED, REVIEWED, TESTED, CI-VERIFIED, AND MERGED
Center scheduling-timezone prerequisite: IMPLEMENTED, INDEPENDENTLY REVIEWED, TESTED, CI-VERIFIED, AND MERGED
VaccinationSession domain & persistence foundation: IMPLEMENTED, INDEPENDENTLY REVIEWED, TESTED, CI-VERIFIED, AND MERGED
Package structure verdict: KEEP CURRENT STRUCTURE
VaccinationSession Definition of Ready: PASS
Backend baseline: 290 tests; Maven test/verify SUCCESS; executable JAR packaged
Supporting backend foundations: VACCINATIONSESSION DOMAIN/PERSISTENCE FOUNDATION INTEGRATED
Next phase: VaccinationSession cross-component validation and atomic one-off DRAFT creation
VaccinationSession operational validation/generation: NOT STARTED
```

The next AI should refresh repository state, reuse the completed domain foundation, and implement the controlled `VaccinationSession cross-component validation and atomic one-off DRAFT creation` slice. Recurrence occurrence generation, lifecycle transition services, migrations, API/security/UI, TimeSlot, resources, capacity, and SessionChangeEvent remain separate later work.

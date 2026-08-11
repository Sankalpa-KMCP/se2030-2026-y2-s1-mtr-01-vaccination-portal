# MF-02 — Vaccination Campaign, Center and Session Management

## AI Engineering Handoff

**Project:** Web-Based Vaccination Portal (`WBVP-SE2030`)

**Function:** MF-02 — Vaccination Campaign, Center and Session Management

**Canonical authority:** `3.3-AI / 2026-08-08-R8`

**Sprint:** Sprint 1 — **IN PROGRESS**

**Current implementation checkpoint:** `origin/develop = 0dd05a5dce856bbaff6924c54fcf3c395920b076`

**Latest completed backend foundation:** `SessionRecurrenceRule`

**Next major phase:** `VaccinationSession architecture and MF-02 cross-component integration planning` — **NOT STARTED**

> This handoff is optimized for the next AI session. The canonical Markdown is the semantic requirements source of truth; live Git history and tests are the implementation source of truth. Do not rebuild completed foundations or treat this handoff as authority to change canonical rules.
>
> The recurrence implementation checkpoint is `0dd05a5dce856bbaff6924c54fcf3c395920b076`. The documentation PR merge containing this handoff is a later metadata checkpoint visible in live Git history; it does not replace or rename the recurrence implementation checkpoint.

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
* SessionRecurrenceRule.

`CenterClosure` remains **IMPLEMENTED, REVIEWED, TESTED, CI-VERIFIED, AND MERGED**.

`CampaignCenterAssignment` remains **IMPLEMENTED, REVIEWED, TESTED, CI-VERIFIED, AND MERGED** at backend-foundation scope through PR #26.

`SessionRecurrenceRule` is **IMPLEMENTED, REVIEWED, TESTED, CI-VERIFIED, AND MERGED** at backend-foundation scope through PR #28. It must not be reconstructed or expanded into concrete-session generation without a separately reviewed integration plan.

Current backend validation is 272 tests with zero failures, errors, or skips. Maven `test` and `verify` succeeded, the JAR packaged successfully, and Backend CI and Frontend CI succeeded before and after the PR #28 merge.

**MF-02 SUPPORTING BACKEND FOUNDATIONS COMPLETE FOR THE CURRENT PRE-VACCINATIONSESSION PHASE.** This milestone covers the current backend-foundation scope for Campaign, DoseProgramme, LocationTarget, QuestionnaireAssignment, VaccinationCenter, OperatingHours, CenterClosure, CampaignCenterAssignment, and SessionRecurrenceRule. It does not mean MF-02, US-13, or Sprint 1 is complete.

Sprint 1 remains in progress. VaccinationSession architecture, recurrence occurrence generation, integrated session validation, migrations, API/security/UI layers, publication integration, and final Definition-of-Done evidence remain pending.

## 2. Authority and Startup Procedure

Use this source order:

1. `docs/Web_Based_Vaccination_Portal_AI_Canonical_Context_v3.3.md` for semantic requirements;
2. live `origin/develop`, merged PRs, code, and tests for implementation truth;
3. `docs/evidence/sprint-1/MF02_Sprint1_Evidence.md` for accumulated engineering evidence;
4. this handoff for current context and safe next steps.

At the start of the next session:

1. fetch `origin` and verify `origin/develop`;
2. inspect any advancement after the latest documentation integration checkpoint built from recurrence merge `0dd05a5dce856bbaff6924c54fcf3c395920b076`;
3. confirm Sprint 1 and US-13 remain in progress;
4. perform broad discovery for `VaccinationSession` and its cross-component dependencies;
5. produce and review a dedicated architecture and lock-ordering plan before implementation;
6. keep occurrence generation/idempotency and concrete-session validity explicit; do not silently fold later resources/capacity/API work into the first session slice.

Do not claim VaccinationSession architecture is already complete. The completed recurrence foundation supplies stable definition data but does not itself generate sessions.

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

The current overall checkpoint is:

```text
origin/develop implementation checkpoint = 0dd05a5dce856bbaff6924c54fcf3c395920b076
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
* MF-01 owns questionnaire version publication; MF-02 owns campaign linkage and still requires the final adapter/FK integration.

Do not conflate administrative targeting, operational authorization, normal hours, closures, recurrence, or actual session validity.

## 5. Shared Temporal and Locking Conventions

Completed effective-history domains generally retain history and avoid normal hard-delete workflows.

Temporal conventions must remain explicit:

* CampaignCenterAssignment uses half-open calendar periods `[activeFrom, activeTo)`.
* CenterClosure uses half-open absolute intervals `[startAt, endAt)`.
* OperatingHours retains its implemented inclusive effective calendar-date convention.
* Campaign service dates and assignment dates are independent configuration boundaries at foundation level; future session validation combines them.

Locking conventions:

* campaign-owned cross-row configuration decisions serialize on the campaign row;
* center-owned cross-row scheduling decisions serialize on the center row;
* scalar parent-ID lookup may route an existing-child mutation before the parent lock;
* authoritative child state and business checks must be obtained only after the appropriate parent lock when stale first-level-cache state is possible;
* future atomic session writes must follow compatible campaign/center lock discipline rather than relying only on read helpers.
* recurrence-rule sibling creation is intentionally unconstrained; existing-rule mutation locks and refreshes that rule row directly.

H2/MySQL-mode concurrency tests provide structural evidence. Production MySQL behavior remains a later integration assurance item.

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

Historical validation at that checkpoint was 55 OperatingHours-specific tests and 152 total backend tests. These are chronology values, not the current total.

Do not reintroduce obsolete prospective OperatingHours architecture work.

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

Validation at that checkpoint was 45 CenterClosure-specific tests: 15 model, 6 persistence, 21 service, and 3 concurrency tests; 194 total backend tests. The committed fixture-isolation correction remains part of the completed test foundation.

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

There is no normal hard-delete service.

### 8.2 Effective periods and lifecycle

The effective period is half-open `[activeFrom, activeTo)`:

* start is inclusive;
* end is exclusive;
* null end is open-ended;
* exact adjacency is allowed;
* zero-length and reversed finite periods are rejected;
* same campaign-center ACTIVE periods cannot overlap;
* CANCELLED rows are neither effective nor overlap blockers.

Implemented lifecycle behavior:

* historical, currently effective, and future creation;
* future-only full-period update;
* prospective shortening/end of a currently effective assignment;
* future cancellation;
* expired ACTIVE and CANCELLED rows remain retained history;
* campaign and center status do not gate foundation configuration;
* assignment dates may extend outside campaign service dates.

Actual future session validity will combine assignment coverage with campaign dates, OperatingHours, CenterClosure, recurrence, and other session constraints.

### 8.3 Persistence, locking, and authoritative reload

The campaign row is the write-serialization guard.

Existing-child mutation ordering is:

```text
scalar campaign ID
→ pessimistic campaign lock
→ authoritative assignment refresh
→ ownership and lifecycle checks
→ overlap check where applicable
→ mutation
```

`EntityManager.refresh()` is intentionally used after campaign locking so a child already managed in the persistence context cannot supply stale lifecycle, ownership, period, or center state to update, prospective-end, or cancellation decisions. Do not remove, relocate, or replace this mechanism without a new correctness review.

No entity mutation occurs before authoritative refresh and validation. Future VaccinationSession integration must use compatible campaign locking when assignment coverage is part of an atomic write decision.

### 8.4 Commits and PR #26

Reviewed implementation commits:

* `1c2fa6edcfac0dd3306aa89b1139f3694a9bb920` — `feat(mf02): add campaign center assignment domain and persistence`
* `c8ff2f1ec487ef39bc17a64360d95e39545bf947` — `feat(mf02): add campaign center assignment lifecycle service`
* `f1e4da3a5d88e4de35986fccdbf699a6718feecd` — `test(mf02): cover campaign center assignment concurrency`
* `5a3d5c23900c28293574d4552cc3a883c09a13fb` — `fix(mf02): refresh assignment state after campaign lock`

Integration facts:

* PR #26 — `feat(mf02): add campaign center assignment backend foundation`;
* source branch: `feature/mf02-campaign-center-assignment`;
* preserved remote source pointer: `5a3d5c23900c28293574d4552cc3a883c09a13fb`;
* merge commit: `f9c2bbaa8f9f0c13a338ea910961820b34e88d9e`;
* merge parents: `7b9398b48008e627fc18e5d7a0f864686a69b588` and `5a3d5c23900c28293574d4552cc3a883c09a13fb`;
* Backend CI and Frontend CI succeeded before merge and after merge;
* no CI correction commit was necessary;
* no hosted GitHub review record was visible; do not claim an approval event.

### 8.5 Test evidence

CampaignCenterAssignment-specific tests: **48**

* model: 8;
* persistence integration: 9;
* service integration: 27;
* concurrency integration: 4.

Backend total at the CampaignCenterAssignment checkpoint: **242 tests**, zero failures, zero errors, zero skipped. This is a historical component checkpoint; the current total is 272.

Concurrency coverage includes:

1. overlapping creates for the same campaign-center;
2. create racing with future update;
3. independent writes for different campaigns;
4. stale managed child followed by authoritative post-lock refresh.

## 9. SessionRecurrenceRule — Completed Contract

### 9.1 Purpose and model

SessionRecurrenceRule is a bounded weekly definition from which individually stored VaccinationSession occurrences may later be generated. It is not a concrete session, normal center availability, a closure, operational campaign-center authorization, a slot, or a capacity/resource plan.

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

The inclusive date range must be bounded, non-reversed, and contain at least one selected weekday. Overnight rules are rejected. Structurally identical, adjacent, and schedule-overlapping recurrence definitions are intentionally permitted because concrete-session conflicts belong to later generation/session validation.

### 9.2 Lifecycle and authoritative mutation

New rules are ACTIVE. Expiry does not change status automatically; expired ACTIVE and CANCELLED rules remain retained history, and no normal hard-delete service exists.

Implemented lifecycle operations:

* historical, current, and future rule creation;
* full definition update only before the authoritative existing rule starts, using its stored timezone and the candidate timezone for local-date boundaries;
* prospective end-date shortening without rewriting elapsed dates, with shortening to rule-local today permitted;
* terminal cancellation while a rule can still affect current/future generation, including its inclusive final day;
* immutable campaign, center, and generation key throughout lifecycle changes.

Creation requires only that campaign and center exist. CampaignStatus, CenterStatus, campaign service dates, CampaignCenterAssignment, OperatingHours, and CenterClosure do not gate recurrence-definition configuration. They remain concrete-session generation/validation concerns.

Existing-rule writes use a direct `PESSIMISTIC_WRITE` rule-row lock followed by `EntityManager.refresh()` before lifecycle decisions or mutation. Independent review verified that this refresh updates both scalar state and an already-initialized weekday element collection after a competing committed update. The committed separate-transaction regression proves stale ACTIVE state cannot overwrite a committed cancellation.

### 9.3 Commits, PR, and validation

Implementation commits:

* `9e56f41b1fd217e73639b7d041d19898a0ecf702` — `feat(mf02): add session recurrence rule domain and persistence`
* `a980a6266f73dc2df1cb8ce52b555f6788284e1f` — `feat(mf02): add session recurrence rule lifecycle service`
* `d7409db1d621ba2995162859b3923ba061992861` — `test(mf02): cover recurrence rule authoritative mutation`

Integration facts:

* PR #28 — `feat(mf02): add session recurrence rule backend foundation`;
* source branch: `feature/mf02-session-recurrence-rule`;
* preserved remote pointer: `d7409db1d621ba2995162859b3923ba061992861`;
* merge commit: `0dd05a5dce856bbaff6924c54fcf3c395920b076`;
* merge parents: `04b53da01a80be85873b3619f9aac2b6f54003b6` and `d7409db1d621ba2995162859b3923ba061992861`;
* exactly four production and four focused test files changed;
* Backend CI and Frontend CI succeeded before and after merge;
* no CI correction commit was necessary;
* no hosted review decision was recorded; the independent engineering review passed without fabricating a GitHub approval event.

SessionRecurrenceRule-specific tests: **30** — 8 model, 7 persistence integration, 14 service integration, and 1 concurrency integration. The current backend total is **272 tests**, with zero failures, errors, or skips; Maven `test` and `verify` succeeded and the executable JAR packaged.

## 10. Story and Sprint Status

Sprint 1 remains **IN PROGRESS**.

| Story | Current status |
| --- | --- |
| US-09 | In progress at full-story scope: backend foundation implemented; publication transition/API/UI/DoD work remains |
| US-10 | In progress: MF-02 foundation hardened; final MF-01 QuestionnaireVersion adapter/FK remains |
| US-11 | Backend foundation complete; later API/UI/integration layers remain |
| US-12 | Backend foundation implemented, reviewed, tested, CI-verified, and merged; Flyway, API/security/UI, VaccinationSession integration, and final DoD evidence remain |
| US-13 | **IN PROGRESS**: VaccinationCenter, OperatingHours, CenterClosure, and SessionRecurrenceRule backend foundations complete; concrete VaccinationSession, occurrence generation, API/UI/migration, and final DoD work remain |
| US-27 | Backend foundation implemented; later API/UI integration remains |

Do not mark Sprint 1, US-12, or US-13 fully Definition-of-Done complete solely from these backend foundations.

## 11. SessionRecurrenceRule Boundary

`SessionRecurrenceRule` is complete at its current backend-foundation scope. Do not rebuild it or mistake it for generated occurrences.

Preserved boundary:

* it belongs to US-13;
* it describes recurrence of proposed sessions rather than normal center availability;
* it must not duplicate OperatingHours or CenterClosure;
* it exposes stable definition and generation-identity data for later VaccinationSession work;
* occurrence expansion, occurrence keys, one-occurrence editing, and future-series impact orchestration remain unimplemented.

Do not add session generation directly to the recurrence entity/service without the VaccinationSession integration planning gate.

## 12. VaccinationSession Boundary

VaccinationSession implementation and occurrence generation remain **NOT STARTED**. The next major phase is architecture and cross-component integration planning. Later session validity must combine, where applicable:

* campaign lifecycle and service dates;
* effective CampaignCenterAssignment coverage for the full session calendar span;
* OperatingHours;
* CenterClosure;
* recurrence intent;
* capacity and other canonical session constraints.

MF02-BR-05 assignment/hours enforcement and MF02-BR-06 closure enforcement are later integrated session concerns. Read helpers alone are insufficient for atomic session writes; compatible parent-lock ordering must be designed deliberately.

## 13. Remaining Sprint 1 Work

* remaining campaign publication prerequisite/transition integration;
* VaccinationSession foundation and cross-component integration planning/implementation;
* recurrence occurrence generation and idempotent occurrence identity;
* later MF02-BR-05 assignment/hours enforcement;
* later MF02-BR-06 closure enforcement;
* Flyway business migrations;
* REST/API/DTO/mapper work where required;
* security/RBAC;
* OpenAPI;
* frontend;
* final MF-01 QuestionnaireVersion adapter/FK;
* production MySQL concurrency assurance;
* final Sprint integration and Definition-of-Done evidence.

## 14. Anti-Regression Guardrails

Do not:

* rebuild or materially redesign completed OperatingHours, CenterClosure, or CampaignCenterAssignment foundations without a new scoped review;
* remove CampaignCenterAssignment's post-lock authoritative refresh;
* conflate CampaignLocationTarget with CampaignCenterAssignment;
* treat ACTIVE status alone as effectiveness outside the applicable period;
* change half-open assignment or closure semantics;
* make assignment configuration depend on campaign status, center status, or campaign service-date containment;
* represent H2 concurrency tests as proof of production MySQL behavior;
* claim hosted approval evidence that GitHub does not contain;
* rebuild or broaden the completed SessionRecurrenceRule foundation without a new scoped review;
* claim that SessionRecurrenceRule creates operationally valid sessions by itself;
* begin VaccinationSession before its cross-component architecture and lock-ordering gate;
* mark Sprint 1 complete prematurely.

## 15. Final Handoff State

```text
Canonical: 3.3-AI / 2026-08-08-R8
Sprint: Sprint 1 — IN PROGRESS
Recurrence implementation merge: PR #28 / 0dd05a5dce856bbaff6924c54fcf3c395920b076
CenterClosure: IMPLEMENTED, REVIEWED, TESTED, CI-VERIFIED, AND MERGED
CampaignCenterAssignment: IMPLEMENTED, REVIEWED, TESTED, CI-VERIFIED, AND MERGED
SessionRecurrenceRule: IMPLEMENTED, REVIEWED, TESTED, CI-VERIFIED, AND MERGED
Backend baseline: 272 tests; Maven test/verify SUCCESS; JAR packaged
Supporting backend foundations: COMPLETE FOR CURRENT PRE-VACCINATIONSESSION PHASE
Next phase: VaccinationSession architecture and MF-02 cross-component integration planning
VaccinationSession: NOT STARTED
```

The next AI should refresh repository state and begin a new controlled planning conversation for VaccinationSession architecture, occurrence generation/idempotency, cross-component validation, and compatible locking. It must not assume that architecture is already complete or begin implementation in an uncontrolled combined slice.

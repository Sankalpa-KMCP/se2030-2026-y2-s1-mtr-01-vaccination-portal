# MF-02 — Vaccination Campaign, Center and Session Management

## AI Engineering Handoff

**Project:** Web-Based Vaccination Portal (`WBVP-SE2030`)

**Function:** MF-02 — Vaccination Campaign, Center and Session Management

**Canonical authority:** `3.3-AI / 2026-08-08-R8`

**Sprint:** Sprint 1 — **IN PROGRESS**

**Current repository checkpoint:** `origin/develop = f9c2bbaa8f9f0c13a338ea910961820b34e88d9e`

**Latest completed backend foundation:** `CampaignCenterAssignment`

**Next controlled domain slice:** `SessionRecurrenceRule` discovery and planning — **NOT STARTED**

> This handoff is optimized for the next AI session. The canonical Markdown is the semantic requirements source of truth; live Git history and tests are the implementation source of truth. Do not rebuild completed foundations or treat this handoff as authority to change canonical rules.

## 1. Executive Snapshot

The following MF-02 backend foundations are already implemented and integrated:

* campaign lifecycle, VaccineType, and TargetGroup;
* CampaignDoseProgramme;
* CampaignLocationTarget;
* CampaignQuestionnaireAssignment;
* VaccinationCenter;
* OperatingHours;
* CenterClosure;
* CampaignCenterAssignment.

`CenterClosure` remains **IMPLEMENTED, REVIEWED, TESTED, CI-VERIFIED, AND MERGED**.

`CampaignCenterAssignment` is **IMPLEMENTED, REVIEWED, TESTED, CI-VERIFIED, AND MERGED** at backend-foundation scope through PR #26. It must not be reconstructed or replaced when work resumes.

Current backend validation is 242 tests with zero failures, errors, or skips. Maven `test` and `verify` succeeded, the JAR packaged successfully, and Backend CI and Frontend CI succeeded before and after the PR #26 merge.

Sprint 1 is not complete. `SessionRecurrenceRule`, VaccinationSession integration, migrations, API/security/UI layers, publication integration, and final Definition-of-Done evidence remain pending.

## 2. Authority and Startup Procedure

Use this source order:

1. `docs/Web_Based_Vaccination_Portal_AI_Canonical_Context_v3.3.md` for semantic requirements;
2. live `origin/develop`, merged PRs, code, and tests for implementation truth;
3. `docs/evidence/sprint-1/MF02_Sprint1_Evidence.md` for accumulated engineering evidence;
4. this handoff for current context and safe next steps.

At the start of the next session:

1. fetch `origin` and verify `origin/develop`;
2. inspect any advancement after `f9c2bbaa8f9f0c13a338ea910961820b34e88d9e`;
3. confirm Sprint 1 and US-13 remain in progress;
4. do broad discovery for `SessionRecurrenceRule` across canonical requirements, current domain conventions, repositories, services, tests, and session-related dependencies;
5. produce and review a dedicated architecture plan before implementation;
6. do not begin VaccinationSession or combine recurrence with another component in the same slice.

Recurrence has not received the same detailed planning gate that preceded CampaignCenterAssignment. Do not claim its architecture is already complete.

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

The current overall checkpoint is:

```text
origin/develop = f9c2bbaa8f9f0c13a338ea910961820b34e88d9e
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
* `SessionRecurrenceRule` will describe recurrence of proposed sessions; it is not an extension of OperatingHours.
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

Current backend total: **242 tests**, zero failures, zero errors, zero skipped. Maven `test` and `verify` both completed with `BUILD SUCCESS`, and the JAR packaged successfully.

Concurrency coverage includes:

1. overlapping creates for the same campaign-center;
2. create racing with future update;
3. independent writes for different campaigns;
4. stale managed child followed by authoritative post-lock refresh.

## 9. Story and Sprint Status

Sprint 1 remains **IN PROGRESS**.

| Story | Current status |
| --- | --- |
| US-09 | In progress at full-story scope: backend foundation implemented; publication transition/API/UI/DoD work remains |
| US-10 | In progress: MF-02 foundation hardened; final MF-01 QuestionnaireVersion adapter/FK remains |
| US-11 | Backend foundation complete; later API/UI/integration layers remain |
| US-12 | Backend foundation implemented, reviewed, tested, CI-verified, and merged; Flyway, API/security/UI, VaccinationSession integration, and final DoD evidence remain |
| US-13 | **IN PROGRESS**: VaccinationCenter, OperatingHours, and CenterClosure foundations complete; SessionRecurrenceRule not started; later VaccinationSession/API/UI/migration work remains |
| US-27 | Backend foundation implemented; later API/UI integration remains |

Do not mark Sprint 1, US-12, or US-13 fully Definition-of-Done complete solely from these backend foundations.

## 10. SessionRecurrenceRule Boundary

`SessionRecurrenceRule` is the next new domain planning slice and is **NOT STARTED**.

Known boundary only:

* it belongs to US-13;
* it describes recurrence of proposed sessions rather than normal center availability;
* it must not duplicate OperatingHours or CenterClosure;
* its persistence, lifecycle, temporal semantics, generation behavior, locking, and relationship to VaccinationSession require fresh discovery and an explicit architecture review.

Do not infer a recurrence model from names alone. Do not implement it before the planning gate.

## 11. VaccinationSession Boundary

VaccinationSession implementation remains pending. Later session validity must combine, where applicable:

* campaign lifecycle and service dates;
* effective CampaignCenterAssignment coverage for the full session calendar span;
* OperatingHours;
* CenterClosure;
* recurrence intent;
* capacity and other canonical session constraints.

MF02-BR-05 assignment/hours enforcement and MF02-BR-06 closure enforcement are later integrated session concerns. Read helpers alone are insufficient for atomic session writes; compatible parent-lock ordering must be designed deliberately.

## 12. Remaining Sprint 1 Work

* broad discovery, architecture review, and foundation implementation for SessionRecurrenceRule;
* remaining campaign publication prerequisite/transition integration;
* VaccinationSession and session-validation integration;
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

## 13. Anti-Regression Guardrails

Do not:

* rebuild or materially redesign completed OperatingHours, CenterClosure, or CampaignCenterAssignment foundations without a new scoped review;
* remove CampaignCenterAssignment's post-lock authoritative refresh;
* conflate CampaignLocationTarget with CampaignCenterAssignment;
* treat ACTIVE status alone as effectiveness outside the applicable period;
* change half-open assignment or closure semantics;
* make assignment configuration depend on campaign status, center status, or campaign service-date containment;
* represent H2 concurrency tests as proof of production MySQL behavior;
* claim hosted approval evidence that GitHub does not contain;
* mark SessionRecurrenceRule as planned or implemented before its dedicated gate;
* begin VaccinationSession in the recurrence foundation slice;
* mark Sprint 1 complete prematurely.

## 14. Final Handoff State

```text
Canonical: 3.3-AI / 2026-08-08-R8
Sprint: Sprint 1 — IN PROGRESS
origin/develop: f9c2bbaa8f9f0c13a338ea910961820b34e88d9e
CenterClosure: IMPLEMENTED, REVIEWED, TESTED, CI-VERIFIED, AND MERGED
CampaignCenterAssignment: IMPLEMENTED, REVIEWED, TESTED, CI-VERIFIED, AND MERGED
Backend baseline: 242 tests; Maven test/verify SUCCESS; JAR packaged
Next slice: SessionRecurrenceRule broad discovery and architecture planning
SessionRecurrenceRule: NOT STARTED
VaccinationSession: NOT STARTED
```

The next AI should refresh repository state, perform broad recurrence discovery, and prepare a dedicated SessionRecurrenceRule architecture plan. It must not begin recurrence implementation or VaccinationSession in the same uncontrolled step.

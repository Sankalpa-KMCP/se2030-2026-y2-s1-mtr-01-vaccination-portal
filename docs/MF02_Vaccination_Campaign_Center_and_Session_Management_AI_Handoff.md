# MF-02 — Vaccination Campaign, Center and Session Management

## AI Engineering Handoff

**Project:** Web-Based Vaccination Portal (`WBVP-SE2030`)
**Major function:** MF-02 — Vaccination Campaign, Center and Session Management
**Canonical version:** `3.3-AI`
**Canonical revision:** `2026-08-08-R8`
**Handoff synchronized:** 2026-08-11
**Sprint:** Sprint 1 — **IN PROGRESS**
**Current integrated checkpoint:** `e70940b7021778ae35dd2976ac5df6b649a28584`
**Current implementation frontier:** `CampaignCenterAssignment` backend foundation
**Architecture verdict:** **CAMPAIGNCENTERASSIGNMENT PLAN READY — SAFE TO IMPLEMENT**

> This is an implementation handoff, not a replacement for canonical requirements. If this handoff, generated code, or an older plan conflicts with the canonical source, the canonical source wins.

---

## 1. Executive Snapshot

The MF-02 backend now includes integrated foundations for:

* campaign lifecycle and publication-oriented configuration;
* vaccine type and target group masters;
* effective-dated campaign dose programmes;
* administrative campaign location targets;
* effective-dated campaign questionnaire assignment on the MF-02 side;
* vaccination center master/lifecycle;
* recurring center `OperatingHours`;
* temporary center unavailability through `CenterClosure`.

The most recent implementation integration is the `CenterClosure` backend foundation. It is **IMPLEMENTED, REVIEWED, TESTED, CI-VERIFIED, AND MERGED** at `e70940b7021778ae35dd2976ac5df6b649a28584`.

The next slice is `CampaignCenterAssignment`. Its architecture contract is approved, but implementation has **NOT STARTED**. Do not infer that US-12 or US-13 is complete.

Current validation baseline:

* 194 backend tests;
* 0 failures;
* 0 errors;
* 0 skipped;
* Maven `test`: success;
* Maven `verify`: success;
* Backend CI: success;
* Frontend CI: success.

---

## 2. Authority and Source Order

Use this order when resolving uncertainty:

1. `docs/Web_Based_Vaccination_Portal_AI_Canonical_Context_v3.3.md`
2. canonical master DOCX at `docs/Web_Based_Vaccination_Portal_AI_Canonical_Master_v3.3.docx`
3. current code and tests on `origin/develop`
4. `docs/evidence/sprint-1/MF02_Sprint1_Evidence.md`
5. this handoff
6. obsolete generated plans or chat context

Canonical authority remains `3.3-AI`, revision `2026-08-08-R8`. Do not modify canonical Markdown or DOCX as part of ordinary feature implementation.

---

## 3. Freshness and Repository Checkpoint

Expected current remote checkpoint:

```text
origin/develop = e70940b7021778ae35dd2976ac5df6b649a28584
```

Always fetch and inspect live repository state before starting. If `develop` has advanced, review the delta and use the latest compatible state rather than blindly resetting or rewriting history.

The current checkpoint is the PR #24 merge commit. It is an integration checkpoint; the underlying feature commits remain the implementation evidence.

---

## 4. Startup Procedure for the Next Agent

Before implementation:

1. verify the repository root, current branch, HEAD, and working tree;
2. run `git fetch origin`;
3. verify current `origin/develop` and inspect advancement;
4. confirm `CampaignCenterAssignment` still does not exist;
5. inspect nearby campaign effective-history entities, repositories, services, and tests;
6. inspect `VaccinationCampaignRepository.findByIdForUpdate(...)`;
7. create a fresh feature branch from current `origin/develop`;
8. keep the feature limited to model, persistence, service, and focused tests.

Do not reuse the CenterClosure or pre-closure hardening branches.

---

## 5. Architecture Baseline

The backend uses Spring Boot, Spring Data JPA, field-access entities, Maven, MySQL as the production target, and H2 in MySQL compatibility mode for current integration tests.

Current MF-02 conventions:

* mandatory lazy parent associations;
* no remove cascade for operational history;
* generated `Long` identifiers;
* enums stored with `EnumType.STRING`;
* `Instant` audit timestamps maintained through lifecycle callbacks;
* UTC application `Clock` for time-sensitive services;
* `LocalDate` for calendar-effective histories;
* `Instant` for absolute timeline events;
* candidate validation before entity mutation;
* cohesive mutation methods instead of broad setter surfaces;
* no custom entity `equals`/`hashCode` without a demonstrated need;
* `IllegalArgumentException` for current validation and missing-state style;
* transactional services and read-only transactions for queries;
* parent-row pessimistic locking for cross-row history/conflict decisions;
* scalar parent-ID lookup before the lock for child mutation workflows.

Flyway business migrations remain deferred. Hibernate/H2 schema generation is test infrastructure, not production migration evidence.

---

## 6. Cross-Function Ownership Boundaries

MF-02 owns campaign, center, assignment, operating-calendar, closure, recurrence, session, and session-resource scheduling concepts.

Key boundaries:

* MF-01 owns questionnaire definitions, versions, publication, eligibility evaluation, and screening submissions.
* MF-02 owns the campaign-to-questionnaire assignment record and references MF-01 through an explicit integration port until the final adapter/FK exists.
* MF-03 owns appointments and waitlists.
* MF-04 owns inventory and stock movement.
* `CampaignLocationTarget` is citizen-facing administrative service-area targeting.
* `CampaignCenterAssignment` is operational authorization for a center to host services for a campaign.

Never substitute location targeting for operational center assignment.

---

## 7. Canonical MF-02 Rules That Shape the Next Work

Important rule boundaries include:

* effective configuration histories must not overlap for the same logical owner/key;
* publication prerequisites are distinct from draft/configuration invariants;
* MF02-BR-05 later requires a session within campaign service dates, center OperatingHours, and an active effective-dated `CampaignCenterAssignment`;
* MF02-BR-06 later combines center closures and other session constraints;
* frontend checks never replace server-side and persistence-level enforcement;
* operational histories must not be casually hard-deleted.

The next assignment foundation does not implement session validation.

---

## 8. Integration History

| Checkpoint | Result |
| --- | --- |
| PR #12 `eb4a4d9` | Campaign, dose-programme, targeting, and questionnaire-assignment foundation |
| PR #16 `201768e` | Vaccination-center foundation and initial hardening |
| PR #17 `bd22cb9` | Canonical R8 DOCX synchronization |
| PR #18 `c120ee8` | Sprint evidence checkpoint |
| PR #19 `8cdb112` | Sprint evidence checkpoint |
| PR #20 `1cad899` | OperatingHours backend foundation |
| PR #21 `433dbe1` | OperatingHours evidence checkpoint |
| PR #22 `fe101bb2adfc1215146793a97cd80fdc3176622c` | Evidence metadata stability checkpoint |
| PR #23 `5a5b3e7` | Pre-closure backend hardening |
| PR #24 `e70940b` | CenterClosure backend foundation |

Metadata-only synchronization checkpoints are not recursively treated as new implementation checkpoints.

---

## 9. Pre-Closure Hardening Checkpoint

PR #23 merged at `5a5b3e7d285b50b7645a1c32b57f79b753b2baff`.

Classification:

* `0cc1ad9` — implementation hardening;
* `3683a0d` — genuine separate-transaction invariant tests;
* `d3efd0e` — narrow questionnaire history-boundary documentation correction;
* merge commit — integration checkpoint only.

The hardening established:

* UTC-safe `Instant` handling for absolute/audit and campaign booking-window timestamps, plus application UTC `Clock` use;
* half-open dose-programme history;
* half-open questionnaire-assignment history;
* intentionally inclusive OperatingHours calendar dates;
* reduced entity mutation surface;
* removal of unsafe custom entity equality/hash behavior;
* simplified dose/questionnaire services and repositories and location-target batch handling;
* genuine separate-transaction locking coverage.

Validation: 149 tests, Maven `test`/`verify` success, Backend CI success, and Frontend CI success.

Hosted independent GitHub approval evidence was not recorded in the controller evidence available for this checkpoint. Preserve that as a neutral process-provenance note; do not invent or backfill an approval event.

---

## 10. OperatingHours — Completed Historical Contract

OperatingHours was merged through PR #20 at `1cad899971c8b18804af31bfb079a24285f7d16d`.

Historical validation at that checkpoint:

* 19 model tests;
* 14 persistence tests;
* 22 service tests;
* 55 OperatingHours-specific tests;
* 152 total backend tests;
* Backend CI and Frontend CI success.

The completed behavior is:

* weekly recurring shifts by `DayOfWeek`;
* inclusive `[effectiveFrom, effectiveTo]` calendar-date applicability;
* half-open `[opening, closing)` time intervals;
* same-day positive-duration shifts only;
* split and boundary-adjacent shifts allowed;
* overlapping center/weekday/date/time configurations rejected;
* deterministic Monday-to-Sunday listing;
* center-parent pessimistic locking for writes;
* scalar center-ID-before-lock ordering for mutation;
* no normal hard-delete service;
* configuration allowed for ACTIVE and INACTIVE centers.

Older handoff material contained many pages of prospective OperatingHours decisions. Those details are now consolidated here because OperatingHours is implemented and should no longer appear as actionable work.

---

## 11. CenterClosure — Completed Contract

CenterClosure was merged through PR #24 at `e70940b7021778ae35dd2976ac5df6b649a28584`.

Feature branch final pointer:

```text
d6c04718639d2356f9f4800730d9d79bed5494bf
```

Commit classification:

* `8d70359` — domain and persistence implementation;
* `f1bd663` — management service implementation;
* `cf47f725` — lifecycle and concurrency tests;
* `d6c0471` — test-only fixture-isolation correction discovered by CI;
* merge commit — CenterClosure integration checkpoint.

Implemented behavior:

* direct mandatory ownership by `VaccinationCenter`;
* absolute `Instant startAt` and `endAt`;
* half-open `[startAt, endAt)` interval;
* positive duration, with boundary adjacency allowed;
* one interval model for partial-day, overnight, full-day-after-boundary-conversion, and multi-day closures;
* mandatory normalized reason, maximum 255 characters;
* status exactly `ACTIVE` and `CANCELLED`;
* no overlapping ACTIVE sibling closures for one center;
* CANCELLED history ignored by active-conflict checks;
* creation may record future, ongoing, or historical closures;
* update and cancellation are strictly future-only (`now < startAt`);
* cancellation is terminal and preserves the row, interval, and original reason;
* CenterStatus and OperatingHours do not gate closure configuration;
* no normal hard-delete service;
* all writes for one center serialize through the center pessimistic lock;
* active-overlap read helper is available for future session integration.

Validation:

* 45 CenterClosure-specific tests: 15 model, 6 persistence integration, 21 service integration, and 3 concurrency integration;
* 194 total backend tests;
* zero failures/errors/skips;
* Maven test and verify success;
* Backend CI and Frontend CI success.

Independent review verdict: **ACCEPT WITH NON-BLOCKING NOTES**. Canonical, domain, persistence, lifecycle, interval/overlap, concurrency, maintainability, test-quality, and data-integrity gates passed. Optional future notes cover Flyway index review, profiling-driven overlap-query optimization, additional persisted audit-transition assertions if useful, and production MySQL concurrency validation.

H2/MySQL-mode tests provide structural concurrency evidence, not proof of production MySQL behavior. Hosted independent GitHub approval evidence was not recorded in the controller evidence available for PR #24; do not represent it as recorded.

---

## 12. Sprint and Story Status

Sprint 1 remains **IN PROGRESS**.

| Story | Current state |
| --- | --- |
| US-09 | Backend foundation implemented; publication workflow/API/UI/DoD evidence pending |
| US-10 | MF-02 foundation implemented and hardened; final MF-01 adapter/FK pending |
| US-11 | Versioning and hardening implemented; later API/UI/integration pending |
| US-12 | Architecture plan complete; implementation **NOT STARTED** |
| US-13 | OperatingHours and CenterClosure complete; recurrence **NOT STARTED**; session/API/UI/migration pending |
| US-27 | Backend foundation implemented; later API/UI evidence pending |

CenterClosure completion is the backend foundation only. It does not complete all of US-13.

---

## 13. Current Stopping Point

The correct next component is `CampaignCenterAssignment`.

State:

* architecture gate complete;
* implementation-ready contract established;
* no entity, enum, repository, service, or tests implemented yet;
* no feature branch should be assumed to exist;
* recurrence and session work remain deferred.

Do not reopen routine Java/Spring decisions unless live repository or canonical evidence creates a concrete contradiction.

---

## 14. CampaignCenterAssignment Purpose

`CampaignCenterAssignment` records operational authorization for one vaccination center to serve one campaign during an effective calendar period.

It is not:

* an administrative visibility target;
* a session;
* a recurrence rule;
* a center opening-hours row;
* a temporary center closure;
* a citizen eligibility rule;
* an appointment, waitlist, inventory, resource, or capacity record.

---

## 15. Approved Assignment Entity Contract

Planned production shape:

* `CampaignCenterAssignmentStatus` — small enum;
* `CampaignCenterAssignment` — cohesive entity;
* `CampaignCenterAssignmentRepository` — minimal query surface;
* `CampaignCenterAssignmentService` — compact transactional service.

Planned logical fields:

| Field | Type | Source of decision |
| --- | --- | --- |
| `id` | `Long` | Canonical concept and repository convention |
| `campaign` | `VaccinationCampaign` | Canonical ownership |
| `center` | `VaccinationCenter` | Canonical ownership |
| `activeFrom` | `LocalDate` | Canonical effective date and approved implementation decision |
| `activeTo` | nullable `LocalDate` | Canonical effective date and approved implementation decision |
| `status` | assignment status enum | Canonical status concept and approved minimal lifecycle |
| `createdAt` | `Instant` | Repository convention |
| `updatedAt` | `Instant` | Repository convention |

Campaign and center ownership are immutable after construction. Do not add public ownership, ID, or audit setters.

---

## 16. Approved Effective-Period Semantics

Use calendar dates:

```java
LocalDate activeFrom;
LocalDate activeTo; // nullable
```

Semantics:

* `[activeFrom, activeTo)`;
* `activeFrom` is inclusive;
* `activeTo` is exclusive;
* `activeTo == null` is open-ended;
* finite periods require `activeFrom < activeTo`;
* adjacent periods are allowed;
* zero-length and reversed periods are invalid.

This agrees with the canonical overlap formula and the hardened dose/questionnaire history convention. It deliberately differs from OperatingHours, whose effective calendar dates are inclusive.

Date effectiveness later covers a session only when the assignment is effective for the session's complete calendar-date span. Do not introduce timestamp precision merely because sessions will later have times.

---

## 17. Approved Assignment Status Model

Use exactly:

* `ACTIVE`
* `CANCELLED`

Meaning:

* `ACTIVE` means the assignment record has not been cancelled; dates determine whether it is currently effective.
* `CANCELLED` is a terminal retained record and is never effective.
* Reaching `activeTo` does not mutate status and does not create an `ENDED` status.

Legal lifecycle:

```text
ACTIVE -> CANCELLED
```

There is no reactivation. Repeated cancellation should reject explicitly using current repository style.

---

## 18. Approved Duplicate and Overlap Policy

For the same campaign-center pair, non-cancelled effective periods must not overlap.

Finite/open-ended overlap is true when:

```text
existing.activeFrom < candidateEnd
AND
candidate.activeFrom < existingEnd
```

where a null end is treated as positive infinity.

Reject:

* exact duplicate;
* partial overlap;
* containment in either direction;
* same-start or same-end overlap;
* any later period while a prior assignment remains open-ended.

Allow:

* adjacency;
* same campaign with different centers;
* different campaigns with the same center;
* overlap with CANCELLED history.

Do not silently close an open-ended assignment. End it explicitly first.

---

## 19. Campaign, Center, and Campaign-Date Interaction

The backend foundation permits assignment configuration independently of current campaign and center lifecycle status.

Therefore:

* campaign status does not gate creation/update/end/cancel in this slice;
* an INACTIVE center may retain history and receive future configuration;
* assignment dates may extend outside campaign service dates;
* the assignment service does not clip periods to campaign dates.

Later publication/session workflows combine campaign status/dates, center status, assignment effectiveness, OperatingHours, CenterClosure, and other constraints. Avoid duplicating those rules early.

---

## 20. Approved Mutation and History Contract

Create:

* require existing campaign and center;
* validate the complete period/status candidate;
* lock the campaign;
* reject overlapping ACTIVE sibling history;
* save one ACTIVE assignment.

Future update:

* ownership is immutable;
* an ACTIVE assignment that has not started may move `activeFrom`/`activeTo`;
* validate the complete candidate before mutation;
* reject overlap excluding the current row;
* do not rewrite started or historical applicability.

Prospective end:

* an ACTIVE started/open assignment may be end-dated prospectively;
* the new exclusive `activeTo` must be after the current date and after `activeFrom`;
* do not backdate the end or extend an already finite period through an end command;
* expiry retains ACTIVE status as historical non-cancelled authorization.

Future cancellation:

* only ACTIVE assignments not yet started may be cancelled;
* transition to CANCELLED;
* retain the row and original dates;
* repeated cancellation rejects.

No normal hard-delete service is planned.

---

## 21. JPA and Database Contract

Use:

* mandatory lazy `ManyToOne` to `VaccinationCampaign`;
* mandatory lazy `ManyToOne` to `VaccinationCenter`;
* non-null foreign keys;
* no remove cascade;
* `EnumType.STRING`;
* non-null `active_from`, status, created/updated timestamps;
* nullable `active_to`;
* manual `Instant` audit callbacks consistent with current entities.

Eventual Flyway migration should include:

* campaign and center foreign keys;
* status/date non-null constraints;
* a finite-period check equivalent to `active_to IS NULL OR active_from < active_to`;
* indexes supporting campaign listing, center history, effective lookup, and campaign-center overlap checks.

Database uniqueness alone cannot enforce interval non-overlap. Do not claim the current Hibernate schema as production migration enforcement.

---

## 22. Minimal Repository Contract

The repository should support only the approved service:

* scalar campaign-ID lookup by assignment ID for mutation lock ordering;
* deterministic campaign assignment listing;
* deterministic center assignment history if required by the approved read contract;
* effective assignment lookup/boolean for campaign, center, and date range;
* ACTIVE overlap existence for create;
* ACTIVE overlap existence excluding one assignment for update.

Use explicit JPQL for null-aware half-open overlap and optional self-exclusion. Avoid loading full sibling entities when only a boolean is needed.

Suggested deterministic ordering:

```text
activeFrom ASC,
activeTo ASC with open-ended handled consistently,
id ASC
```

Do not add speculative controller filters or generic history queries.

---

## 23. Locking and Concurrency Contract

Serialize assignment writes through the campaign row using the existing campaign pessimistic write lock.

Why campaign locking is sufficient:

* every competing write for one campaign-center pair shares the same campaign;
* locking one stable existing parent protects the cross-row overlap decision;
* it avoids double-lock ordering and deadlock complexity;
* it matches existing dose-programme/questionnaire campaign-history conventions.

Tradeoff: changes for different centers in the same campaign serialize. This is acceptable for the current scale and simpler than a compound guard entity. Different campaigns remain independent.

Ordering:

* create: validate candidate, resolve required parents, lock campaign, check overlap, save;
* update/end/cancel: validate command inputs, resolve scalar campaign ID from child ID, lock campaign, reload child, verify ownership, check lifecycle/time/history rules, check overlap if applicable, mutate, persist.

No child state should be relied on before the parent lock in a mutation workflow. All failure-producing checks must occur before mutation.

Future session creation will need a deliberate atomic locking strategy. The assignment read helper alone is not an atomic authorization guarantee.

---

## 24. Compact Service Contract

Expected cohesive operations:

* create assignment;
* get by ID;
* list assignments for a campaign;
* list history for a center only if required by the approved contract;
* update a future assignment period;
* prospectively end an active assignment;
* cancel a future assignment;
* test/find effectiveness for a future session date span.

The service should stay small and use existing repositories and `Clock`. Do not introduce a new exception hierarchy, validator framework, interval framework, lock manager, or generic effective-history base class.

---

## 25. Future VaccinationSession Consumer Contract

A later session validator must be able to establish:

> One non-cancelled CampaignCenterAssignment for the campaign-center pair covers the session's full calendar-date span.

For an inclusive session date span `[sessionStartDate, sessionEndDate]`, the assignment must satisfy:

```text
assignment.activeFrom <= sessionStartDate
AND
(assignment.activeTo IS NULL OR sessionEndDate < assignment.activeTo)
AND
assignment.status = ACTIVE
```

This helper does not implement:

* campaign service-date validation;
* OperatingHours validation;
* CenterClosure validation;
* session recurrence;
* capacity or resource validation.

Future atomic session creation must lock consistently before relying on the result.

---

## 26. High-Signal Test Plan for CampaignCenterAssignment

Model tests should cover:

* required campaign, center, and start;
* finite and open-ended construction;
* equal/reversed period rejection;
* default ACTIVE status;
* complete candidate update and exception-safety;
* cancellation terminality;
* history mutation protection;
* narrow mutation surface.

Persistence tests should cover:

* mandatory lazy relationships, enum, and audit behavior;
* deterministic campaign/center listing;
* start-inclusive/end-exclusive effective lookup;
* open-ended effectiveness;
* adjacency versus overlap;
* exact duplicates and containment;
* self-exclusion while still detecting another sibling;
* different-center and different-campaign independence.

Service tests should cover:

* finite/open-ended creation;
* missing campaign/center;
* overlap rejection and adjacency;
* lifecycle-status independence for campaign/center configuration;
* future update and overlap rejection;
* failed update preserves history;
* started/historical mutation restrictions;
* prospective end-dating and retroactive rejection;
* future cancellation, repeat rejection, and retention;
* effective lookup at both boundaries.

Concurrency tests should use separate service-proxy transactions and bounded futures for:

1. two overlapping creates for the same campaign-center;
2. create racing with an overlapping update for the same pair;
3. independent assignments for different campaigns.

Assert persisted invariants rather than exact winner order. State explicitly that H2/MySQL-mode is structural evidence, not production MySQL proof.

---

## 27. Complexity Guardrail

Expected production shape is one small enum, one entity, one repository, and one service. It should be comparable to existing effective-history components and not materially larger than its business behavior requires.

Avoid:

* generated getter/setter boilerplate beyond JPA/domain needs;
* custom equality/hash behavior;
* generic interval or assignment frameworks;
* DTO/controller/security layers in the foundation slice;
* speculative status values;
* duplicated candidate validation across service and entity;
* comments that restate obvious code.

Test volume should be driven by business signal, not a raw count target.

---

## 28. Suggested Commit Sequence

A coherent sequence is:

1. `feat(mf02): add campaign center assignment domain and persistence`
2. `feat(mf02): add campaign center assignment management service`
3. `test(mf02): cover campaign center assignment concurrency invariants`

A fourth commit is acceptable if separating persistence tests materially improves reviewability. Do not create one commit per file.

---

## 29. Explicitly Deferred from the Next Slice

Do not implement:

* `VaccinationSession` or MF02-BR-05 enforcement;
* `SessionRecurrenceRule` or `TimeSlot`;
* CenterClosure changes or optional cleanup;
* staff/resource unavailability or capacity;
* appointments/waitlists;
* inventory;
* MF-01 questionnaire work;
* REST controllers, DTOs, mappers, or OpenAPI;
* frontend;
* security/RBAC;
* Flyway migration infrastructure;
* production MySQL concurrency proof;
* evidence or canonical-document updates;
* PR or merge work unless separately requested.

---

## 30. SessionRecurrenceRule Boundary

Recurrence has **NOT STARTED**. It belongs to US-13 but is a separate architecture and implementation slice after CampaignCenterAssignment.

Do not infer recurrence from OperatingHours. OperatingHours defines normal center availability; recurrence defines how proposed session instances repeat. CenterClosure remains an exception layer checked against resulting session intervals.

---

## 31. VaccinationSession Boundary

VaccinationSession has **NOT STARTED**. When later designed, it must combine rather than collapse:

* campaign service dates/status;
* effective CampaignCenterAssignment;
* center lifecycle;
* OperatingHours;
* CenterClosure;
* recurrence output;
* resources/staff/capacity;
* other canonical constraints.

No existing helper by itself makes session creation valid or atomic.

---

## 32. Database and Migration Status

MySQL is the production target. H2/MySQL mode currently supports repository and structural concurrency integration tests.

Still required later:

* Flyway infrastructure/business migrations for implemented MF-02 tables;
* production FK/check/index definitions;
* migration verification on MySQL;
* production MySQL lock/concurrency validation.

Do not overstate Hibernate-generated test schemas as completed production database work.

---

## 33. API, Security, and UI Status

The current work is backend foundation only. Still pending where applicable:

* REST controllers;
* request/response DTOs and mappers;
* OpenAPI;
* role/permission enforcement;
* frontend screens and validation;
* end-to-end flows and screenshots.

Critical rules must be enforced server-side even when UI validation is later added.

---

## 34. Questionnaire Integration Boundary

MF-02 currently uses `QuestionnaireVersionPublicationPort` to avoid a fake production bypass while MF-01 remains incomplete.

Still deferred:

* authoritative MF-01 `QuestionnaireVersion` entity/table;
* production adapter for the publication port;
* final FK from campaign questionnaire assignment to MF-01;
* end-to-end historical screening submission retention evidence.

Do not absorb MF-01 ownership into CampaignCenterAssignment work.

---

## 35. Evidence and Metadata Policy

The living evidence record is:

```text
docs/evidence/sprint-1/MF02_Sprint1_Evidence.md
```

Preserve historical checkpoints, raw test totals, CI outcomes, and implementation versus merge-commit classification. Metadata-only documentation commits are evidence checkpoints, not new implementation checkpoints.

Do not recursively synchronize every evidence file merely because a new documentation commit exists.

---

## 36. Git Workflow

For future implementation:

* branch from refreshed `origin/develop`;
* preserve unrelated user changes;
* use normal focused commits;
* never force-push, amend reviewed history, or rebase solely for ceremony;
* run focused tests during development and full Maven test/verify before handoff;
* use `git diff --check` and inspect the complete feature diff;
* push/create PR/merge only when explicitly authorized;
* preserve source branches unless deletion is explicitly requested.

Do not place controller prompt identifiers in branches, code, comments, commits, PR metadata, or documentation.

---

## 37. Definition-of-Done Reality

Backend foundation completion does not equal story completion. Sprint stories still need some combination of:

* adjacent domain integration;
* production migrations;
* API and authorization;
* frontend;
* CI and environment validation;
* integration demonstrations;
* traceability/evidence.

Keep story status honest and incremental.

---

## 38. Remaining MF-02 Work

Current remaining sequence, subject to later architecture gates:

1. implement, review, and integrate `CampaignCenterAssignment`;
2. plan and implement `SessionRecurrenceRule`;
3. plan and implement `VaccinationSession`/`TimeSlot` foundations;
4. enforce MF02-BR-05 and MF02-BR-06 atomically;
5. complete publication transition/prerequisite behavior;
6. add Flyway migrations and MySQL verification;
7. add API, security, frontend, and OpenAPI layers;
8. complete MF-01 adapter/FK integration;
9. capture final Definition-of-Done evidence.

---

## 39. Anti-Regression Checklist

Before accepting future MF-02 changes, verify that they do not:

* conflate location targeting with center assignment;
* use `CenterStatus.INACTIVE` for temporary closure;
* rewrite historical effective records retroactively;
* introduce inclusive-end overlap mistakes in half-open histories;
* bypass parent locking for sibling conflict decisions;
* load mutable child state before the parent lock;
* add hard-delete services for operational history;
* add remove cascades from parents;
* add custom entity equality/hash without need;
* treat H2 as production MySQL proof;
* absorb sessions, recurrence, inventory, eligibility, or appointments into the assignment slice;
* mark US-12, US-13, or Sprint 1 complete prematurely.

---

## 40. Final Handoff State

```text
Project: WBVP-SE2030
Function: MF-02
Canonical: 3.3-AI / 2026-08-08-R8
Sprint: Sprint 1 — IN PROGRESS
origin/develop: e70940b7021778ae35dd2976ac5df6b649a28584
Latest completed component: CenterClosure backend foundation
CenterClosure: IMPLEMENTED, REVIEWED, TESTED, CI-VERIFIED, AND MERGED
Backend baseline: 194 tests, Maven test/verify success
Next component: CampaignCenterAssignment
Architecture: CAMPAIGNCENTERASSIGNMENT PLAN READY — SAFE TO IMPLEMENT
Implementation: NOT STARTED
Recurrence: NOT STARTED
Sessions: NOT STARTED
```

The next agent should implement only the approved `CampaignCenterAssignment` backend foundation after refreshing live repository state. Do not begin recurrence or session work in the same slice.

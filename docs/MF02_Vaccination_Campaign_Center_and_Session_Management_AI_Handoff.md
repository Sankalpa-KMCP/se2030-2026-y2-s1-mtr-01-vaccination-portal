# MF-02 — Vaccination Campaign, Center and Session Management
## AI Engineering Handoff

**Project:** Web-Based Vaccination Portal (`WBVP-SE2030`)
**Function:** MF-02 — Vaccination Campaign, Center and Session Management
**Canonical authority:** `3.3-AI / 2026-08-14-R9`
**Canonical source model:** Markdown-only; the canonical Markdown file is the sole normative semantic source.
**Sprint:** Sprint 1 — **IN PROGRESS**
**MF-02:** **IN PROGRESS**
**US-13:** **IN PROGRESS**
**Last synchronized implementation checkpoint:** PR #73
**Reviewed PR #73 source:** `75cd1bb34a0ec6bed32e66387bfa54bca01b4b31`
**PR #73 base:** `5fe33208d11a21ced5c6c1c520dfe01c13858bf0`
**PR #73 merge:** `d3fbb3eb95333d1da0b3e5c33283a1bcf81635e9`
**PR #73 merged at:** `2026-08-15T06:30:15Z`
**Preceding scheduling milestone:** PR #71 (`6d52449db4f29099f3a979e824a7efcae8f4ea54`)
**Current verified backend baseline:** **591 tests**, 0 failures, 0 errors, 0 skipped; clean Maven test/verify PASS; Backend CI PASS; Frontend CI PASS.
**PR #73 net-new executions:** 0 (behavior/assertion hardening within existing 591 suite).

> This file is a maintained engineering snapshot. Canonical Markdown controls semantics; live Git/code/tests control implementation truth. This handoff must not be used to override canonical rules.

## 1. Authority Order

Use this order when facts conflict:

1. `docs/Web_Based_Vaccination_Portal_AI_Canonical_Context_v3.3.md` — semantic/business source of truth.
2. Live `origin/develop`, merged PRs, production code, tests, and CI — implementation truth.
3. `docs/MF02_Vaccination_Campaign_Center_and_Session_Management_AI_Handoff.md` — current MF-02 engineering state.
4. `docs/evidence/sprint-1/MF02_Sprint1_Evidence.md` — implementation/verification chronology.
5. `PROJECT_CONTEXT.md` — orientation summary only.
6. `MF02_Completion_and_Hardening_Plan.md` — planned work, not evidence of implementation.

Future agents must fetch live `origin/develop` before treating `d3fbb3eb95333d1da0b3e5c33283a1bcf81635e9` as the repository tip.

## 2. Current Milestone

### Milestone reached

**MF-02 CORE BACKEND PLANNING / SESSION-CHANGE FOUNDATIONS, CAMPAIGN SCHEDULING APPROVAL, AND QUESTIONNAIRE INVARIANT HARDENING ARE INTEGRATED.**

This does **not** mean MF-02 is complete.

The current checkpoint includes campaign scheduling approval (PR #71) and defensive questionnaire publication invariant revalidation (PR #73) on top of the already integrated campaign, center, recurrence, session, reverse-impact, publication, suspension, terminal-planning, and SUSPENDED-planning foundations.

### Current verified integrations

1. **PR #73 — `fix(mf02): enforce published questionnaire when scheduling`**
   - reviewed source: `75cd1bb34a0ec6bed32e66387bfa54bca01b4b31`
   - merge: `d3fbb3eb95333d1da0b3e5c33283a1bcf81635e9`
   - base: `5fe33208d11a21ced5c6c1c520dfe01c13858bf0`
   - merge method: normal merge commit
   - Backend CI: PASS
   - Frontend CI: PASS
   - authoritative backend baseline: **591**
   - net-new executions: **0** (hardened invariant assertions and aligned fixtures)

2. **PR #71 — `feat(mf02): add campaign scheduling approval`**
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
- `PUBLISHED/ACTIVE -> SUSPENDED`

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

## 4. Campaign `DRAFT -> SCHEDULED` Contract — PR #71

PR #71 implements internal campaign scheduling approval.

### Source and target
- source: `DRAFT` only
- target: `SCHEDULED`

### Authoritative service command
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

### Center validity policy
For scheduling approval, relevant assignments are ACTIVE assignments whose half-open assignment period overlaps the inclusive campaign service range.

- zero relevant assignments: reject;
- relevant assignment to INACTIVE center: reject;
- any relevant overlapping assignment to INACTIVE center: reject;
- non-overlapping assignments: do not block;
- full-period center coverage is not required;
- OperatingHours and CenterClosure are not campaign-scheduling prerequisites.

### Questionnaire publication invariant (PR #73 hardening)
Under canonical rule `MF02-BR-02`, a valid questionnaire assignment must reference a published questionnaire version. PR #73 hardened `scheduleCampaign(...)` to defensively revalidate `publicationPort.isPublished(questionnaireVersionId)` under the campaign lock before status mutation, matching `publishCampaign(...)`. If un-published, scheduling fails deterministically with `IllegalStateException` and the campaign remains in `DRAFT`.

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

Critical write services are transactional. Read-only preview operations use read-only transactions where appropriate.

Do not add pessimistic locks merely for style. Add them only for a real invariant/race, and preserve the established order to avoid inversion.

## 6. Current Test / Verification State

### Authoritative current backend baseline
**591 tests**

- failures: 0
- errors: 0
- skipped: 0
- clean Maven test: PASS
- clean Maven verify: PASS
- Surefire reports at PR #71 candidate: 66 XML files
- Surefire-summed executions: 591
- Backend CI: PASS
- Frontend CI: PASS

### Baseline correction history
A prior non-clean/reporting value of 566 was superseded by clean-build reconciliation:
- PR #67 backend code checkpoint: **560**
- PR #69: **569**
- PR #71: **591**

Do not resurrect 566 or 575 as authoritative current evidence.

### Test architecture
Current integration/concurrency tests predominantly use H2 in MySQL compatibility mode. This provides structural regression evidence but does **not** certify production MySQL lock/deadlock semantics.

## 7. Architecture / Maintainability Audit — Current Repository Snapshot

The current repository is not broadly obsolete. The dominant issue is maintainability from feature accumulation.

Verified sizes in the uploaded clean repository snapshot:

- `SessionSeriesChangeService.java`: ~1,103 lines
- `VaccinationSessionService.java`: ~809 lines
- `VaccinationCampaignService.java`: ~428 lines
- `OperatingHoursService.java`: ~409 lines

Current MF-02 production Java files under the campaign module: 63.
Current MF-02 test Java files in the flat campaign test package: 65.

### Architectural verdict
Keep the current **layered modular monolith**. Do not perform a wholesale architecture rewrite.

Recommended bounded refactoring targets:
1. `SessionSeriesChangeService`
2. `VaccinationSessionService`
3. duplicated preview/impact/fingerprint/event-construction logic

Refactoring must be behavior-preserving and separated from new lifecycle implementation.

## 8. Production-Layer Gaps

The following are not complete in the uploaded repository snapshot:

- MF-02 REST controllers
- request/response DTOs
- mappers
- versioned `/api/v1` implementation
- Spring Security/RBAC/authentication enforcement
- OpenAPI
- Flyway business migrations
- production MySQL Testcontainers assurance
- MF-02 frontend business screens
- frontend automated tests
- production MF-03 impact adapter
- reliable transactional outbox delivery
- final MF-01 production adapter/FK integration

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

### 9.2 Oversized orchestration services
Large methods/services increase reasoning cost and future regression risk. Refactor in a dedicated behavior-preserving slice.

### 9.3 Optional critical integration ports
The snapshot uses optional injection for:
- `QuestionnaireVersionPublicationPort`
- `SessionImpactReader`

Development/test flexibility is useful, but production-required integrations should ultimately fail fast through explicit production configuration/adapters.

### 9.4 Timing-based concurrency test
`CampaignAssignmentReverseImpactConcurrencyIntegrationTests` still contains `Thread.sleep(300)`. Replace this with deterministic synchronization.

### 9.5 H2-only database concurrency assurance
Keep H2 for fast regression coverage, but add a focused MySQL Testcontainers suite for the highest-risk lock/idempotency paths.

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

### `PUBLISHED -> ACTIVE`
Need authoritative service-start time/date semantics and activation authority.

### `SUSPENDED -> PUBLISHED / ACTIVE`
Need blocking-condition-clearance semantics and target-state/time rule.

### `-> COMPLETED`
Need an operational definition of “service work closed.”

### `-> CANCELLED`
Need reason, dependency/impact policy, truthful MF-03 impact integration, and no silent appointment mutation.

### `COMPLETED/CANCELLED -> ARCHIVED`
Retention duration/policy remains `USER_DATA_REQUIRED`.

### DRAFT campaign / child session scheduling
The separate policy question of whether a child DRAFT session may become SCHEDULED while its parent Campaign remains DRAFT is still unresolved unless later live repository evidence shows otherwise.

## 12. MF-02 Ownership Boundaries

Do not move responsibilities across modules:

- MF-01 owns screening questions, questionnaire versions, eligibility rules, submissions, answers, and eligibility assessments.
- MF-02 owns campaigns, center planning, operating calendars, recurrence, sessions, resources/capacity, schedule changes, and operational utilization.
- MF-03 owns appointments, waitlists, rebooking, and check-in.
- MF-04 owns batches, stock quantities, allocations, and stock movements.
- MF-05 owns vaccination administration records and certificates.

MF-02 may read aggregate impact/readiness information through ports. It must not silently mutate MF-03 appointment/waitlist history.

## 13. Immediate Engineering Order

Do **not** automatically start another lifecycle transition.

Recommended order:
1. synchronize maintained documentation to PR #73 — represented by this step;
2. perform bounded service decomposition for the two oversized orchestration services;
3. strengthen concurrency assurance (remove fixed sleep; add focused MySQL Testcontainers);
4. finish remaining campaign lifecycle transitions;
5. complete MF-01/MF-03 production adapters and outbox boundary;
6. add API/DTO/OpenAPI;
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

## 15. Status Discipline

Never claim:
- MF-02 complete;
- Sprint 1 complete;
- US-13 complete;
- production API/security/migrations/frontend complete;
- MySQL concurrency certified;
- 566 or 575 as the current backend baseline.

Current status remains:

- Sprint 1: **IN PROGRESS**
- MF-02: **IN PROGRESS**
- US-13: **IN PROGRESS**
- current verified backend baseline: **591**

## 16. Startup Procedure for the Next AI Session

1. Read canonical Markdown `3.3-AI / 2026-08-14-R9`.
2. Fetch live `origin/develop`.
3. Verify whether `6d52449db4f29099f3a979e824a7efcae8f4ea54` remains an ancestor/current checkpoint.
4. Run/inspect current backend tests before changing behavior.
5. Read this handoff and Sprint evidence.
6. Review `MF02_Completion_and_Hardening_Plan.md`.
7. Do not rebuild completed foundations.
8. Resolve known consistency/hardening issues before increasing lifecycle scope.

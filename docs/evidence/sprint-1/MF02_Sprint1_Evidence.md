# MF-02 Sprint 1 Evidence Record

**Project:** Web-Based Vaccination Portal (`WBVP-SE2030`)
**Function:** MF-02 — Vaccination Campaign, Center and Session Management
**Canonical authority:** `3.3-AI / 2026-08-14-R9`
**Source authority:** canonical Markdown only
**Sprint:** Sprint 1 — **IN PROGRESS**
**MF-02:** **IN PROGRESS**
**US-13:** **IN PROGRESS**
**Evidence synchronized through implementation:** PR #71
**Latest verified implementation merge:** `6d52449db4f29099f3a979e824a7efcae8f4ea54`
**Last updated:** 2026-08-14

> This is an implementation/verification evidence record. It is not the semantic requirements source of truth. Historical test counts are checkpoint-scoped and must not be presented as the current baseline.

## 1. Current Verification Snapshot

PR #71 — `feat(mf02): add campaign scheduling approval`

- reviewed source: `422fce3768646310448c3c82efec60c654afa0c7`
- base: `c3bf7c2fb03513d8540a78dcc4ab5d93f322f468`
- merge: `6d52449db4f29099f3a979e824a7efcae8f4ea54`
- merged at: `2026-08-14T10:27:54Z`
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
- previous clean baseline: **569**
- net-new executions: **22**

## 2. Test-Baseline Reconciliation

The maintained baseline sequence relevant to the latest lifecycle work is:

| Checkpoint | Authoritative executable tests | Notes |
|---|---:|---|
| PR #61 lifecycle integrity | 523 | historical |
| PR #65 suspension | 554 | historical |
| PR #67 terminal planning guard | 560 | clean-build reconciled; earlier 566 reporting superseded |
| PR #69 SUSPENDED planning freeze | 569 | +9 from clean PR #67 baseline |
| PR #71 campaign scheduling approval | **591** | +22 from PR #69/current pre-PR71 baseline |

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
The scheduling command does not independently require the linked questionnaire version publication-port check. Existing `publishCampaign(...)` still performs the publication requirement.

A follow-up domain-consistency review is required because normal CampaignQuestionnaireAssignment creation already enforces a published questionnaire version; see the maintained handoff/plan.

### Tests added
PR #71 adds 22 executions covering:
- DRAFT -> SCHEDULED domain transition
- non-DRAFT rejection
- same-day/past/future boundaries
- inactive vaccine type / target group
- missing/ambiguous dose programme
- missing questionnaire assignment
- scheduling/publication questionnaire boundary
- zero/cancelled/non-overlapping center assignments
- inclusive/half-open overlap behavior
- inactive/mixed centers
- non-overlapping inactive center not blocking
- concurrent scheduling serialization

## 6. Concurrency / Transaction Evidence

Current high-risk workflows use transactional boundaries and explicit pessimistic locking where needed.

Representative established lock orders:
- campaign-owned configuration: Campaign
- center-owned calendar writes: Center
- occurrence generation: Campaign -> Center -> Rule
- same-center session change: Campaign -> Center -> Session
- center transfer: Campaign -> Center(min) -> Center(max) -> Session
- series confirmation: Campaign -> Center -> Rule -> Sessions sorted by ID
- campaign scheduling: Campaign

Separate-transaction concurrency regression tests exist for real race conditions.

### Limitation
Most integration/concurrency evidence runs on H2 in MySQL compatibility mode. This is structural regression evidence, not production MySQL certification. A focused MySQL Testcontainers suite remains required for production-grade confidence.

## 7. Historical Milestone Chronology

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

## 8. Carried-Forward Findings / Risks

### Domain consistency
Normal questionnaire assignment creation requires a published version, while one PR #71 test models an unpublished assignment through direct persistence and treats it as schedulable. Reconcile this before more lifecycle growth.

### Maintainability
Verified current snapshot:
- `SessionSeriesChangeService`: ~1,103 lines
- `VaccinationSessionService`: ~809 lines

Refactor separately from feature work.

### Optional critical ports
Production-facing adapters for questionnaire publication and MF-03 session impact are still optional-injection boundaries in the current code. Production configuration should ultimately fail fast when required adapters are absent.

### Concurrency test quality
At least one existing concurrency regression still uses `Thread.sleep(300)` and should be rewritten with deterministic synchronization.

### Audit deletion
Lifecycle/change event repositories inherit normal delete APIs, so “append-only” is not deletion-proof persistence.

## 9. Remaining Sprint / Function Work

Still not complete:
- remaining campaign lifecycle transitions: ACTIVE, resume, completion, campaign cancellation, archival
- archival retention value (`USER_DATA_REQUIRED`)
- DRAFT parent-campaign / child-session scheduling policy
- MF-01 production adapter/FK
- MF-03 production impact adapter
- transactional outbox
- REST controllers/DTO/mappers
- `/api/v1` alignment
- Spring Security/RBAC
- OpenAPI
- Flyway migrations
- production MySQL concurrency assurance
- MF-02 frontend
- frontend tests
- final end-to-end / Definition-of-Done evidence

## 10. Sprint Status

Current status remains:
- Sprint 1: **IN PROGRESS**
- MF-02: **IN PROGRESS**
- US-13: **IN PROGRESS**

Do not infer completion from the size of the backend test suite.

## 11. Evidence Maintenance Rules

1. Canonical Markdown is the sole semantic authority.
2. Live Git/code/tests control implementation truth.
3. A documentation-only merge must not be mislabeled as a feature implementation checkpoint.
4. Historical test counts must remain tied to their historical checkpoints.
5. Clean-build counts supersede non-clean/transient counts.
6. Future evidence updates should append concise milestone entries rather than embedding controller transcripts.
7. Before claiming “current,” fetch live `origin/develop`.

# Web-Based Vaccination Portal — Current Architecture and File Tree

This document describes the implemented ownership layout on the architecture refactor branch.
The older canonical specification and MF handoffs remain business/reference history; their
technical package examples are superseded by this document. Do not pre-create placeholder files.
The application remains a modular monolith with one database and existing HTTP contracts.

## Backend ownership

```text
backend/src/main/java/com/se2030/vaccinationportal/
├── appointment/
│   ├── api/{query,vaccination}
│   ├── booking/{service,dto,web}
│   ├── waitlist/{service,dto,web}
│   ├── checkin/{service,dto,adapter}
│   ├── query/{service,adapter}
│   ├── persistence/{model,repository}
│   ├── exception
│   └── web
├── campaign/
│   ├── api/{appointment,availability,capacity,questionnaire,reference,screening,session,staff,vaccination}
│   ├── core/{model,repository,service,dto,web,query}
│   ├── reference/{model,repository,service,dto,web,adapter}
│   ├── center/{model,repository,service,dto,web,query}
│   ├── questionnaire/{model,repository,service,dto,web}
│   ├── session/
│   │   ├── planning/{model,repository,service,dto,web,adapter}
│   │   └── recurrence/{model,repository,service,dto,web}
│   ├── analytics/{dto,service,web}
│   ├── exception
│   └── web/{dto/request,dto/response}
├── screening/
│   ├── api/{eligibility,query}
│   ├── questionnaire/{model,repository,service,dto,web}
│   ├── submission/{model,repository,service,dto,mapper,web}
│   ├── eligibility/{model,repository,service,dto,mapper,adapter,web}
│   ├── correction/{service,dto,web}
│   ├── query/{service,mapper,dto}
│   ├── exception
│   └── validation
├── vaccination/
│   ├── administration/{model,repository,service,dto,mapper,exception,integration,web}
│   ├── certificate/{model,repository,service,dto,mapper,exception,web}
│   ├── query/{model,service,dto}
│   ├── validation
│   └── web
├── inventory/{controller,service,repository,model,dto,dependency,exception}
├── assistant/{config,controller,dto,exception,policy,provider,service,tool}
└── shared/auth/{api,query,bootstrap,controller,dto,exception,model,repository,security,service}
```

The tree shows capability roots and relevant child areas, not every class. A capability may also
have mapper, validation or integration children where its existing workflow requires them.
Production providers live in capability `query` or `adapter` packages, outside public `api`.
Inventory's existing `dependency` package is its public batch contract, not a generic shared bucket.

## Frontend ownership

```text
frontend/
├── eslint/{featureBoundaries.js,featureBoundaries.test.js}
└── src/
    ├── app/{App.jsx,AuthenticatedLayout.jsx,ProtectedRoute.jsx,routes.jsx,pages}
    ├── auth/{api,components,constants,context,pages,utils}
    ├── components/common
    ├── features/
    │   ├── analytics
    │   ├── appointment-management/{api,components,pages,citizen-appointment.css}
    │   ├── assistant
    │   ├── campaign-management
    │   ├── center-operations
    │   ├── citizen-dashboard
    │   ├── citizen-profile
    │   ├── inventory-management/{api,hooks,pages,utils}
    │   ├── public-landing
    │   ├── screening/{api,components,hooks,pages,utils,citizen-screening.css}
    │   ├── session-management/{api,components,hooks,pages}
    │   └── vaccination-administration
    ├── shared/
    │   ├── api/{apiClient.js,readApiErrorDetails.js}
    │   ├── auth/authStorage.js
    │   ├── time/timezoneUtils.js
    │   ├── assets/citizen-health-banner.png
    │   └── ui/citizen/{CitizenPageHeader, CitizenErrorState, CitizenStatePanel,
    │                   CitizenStatusBadge, CitizenWorkflowGuide, citizen-ui.css}
    └── styles
```

Tests follow their components/capabilities. The removed `features/citizen-ui` and `src/services`
areas have no replacement compatibility shims. `CitizenSection` belongs to Screening.
Auth owns login/session workflows; shared auth storage only owns browser token infrastructure.

## Feature dependency direction

```text
Assistant -> Appointment.api.query -> Campaign.api.appointment
Assistant -> Screening.api.query
Assistant -> Campaign.api.staff
Assistant -> DigitalCertificateService / CitizenProfileService
Appointment -> Campaign capacity, availability, display, vaccination-read APIs
Appointment -> Screening.api.eligibility
Inventory -> Campaign.api.reference
Screening -> Campaign.api.screening + Shared Auth AccountDisplayReader
Vaccination -> Appointment.api.vaccination + Inventory.dependency
```

Campaign owns `QuestionnaireVersionPublicationPort` and `SessionImpactReader`; Screening and
Appointment implement them respectively. These are inverted runtime collaborations, not Campaign
imports of consumer implementations. Campaign has no Java dependency on Appointment.
The retained Assistant application services are intentionally appropriate public use cases.
Shared backend infrastructure has no business-feature dependency.

## Persistence and transaction boundaries

Java access to another feature's repositories or JPA entities is prohibited. Immutable query
snapshots contain facts, not entities or callbacks. There is one explicit persistence-query exception:
`AppointmentRepository.countBySessionIdAndStatusIn` joins Campaign `TimeSlot` inside a single JPQL
statement for the Campaign-owned session-impact count. It preserves the existing count consistency
under session locks; replacing it with two separate reads would change that contract.

Booking retains the citizen/campaign guard, eligibility and duplicate checks before capacity
reservation. Waitlist FIFO, mutual exclusion and promotion behavior are unchanged. Campaign capacity
keeps campaign -> session -> slot lock order. Session-change preview/confirmation tokens remain
feature-owned. Vaccination finalization remains one transaction for record creation, Inventory
stock/audit deduction, Appointment completion, certificate issuance and draft finalization.
No new transaction, independent propagation, lock or schema is introduced by ownership moves.
Existing finalization does not create a subsequent-dose draft; the refactor does not add that rule.

`EffectiveVaccinationRecordService.resolve` is public across Vaccination capability packages.
Its body is unchanged; the existing Spring transaction attribute source already recognized its
previous package-visible annotation. It is not a new transaction boundary.

## Assistant timing and principal scoping

Appointment, Eligibility and Staff queries derive identity from `UserPrincipal`. User IDs typed
in prompts cannot select another account. Appointment reads retrieve rows and bulk slot facts,
then sample the existing Clock once and classify. Combined reads retrieve waitlist base rows
before classification and perform queue counts afterward. Eligibility samples after retrieval
and latest-per-campaign reduction. Five deterministic regressions protect those timing boundaries.
Providers expose no clinical notes or repository-shaped state. Tool tests mock provider queries;
provider tests exercise repositories; persisted Spring tests verify the production graph and isolation.

## Frontend workflow and styling boundaries

`useSessionRecurrenceWorkflow`, `useSessionChangeControl`, `useQuestionnaireBuilder` and
`useInventoryManagement` own their respective feature orchestration. View files retain JSX,
wording and accessibility. Request generations, stale-response guards and confirmation/mutation
serialization remain together in the owning workflow. Business API functions stay feature-owned.

Shared citizen components explicitly import their shared stylesheet. Appointment and Screening
styles stay in their features. The shared campaign-card base plus Dashboard override remains an
intentional cascade relationship; consider its order when changing styles. Shared assets are
reused directly without deep imports into another feature.

## Architecture enforcement

`backend/src/test/java/com/se2030/vaccinationportal/architecture/FeatureBoundaryTests.java`
uses Spring's existing ASM dependency to inspect compiled types, signatures, annotations, fields,
calls, lambdas and record components. It checks repository/entity isolation, API purity, shared
direction, Assistant seams, Campaign -> Appointment absence, internal services and the explicit
JPQL exception. No new test dependency was added. Reflection, arbitrary string class names and
native SQL are not proven by bytecode dependency scanning and still require review.

`frontend/eslint/featureBoundaries.js` resolves relative imports, exports and literal dynamic
imports. Production shared code cannot depend on business features/Auth workflows; features use
another feature's APIs/constants or the small explicit list of provider-owned presentation entry
points. Route composition belongs in `app`. Test fixtures are excluded from production boundary
rules and the rule itself has valid/invalid RuleTester coverage. Computed dynamic module paths
are outside static resolution; none are needed by the current production graph.

## Migrations and validation

Active Flyway location: `backend/src/main/resources/db/migration` (`classpath:db/migration`).
There are 22 applied-history scripts, V0 through V21. Their contents and schema are unchanged.
`database/migrations` contains historical reference scripts, not an additional Flyway location;
see [database guide](../database/README.md). Do not apply them in addition to runtime migrations.

From `backend`, run `./mvnw test` (Windows: `.\mvnw.cmd test`) and compile/test-compile.
Run the existing `mysql-concurrency` profile when Docker is available for real MySQL migration
and locking checks. From `frontend`, run `npm test`, `npm run lint` and `npm run build`.
Run `git diff --check` and `git diff --cached --check` before committing an ownership milestone.
See [contributing](../CONTRIBUTING.md) for the development workflow.

## Refactor invariant audit

Compared with starting commit `975c039e510a5f0630fc2223bfe17b004f40a9ed`:

| Production evidence | Starting | Final | Result |
| --- | ---: | ---: | --- |
| Direct cross-feature repository imports | 4 | 0 | Removed through provider ownership |
| Direct cross-feature JPA entity imports | 2 | 0 | Removed; enum/value imports are not counted as entities |
| Cross-feature service-package imports | 8 | 2 | Three old entries were contracts/results, not implementations; two retained application services remain |
| Transaction annotations | 225 | 225 | Same annotation values |
| Lock annotations | 28 | 28 | Same annotation values |
| HTTP mapping annotations | 246 | 246 | Same routes/method mappings |
| Authorization annotations | 173 | 173 | Same policy values |
| JPA mapping annotations checked | 802 | 802 | Same table/relationship/column/index values |

Booking, waitlist join/promotion, capacity and finalization bodies are unchanged after package/import
normalization. Session change/series preview services are preserved by the Campaign move comparisons.
SecurityConfig, JwtAuthenticationFilter and UserPrincipal are unchanged; all runtime resources,
active Flyway scripts, frontend app route files and dependency manifests are byte-identical to the
starting revision. Provider wiring and entity/repository scanning are exercised by real Spring tests.
These counts complement behavioral/integration tests; annotation equality alone is not a proof of
all runtime behavior. Reflection/native SQL limitations and the named JPQL exception remain explicit.

The earlier provider-next-to-interface package debt and repository-mocked Assistant tool test seam
are resolved: providers now live under implementation capabilities, and unit/provider/integration
responsibilities are separated. The existing public presentation allowlist and SessionImpact join
are retained architectural exceptions rather than unfinished package moves.

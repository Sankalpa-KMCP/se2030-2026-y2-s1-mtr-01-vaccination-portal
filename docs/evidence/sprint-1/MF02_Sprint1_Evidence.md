# MF-02: Vaccination Campaign, Center and Session Management — Sprint 1 Evidence

- **Canonical Project:** `WBVP-SE2030`
- **Canonical Version:** `3.3-AI`
- **Canonical Revision:** `2026-08-08-R8`
- **Major Function:** `MF-02 — Vaccination Campaign, Center and Session Management`
- **Current Sprint:** `Sprint 1`
- **Sprint 1 Status:** `IN PROGRESS`
- **Latest Verified Develop Documentation Checkpoint:** `bd22cb973bad83c8456897805bc2df95b65593d0`
- **Previous Backend Implementation Checkpoint:** `201768ecd7efb01f4d008db6174c34771edbb6e7`

---

## 1. Executive Summary & Sprint Status

Sprint 1 is actively **IN PROGRESS**. The foundational domain models, persistence repositories, concurrency-hardened service layers, and documentation synchronization for **MF-02 (Vaccination Campaign, Center, and Session Management)** have been implemented, verified, and merged into `develop`.

### Status Matrix

| Component / Slice | Implementation Status | Integration Checkpoint | Verification |
| :--- | :--- | :--- | :--- |
| **Canonical DOCX Sync (PR #17)** | **COMPLETED & MERGED** | `bd22cb9` (Merge commit) | CI passed (Backend & Frontend) |
| **Campaign Master & Lifecycle Models** | **COMPLETED & MERGED** | `eb4a4d9` (PR #12) / `201768e` (PR #16) | Unit & JPA Tests passing |
| **Dose-Programme Versioning & Safety** | **COMPLETED & MERGED** | `201768e` (PR #16) | Concurrency & Boundary Tests passing |
| **Location Target Hierarchy** | **COMPLETED & MERGED** | `eb4a4d9` (PR #12) | Persistence & Hierarchy Tests passing |
| **Questionnaire Assignment Service** | **COMPLETED & MERGED** | `201768e` (PR #16) | Concurrency & Safety Tests passing |
| **Vaccination Center Master & Service** | **COMPLETED & MERGED** | `201768e` (PR #16) | Persistence & Lifecycle Tests passing |
| **Center Operating Hours (`OperatingHours`)** | **NOT STARTED** | Pending next feature slice | Pending |
| **Center Closures (`CenterClosure`)** | **NOT STARTED** | Pending roadmap slice | Pending |
| **Campaign-Center Assignment** | **NOT STARTED** | Pending roadmap slice | Pending |
| **Session & Capacity Engine** | **NOT STARTED** | Pending roadmap slice | Pending |
| **Campaign Publication Transition Engine** | **NOT STARTED** | Pending roadmap slice | Pending |
| **MF-02 REST Controllers & DTOs** | **NOT STARTED** | Pending web layer slice | Pending |

---

## 2. Documentation Integration & Canonical Synchronization (PR #17)

The canonical human-readable master specification was synchronized and merged into `develop` through **Pull Request #17**.

- **PR Number:** `#17`
- **PR URL:** [https://github.com/Sankalpa-KMCP/se2030-2026-y2-s1-mtr-01-vaccination-portal/pull/17](https://github.com/Sankalpa-KMCP/se2030-2026-y2-s1-mtr-01-vaccination-portal/pull/17)
- **Base Branch:** `develop`
- **Head Branch:** `docs/sync-canonical-master-r8`
- **Merge Commit on `develop`:** `bd22cb973bad83c8456897805bc2df95b65593d0`
- **Merge Status:** `MERGED`
- **Automated CI Status:**
  - `Backend CI`: **SUCCESS**
  - `Frontend CI`: **SUCCESS**

### Commits Included in PR #17

1. `8a26fe6` — `docs: add latest synced version of vaccination portal AI canonical documentation`
2. `16d4193` — `docs: restore canonical master filename`

### Verification & File Integrity Checks

- **Canonical Master DOCX Path:** `docs/Web_Based_Vaccination_Portal_AI_Canonical_Master_v3.3.docx` (present and verified at standard canonical path).
- **Duplicate Removal:** `docs/Web_Based_Vaccination_Portal_AI_Canonical_Master_v3.3_R8_SYNCED.docx` is completely absent.
- **Authoritative Canonical Markdown:** `docs/Web_Based_Vaccination_Portal_AI_Canonical_Context_v3.3.md` remained 100% unchanged by PR #17.
- **Scope Boundary:** PR #17 contained strictly the single intended canonical DOCX file update (`1 file changed, 0 insertions(+), 0 deletions(-)`), with zero changes to application code, tests, or configurations.

---

## 3. MF-02 Backend Implementation & Hardening History (49 Commits)

The MF-02 backend foundations comprise **49 commits** merged into `develop` across **PR #12** (`eb4a4d9`) and **PR #16** (`201768e`).

### A. Campaign & Versioning Foundations (PR #12 — 27 Commits)

- **Test Infrastructure:** Isolated test schema lifecycle configuration (`3212129`).
- **Campaign Domain & Master Data:**
  - `CampaignStatus` model (`844ba27`)
  - `VaccineType` master entity (`292494b`)
  - `TargetGroup` domain entity (`1944f88`)
  - `VaccinationCampaign` domain entity (`c9027fd`)
  - Persistence repositories and tests (`5813adf`, `3bc70c2`)
- **Dose Programme Domain & Service:**
  - `DoseCategory` model and tests (`9e0b4a1`, `e799b43`)
  - `CampaignDoseProgramme` model and validation (`f807ce4`, `9095012`)
  - Dose programme repository (`2a8e925`)
  - Effective dose programme versioning service and persistence tests (`c14a5c3`, `8473409`)
- **Location Target Domain & Service:**
  - `LocationTargetType` model and tests (`fbc6b67`, `6361ed7`)
  - `CampaignLocationTarget` model and validation (`cc8da25`, `f902e5d`)
  - Location target repository (`ec169ad`)
  - Administrative service area targeting service and tests (`b2c9888`, `73a3246`)
- **Questionnaire Assignment Domain & Service:**
  - Cross-module `QuestionnaireVersionPublicationPort` interface (`c6c2ae3`)
  - `CampaignQuestionnaireAssignment` model and validation (`08354de`, `554bd9c`)
  - Questionnaire assignment repository (`a7bac6a`)
  - Effective questionnaire assignment versioning service and tests (`71ec4fe`, `f34ecb2`)

### B. Vaccination Center Management & Concurrency Hardening (PR #16 — 22 Commits)

- **Vaccination Center Master Data & Service:**
  - `CenterStatus` model (`ACTIVE`, `INACTIVE`) and unit tests (`336301f`, `2a01e91`)
  - `VaccinationCenter` domain entity with unique center code and capacity constraints (`c6d9e91`, `db9d004`)
  - `VaccinationCenterRepository` with active center querying (`e7e3d5b`)
  - `VaccinationCenterService` lifecycle management and persistence tests (`0739418`, `d4441e9`)
- **Dose-Programme Null-Date Safety & Clock Integration:**
  - Guard effective start and end dates against null values (`089e453`, `150f6b4`)
  - Provide deterministic `Clock` bean in Spring application context (`4b7ead6`)
- **Future-Only Replacement Invariants:**
  - Enforce `MF02-BR-18` future-only replacement on dose programme versions (`9aac0a1`, `fd5235d`)
  - Enforce `MF02-BR-17` future-only replacement on questionnaire assignments (`c3aac3a`, `2b41129`)
- **Pessimistic Row Locking & Concurrency Serialization:**
  - Add locked campaign lookup `findByIdForUpdate` with `LockModeType.PESSIMISTIC_WRITE` (`e1358a3`, `c1047f7`)
  - Serialize dose-programme configuration changes against concurrent creation/replacement races (`4cc3339`)
  - Serialize questionnaire-assignment configuration changes against concurrent race conditions (`f7121ac`)
- **Persistence Safety & Semantics Clarification:**
  - Corrected test assertion semantics for rejected questionnaire assignment safety (`4d33729`)
- **Documentation & Invariant Clarification:**
  - Clarify effective configuration invariants vs publication prerequisites for `MF02-BR-02` (`9452e9c`, `154824c`)
  - Updated backend `README.md` implementation status (`2655866`)

---

## 4. Test & Verification Evidence

All 97 unit and persistence integration tests execute cleanly with zero failures or errors under both standard test runs and complete build verification passes.

```text
Results:

Tests run: 97, Failures: 0, Errors: 0, Skipped: 0

[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

### Verified Test Suites

- `HealthEndpointIntegrationTests`: Actuator health check verification
- `VaccinationPortalApplicationTests`: Context load verification
- `CampaignStatusTests`, `CenterStatusTests`, `DoseCategoryTests`, `LocationTargetTypeTests`: Enum model integrity
- `VaccinationCampaignModelTests`, `CampaignPersistenceIntegrationTests`: Campaign lifecycle & persistence
- `CampaignDoseProgrammeModelTests`, `CampaignDoseProgrammePersistenceIntegrationTests`: Dose programme versioning, non-overlapping intervals, future-only replacements, and date safety
- `CampaignLocationTargetModelTests`, `CampaignLocationTargetPersistenceIntegrationTests`: Location target validation, hierarchy matching, and persistence
- `CampaignQuestionnaireAssignmentModelTests`, `CampaignQuestionnaireAssignmentPersistenceIntegrationTests`: Questionnaire assignment versioning, publication port validation, future-only replacements, and safety
- `VaccinationCenterModelTests`, `VaccinationCenterPersistenceIntegrationTests`: Center code uniqueness, status transitions, ceiling constraints, and persistence lifecycle

---

## 5. Business Rule Invariant Alignment

### MF02-BR-02: Configuration History Invariant vs. Publication Prerequisite

1. **Configuration History Invariant (Draft / Ongoing Configuration):**
   - A campaign may temporarily have zero dose programmes or zero questionnaire assignments while in draft mode.
   - At most one dose programme and at most one questionnaire assignment can be effective for any specific campaign date.
   - Effective date intervals (`effectiveFrom` to `activeTo`) must never overlap.
2. **Publication Prerequisite Boundary:**
   - Transition to `PUBLISHED` or `SCHEDULED` strictly requires **exactly one** effective dose programme version and **exactly one** effective questionnaire assignment referencing a **published** questionnaire version.

### MF02-BR-17 & MF02-BR-18: Future-Only Replacements

- Replacing an currently active dose programme or questionnaire assignment requires the replacement to have `effectiveFrom > today`.
- Historical records are bounded by setting `activeTo = replacement.effectiveFrom - 1 day` without retroactive mutation or hard deletion.

### Concurrency Protection (Section 12.4)

- Modifications to campaign dose programmes and questionnaire assignments acquire a pessimistic write lock (`LockModeType.PESSIMISTIC_WRITE`) on the parent `VaccinationCampaign` record before querying or mutating effective version history, preventing concurrent race conditions.

---

## 6. Cross-Module Boundaries

- **MF-01 Screening & Eligibility Boundary:**
  - MF-01 owns medical questionnaires and rules.
  - MF-02 references questionnaire versions via `CampaignQuestionnaireAssignment`.
  - The decoupled interface `QuestionnaireVersionPublicationPort` verifies questionnaire publication status until direct entity integration is introduced.

---

## 7. Planned Roadmap & Pending Slices (Sprint 1 Remaining)

The following backend and frontend areas remain to be implemented in upcoming logical slices of Sprint 1:

1. **Operating Hours Slice (`OperatingHours`):**
   - Status: **NOT STARTED**
   - Scope: Daily operating schedules per center, day of week, open/close time intervals, default slot duration, and concurrent capacity.
2. **Center Closures Slice (`CenterClosure`):**
   - Status: **NOT STARTED**
   - Scope: Temporary closure date intervals, closure reason documentation, and booking override logic.
3. **Campaign-Center Assignment Slice (`CampaignCenterAssignment`):**
   - Status: **NOT STARTED**
   - Scope: Linking campaigns to authorized centers with center-specific campaign capacity allocation.
4. **Session Management & Capacity Planning:**
   - Status: **NOT STARTED**
   - Scope: Concrete daily session scheduling and appointment capacity generation.
5. **Campaign State Transition Engine:**
   - Status: **NOT STARTED**
   - Scope: Strict lifecycle transition checks (`DRAFT -> SCHEDULED -> PUBLISHED -> ACTIVE -> SUSPENDED -> COMPLETED -> CANCELLED -> ARCHIVED`).
6. **REST API & Controller Layer:**
   - Status: **NOT STARTED**
   - Scope: Versioned REST endpoints under `/api/v1/campaigns` and `/api/v1/centers`, DTO request/response schemas, validation handlers.
7. **Shared Platform Infrastructure:**
   - Status: **NOT STARTED**
   - Scope: Flyway database migrations, Spring Security with RBAC, and OpenAPI / Swagger documentation.

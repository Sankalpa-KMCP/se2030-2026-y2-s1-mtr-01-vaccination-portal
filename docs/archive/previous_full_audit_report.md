# LITERAL REPOSITORY FACT PACK

> Historical specification/evidence: package trees, implementation status and migration counts below describe their original revision. Current ownership and setup are recorded in the [current architecture](../Web_Based_Vaccination_Portal_Recommended_Full_File_Tree.md). Applied runtime Flyway history is V0–V21 under `backend/src/main/resources/db/migration`; reference scripts are not runtime migrations. Business requirements remain reference material.

**Repository Target:** `develop` @ `227d42d08f51194d2004cc651e419273f30db194`  
**Working Tree Status:** Clean (HEAD verified unaltered)  
**Evidence Standard:** Strict Source Code Inspection (`Java`, `JSX/JS`, `SQL`, `XML`, `JSON`, `YAML`)

---

## A. Technology

* **Backend Java version:** Java 21  
  * FILE: [`backend/pom.xml`](file:///e:/SE%20PROJECT_Vac/se2030-2026-y2-s1-mtr-01-vaccination-portal/backend/pom.xml#L18)  
  * SYMBOL: `<java.version>21</java.version>`  
  * LITERAL FACT: Configured for Java 21.  
  * CONFIDENCE: VERIFIED
* **Spring Boot version:** 4.1.0  
  * FILE: [`backend/pom.xml`](file:///e:/SE%20PROJECT_Vac/se2030-2026-y2-s1-mtr-01-vaccination-portal/backend/pom.xml#L8)  
  * SYMBOL: `<parent><version>4.1.0</version></parent>` under `<artifactId>spring-boot-starter-parent</artifactId>`  
  * LITERAL FACT: Configured with starter parent version 4.1.0.  
  * CONFIDENCE: VERIFIED
* **Frontend React version:** ^19.2.8  
  * FILE: [`frontend/package.json`](file:///e:/SE%20PROJECT_Vac/se2030-2026-y2-s1-mtr-01-vaccination-portal/frontend/package.json#L16-L17)  
  * SYMBOL: `"react": "^19.2.8"`, `"react-dom": "^19.2.8"`  
  * LITERAL FACT: Uses React 19.2.8.  
  * CONFIDENCE: VERIFIED
* **Frontend language actually used in `src/`:** JavaScript (JSX / JS)  
  * FILE: [`frontend/src/`](file:///e:/SE%20PROJECT_Vac/se2030-2026-y2-s1-mtr-01-vaccination-portal/frontend/src)  
  * SYMBOL: File system inventory: 214 `.jsx` files, 51 `.js` files, 0 `.ts` files, 0 `.tsx` files.  
  * LITERAL FACT: Codebase implementation in `frontend/src/` is pure JSX/JavaScript.  
  * CONFIDENCE: VERIFIED
* **Styling libraries actually installed/used:** Bootstrap 5.3.8 & Bootstrap Icons 1.13.1  
  * FILE: [`frontend/package.json`](file:///e:/SE%20PROJECT_Vac/se2030-2026-y2-s1-mtr-01-vaccination-portal/frontend/package.json#L14-L15)  
  * SYMBOL: `"bootstrap": "^5.3.8"`, `"bootstrap-icons": "^1.13.1"`  
  * LITERAL FACT: Styling is provided by Bootstrap 5 and Bootstrap Icons. Tailwind CSS is N
<truncated 57682 bytes>
tmentId: Long`. |
| **Waitlist `priorityScore`** | **FALSE** | Does not exist; waitlist candidate ordering is strict **FIFO** by `joined_at` ASC, then `id` ASC. |
| **Vaccine temperature thresholds** | **FALSE** | Does not exist; `VaccineBatch` contains no storage temperature min/max fields. |
| **Adverse reaction tracking** | **FALSE** | Does not exist; no adverse reaction fields or workflows exist on vaccination drafts or records. |
| **Automatic preliminary ELIGIBLE result** | **FALSE** | Rule evaluation produces only `PRELIMINARY_PASS` or `PRELIMINARY_REVIEW_REQUIRED`. `ELIGIBLE` status is set only by human clinical review. |
| **`QuestionnaireVersion` $\rightarrow$ `ScreeningQuestion` direct composition** | **FALSE** | Relationship occurs strictly through **`QuestionnaireItem`**. |
| **`VaccinationRecord.immutableChecksum`** | **FALSE** | Does not exist on `VaccinationRecord`. |
| **Certificate UUID field** | **FALSE** | Certificate has `certificateReference` (`"CERT-" + UUID`) and `verificationToken` (64-char hex string), but no `certificateUuid` field. |
| **1:1 `VaccinationRecord` $\rightarrow$ `DigitalCertificate`** | **FALSE** | Relationship is **1:N** across reissue/supersession lifecycle, constrained to at most one `VALID` certificate via MySQL stored column. |
| **Sprint 1 velocity ~18 story points** | **FALSE** | Completely undocumented in evidence artifacts. Velocity and story points are **UNKNOWN**. |

---

## L. Remaining Unknowns

1. **Formal 5-Function Academic Approval:** Written confirmation or meeting record from the academic supervisor/module leader formally authorizing the 5-Major-Function modular structure.
2. **Sprint Calendar Dates (Sprints 1â€“4):** Exact calendar start and end dates for Sprints 2, 3, and 4 (Sprint 1 merge is confirmed as 2026-08-26).
3. **Student Registration Number for Member 4:** Official SLIIT student ID for Shabeeha Miftha.
4. **Planned vs Achieved Velocity:** Formal Scrum velocity and story point burn-down figures, which are completely absent from the repository.

# Web-Based Vaccination Portal - Recommended Full File Tree

**Canonical project:** `WBVP-SE2030`  
**Canonical version:** `3.3-AI`  
**Aligned canonical revision:** `2026-08-08-R8`  
**Aligned handoff family:** `2026-08-08-H9`  
**Document role:** Recommended target/reference structure; not an instruction to pre-create every file.

> The live repository is authoritative for what already exists. Before creating or moving files, inspect the repository and the relevant MF handoff. Create only the files required for the current logical slice.
>
> Canonical ownership beats folder convenience. In particular, MF-02 owns `VaccineType`; MF-04 references it. MF-05 backend code belongs under `administration/`, not `vaccination/`.

```text
se2030-2026-y2-s1-mtr-01-vaccination-portal/
|
|-- README.md
|-- CONTRIBUTING.md
|-- LICENSE
|-- .gitignore
|-- .editorconfig
|-- docker-compose.yml
|
|-- .github/
|   |-- ISSUE_TEMPLATE/
|   |   `-- user-story.md
|   |-- pull_request_template.md
|   `-- workflows/
|       |-- backend-ci.yml
|       `-- frontend-ci.yml
|
|-- docs/
|   |-- Web_Based_Vaccination_Portal_AI_Canonical_Context_v3.3.md
|   |-- Web_Based_Vaccination_Portal_AI_Canonical_Master_v3.3.docx
|   |-- Web_Based_Vaccination_Portal_Recommended_Full_File_Tree.md
|   |-- handoffs/
|   |   |-- MF01_Pre_Vaccination_Screening_and_Eligibility_Rule_Management_AI_Handoff.md
|   |   |-- MF02_Vaccination_Campaign_Center_and_Session_Management_AI_Handoff.md
|   |   |-- MF03_Appointment_Waitlist_and_Check_In_Management_AI_Handoff.md
|   |   |-- MF04_Vaccine_Inventory_and_Batch_Management_AI_Handoff.md
|   |   `-- MF05_Vaccination_Administration_Draft_Records_and_Digital_Certificate_Management_AI_Handoff.md
|   |-- assessment/
|   |   `-- IT2030.docx
|   |-- architecture/
|   |   |-- system-architecture.md
|   |   |-- frontend-architecture.md
|   |   |-- backend-architecture.md
|   |   |-- database-architecture.md
|   |   `-- api-conventions.md
|   |-- api/
|   |   |-- shared-access-api.md
|   |   |-- mf01-screening-api.md
|   |   |-- mf02-campaign-api.md
|   |   |-- mf03-appointment-api.md
|   |   |-- mf04-inventory-api.md
|   |   `-- mf05-administration-api.md
|   |-- decisions/
|   |   |-- README.md
|   |   `-- architecture-decisions.md
|   `-- evidence/
|       |-- README.md
|       |-- sprint-1/
|       |-- sprint-2/
|       |-- sprint-3/
|       `-- sprint-4/
|
|-- frontend/
|   |-- README.md
|   |-- package.json
|   |-- package-lock.json
|   |-- vite.config.js
|   |-- eslint.config.js
|   |-- index.html
|   |-- .env.example
|   |-- public/
|   `-- src/
|       |-- main.jsx
|       |-- app/
|       |   |-- App.jsx
|       |   |-- router.jsx
|       |   `-- providers.jsx
|       |-- assets/
|       |-- components/
|       |   `-- common/
|       |       |-- Header.jsx
|       |       |-- Footer.jsx
|       |       |-- LoadingSpinner.jsx
|       |       |-- ErrorAlert.jsx
|       |       |-- SuccessAlert.jsx
|       |       |-- ConfirmDialog.jsx
|       |       |-- EmptyState.jsx
|       |       |-- Pagination.jsx
|       |       |-- SearchInput.jsx
|       |       `-- StatusBadge.jsx
|       |-- layouts/
|       |   |-- PublicLayout.jsx
|       |   |-- CitizenLayout.jsx
|       |   |-- StaffLayout.jsx
|       |   `-- AdminLayout.jsx
|       |-- services/
|       |   `-- apiClient.js
|       |-- hooks/
|       |   |-- useApi.js
|       |   `-- usePagination.js
|       |-- constants/
|       |   |-- apiRoutes.js
|       |   `-- appConstants.js
|       |-- utils/
|       |   |-- dateUtils.js
|       |   |-- formatUtils.js
|       |   `-- validationUtils.js
|       |-- styles/
|       |   `-- global.css
|       `-- features/
|           |-- access/                                      # shared supporting function
|           |   |-- api/
|           |   |-- components/
|           |   |-- pages/
|           |   |-- hooks/
|           |   `-- validation/
|           |
|           |-- screening/                                   # MF-01
|           |   |-- api/
|           |   |   `-- screeningApi.js
|           |   |-- components/
|           |   |   |-- ScreeningQuestionForm.jsx
|           |   |   |-- QuestionnaireBuilder.jsx
|           |   |   |-- ScreeningForm.jsx
|           |   |   |-- PreliminaryResultCard.jsx
|           |   |   |-- EligibilityDecisionForm.jsx
|           |   |   `-- EligibilityStatusCard.jsx
|           |   |-- pages/
|           |   |   |-- ScreeningConfigurationPage.jsx
|           |   |   |-- QuestionnaireManagementPage.jsx
|           |   |   |-- CitizenScreeningPage.jsx
|           |   |   |-- EligibilityReviewQueuePage.jsx
|           |   |   `-- EligibilityHistoryPage.jsx
|           |   |-- hooks/
|           |   `-- validation/
|           |
|           |-- campaign-management/                         # MF-02
|           |   |-- api/
|           |   |   |-- campaignApi.js
|           |   |   |-- centerApi.js
|           |   |   `-- sessionApi.js
|           |   |-- components/
|           |   |   |-- CampaignForm.jsx
|           |   |   |-- CampaignTable.jsx
|           |   |   |-- DoseProgrammeForm.jsx
|           |   |   |-- QuestionnaireSelector.jsx
|           |   |   |-- CenterForm.jsx
|           |   |   |-- OperatingHoursForm.jsx
|           |   |   |-- CenterClosureForm.jsx
|           |   |   |-- CampaignCenterAssignmentForm.jsx
|           |   |   |-- RecurrenceRuleForm.jsx
|           |   |   |-- SessionForm.jsx
|           |   |   |-- ResourceAssignmentForm.jsx
|           |   |   |-- CapacityPlanPanel.jsx
|           |   |   `-- SessionChangeImpactDialog.jsx
|           |   |-- pages/
|           |   |   |-- CampaignManagementPage.jsx
|           |   |   |-- CampaignDetailsPage.jsx
|           |   |   |-- CenterManagementPage.jsx
|           |   |   |-- SessionCalendarPage.jsx
|           |   |   |-- SessionDetailsPage.jsx
|           |   |   `-- OperationalDashboardPage.jsx
|           |   |-- hooks/
|           |   `-- validation/
|           |
|           |-- appointment-management/                      # MF-03
|           |   |-- api/
|           |   |   |-- availabilityApi.js
|           |   |   |-- appointmentApi.js
|           |   |   |-- waitlistApi.js
|           |   |   `-- checkInApi.js
|           |   |-- components/
|           |   |   |-- AvailabilitySearch.jsx
|           |   |   |-- SessionAvailabilityCard.jsx
|           |   |   |-- AppointmentCard.jsx
|           |   |   |-- AppointmentStatusBadge.jsx
|           |   |   |-- RescheduleDialog.jsx
|           |   |   |-- WaitlistCard.jsx
|           |   |   |-- WaitlistPosition.jsx
|           |   |   `-- CheckInPanel.jsx
|           |   |-- pages/
|           |   |   |-- AppointmentBookingPage.jsx
|           |   |   |-- MyAppointmentsPage.jsx
|           |   |   |-- WaitlistPage.jsx
|           |   |   `-- CheckInManagementPage.jsx
|           |   |-- hooks/
|           |   `-- validation/
|           |
|           |-- vaccine-inventory/                           # MF-04
|           |   |-- api/
|           |   |   |-- batchApi.js
|           |   |   |-- inventoryApi.js
|           |   |   `-- transferApi.js
|           |   |-- components/
|           |   |   |-- BatchForm.jsx
|           |   |   |-- BatchTable.jsx
|           |   |   |-- CenterStockTable.jsx
|           |   |   |-- ReceiptForm.jsx
|           |   |   |-- TransferForm.jsx
|           |   |   |-- AdjustmentForm.jsx
|           |   |   |-- StockTransactionTable.jsx
|           |   |   |-- LowStockAlert.jsx
|           |   |   `-- NearExpiryAlert.jsx
|           |   |-- pages/
|           |   |   |-- BatchManagementPage.jsx
|           |   |   |-- CenterInventoryPage.jsx
|           |   |   |-- StockMovementPage.jsx
|           |   |   `-- InventoryAlertsPage.jsx
|           |   |-- hooks/
|           |   `-- validation/
|           |
|           `-- vaccination-administration/                  # MF-05
|               |-- api/
|               |   |-- vaccinationDraftApi.js
|               |   |-- vaccinationApi.js
|               |   `-- certificateApi.js
|               |-- components/
|               |   |-- CheckedInWorkQueue.jsx
|               |   |-- VaccinationDraftForm.jsx
|               |   |-- AdministrationConfirmationDialog.jsx
|               |   |-- VaccinationHistoryTable.jsx
|               |   |-- CorrectionRequestForm.jsx
|               |   |-- CertificateCard.jsx
|               |   `-- CertificateVerificationForm.jsx
|               |-- pages/
|               |   |-- VaccinationWorkQueuePage.jsx
|               |   |-- VaccinationDraftPage.jsx
|               |   |-- VaccinationHistoryPage.jsx
|               |   |-- VaccinationCorrectionPage.jsx
|               |   |-- CertificatePage.jsx
|               |   `-- CertificateVerificationPage.jsx
|               |-- hooks/
|               `-- validation/
|
|-- backend/
|   |-- README.md
|   |-- pom.xml
|   |-- mvnw
|   |-- mvnw.cmd
|   |-- .env.example
|   |-- .mvn/wrapper/
|   `-- src/
|       |-- main/
|       |   |-- java/com/se2030/vaccinationportal/
|       |   |   |-- VaccinationPortalApplication.java
|       |   |   |
|       |   |   |-- shared/
|       |   |   |   |-- access/
|       |   |   |   |   |-- controller/
|       |   |   |   |   |-- dto/
|       |   |   |   |   |-- model/
|       |   |   |   |   |-- repository/
|       |   |   |   |   `-- service/
|       |   |   |   |-- security/
|       |   |   |   |-- config/
|       |   |   |   |-- exception/
|       |   |   |   |-- dto/
|       |   |   |   |-- audit/
|       |   |   |   |-- notification/
|       |   |   |   |-- outbox/
|       |   |   |   |-- validation/
|       |   |   |   `-- util/
|       |   |   |
|       |   |   |-- screening/                              # MF-01
|       |   |   |   |-- controller/
|       |   |   |   |   |-- ScreeningConfigurationController.java
|       |   |   |   |   |-- QuestionnaireController.java
|       |   |   |   |   |-- ScreeningSubmissionController.java
|       |   |   |   |   `-- EligibilityReviewController.java
|       |   |   |   |-- dto/
|       |   |   |   |   |-- question/
|       |   |   |   |   |-- questionnaire/
|       |   |   |   |   |-- submission/
|       |   |   |   |   `-- eligibility/
|       |   |   |   |-- model/
|       |   |   |   |   |-- ConsentNoticeVersion.java
|       |   |   |   |   |-- ScreeningQuestion.java
|       |   |   |   |   |-- AnswerOption.java
|       |   |   |   |   |-- EligibilityRule.java
|       |   |   |   |   |-- Questionnaire.java
|       |   |   |   |   |-- QuestionnaireVersion.java
|       |   |   |   |   |-- QuestionnaireVersionStatus.java
|       |   |   |   |   |-- QuestionnaireItem.java
|       |   |   |   |   |-- ScreeningSubmission.java
|       |   |   |   |   |-- ScreeningSubmissionStatus.java
|       |   |   |   |   |-- ScreeningAnswer.java
|       |   |   |   |   |-- PreliminaryScreeningResult.java
|       |   |   |   |   |-- EligibilityAssessment.java
|       |   |   |   |   `-- EligibilityAssessmentStatus.java
|       |   |   |   |-- repository/
|       |   |   |   |-- service/
|       |   |   |   |   |-- ScreeningConfigurationService.java
|       |   |   |   |   |-- QuestionnaireVersionService.java
|       |   |   |   |   |-- ScreeningSubmissionService.java
|       |   |   |   |   |-- PreliminaryScreeningService.java
|       |   |   |   |   `-- EligibilityReviewService.java
|       |   |   |   |-- mapper/
|       |   |   |   `-- validation/
|       |   |   |
|       |   |   |-- campaign/                               # MF-02
|       |   |   |   |-- controller/
|       |   |   |   |   |-- VaccineTypeController.java
|       |   |   |   |   |-- CampaignController.java
|       |   |   |   |   |-- VaccinationCenterController.java
|       |   |   |   |   |-- CampaignCenterAssignmentController.java
|       |   |   |   |   `-- VaccinationSessionController.java
|       |   |   |   |-- dto/
|       |   |   |   |   |-- campaign/
|       |   |   |   |   |-- center/
|       |   |   |   |   |-- assignment/
|       |   |   |   |   |-- recurrence/
|       |   |   |   |   |-- session/
|       |   |   |   |   |-- resource/
|       |   |   |   |   `-- capacity/
|       |   |   |   |-- model/
|       |   |   |   |   |-- VaccineType.java
|       |   |   |   |   |-- TargetGroup.java
|       |   |   |   |   |-- CampaignDoseProgramme.java
|       |   |   |   |   |-- VaccinationCampaign.java
|       |   |   |   |   |-- CampaignStatus.java
|       |   |   |   |   |-- CampaignLocationTarget.java
|       |   |   |   |   |-- CampaignQuestionnaireAssignment.java
|       |   |   |   |   |-- VaccinationCenter.java
|       |   |   |   |   |-- CenterStatus.java
|       |   |   |   |   |-- CampaignCenterAssignment.java
|       |   |   |   |   |-- OperatingHours.java
|       |   |   |   |   |-- CenterClosure.java
|       |   |   |   |   |-- SessionRecurrenceRule.java
|       |   |   |   |   |-- VaccinationSession.java
|       |   |   |   |   |-- SessionLifecycleStatus.java
|       |   |   |   |   |-- SessionBookingStatus.java
|       |   |   |   |   |-- TimeSlot.java
|       |   |   |   |   |-- OperationalResource.java
|       |   |   |   |   |-- OperationalResourceStatus.java
|       |   |   |   |   |-- SessionResourceAssignment.java
|       |   |   |   |   |-- StaffAssignment.java
|       |   |   |   |   |-- SessionCapacityPlan.java
|       |   |   |   |   `-- SessionChangeEvent.java
|       |   |   |   |-- repository/
|       |   |   |   |-- service/
|       |   |   |   |   |-- CampaignService.java
|       |   |   |   |   |-- VaccinationCenterService.java
|       |   |   |   |   |-- RecurringSessionService.java
|       |   |   |   |   |-- VaccinationSessionService.java
|       |   |   |   |   |-- SessionResourceService.java
|       |   |   |   |   |-- SessionCapacityService.java
|       |   |   |   |   `-- SessionChangeService.java
|       |   |   |   |-- mapper/
|       |   |   |   `-- validation/
|       |   |   |
|       |   |   |-- appointment/                            # MF-03
|       |   |   |   |-- controller/
|       |   |   |   |   |-- AvailabilityController.java
|       |   |   |   |   |-- AppointmentController.java
|       |   |   |   |   |-- WaitlistController.java
|       |   |   |   |   `-- CheckInController.java
|       |   |   |   |-- dto/
|       |   |   |   |   |-- availability/
|       |   |   |   |   |-- appointment/
|       |   |   |   |   |-- waitlist/
|       |   |   |   |   `-- checkin/
|       |   |   |   |-- model/
|       |   |   |   |   |-- Appointment.java
|       |   |   |   |   |-- AppointmentStatus.java
|       |   |   |   |   |-- AppointmentChangeEvent.java
|       |   |   |   |   |-- WaitlistEntry.java
|       |   |   |   |   |-- WaitlistStatus.java
|       |   |   |   |   |-- WaitlistChangeEvent.java
|       |   |   |   |   |-- CheckInEvent.java
|       |   |   |   |   |-- CheckInEventType.java
|       |   |   |   |   `-- ModuleEventConsumption.java
|       |   |   |   |-- repository/
|       |   |   |   |-- service/
|       |   |   |   |   |-- AvailabilityService.java
|       |   |   |   |   |-- AppointmentBookingService.java
|       |   |   |   |   |-- AppointmentRescheduleService.java
|       |   |   |   |   |-- AppointmentCancellationService.java
|       |   |   |   |   |-- WaitlistService.java
|       |   |   |   |   |-- CheckInService.java
|       |   |   |   |   `-- SessionChangeConsumerService.java
|       |   |   |   |-- mapper/
|       |   |   |   `-- validation/
|       |   |   |
|       |   |   |-- inventory/                              # MF-04
|       |   |   |   |-- controller/
|       |   |   |   |   |-- ManufacturerController.java
|       |   |   |   |   |-- VaccineBatchController.java
|       |   |   |   |   |-- InventoryController.java
|       |   |   |   |   `-- StockTransferController.java
|       |   |   |   |-- dto/
|       |   |   |   |   |-- manufacturer/
|       |   |   |   |   |-- batch/
|       |   |   |   |   |-- inventory/
|       |   |   |   |   |-- transaction/
|       |   |   |   |   `-- transfer/
|       |   |   |   |-- model/
|       |   |   |   |   |-- Manufacturer.java
|       |   |   |   |   |-- VaccineBatch.java
|       |   |   |   |   |-- BatchStatus.java
|       |   |   |   |   |-- CenterInventory.java
|       |   |   |   |   |-- StockTransfer.java
|       |   |   |   |   |-- StockTransaction.java
|       |   |   |   |   `-- StockTransactionType.java
|       |   |   |   |-- repository/
|       |   |   |   |-- service/
|       |   |   |   |   |-- ManufacturerService.java
|       |   |   |   |   |-- VaccineBatchService.java
|       |   |   |   |   |-- InventoryReceiptService.java
|       |   |   |   |   |-- StockTransferService.java
|       |   |   |   |   |-- StockAdjustmentService.java
|       |   |   |   |   |-- InventoryQueryService.java
|       |   |   |   |   `-- InventoryAlertService.java
|       |   |   |   |-- mapper/
|       |   |   |   `-- validation/
|       |   |   |
|       |   |   `-- administration/                         # MF-05
|       |   |       |-- controller/
|       |   |       |   |-- VaccinationWorkQueueController.java
|       |   |       |   |-- VaccinationDraftController.java
|       |   |       |   |-- VaccinationFinalizationController.java
|       |   |       |   |-- VaccinationHistoryController.java
|       |   |       |   |-- VaccinationCorrectionController.java
|       |   |       |   |-- CertificateController.java
|       |   |       |   `-- CertificateVerificationController.java
|       |   |       |-- dto/
|       |   |       |   |-- draft/
|       |   |       |   |-- finalization/
|       |   |       |   |-- history/
|       |   |       |   |-- correction/
|       |   |       |   `-- certificate/
|       |   |       |-- model/
|       |   |       |   |-- VaccinationDraft.java
|       |   |       |   |-- VaccinationDraftStatus.java
|       |   |       |   |-- VaccinationRecord.java
|       |   |       |   |-- VaccinationCorrection.java
|       |   |       |   |-- VaccinationCorrectionStatus.java
|       |   |       |   |-- CertificateIssue.java
|       |   |       |   |-- CertificateStatus.java
|       |   |       |   |-- CertificateVerificationLog.java
|       |   |       |   `-- ApiIdempotencyRecord.java
|       |   |       |-- repository/
|       |   |       |-- service/
|       |   |       |   |-- VaccinationWorkQueueService.java
|       |   |       |   |-- VaccinationDraftService.java
|       |   |       |   |-- VaccinationFinalizationService.java
|       |   |       |   |-- VaccinationHistoryService.java
|       |   |       |   |-- VaccinationCorrectionService.java
|       |   |       |   |-- CertificateService.java
|       |   |       |   `-- CertificateVerificationService.java
|       |   |       |-- mapper/
|       |   |       `-- validation/
|       |   |
|       |   `-- resources/
|       |       |-- application.properties
|       |       |-- application-local.properties
|       |       `-- application-demo.properties
|       |
|       `-- test/
|           |-- java/com/se2030/vaccinationportal/
|           |   |-- shared/
|           |   |-- screening/
|           |   |-- campaign/
|           |   |-- appointment/
|           |   |-- inventory/
|           |   `-- administration/
|           `-- resources/
|               `-- application-test.properties
|
|-- database/
|   |-- README.md
|   |-- .env.example
|   |-- migrations/
|   |   |-- V001__shared_foundation.sql
|   |   |-- V002__mf01_screening_foundation.sql
|   |   |-- V003__mf02_campaign_foundation.sql
|   |   |-- V004__mf03_appointment_foundation.sql
|   |   |-- V005__mf04_inventory_foundation.sql
|   |   `-- V006__mf05_administration_foundation.sql
|   |-- seeds/
|   |   |-- development/
|   |   `-- testing/
|   `-- diagrams/
|       |-- er-diagram.md
|       |-- er-diagram.png
|       `-- database-schema.md
|
`-- tests/
    |-- README.md
    |-- integration/
    |   |-- shared/
    |   |-- screening/
    |   |-- campaign/
    |   |-- appointment/
    |   |-- inventory/
    |   `-- administration/
    |-- end-to-end/
    |   |-- citizen-screening-flow.spec.js
    |   |-- appointment-booking-flow.spec.js
    |   |-- inventory-flow.spec.js
    |   `-- vaccination-certificate-flow.spec.js
    `-- acceptance/
        |-- mf01-acceptance.md
        |-- mf02-acceptance.md
        |-- mf03-acceptance.md
        |-- mf04-acceptance.md
        `-- mf05-acceptance.md
```

## Ownership Summary

| Area | Frontend | Backend | Backend tests |
|---|---|---|---|
| Shared access/support | `frontend/src/features/access/` | `backend/.../shared/` | `backend/src/test/.../shared/` |
| MF-01 | `frontend/src/features/screening/` | `backend/.../screening/` | `backend/src/test/.../screening/` |
| MF-02 | `frontend/src/features/campaign-management/` | `backend/.../campaign/` | `backend/src/test/.../campaign/` |
| MF-03 | `frontend/src/features/appointment-management/` | `backend/.../appointment/` | `backend/src/test/.../appointment/` |
| MF-04 | `frontend/src/features/vaccine-inventory/` | `backend/.../inventory/` | `backend/src/test/.../inventory/` |
| MF-05 | `frontend/src/features/vaccination-administration/` | `backend/.../administration/` | `backend/src/test/.../administration/` |

## File-Tree Rules

1. This document is a **target/reference tree**, not a checklist to pre-create every file.
2. Inspect the live repository before creating or moving anything.
3. Read the canonical Markdown and the relevant MF handoff before implementation.
4. Keep business entities inside the owning module even when other modules reference them.
5. `VaccineType` is owned by MF-02; MF-04 must not create `Vaccine`, `VaccineType`, `VaccineRepository` or another master lifecycle.
6. MF-05 backend code uses `administration/`; do not create a competing `vaccination/` backend package.
7. MF-03 uses `CheckInEvent` / `CheckInEventType`, not an alternative `CheckInRecord` model.
8. MF-05 uses `CertificateIssue`, `CertificateVerificationLog` and `ApiIdempotencyRecord`; do not replace them with simplified `DigitalCertificate` or generic `IdempotencyRecord` models.
9. Shared account/access, security, audit, notifications and transactional-outbox infrastructure belong under shared/supporting code, not inside one MF.
10. Use DTOs for public API input/output; do not expose JPA entities directly.
11. Keep controllers thin; business rules belong in services/domain validation and critical constraints also require database enforcement.
12. Flyway migration versions are **global**. The example `V001`-`V006` sequence is only the initial baseline; every future migration must use the next globally coordinated version. Do not create independent per-branch version sequences.
13. Do not use Hibernate `create`, `create-drop` or `update` as the committed production/demo schema-management strategy.
14. Use only synthetic/anonymized seed and test data. Never commit `.env`, secrets, real citizen data, real screening/medical data or production dumps.
15. Tests should mirror the module they verify, and cross-module integration tests should live in shared/integration test locations rather than duplicating another module's business implementation.
16. Exact final filenames may evolve as code is implemented. Canonical semantics and ownership are authoritative.

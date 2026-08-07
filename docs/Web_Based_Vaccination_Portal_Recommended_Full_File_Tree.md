# Web-Based Vaccination Portal — Recommended Full File Tree

> This is the recommended target structure for the project.
> It includes the current shared foundation plus the files the team may add while implementing MF-01 through MF-05.
>
> **Do not create every file immediately.** Create files only when the related feature is implemented.

```text
se2030-2026-y2-s1-mtr-01-vaccination-portal/
│
├── README.md
├── CONTRIBUTING.md
├── LICENSE
├── .gitignore
├── .editorconfig
├── compose.yaml
│
├── .github/
│   ├── ISSUE_TEMPLATE/
│   │   └── user-story.md
│   ├── pull_request_template.md
│   └── workflows/
│       ├── frontend-ci.yml
│       └── backend-ci.yml
│
├── docs/
│   ├── Web_Based_Vaccination_Portal_AI_Canonical_Context_v3.3.md
│   ├── Web_Based_Vaccination_Portal_AI_Canonical_Master_v3.3.docx
│   ├── architecture/
│   │   ├── system-architecture.md
│   │   ├── frontend-architecture.md
│   │   ├── backend-architecture.md
│   │   ├── database-architecture.md
│   │   └── api-conventions.md
│   ├── api/
│   │   ├── mf01-screening-api.md
│   │   ├── mf02-campaign-api.md
│   │   ├── mf03-appointment-api.md
│   │   ├── mf04-inventory-api.md
│   │   └── mf05-vaccination-api.md
│   └── decisions/
│       ├── README.md
│       └── architecture-decisions.md
│
├── frontend/
│   ├── README.md
│   ├── package.json
│   ├── package-lock.json
│   ├── vite.config.js
│   ├── eslint.config.js
│   ├── index.html
│   ├── .env.example
│   ├── public/
│   │   ├── favicon.ico
│   │   └── images/
│   └── src/
│       ├── main.jsx
│       ├── app/
│       │   ├── App.jsx
│       │   ├── router.jsx
│       │   └── providers.jsx
│       ├── assets/
│       │   ├── images/
│       │   └── icons/
│       ├── components/
│       │   └── common/
│       │       ├── Header.jsx
│       │       ├── Footer.jsx
│       │       ├── LoadingSpinner.jsx
│       │       ├── ErrorAlert.jsx
│       │       ├── SuccessAlert.jsx
│       │       ├── ConfirmDialog.jsx
│       │       ├── EmptyState.jsx
│       │       ├── Pagination.jsx
│       │       ├── SearchInput.jsx
│       │       └── StatusBadge.jsx
│       ├── layouts/
│       │   ├── PublicLayout.jsx
│       │   ├── CitizenLayout.jsx
│       │   ├── StaffLayout.jsx
│       │   └── AdminLayout.jsx
│       ├── services/
│       │   └── apiClient.js
│       ├── hooks/
│       │   ├── useApi.js
│       │   └── usePagination.js
│       ├── constants/
│       │   ├── apiRoutes.js
│       │   └── appConstants.js
│       ├── utils/
│       │   ├── dateUtils.js
│       │   ├── formatUtils.js
│       │   └── validationUtils.js
│       ├── styles/
│       │   └── global.css
│       └── features/
│           ├── screening/                                  # MF-01
│           │   ├── api/
│           │   │   └── screeningApi.js
│           │   ├── components/
│           │   │   ├── ScreeningForm.jsx
│           │   │   ├── ScreeningQuestion.jsx
│           │   │   ├── ScreeningResult.jsx
│           │   │   ├── EligibilityResultCard.jsx
│           │   │   └── ScreeningHistoryTable.jsx
│           │   ├── pages/
│           │   │   ├── ScreeningPage.jsx
│           │   │   ├── EligibilityResultPage.jsx
│           │   │   ├── ScreeningHistoryPage.jsx
│           │   │   ├── ScreeningQuestionManagementPage.jsx
│           │   │   └── EligibilityRuleManagementPage.jsx
│           │   ├── hooks/
│           │   │   ├── useScreening.js
│           │   │   └── useEligibility.js
│           │   ├── validation/
│           │   │   └── screeningValidation.js
│           │   └── constants/
│           │       └── screeningConstants.js
│           │
│           ├── campaign-management/                        # MF-02
│           │   ├── api/
│           │   │   ├── campaignApi.js
│           │   │   ├── centerApi.js
│           │   │   └── sessionApi.js
│           │   ├── components/
│           │   │   ├── CampaignForm.jsx
│           │   │   ├── CampaignTable.jsx
│           │   │   ├── CampaignStatusBadge.jsx
│           │   │   ├── CenterForm.jsx
│           │   │   ├── CenterTable.jsx
│           │   │   ├── CampaignCenterAssignmentForm.jsx
│           │   │   ├── SessionForm.jsx
│           │   │   ├── SessionTable.jsx
│           │   │   ├── SessionCapacityDisplay.jsx
│           │   │   └── SessionStatusBadge.jsx
│           │   ├── pages/
│           │   │   ├── CampaignListPage.jsx
│           │   │   ├── CampaignCreatePage.jsx
│           │   │   ├── CampaignEditPage.jsx
│           │   │   ├── CampaignDetailsPage.jsx
│           │   │   ├── CenterManagementPage.jsx
│           │   │   ├── CampaignCenterAssignmentPage.jsx
│           │   │   ├── SessionManagementPage.jsx
│           │   │   ├── SessionCreatePage.jsx
│           │   │   └── SessionDetailsPage.jsx
│           │   ├── hooks/
│           │   │   ├── useCampaigns.js
│           │   │   ├── useCenters.js
│           │   │   └── useSessions.js
│           │   ├── validation/
│           │   │   ├── campaignValidation.js
│           │   │   ├── centerValidation.js
│           │   │   └── sessionValidation.js
│           │   └── constants/
│           │       └── campaignConstants.js
│           │
│           ├── appointment-management/                     # MF-03
│           │   ├── api/
│           │   │   ├── appointmentApi.js
│           │   │   ├── waitlistApi.js
│           │   │   └── checkInApi.js
│           │   ├── components/
│           │   │   ├── AppointmentForm.jsx
│           │   │   ├── AppointmentCard.jsx
│           │   │   ├── AppointmentTable.jsx
│           │   │   ├── AppointmentStatusBadge.jsx
│           │   │   ├── WaitlistCard.jsx
│           │   │   ├── WaitlistTable.jsx
│           │   │   ├── CheckInForm.jsx
│           │   │   └── SessionAvailabilityList.jsx
│           │   ├── pages/
│           │   │   ├── AppointmentBookingPage.jsx
│           │   │   ├── MyAppointmentsPage.jsx
│           │   │   ├── AppointmentDetailsPage.jsx
│           │   │   ├── WaitlistPage.jsx
│           │   │   └── CheckInManagementPage.jsx
│           │   ├── hooks/
│           │   │   ├── useAppointments.js
│           │   │   ├── useWaitlist.js
│           │   │   └── useCheckIn.js
│           │   ├── validation/
│           │   │   └── appointmentValidation.js
│           │   └── constants/
│           │       └── appointmentConstants.js
│           │
│           ├── vaccine-inventory/                          # MF-04
│           │   ├── api/
│           │   │   ├── vaccineApi.js
│           │   │   ├── batchApi.js
│           │   │   └── stockApi.js
│           │   ├── components/
│           │   │   ├── VaccineForm.jsx
│           │   │   ├── VaccineTable.jsx
│           │   │   ├── BatchForm.jsx
│           │   │   ├── BatchTable.jsx
│           │   │   ├── StockLevelCard.jsx
│           │   │   ├── StockMovementTable.jsx
│           │   │   ├── ExpiryWarning.jsx
│           │   │   └── InventoryStatusBadge.jsx
│           │   ├── pages/
│           │   │   ├── VaccineManagementPage.jsx
│           │   │   ├── BatchManagementPage.jsx
│           │   │   ├── InventoryDashboardPage.jsx
│           │   │   ├── StockMovementPage.jsx
│           │   │   └── ExpiringBatchesPage.jsx
│           │   ├── hooks/
│           │   │   ├── useVaccines.js
│           │   │   ├── useBatches.js
│           │   │   └── useInventory.js
│           │   ├── validation/
│           │   │   └── inventoryValidation.js
│           │   └── constants/
│           │       └── inventoryConstants.js
│           │
│           └── vaccination-administration/                 # MF-05
│               ├── api/
│               │   ├── vaccinationApi.js
│               │   ├── vaccinationDraftApi.js
│               │   └── certificateApi.js
│               ├── components/
│               │   ├── VaccinationDraftForm.jsx
│               │   ├── VaccinationRecordCard.jsx
│               │   ├── VaccinationHistoryTable.jsx
│               │   ├── CertificateCard.jsx
│               │   ├── CertificateVerificationForm.jsx
│               │   └── CorrectionRequestForm.jsx
│               ├── pages/
│               │   ├── VaccinationAdministrationPage.jsx
│               │   ├── VaccinationDraftPage.jsx
│               │   ├── VaccinationHistoryPage.jsx
│               │   ├── CertificatePage.jsx
│               │   ├── CertificateVerificationPage.jsx
│               │   └── VaccinationCorrectionPage.jsx
│               ├── hooks/
│               │   ├── useVaccinationRecords.js
│               │   └── useCertificates.js
│               ├── validation/
│               │   └── vaccinationValidation.js
│               └── constants/
│                   └── vaccinationConstants.js
│
├── backend/
│   ├── README.md
│   ├── pom.xml
│   ├── mvnw
│   ├── mvnw.cmd
│   ├── .env.example
│   ├── .mvn/
│   │   └── wrapper/
│   │       ├── maven-wrapper.jar
│   │       └── maven-wrapper.properties
│   └── src/
│       ├── main/
│       │   ├── java/
│       │   │   └── com/
│       │   │       └── se2030/
│       │   │           └── vaccinationportal/
│       │   │               ├── VaccinationPortalApplication.java
│       │   │               ├── shared/
│       │   │               │   ├── config/
│       │   │               │   │   ├── WebConfig.java
│       │   │               │   │   └── JpaConfig.java
│       │   │               │   ├── exception/
│       │   │               │   │   ├── GlobalExceptionHandler.java
│       │   │               │   │   ├── ResourceNotFoundException.java
│       │   │               │   │   ├── ConflictException.java
│       │   │               │   │   └── ValidationException.java
│       │   │               │   ├── dto/
│       │   │               │   │   ├── ApiErrorResponse.java
│       │   │               │   │   └── ApiMessageResponse.java
│       │   │               │   ├── audit/
│       │   │               │   │   ├── AuditableEntity.java
│       │   │               │   │   └── AuditLog.java
│       │   │               │   ├── validation/
│       │   │               │   │   └── ValidationUtils.java
│       │   │               │   └── util/
│       │   │               │       ├── DateTimeUtils.java
│       │   │               │       └── IdUtils.java
│       │   │               │
│       │   │               ├── screening/                           # MF-01
│       │   │               │   ├── controller/
│       │   │               │   │   ├── ScreeningController.java
│       │   │               │   │   ├── ScreeningQuestionController.java
│       │   │               │   │   └── EligibilityRuleController.java
│       │   │               │   ├── dto/
│       │   │               │   │   ├── ScreeningRequest.java
│       │   │               │   │   ├── ScreeningResponse.java
│       │   │               │   │   ├── ScreeningQuestionRequest.java
│       │   │               │   │   ├── ScreeningQuestionResponse.java
│       │   │               │   │   ├── EligibilityRuleRequest.java
│       │   │               │   │   └── EligibilityResultResponse.java
│       │   │               │   ├── model/
│       │   │               │   │   ├── ScreeningQuestion.java
│       │   │               │   │   ├── ScreeningSubmission.java
│       │   │               │   │   ├── ScreeningAnswer.java
│       │   │               │   │   ├── EligibilityRule.java
│       │   │               │   │   └── EligibilityResult.java
│       │   │               │   ├── repository/
│       │   │               │   │   ├── ScreeningQuestionRepository.java
│       │   │               │   │   ├── ScreeningSubmissionRepository.java
│       │   │               │   │   ├── ScreeningAnswerRepository.java
│       │   │               │   │   ├── EligibilityRuleRepository.java
│       │   │               │   │   └── EligibilityResultRepository.java
│       │   │               │   ├── service/
│       │   │               │   │   ├── ScreeningService.java
│       │   │               │   │   ├── ScreeningQuestionService.java
│       │   │               │   │   └── EligibilityRuleService.java
│       │   │               │   ├── mapper/
│       │   │               │   │   └── ScreeningMapper.java
│       │   │               │   └── validation/
│       │   │               │       └── ScreeningValidator.java
│       │   │               │
│       │   │               ├── campaign/                            # MF-02
│       │   │               │   ├── controller/
│       │   │               │   │   ├── CampaignController.java
│       │   │               │   │   ├── VaccinationCenterController.java
│       │   │               │   │   ├── CampaignCenterAssignmentController.java
│       │   │               │   │   └── VaccinationSessionController.java
│       │   │               │   ├── dto/
│       │   │               │   │   ├── campaign/
│       │   │               │   │   │   ├── CampaignCreateRequest.java
│       │   │               │   │   │   ├── CampaignUpdateRequest.java
│       │   │               │   │   │   └── CampaignResponse.java
│       │   │               │   │   ├── center/
│       │   │               │   │   │   ├── CenterCreateRequest.java
│       │   │               │   │   │   ├── CenterUpdateRequest.java
│       │   │               │   │   │   └── CenterResponse.java
│       │   │               │   │   ├── assignment/
│       │   │               │   │   │   ├── CampaignCenterAssignmentRequest.java
│       │   │               │   │   │   └── CampaignCenterAssignmentResponse.java
│       │   │               │   │   └── session/
│       │   │               │   │       ├── SessionCreateRequest.java
│       │   │               │   │       ├── SessionUpdateRequest.java
│       │   │               │   │       └── SessionResponse.java
│       │   │               │   ├── model/
│       │   │               │   │   ├── VaccinationCampaign.java
│       │   │               │   │   ├── CampaignStatus.java
│       │   │               │   │   ├── VaccinationCenter.java
│       │   │               │   │   ├── CampaignCenterAssignment.java
│       │   │               │   │   ├── VaccinationSession.java
│       │   │               │   │   ├── SessionLifecycleStatus.java
│       │   │               │   │   ├── SessionBookingStatus.java
│       │   │               │   │   ├── SessionChangeEvent.java
│       │   │               │   │   ├── CampaignQuestionnaireAssignment.java
│       │   │               │   │   └── CampaignDoseProgramme.java
│       │   │               │   ├── repository/
│       │   │               │   │   ├── VaccinationCampaignRepository.java
│       │   │               │   │   ├── VaccinationCenterRepository.java
│       │   │               │   │   ├── CampaignCenterAssignmentRepository.java
│       │   │               │   │   ├── VaccinationSessionRepository.java
│       │   │               │   │   └── SessionChangeEventRepository.java
│       │   │               │   ├── service/
│       │   │               │   │   ├── CampaignService.java
│       │   │               │   │   ├── VaccinationCenterService.java
│       │   │               │   │   ├── CampaignCenterAssignmentService.java
│       │   │               │   │   ├── VaccinationSessionService.java
│       │   │               │   │   └── SessionCapacityService.java
│       │   │               │   ├── mapper/
│       │   │               │   │   ├── CampaignMapper.java
│       │   │               │   │   ├── CenterMapper.java
│       │   │               │   │   └── SessionMapper.java
│       │   │               │   └── validation/
│       │   │               │       ├── CampaignValidator.java
│       │   │               │       ├── CenterValidator.java
│       │   │               │       └── SessionValidator.java
│       │   │               │
│       │   │               ├── appointment/                         # MF-03
│       │   │               │   ├── controller/
│       │   │               │   │   ├── AppointmentController.java
│       │   │               │   │   ├── WaitlistController.java
│       │   │               │   │   └── CheckInController.java
│       │   │               │   ├── dto/
│       │   │               │   │   ├── AppointmentCreateRequest.java
│       │   │               │   │   ├── AppointmentResponse.java
│       │   │               │   │   ├── WaitlistJoinRequest.java
│       │   │               │   │   ├── WaitlistResponse.java
│       │   │               │   │   ├── CheckInRequest.java
│       │   │               │   │   └── CheckInResponse.java
│       │   │               │   ├── model/
│       │   │               │   │   ├── Appointment.java
│       │   │               │   │   ├── AppointmentStatus.java
│       │   │               │   │   ├── WaitlistEntry.java
│       │   │               │   │   ├── WaitlistStatus.java
│       │   │               │   │   ├── WaitlistChangeEvent.java
│       │   │               │   │   ├── CheckInRecord.java
│       │   │               │   │   └── ModuleEventConsumption.java
│       │   │               │   ├── repository/
│       │   │               │   │   ├── AppointmentRepository.java
│       │   │               │   │   ├── WaitlistEntryRepository.java
│       │   │               │   │   ├── WaitlistChangeEventRepository.java
│       │   │               │   │   ├── CheckInRecordRepository.java
│       │   │               │   │   └── ModuleEventConsumptionRepository.java
│       │   │               │   ├── service/
│       │   │               │   │   ├── AppointmentService.java
│       │   │               │   │   ├── WaitlistService.java
│       │   │               │   │   ├── CheckInService.java
│       │   │               │   │   └── SessionChangeConsumerService.java
│       │   │               │   ├── mapper/
│       │   │               │   │   └── AppointmentMapper.java
│       │   │               │   └── validation/
│       │   │               │       └── AppointmentValidator.java
│       │   │               │
│       │   │               ├── inventory/                           # MF-04
│       │   │               │   ├── controller/
│       │   │               │   │   ├── VaccineController.java
│       │   │               │   │   ├── VaccineBatchController.java
│       │   │               │   │   └── InventoryController.java
│       │   │               │   ├── dto/
│       │   │               │   │   ├── VaccineRequest.java
│       │   │               │   │   ├── VaccineResponse.java
│       │   │               │   │   ├── VaccineBatchRequest.java
│       │   │               │   │   ├── VaccineBatchResponse.java
│       │   │               │   │   ├── StockMovementRequest.java
│       │   │               │   │   └── StockSummaryResponse.java
│       │   │               │   ├── model/
│       │   │               │   │   ├── Vaccine.java
│       │   │               │   │   ├── VaccineBatch.java
│       │   │               │   │   ├── BatchStatus.java
│       │   │               │   │   ├── InventoryStock.java
│       │   │               │   │   ├── StockMovement.java
│       │   │               │   │   └── StockMovementType.java
│       │   │               │   ├── repository/
│       │   │               │   │   ├── VaccineRepository.java
│       │   │               │   │   ├── VaccineBatchRepository.java
│       │   │               │   │   ├── InventoryStockRepository.java
│       │   │               │   │   └── StockMovementRepository.java
│       │   │               │   ├── service/
│       │   │               │   │   ├── VaccineService.java
│       │   │               │   │   ├── VaccineBatchService.java
│       │   │               │   │   └── InventoryService.java
│       │   │               │   ├── mapper/
│       │   │               │   │   └── InventoryMapper.java
│       │   │               │   └── validation/
│       │   │               │       └── InventoryValidator.java
│       │   │               │
│       │   │               └── vaccination/                         # MF-05
│       │   │                   ├── controller/
│       │   │                   │   ├── VaccinationAdministrationController.java
│       │   │                   │   ├── VaccinationRecordController.java
│       │   │                   │   └── CertificateController.java
│       │   │                   ├── dto/
│       │   │                   │   ├── VaccinationDraftRequest.java
│       │   │                   │   ├── VaccinationDraftResponse.java
│       │   │                   │   ├── VaccinationRecordResponse.java
│       │   │                   │   ├── VaccinationCorrectionRequest.java
│       │   │                   │   ├── VaccinationCorrectionResponse.java
│       │   │                   │   └── CertificateResponse.java
│       │   │                   ├── model/
│       │   │                   │   ├── VaccinationDraft.java
│       │   │                   │   ├── VaccinationRecord.java
│       │   │                   │   ├── VaccinationCorrection.java
│       │   │                   │   ├── VaccinationCorrectionStatus.java
│       │   │                   │   ├── DigitalCertificate.java
│       │   │                   │   ├── CertificateStatus.java
│       │   │                   │   └── IdempotencyRecord.java
│       │   │                   ├── repository/
│       │   │                   │   ├── VaccinationDraftRepository.java
│       │   │                   │   ├── VaccinationRecordRepository.java
│       │   │                   │   ├── VaccinationCorrectionRepository.java
│       │   │                   │   ├── DigitalCertificateRepository.java
│       │   │                   │   └── IdempotencyRecordRepository.java
│       │   │                   ├── service/
│       │   │                   │   ├── VaccinationDraftService.java
│       │   │                   │   ├── VaccinationAdministrationService.java
│       │   │                   │   ├── VaccinationCorrectionService.java
│       │   │                   │   └── CertificateService.java
│       │   │                   ├── mapper/
│       │   │                   │   └── VaccinationMapper.java
│       │   │                   ├── validation/
│       │   │                   │   └── VaccinationValidator.java
│       │   │                   └── certificate/
│       │   │                       ├── CertificateGenerator.java
│       │   │                       └── CertificateVerificationService.java
│       │   └── resources/
│       │       └── application.properties
│       └── test/
│           ├── java/
│           │   └── com/
│           │       └── se2030/
│           │           └── vaccinationportal/
│           │               ├── VaccinationPortalApplicationTests.java
│           │               ├── HealthEndpointIntegrationTests.java
│           │               ├── screening/
│           │               │   ├── ScreeningServiceTests.java
│           │               │   ├── EligibilityRuleServiceTests.java
│           │               │   └── ScreeningControllerTests.java
│           │               ├── campaign/
│           │               │   ├── CampaignServiceTests.java
│           │               │   ├── VaccinationCenterServiceTests.java
│           │               │   ├── CampaignCenterAssignmentServiceTests.java
│           │               │   ├── VaccinationSessionServiceTests.java
│           │               │   ├── SessionCapacityServiceTests.java
│           │               │   └── CampaignControllerTests.java
│           │               ├── appointment/
│           │               │   ├── AppointmentServiceTests.java
│           │               │   ├── WaitlistServiceTests.java
│           │               │   ├── CheckInServiceTests.java
│           │               │   └── AppointmentControllerTests.java
│           │               ├── inventory/
│           │               │   ├── VaccineServiceTests.java
│           │               │   ├── VaccineBatchServiceTests.java
│           │               │   ├── InventoryServiceTests.java
│           │               │   └── InventoryControllerTests.java
│           │               └── vaccination/
│           │                   ├── VaccinationDraftServiceTests.java
│           │                   ├── VaccinationAdministrationServiceTests.java
│           │                   ├── VaccinationCorrectionServiceTests.java
│           │                   ├── CertificateServiceTests.java
│           │                   └── VaccinationControllerTests.java
│           └── resources/
│               └── application-test.properties
│
├── database/
│   ├── README.md
│   ├── .env.example
│   ├── migrations/
│   │   ├── V001__create_shared_tables.sql
│   │   ├── V002__create_screening_tables.sql
│   │   ├── V003__create_campaign_tables.sql
│   │   ├── V004__create_appointment_tables.sql
│   │   ├── V005__create_inventory_tables.sql
│   │   └── V006__create_vaccination_tables.sql
│   ├── seeds/
│   │   ├── development/
│   │   │   ├── campaigns.sql
│   │   │   ├── centers.sql
│   │   │   ├── sessions.sql
│   │   │   └── synthetic-test-data.sql
│   │   └── testing/
│   │       └── synthetic-integration-data.sql
│   └── diagrams/
│       ├── er-diagram.md
│       ├── er-diagram.png
│       └── database-schema.md
│
└── tests/
    ├── README.md
    ├── integration/
    │   ├── screening/
    │   ├── campaign/
    │   ├── appointment/
    │   ├── inventory/
    │   └── vaccination/
    ├── end-to-end/
    │   ├── citizen-screening-flow.spec.js
    │   ├── appointment-booking-flow.spec.js
    │   ├── inventory-flow.spec.js
    │   └── vaccination-certificate-flow.spec.js
    └── acceptance/
        ├── mf01-acceptance.md
        ├── mf02-acceptance.md
        ├── mf03-acceptance.md
        ├── mf04-acceptance.md
        └── mf05-acceptance.md
```

## Team Ownership Summary

- **MF-01**
  - Frontend: `frontend/src/features/screening/`
  - Backend: `backend/src/main/java/com/se2030/vaccinationportal/screening/`
  - Tests: `backend/src/test/java/com/se2030/vaccinationportal/screening/`

- **MF-02**
  - Frontend: `frontend/src/features/campaign-management/`
  - Backend: `backend/src/main/java/com/se2030/vaccinationportal/campaign/`
  - Tests: `backend/src/test/java/com/se2030/vaccinationportal/campaign/`

- **MF-03**
  - Frontend: `frontend/src/features/appointment-management/`
  - Backend: `backend/src/main/java/com/se2030/vaccinationportal/appointment/`
  - Tests: `backend/src/test/java/com/se2030/vaccinationportal/appointment/`

- **MF-04**
  - Frontend: `frontend/src/features/vaccine-inventory/`
  - Backend: `backend/src/main/java/com/se2030/vaccinationportal/inventory/`
  - Tests: `backend/src/test/java/com/se2030/vaccinationportal/inventory/`

- **MF-05**
  - Frontend: `frontend/src/features/vaccination-administration/`
  - Backend: `backend/src/main/java/com/se2030/vaccinationportal/vaccination/`
  - Tests: `backend/src/test/java/com/se2030/vaccinationportal/vaccination/`

## Standard Backend Pattern

```text
<feature>/
├── controller/     # REST endpoints
├── dto/            # API request/response objects
├── model/          # entities, enums, domain objects
├── repository/     # Spring Data database access
├── service/        # business logic
├── mapper/         # entity ↔ DTO conversion
└── validation/     # feature-specific validation
```

## Standard Frontend Pattern

```text
<feature>/
├── api/            # backend API calls
├── components/     # reusable feature UI
├── pages/          # complete screens/routes
├── hooks/          # feature-specific React hooks
├── validation/     # client-side validation
└── constants/      # feature constants when needed
```

## Important Rules

1. Do not create all files at the beginning.
2. Create files only when the related feature is being implemented.
3. Do not add placeholder Java/React files just to populate folders.
4. Keep business code inside the owning feature module.
5. Put only genuinely reusable code in shared/common folders.
6. Do not place feature-specific code in `shared/`.
7. Use DTOs for API input/output instead of exposing JPA entities directly.
8. Keep controllers thin; business logic belongs in services.
9. Keep database migrations version-controlled.
10. Use only synthetic seed/test data.
11. Never commit `.env`, secrets, real citizen data, medical data, or production dumps.
12. Tests should mirror the feature they verify.
13. Exact final filenames can evolve; canonical project requirements remain authoritative.

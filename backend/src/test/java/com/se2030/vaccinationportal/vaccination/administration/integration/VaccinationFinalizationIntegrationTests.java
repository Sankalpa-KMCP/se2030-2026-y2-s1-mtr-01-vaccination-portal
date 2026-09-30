package com.se2030.vaccinationportal.vaccination.administration.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.se2030.vaccinationportal.appointment.persistence.model.Appointment;
import com.se2030.vaccinationportal.appointment.persistence.model.AppointmentStatus;
import com.se2030.vaccinationportal.appointment.persistence.repository.AppointmentRepository;
import com.se2030.vaccinationportal.campaign.CampaignTestFixtures;
import com.se2030.vaccinationportal.campaign.core.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.center.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.reference.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.session.planning.model.TimeSlot;
import com.se2030.vaccinationportal.campaign.core.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.center.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.session.planning.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.reference.model.VaccineType;
import com.se2030.vaccinationportal.campaign.reference.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.session.planning.repository.TimeSlotRepository;
import com.se2030.vaccinationportal.campaign.core.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.center.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.session.planning.repository.VaccinationSessionRepository;
import com.se2030.vaccinationportal.campaign.reference.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.inventory.exception.BatchUnavailableException;
import com.se2030.vaccinationportal.inventory.model.BatchStatus;
import com.se2030.vaccinationportal.inventory.model.CenterInventory;
import com.se2030.vaccinationportal.inventory.model.Manufacturer;
import com.se2030.vaccinationportal.inventory.model.StockTransaction;
import com.se2030.vaccinationportal.inventory.model.StockTransactionType;
import com.se2030.vaccinationportal.inventory.model.VaccineBatch;
import com.se2030.vaccinationportal.inventory.repository.CenterInventoryRepository;
import com.se2030.vaccinationportal.inventory.repository.ManufacturerRepository;
import com.se2030.vaccinationportal.inventory.repository.StockTransactionRepository;
import com.se2030.vaccinationportal.inventory.repository.VaccineBatchRepository;
import com.se2030.vaccinationportal.vaccination.administration.dto.FinalizeVaccinationResponse;
import com.se2030.vaccinationportal.vaccination.administration.exception.InvalidVaccinationDraftException;
import com.se2030.vaccinationportal.vaccination.administration.exception.VaccinationBatchVaccineTypeMismatchException;
import com.se2030.vaccinationportal.vaccination.certificate.model.CertificateStatus;
import com.se2030.vaccinationportal.vaccination.certificate.model.DigitalCertificate;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationDraft;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationDraftStatus;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationRecord;
import com.se2030.vaccinationportal.vaccination.certificate.repository.DigitalCertificateRepository;
import com.se2030.vaccinationportal.vaccination.administration.repository.VaccinationDraftRepository;
import com.se2030.vaccinationportal.vaccination.administration.repository.VaccinationRecordRepository;
import com.se2030.vaccinationportal.vaccination.administration.service.VaccinationFinalizationService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class VaccinationFinalizationIntegrationTests {

    @Autowired
    private VaccinationFinalizationService finalizationService;

    @Autowired
    private VaccinationDraftRepository draftRepository;

    @Autowired
    private VaccinationRecordRepository recordRepository;

    @Autowired
    private DigitalCertificateRepository certificateRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private VaccinationCenterRepository centerRepository;

    @Autowired
    private VaccinationCampaignRepository campaignRepository;

    @Autowired
    private VaccinationSessionRepository sessionRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private VaccineTypeRepository vaccineTypeRepository;

    @Autowired
    private TargetGroupRepository targetGroupRepository;

    @Autowired
    private ManufacturerRepository manufacturerRepository;

    @Autowired
    private VaccineBatchRepository batchRepository;

    @Autowired
    private CenterInventoryRepository centerInventoryRepository;

    @Autowired
    private StockTransactionRepository stockTransactionRepository;

    private VaccinationCenter center;
    private VaccinationCampaign campaign;
    private VaccinationSession session;
    private TimeSlot slot;
    private VaccineBatch batch;
    private CenterInventory inventory;
    private Appointment appointment;

    @BeforeEach
    void setUp() {
        tearDown();
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        center = centerRepository.save(new VaccinationCenter(
                "CTR-" + suffix, "Center " + suffix, "123 Clinic Way", "+123456789",
                CenterStatus.ACTIVE, 60, 120, "UTC"
        ));

        VaccineType vaccineType = vaccineTypeRepository.save(new VaccineType("VT-" + suffix, "Vaccine " + suffix));
        TargetGroup targetGroup = targetGroupRepository.save(new TargetGroup("TG-" + suffix, "Adults " + suffix));

        campaign = campaignRepository.save(CampaignTestFixtures.campaign(
                "CMP-" + suffix, "Campaign " + suffix, "Description " + suffix,
                vaccineType, targetGroup,
                LocalDate.now().minusDays(1), LocalDate.now().plusMonths(6),
                Instant.now().minusSeconds(86400), Instant.now().plusSeconds(86400 * 30),
                CampaignStatus.PUBLISHED
        ));

        session = sessionRepository.save(new VaccinationSession(
                campaign, center,
                Instant.now().minusSeconds(3600), Instant.now().plusSeconds(7200),
                Instant.now().minusSeconds(7200), "UTC"
        ));

        slot = timeSlotRepository.save(new TimeSlot(
                session, Instant.now().minusSeconds(1800), Instant.now().plusSeconds(1800), 10
        ));

        appointment = appointmentRepository.save(new Appointment(
                1001L, campaign.getId(), slot.getId(), AppointmentStatus.CHECKED_IN, Instant.now().minusSeconds(600)
        ));

        Manufacturer manufacturer = manufacturerRepository.save(new Manufacturer("MF-" + suffix, "Maker " + suffix));

        batch = batchRepository.save(new VaccineBatch(
                manufacturer.getId(), vaccineType.getId(), "BATCH-" + suffix,
                LocalDate.now().minusDays(10), LocalDate.now().plusMonths(12),
                BatchStatus.ACTIVE
        ));

        inventory = centerInventoryRepository.save(new CenterInventory(center.getId(), batch.getId(), 5));
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        stockTransactionRepository.deleteAll();
        certificateRepository.deleteAll();
        recordRepository.deleteAll();
        draftRepository.deleteAll();
        appointmentRepository.deleteAll();
        timeSlotRepository.deleteAll();
        sessionRepository.deleteAll();
        campaignRepository.deleteAll();
        centerInventoryRepository.deleteAll();
        batchRepository.deleteAll();
        centerRepository.deleteAll();
        vaccineTypeRepository.deleteAll();
        targetGroupRepository.deleteAll();
        manufacturerRepository.deleteAll();
    }

    @Test
    void successfulFinalization_persistsRecord_deductsInventory_completesAppointment_issuesCertificate_andFinalizesDraft() {
        VaccinationDraft draft = new VaccinationDraft(appointment.getId());
        draft.setVaccinatorId(501L);
        draft.setVaccineBatchId(batch.getId());
        draft.setDoseNumber(1);
        draft.setAdministrationTime(Instant.now().minusSeconds(120));
        draft.setAuthorizedNotes("Clinical administration completed smoothly.");
        draft.setPhysicalAdministrationConfirmed(true);
        draft.setStatus(VaccinationDraftStatus.READY_FOR_FINALIZATION);
        draft = draftRepository.saveAndFlush(draft);

        FinalizeVaccinationResponse response = finalizationService.finalizeDraft(draft.getId(), 501L);

        assertNotNull(response);
        assertEquals(draft.getId(), response.draftId());
        assertNotNull(response.vaccinationRecordId());
        assertNotNull(response.certificateReference());

        // 1. Verify Vaccination Record persisted
        VaccinationRecord record = recordRepository.findById(response.vaccinationRecordId()).orElseThrow();
        assertEquals(appointment.getId(), record.getAppointmentId());
        assertEquals(1001L, record.getCitizenId());
        assertEquals(campaign.getId(), record.getCampaignId());
        assertEquals(center.getId(), record.getCenterId());
        assertEquals(501L, record.getVaccinatorId());
        assertEquals(batch.getId(), record.getVaccineBatchId());
        assertEquals(1, record.getDoseNumber());

        // 2. Verify Inventory deduction
        CenterInventory updatedInventory = centerInventoryRepository.findByCenterIdAndBatchId(center.getId(), batch.getId()).orElseThrow();
        assertEquals(4, updatedInventory.getQuantity());

        StockTransaction transaction = stockTransactionRepository.findByVaccinationRecordId(record.getId()).orElseThrow();
        assertEquals(StockTransactionType.VACCINATION_DEDUCTION, transaction.getType());
        assertEquals(-1, transaction.getDelta());
        assertEquals(5, transaction.getBeforeQty());
        assertEquals(4, transaction.getAfterQty());

        // 3. Verify Appointment completed
        Appointment updatedAppointment = appointmentRepository.findById(appointment.getId()).orElseThrow();
        assertEquals(AppointmentStatus.COMPLETED, updatedAppointment.getStatus());

        // 4. Verify Digital Certificate issued
        List<DigitalCertificate> certs = certificateRepository.findByVaccinationRecordIdOrderByIssuedAtAscIdAsc(record.getId());
        assertThat(certs).hasSize(1);
        DigitalCertificate cert = certs.get(0);
        assertEquals(CertificateStatus.VALID, cert.getStatus());
        assertEquals(response.certificateReference(), cert.getCertificateReference());

        // 5. Verify Draft transitioned to FINALIZED
        VaccinationDraft updatedDraft = draftRepository.findById(draft.getId()).orElseThrow();
        assertEquals(VaccinationDraftStatus.FINALIZED, updatedDraft.getStatus());
    }

    @Test
    void mismatchedBatchVaccineType_abortsFinalizationBeforeAnyIrreversibleStep() {
        // A batch of an unrelated vaccine type must never finalize: no record, no stock deduction,
        // no appointment completion, no certificate (R02 regression).
        VaccineType otherType = vaccineTypeRepository.save(new VaccineType(
                "VT-OTHER-" + UUID.randomUUID().toString().substring(0, 8), "Other Vaccine"));
        Manufacturer manufacturer = manufacturerRepository.save(new Manufacturer(
                "MF-OTHER-" + UUID.randomUUID().toString().substring(0, 8), "Other Maker"));
        VaccineBatch unrelatedBatch = batchRepository.save(new VaccineBatch(
                manufacturer.getId(), otherType.getId(), "BATCH-OTHER-" + UUID.randomUUID().toString().substring(0, 8),
                LocalDate.now().minusDays(10), LocalDate.now().plusMonths(12),
                BatchStatus.ACTIVE
        ));
        centerInventoryRepository.save(new CenterInventory(center.getId(), unrelatedBatch.getId(), 5));

        VaccinationDraft draft = new VaccinationDraft(appointment.getId());
        draft.setVaccinatorId(501L);
        draft.setVaccineBatchId(unrelatedBatch.getId());
        draft.setDoseNumber(1);
        draft.setAdministrationTime(Instant.now().minusSeconds(120));
        draft.setPhysicalAdministrationConfirmed(true);
        draft.setStatus(VaccinationDraftStatus.READY_FOR_FINALIZATION);
        draft = draftRepository.saveAndFlush(draft);

        Long draftId = draft.getId();

        assertThatThrownBy(() -> finalizationService.finalizeDraft(draftId, 501L))
                .isInstanceOf(VaccinationBatchVaccineTypeMismatchException.class);

        // No vaccination record created
        assertThat(recordRepository.existsByAppointmentId(appointment.getId())).isFalse();

        // Zero stock mutation on the unrelated batch
        CenterInventory unrelatedInventory = centerInventoryRepository
                .findByCenterIdAndBatchId(center.getId(), unrelatedBatch.getId()).orElseThrow();
        assertEquals(5, unrelatedInventory.getQuantity());

        // Appointment remains CHECKED_IN
        Appointment currentAppointment = appointmentRepository.findById(appointment.getId()).orElseThrow();
        assertEquals(AppointmentStatus.CHECKED_IN, currentAppointment.getStatus());

        // No digital certificate issued
        assertThat(certificateRepository.findAll()).isEmpty();

        // Draft remains READY_FOR_FINALIZATION
        VaccinationDraft currentDraft = draftRepository.findById(draftId).orElseThrow();
        assertEquals(VaccinationDraftStatus.READY_FOR_FINALIZATION, currentDraft.getStatus());
    }

    @Test
    void downstreamFailure_rollsBackTransactionAtomically() {
        // Set stock quantity to 0 so deduction fails due to INSUFFICIENT_STOCK
        inventory.setQuantity(0);
        centerInventoryRepository.saveAndFlush(inventory);

        VaccinationDraft draft = new VaccinationDraft(appointment.getId());
        draft.setVaccinatorId(501L);
        draft.setVaccineBatchId(batch.getId());
        draft.setDoseNumber(1);
        draft.setAdministrationTime(Instant.now().minusSeconds(60));
        draft.setPhysicalAdministrationConfirmed(true);
        draft.setStatus(VaccinationDraftStatus.READY_FOR_FINALIZATION);
        draft = draftRepository.saveAndFlush(draft);

        Long draftId = draft.getId();

        assertThatThrownBy(() -> finalizationService.finalizeDraft(draftId, 501L))
                .isInstanceOf(BatchUnavailableException.class);

        // Assert atomic rollback:
        // No vaccination record created
        assertThat(recordRepository.existsByAppointmentId(appointment.getId())).isFalse();

        // Appointment remains CHECKED_IN
        Appointment currentAppointment = appointmentRepository.findById(appointment.getId()).orElseThrow();
        assertEquals(AppointmentStatus.CHECKED_IN, currentAppointment.getStatus());

        // Draft remains READY_FOR_FINALIZATION
        VaccinationDraft currentDraft = draftRepository.findById(draftId).orElseThrow();
        assertEquals(VaccinationDraftStatus.READY_FOR_FINALIZATION, currentDraft.getStatus());

        // No digital certificate exists for this appointment
        assertThat(certificateRepository.findAll().stream()
                .filter(c -> recordRepository.findById(c.getVaccinationRecordId())
                        .map(r -> r.getAppointmentId().equals(appointment.getId()))
                        .orElse(false)))
                .isEmpty();
    }

    @Test
    void duplicateFinalization_isRejectedAndPreventsDoubleDeduction() {
        VaccinationDraft draft = new VaccinationDraft(appointment.getId());
        draft.setVaccinatorId(501L);
        draft.setVaccineBatchId(batch.getId());
        draft.setDoseNumber(1);
        draft.setAdministrationTime(Instant.now().minusSeconds(120));
        draft.setPhysicalAdministrationConfirmed(true);
        draft.setStatus(VaccinationDraftStatus.READY_FOR_FINALIZATION);
        draft = draftRepository.saveAndFlush(draft);

        // First finalization succeeds
        FinalizeVaccinationResponse firstResponse = finalizationService.finalizeDraft(draft.getId(), 501L);
        assertNotNull(firstResponse);

        CenterInventory inventoryAfterFirst = centerInventoryRepository.findByCenterIdAndBatchId(center.getId(), batch.getId()).orElseThrow();
        assertEquals(4, inventoryAfterFirst.getQuantity());

        // Second finalization attempt fails
        Long draftId = draft.getId();
        assertThatThrownBy(() -> finalizationService.finalizeDraft(draftId, 501L))
                .isInstanceOf(InvalidVaccinationDraftException.class)
                .hasMessageContaining("already finalized");

        // Inventory is not deducted again
        CenterInventory inventoryAfterSecond = centerInventoryRepository.findByCenterIdAndBatchId(center.getId(), batch.getId()).orElseThrow();
        assertEquals(4, inventoryAfterSecond.getQuantity());
    }
}

package com.se2030.vaccinationportal.vaccination.certificate.service;

import com.se2030.vaccinationportal.appointment.api.vaccination.AppointmentValidationPort;
import com.se2030.vaccinationportal.appointment.api.vaccination.AppointmentValidationSnapshot;
import com.se2030.vaccinationportal.campaign.api.vaccination.CampaignVaccinationReader;
import com.se2030.vaccinationportal.inventory.dependency.AvailableBatchSnapshot;
import com.se2030.vaccinationportal.inventory.dependency.BatchDisplaySnapshot;
import com.se2030.vaccinationportal.inventory.dependency.InventoryBatchPort;
import com.se2030.vaccinationportal.shared.auth.api.AccountDisplayReader;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationRecord;
import com.se2030.vaccinationportal.vaccination.administration.repository.VaccinationRecordRepository;
import com.se2030.vaccinationportal.vaccination.certificate.repository.DigitalCertificateRepository;
import com.se2030.vaccinationportal.vaccination.query.service.EffectiveVaccinationRecordService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DigitalCertificatePortDisplayTests {
    private final VaccinationRecordRepository records = mock(VaccinationRecordRepository.class);
    private final DigitalCertificateRepository certificates = mock(DigitalCertificateRepository.class);
    private final InventoryBatchPort inventory = mock(InventoryBatchPort.class);
    private final CampaignVaccinationReader campaign = mock(CampaignVaccinationReader.class);
    private final AccountDisplayReader accounts = mock(AccountDisplayReader.class);
    private final AppointmentValidationPort appointments = mock(AppointmentValidationPort.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-02T08:30:00Z"), ZoneOffset.UTC);
    private final DigitalCertificateService service = new DigitalCertificateService(records, certificates,
            mock(EffectiveVaccinationRecordService.class), clock);

    private void connectPorts() {
        service.setInventoryBatchPort(inventory);
        service.setCampaignVaccinationReader(campaign);
        service.setAccountDisplayReader(accounts);
        service.setAppointmentValidationPort(appointments);
    }

    @Test
    void recordDetailsRetainNamesAndBatchIdentityFromOwnerPorts() {
        connectPorts();
        VaccinationRecord record = mock(VaccinationRecord.class);
        when(record.getId()).thenReturn(1L);
        when(record.getCitizenId()).thenReturn(2L);
        when(record.getCampaignId()).thenReturn(3L);
        when(record.getCenterId()).thenReturn(4L);
        when(record.getVaccineBatchId()).thenReturn(5L);
        when(records.findById(1L)).thenReturn(Optional.of(record));
        when(certificates.findFirstByVaccinationRecordIdAndStatusOrderByIssuedAtDescIdDesc(
                1L, com.se2030.vaccinationportal.vaccination.certificate.model.CertificateStatus.VALID))
                .thenReturn(Optional.empty());
        when(certificates.findByVaccinationRecordIdOrderByIssuedAtAscIdAsc(1L)).thenReturn(List.of());
        when(accounts.findFullName(2L)).thenReturn(Optional.of("Citizen Name"));
        when(campaign.findCampaignNameById(3L)).thenReturn(Optional.of("Campaign Name"));
        when(campaign.findCenterNameById(4L)).thenReturn(Optional.of("Center Name"));
        when(campaign.findVaccineTypeNameById(6L)).thenReturn(Optional.of("Vaccine Name"));
        when(inventory.findBatchDisplay(5L)).thenReturn(Optional.of(new BatchDisplaySnapshot("LOT-5", 6L)));

        var details = service.getVaccinationRecordDetails(1L);
        assertThat(details.citizenName()).isEqualTo("Citizen Name");
        assertThat(details.campaignName()).isEqualTo("Campaign Name");
        assertThat(details.centerName()).isEqualTo("Center Name");
        assertThat(details.batchNumber()).isEqualTo("LOT-5");
        assertThat(details.vaccineName()).isEqualTo("Vaccine Name");
    }

    @Test
    void absentOwnerDataRetainsExistingDisplayFallbacks() {
        connectPorts();
        VaccinationRecord record = mock(VaccinationRecord.class);
        when(record.getId()).thenReturn(1L);
        when(record.getCitizenId()).thenReturn(2L);
        when(record.getCampaignId()).thenReturn(3L);
        when(record.getCenterId()).thenReturn(4L);
        when(record.getVaccineBatchId()).thenReturn(5L);
        when(records.findById(1L)).thenReturn(Optional.of(record));
        when(certificates.findFirstByVaccinationRecordIdAndStatusOrderByIssuedAtDescIdDesc(
                1L, com.se2030.vaccinationportal.vaccination.certificate.model.CertificateStatus.VALID))
                .thenReturn(Optional.empty());
        when(certificates.findByVaccinationRecordIdOrderByIssuedAtAscIdAsc(1L)).thenReturn(List.of());
        when(accounts.findFullName(2L)).thenReturn(Optional.empty());
        when(campaign.findCampaignNameById(3L)).thenReturn(Optional.empty());
        when(campaign.findCenterNameById(4L)).thenReturn(Optional.empty());
        when(inventory.findBatchDisplay(5L)).thenReturn(Optional.empty());

        var details = service.getVaccinationRecordDetails(1L);
        assertThat(details.citizenName()).isEqualTo("Citizen #2");
        assertThat(details.campaignName()).isNull();
        assertThat(details.centerName()).isEqualTo("Center #4");
        assertThat(details.batchNumber()).isEqualTo("Batch #5");
        assertThat(details.vaccineName()).isEqualTo("Authorized Vaccine");
    }

    @Test
    void appointmentBatchSelectionUsesInventoryFilteringAndCampaignDisplayName() {
        connectPorts();
        LocalDate today = LocalDate.of(2026, 9, 2);
        when(appointments.findByAppointmentId(9L)).thenReturn(Optional.of(
                new AppointmentValidationSnapshot(9L, true, 2L, 3L, 8L, 4L, 6L)));
        when(inventory.findAvailableBatches(4L, 6L, today)).thenReturn(List.of(
                new AvailableBatchSnapshot(5L, "LOT-5", 6L, today.plusMonths(1), 3)));
        when(campaign.findVaccineTypeNameById(6L)).thenReturn(Optional.of("Vaccine Name"));

        var batches = service.getAvailableBatchesForAppointment(9L);
        assertThat(batches).hasSize(1);
        assertThat(batches.get(0).id()).isEqualTo(5L);
        assertThat(batches.get(0).vaccineName()).isEqualTo("Vaccine Name");
        assertThat(batches.get(0).availableQuantity()).isEqualTo(3);
    }
}

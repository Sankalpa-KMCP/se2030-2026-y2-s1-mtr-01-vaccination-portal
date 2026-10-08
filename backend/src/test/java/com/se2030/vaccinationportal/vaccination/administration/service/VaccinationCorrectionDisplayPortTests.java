package com.se2030.vaccinationportal.vaccination.administration.service;

import com.se2030.vaccinationportal.shared.auth.api.AccountDisplayReader;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationCorrection;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationRecord;
import com.se2030.vaccinationportal.vaccination.administration.repository.VaccinationCorrectionRepository;
import com.se2030.vaccinationportal.vaccination.administration.repository.VaccinationRecordRepository;
import com.se2030.vaccinationportal.vaccination.administration.strategy.CorrectionRequestStrategy;
import com.se2030.vaccinationportal.vaccination.certificate.repository.DigitalCertificateRepository;
import com.se2030.vaccinationportal.vaccination.query.service.EffectiveVaccinationRecordService;
import com.se2030.vaccinationportal.vaccination.validation.VaccinationCorrectionValueCodec;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VaccinationCorrectionDisplayPortTests {
    private final VaccinationRecordRepository records = mock(VaccinationRecordRepository.class);
    private final VaccinationCorrectionRepository corrections = mock(VaccinationCorrectionRepository.class);
    private final DigitalCertificateRepository certificates = mock(DigitalCertificateRepository.class);
    private final AccountDisplayReader accounts = mock(AccountDisplayReader.class);
    private final VaccinationCorrectionService service = new VaccinationCorrectionService(records, corrections,
            mock(EffectiveVaccinationRecordService.class), mock(VaccinationCorrectionValueCodec.class),
            Clock.systemUTC(), certificates, accounts, List.of(mock(CorrectionRequestStrategy.class)));

    @Test
    void correctionDetailRetainsCitizenNameAndVaccinatorEmailFallback() {
        VaccinationCorrection correction = mock(VaccinationCorrection.class);
        VaccinationRecord record = mock(VaccinationRecord.class);
        when(correction.getVaccinationRecordId()).thenReturn(1L);
        when(record.getId()).thenReturn(1L);
        when(record.getCitizenId()).thenReturn(2L);
        when(record.getVaccinatorId()).thenReturn(3L);
        when(records.findById(1L)).thenReturn(Optional.of(record));
        when(certificates.findByVaccinationRecordIdOrderByIssuedAtAscIdAsc(1L)).thenReturn(List.of());
        when(accounts.findFullName(2L)).thenReturn(Optional.of("Citizen Name"));
        when(accounts.findDisplayName(3L)).thenReturn(Optional.of("vaccinator@example.com"));

        var detail = service.toDetailResponse(correction);
        assertThat(detail.citizenName()).isEqualTo("Citizen Name");
        assertThat(detail.vaccinatorName()).isEqualTo("vaccinator@example.com");
    }

    @Test
    void missingAccountsRetainIdentityFallbacks() {
        VaccinationCorrection correction = mock(VaccinationCorrection.class);
        VaccinationRecord record = mock(VaccinationRecord.class);
        when(correction.getVaccinationRecordId()).thenReturn(1L);
        when(record.getId()).thenReturn(1L);
        when(record.getCitizenId()).thenReturn(2L);
        when(record.getVaccinatorId()).thenReturn(3L);
        when(records.findById(1L)).thenReturn(Optional.of(record));
        when(certificates.findByVaccinationRecordIdOrderByIssuedAtAscIdAsc(1L)).thenReturn(List.of());
        when(accounts.findFullName(2L)).thenReturn(Optional.empty());
        when(accounts.findDisplayName(3L)).thenReturn(Optional.empty());

        var detail = service.toDetailResponse(correction);
        assertThat(detail.citizenName()).isEqualTo("Citizen #2");
        assertThat(detail.vaccinatorName()).isEqualTo("User #3");
    }
}

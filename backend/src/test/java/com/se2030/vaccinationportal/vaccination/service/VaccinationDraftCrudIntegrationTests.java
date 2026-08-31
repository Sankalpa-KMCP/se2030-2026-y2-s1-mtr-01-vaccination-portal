package com.se2030.vaccinationportal.vaccination.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.se2030.vaccinationportal.vaccination.dependency.AppointmentValidationPort;
import com.se2030.vaccinationportal.vaccination.dependency.AppointmentValidationSnapshot;
import com.se2030.vaccinationportal.vaccination.dto.CreateVaccinationDraftRequest;
import com.se2030.vaccinationportal.vaccination.dto.UpdateVaccinationDraftRequest;
import com.se2030.vaccinationportal.vaccination.model.VaccinationDraft;
import com.se2030.vaccinationportal.vaccination.repository.DigitalCertificateRepository;
import com.se2030.vaccinationportal.vaccination.repository.VaccinationDraftRepository;
import com.se2030.vaccinationportal.vaccination.repository.VaccinationRecordRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VaccinationDraftCrudIntegrationTests {

    @Autowired
    private VaccinationDraftService draftService;

    @Autowired
    private VaccinationDraftRepository draftRepository;

    @Autowired
    private VaccinationRecordRepository recordRepository;

    @Autowired
    private DigitalCertificateRepository certificateRepository;

    @MockitoBean
    private AppointmentValidationPort appointmentValidationPort;

    @Test
    void draftCrudDoesNotCreatePermanentRecordsOrCertificates() {
        when(appointmentValidationPort.findByAppointmentId(99001L))
                .thenReturn(Optional.of(new AppointmentValidationSnapshot(99001L, true)));
        long recordCountBefore = recordRepository.count();
        long certificateCountBefore = certificateRepository.count();

        VaccinationDraft created = draftService.createDraft(new CreateVaccinationDraftRequest(
                99001L,
                99002L,
                99003L,
                1,
                Instant.now().minusSeconds(60),
                null,
                "Temporary draft",
                false
        ));
        assertTrue(draftRepository.existsById(created.getId()));

        VaccinationDraft updated = draftService.updateDraft(
                created.getId(),
                new UpdateVaccinationDraftRequest(
                        null, null, 2, null, null, "Updated temporary draft", null
                )
        );
        assertEquals(2, updated.getDoseNumber());

        draftService.deleteDraft(created.getId());
        draftRepository.flush();

        assertFalse(draftRepository.existsById(created.getId()));
        assertEquals(recordCountBefore, recordRepository.count());
        assertEquals(certificateCountBefore, certificateRepository.count());
    }
}

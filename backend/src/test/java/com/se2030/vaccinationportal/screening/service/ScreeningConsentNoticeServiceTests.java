package com.se2030.vaccinationportal.screening.service;

import com.se2030.vaccinationportal.screening.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.screening.model.ConsentNoticeVersion;
import com.se2030.vaccinationportal.screening.repository.ConsentNoticeVersionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScreeningConsentNoticeServiceTests {

    @Mock
    private ConsentNoticeVersionRepository repository;

    @Test
    void resolvesLatestNonRetiredNoticeOnlyWithinCanonicalScreeningFamily() {
        ConsentNoticeVersion latest = new ConsentNoticeVersion();
        latest.setId(30L);
        latest.setNoticeCode(ScreeningConsentNoticeService.SCREENING_NOTICE_CODE);
        latest.setVersionNumber(3);
        when(repository.findFirstByNoticeCodeAndRetiredAtIsNullOrderByVersionNumberDesc(
                ScreeningConsentNoticeService.SCREENING_NOTICE_CODE
        )).thenReturn(Optional.of(latest));

        ScreeningConsentNoticeService service = new ScreeningConsentNoticeService(repository);

        assertThat(service.getCurrentScreeningConsentNotice()).isSameAs(latest);
        verify(repository).findFirstByNoticeCodeAndRetiredAtIsNullOrderByVersionNumberDesc(
                "PRE_VACCINATION_SCREENING"
        );
    }

    @Test
    void missingCanonicalScreeningNoticeIsReportedAsMissingResource() {
        when(repository.findFirstByNoticeCodeAndRetiredAtIsNullOrderByVersionNumberDesc(
                ScreeningConsentNoticeService.SCREENING_NOTICE_CODE
        )).thenReturn(Optional.empty());

        ScreeningConsentNoticeService service = new ScreeningConsentNoticeService(repository);

        assertThatThrownBy(service::getCurrentScreeningConsentNotice)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("PRE_VACCINATION_SCREENING");
    }
}

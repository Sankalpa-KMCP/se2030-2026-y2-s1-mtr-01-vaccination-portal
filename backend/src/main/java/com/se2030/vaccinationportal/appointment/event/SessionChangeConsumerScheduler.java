package com.se2030.vaccinationportal.appointment.event;

import com.se2030.vaccinationportal.campaign.api.session.SessionChangeNotice;
import com.se2030.vaccinationportal.campaign.api.session.SessionChangeReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Polls the durable MF-02 change stream; persisted consumption rows make retries idempotent. */
@Component
@ConditionalOnProperty(name = "app.mf03.session-change-consumer.enabled",
        havingValue = "true", matchIfMissing = true)
public class SessionChangeConsumerScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SessionChangeConsumerScheduler.class);
    private final SessionChangeReader reader;
    private final SessionChangeEventProcessor processor;
    private final SessionChangeFailureRecorder failures;

    public SessionChangeConsumerScheduler(SessionChangeReader reader,
            SessionChangeEventProcessor processor, SessionChangeFailureRecorder failures) {
        this.reader = reader;
        this.processor = processor;
        this.failures = failures;
    }

    @Scheduled(fixedDelayString = "${app.mf03.session-change-consumer.poll-ms:5000}")
    public void processPending() {
        for (SessionChangeNotice notice : reader.findAllOrdered()) {
            try {
                processor.process(notice);
            } catch (RuntimeException error) {
                failures.record(notice.eventId(), error);
                LOGGER.warn("MF-03 could not consume session change event {}: {}",
                        notice.eventId(), error.getClass().getSimpleName());
            }
        }
    }
}

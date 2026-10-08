package com.se2030.vaccinationportal.appointment.event;

import com.se2030.vaccinationportal.appointment.persistence.model.ModuleEventConsumption;
import com.se2030.vaccinationportal.appointment.persistence.model.ModuleEventConsumptionStatus;
import com.se2030.vaccinationportal.appointment.persistence.repository.ModuleEventConsumptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Persists a retryable failure after the business transaction has rolled back. */
@Service
public class SessionChangeFailureRecorder {
    private final ModuleEventConsumptionRepository repository;

    public SessionChangeFailureRecorder(ModuleEventConsumptionRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long eventId, RuntimeException error) {
        ModuleEventConsumption consumption = repository
                .findByConsumerCodeAndSourceEventId(SessionChangeEventProcessor.CONSUMER_CODE, eventId)
                .orElse(null);
        if (consumption != null && consumption.getStatus() == ModuleEventConsumptionStatus.COMPLETED) {
            return;
        }
        if (consumption != null && consumption.getStatus() == ModuleEventConsumptionStatus.PROCESSING) {
            return;
        }
        if (consumption == null) {
            consumption = new ModuleEventConsumption(SessionChangeEventProcessor.CONSUMER_CODE, eventId);
        } else {
            consumption.retry();
        }
        consumption.fail(error.getMessage());
        repository.save(consumption);
    }
}

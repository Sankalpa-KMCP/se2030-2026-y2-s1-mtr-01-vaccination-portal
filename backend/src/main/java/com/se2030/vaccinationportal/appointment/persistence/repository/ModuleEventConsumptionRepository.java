package com.se2030.vaccinationportal.appointment.persistence.repository;

import com.se2030.vaccinationportal.appointment.persistence.model.ModuleEventConsumption;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModuleEventConsumptionRepository extends JpaRepository<ModuleEventConsumption, Long> {
    Optional<ModuleEventConsumption> findByConsumerCodeAndSourceEventId(String consumerCode, Long sourceEventId);
}

package com.se2030.vaccinationportal.appointment.persistence.repository;

import com.se2030.vaccinationportal.appointment.persistence.model.WaitlistChangeEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WaitlistChangeEventRepository extends JpaRepository<WaitlistChangeEvent, Long> {
    List<WaitlistChangeEvent> findByWaitlistEntryIdOrderByCreatedAtAscIdAsc(Long waitlistEntryId);
}

package com.se2030.vaccinationportal.campaign.repository;

import com.se2030.vaccinationportal.campaign.model.TimeSlot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ts FROM TimeSlot ts WHERE ts.id = :id")
    Optional<TimeSlot> findByIdForUpdate(@Param("id") Long id);

    List<TimeSlot> findBySessionIdOrderByStartTimeAsc(Long sessionId);

    List<TimeSlot> findBySessionId(Long sessionId);

    @Query("SELECT SUM(ts.capacity) FROM TimeSlot ts WHERE ts.session.id = :sessionId")
    Integer sumCapacityBySessionId(@Param("sessionId") Long sessionId);

    @Query("SELECT SUM(ts.reservedCount) FROM TimeSlot ts WHERE ts.session.id = :sessionId")
    Integer sumReservedCountBySessionId(@Param("sessionId") Long sessionId);

    void deleteBySessionId(Long sessionId);
}

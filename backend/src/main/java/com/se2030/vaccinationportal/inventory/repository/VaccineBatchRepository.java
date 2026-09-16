package com.se2030.vaccinationportal.inventory.repository;

import com.se2030.vaccinationportal.inventory.model.VaccineBatch;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VaccineBatchRepository extends JpaRepository<VaccineBatch, Long> {
    boolean existsByManufacturerId(Long manufacturerId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from VaccineBatch b where b.id = :id")
    Optional<VaccineBatch> findByIdForUpdate(@Param("id") Long id);
}

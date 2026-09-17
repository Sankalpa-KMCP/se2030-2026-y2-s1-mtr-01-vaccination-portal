package com.se2030.vaccinationportal.inventory.repository;

import com.se2030.vaccinationportal.inventory.model.CenterInventory;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CenterInventoryRepository extends JpaRepository<CenterInventory, Long> {
    boolean existsByBatchId(Long batchId);
    Optional<CenterInventory> findByCenterIdAndBatchId(Long centerId, Long batchId);
    List<CenterInventory> findAllByBatchId(Long batchId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from CenterInventory i where i.id = :id")
    Optional<CenterInventory> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from CenterInventory i where i.centerId = :centerId and i.batchId = :batchId")
    Optional<CenterInventory> findByCenterAndBatchForUpdate(
            @Param("centerId") Long centerId, @Param("batchId") Long batchId);
}

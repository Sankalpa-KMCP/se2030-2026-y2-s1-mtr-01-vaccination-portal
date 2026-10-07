package com.se2030.vaccinationportal.inventory.repository;

import com.se2030.vaccinationportal.inventory.model.StockTransaction;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

public interface StockTransactionRepository extends JpaRepository<StockTransaction, Long> {
    boolean existsByInventoryId(Long inventoryId);
    boolean existsByVaccinationRecordId(Long vaccinationRecordId);
    Optional<StockTransaction> findByVaccinationRecordId(Long vaccinationRecordId);
    List<StockTransaction> findTop100ByOrderByCreatedAtDescIdDesc();

    @Query("select case when count(t) > 0 then true else false end from StockTransaction t "
            + "where t.inventoryId in (select i.id from CenterInventory i where i.batchId = :batchId)")
    boolean existsByBatchId(@Param("batchId") Long batchId);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select t.id from StockTransaction t where t.inventoryId in :inventoryIds")
    List<Long> findIdsByInventoryIdsForUpdate(@Param("inventoryIds") List<Long> inventoryIds);
}

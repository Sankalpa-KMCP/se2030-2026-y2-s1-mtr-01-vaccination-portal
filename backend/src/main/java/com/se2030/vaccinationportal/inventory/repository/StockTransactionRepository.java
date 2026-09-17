package com.se2030.vaccinationportal.inventory.repository;

import com.se2030.vaccinationportal.inventory.model.StockTransaction;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockTransactionRepository extends JpaRepository<StockTransaction, Long> {
    boolean existsByInventoryId(Long inventoryId);
    boolean existsByVaccinationRecordId(Long vaccinationRecordId);
    Optional<StockTransaction> findByVaccinationRecordId(Long vaccinationRecordId);
    List<StockTransaction> findTop100ByOrderByCreatedAtDescIdDesc();
}

package com.se2030.vaccinationportal.inventory.repository;

import com.se2030.vaccinationportal.inventory.model.StockTransfer;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockTransferRepository extends JpaRepository<StockTransfer, Long> {
    List<StockTransfer> findTop100ByOrderByIdDesc();
}

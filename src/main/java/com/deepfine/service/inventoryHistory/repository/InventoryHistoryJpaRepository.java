package com.deepfine.service.inventoryHistory.repository;

import com.deepfine.service.inventoryHistory.domain.InventoryHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryHistoryJpaRepository extends JpaRepository<InventoryHistoryEntity, Long> {
}

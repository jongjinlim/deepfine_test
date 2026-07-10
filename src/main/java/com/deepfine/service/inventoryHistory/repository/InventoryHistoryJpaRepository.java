package com.deepfine.service.inventoryHistory.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryHistoryJpaRepository extends JpaRepository<InventoryHistoryEntity, Long> {

    List<InventoryHistoryEntity> findByInventoryId(Long inventoryId);
}

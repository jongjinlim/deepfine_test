package com.deepfine.service.inventory.service;

import com.deepfine.service.inventory.domain.Inventory;

import java.util.Optional;

public interface InventoryRepository {

    Optional<Inventory> findById(Long id);

    Optional<Inventory> findByName(String name);

    Inventory save(Inventory inventory);
}

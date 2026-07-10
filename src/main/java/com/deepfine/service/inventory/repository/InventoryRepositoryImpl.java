package com.deepfine.service.inventory.repository;

import com.deepfine.service.inventory.domain.Inventory;
import com.deepfine.service.inventory.service.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class InventoryRepositoryImpl implements InventoryRepository {

    private final InventoryJpaRepository inventoryJpaRepository;

    @Override
    public Optional<Inventory> findById(Long id) {
        return inventoryJpaRepository.findById(id).map(InventoryEntity::toModel);
    }

    @Override
    public Optional<Inventory> findByName(String name) {
        return inventoryJpaRepository.findByName(name).map(InventoryEntity::toModel);
    }

    @Override
    public Inventory save(Inventory inventory) {
        return inventoryJpaRepository.save(InventoryEntity.from(inventory)).toModel();
    }
}

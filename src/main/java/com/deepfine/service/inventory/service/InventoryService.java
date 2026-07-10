package com.deepfine.service.inventory.service;

import com.deepfine.error.ErrorCode;
import com.deepfine.error.GlobalException;
import com.deepfine.service.inventory.domain.Inventory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryLockedOperations inventoryLockedOperations;

    public Inventory register(String name) {
        return inventoryLockedOperations.register(name);
    }

    public Inventory getInventory(Long inventoryId) {
        return inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new GlobalException(ErrorCode.PRODUCT_NOT_FOUND, "id=" + inventoryId));
    }

    public Inventory increase(String name, int quantity) {
        return inventoryLockedOperations.increase(name, quantity);
    }

    public Inventory decrease(Long inventoryId, int quantity) {
        String name = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new GlobalException(ErrorCode.PRODUCT_NOT_FOUND, "id=" + inventoryId))
                .getName();
        return inventoryLockedOperations.decrease(name, inventoryId, quantity);
    }
}

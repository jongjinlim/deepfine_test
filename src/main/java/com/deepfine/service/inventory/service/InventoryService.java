package com.deepfine.service.inventory.service;

import com.deepfine.config.redis.DistributedLock;
import com.deepfine.enums.InventoryChangeType;
import com.deepfine.error.ErrorCode;
import com.deepfine.error.GlobalException;
import com.deepfine.service.inventory.domain.Inventory;
import com.deepfine.service.inventoryHistory.domain.InventoryChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    private final ApplicationEventPublisher eventPublisher;

    public Inventory register(String name) {
        inventoryRepository.findByName(name).ifPresent(existing -> {
            throw new GlobalException(ErrorCode.DUPLICATE_PRODUCT_NAME, "name=" + name);
        });
        return inventoryRepository.save(Inventory.builder().name(name).quantity(0).build());
    }

    @DistributedLock(key = "#name")
    public Inventory increase(String name, int quantity) {
        Inventory inventory = inventoryRepository.findByName(name)
                .orElseGet(() -> Inventory.builder().name(name).quantity(0).build());
        inventory.increase(quantity);
        Inventory saved = inventoryRepository.save(inventory);
        publish(saved, InventoryChangeType.INCREASE, quantity);
        return saved;
    }

    @DistributedLock(key = "#inventoryId")
    public Inventory decrease(Long inventoryId, int quantity) {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new GlobalException(ErrorCode.PRODUCT_NOT_FOUND, "id=" + inventoryId));
        inventory.decrease(quantity);
        Inventory saved = inventoryRepository.save(inventory);
        publish(saved, InventoryChangeType.DECREASE, quantity);
        return saved;
    }

    private void publish(Inventory inventory, InventoryChangeType type, int changeQuantity) {
        eventPublisher.publishEvent(
                new InventoryChangedEvent(inventory.getInventoryId(), type, changeQuantity, inventory.getQuantity()));
    }

}

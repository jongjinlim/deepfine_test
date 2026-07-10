package com.deepfine.service.inventory.service;

import com.deepfine.config.redis.DistributedLock;
import com.deepfine.enums.InventoryChangeType;
import com.deepfine.error.ErrorCode;
import com.deepfine.error.GlobalException;
import com.deepfine.service.inventory.domain.Inventory;
import com.deepfine.service.inventoryHistory.domain.InventoryChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 재고 변경(등록/입고/출고)은 전부 상품명(name) 기준 분산락으로 묶여 동작해야 한다.
 * decrease는 id만 받지만 결국 같은 상품(name)에 대한 락을 걸어야 register/increase와
 * 동시에 들어와도 정합성이 유지되므로, id로 조회한 name을 여기 인자로 넘겨 받는다.
 * 같은 이름으로 lock 을 걸어야 입/출고가 동시에 진행되도 동시성 & 데이터 정합성 유지가능 ..
 */
@Component
@RequiredArgsConstructor
public class InventoryLockedOperations {

    private final InventoryRepository inventoryRepository;
    private final ApplicationEventPublisher eventPublisher;

    @DistributedLock(key = "#name")
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

    @DistributedLock(key = "#name")
    public Inventory decrease(String name, Long inventoryId, int quantity) {
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

package com.deepfine.service.inventoryHistory.domain;

import com.deepfine.enums.InventoryChangeType;

/**
 * 재고 수량이 변경(입고/출고)된 뒤 발행되는 이벤트.
 * 재고 변경 트랜잭션이 커밋된 후 이력 기록 등 후속 처리를 트리거하기 위해 사용한다.
 */
public record InventoryChangedEvent(
        Long inventoryId,
        InventoryChangeType type,
        int changeQuantity,
        int resultQuantity
) {
}

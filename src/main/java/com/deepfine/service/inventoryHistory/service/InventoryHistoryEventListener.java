package com.deepfine.service.inventoryHistory.service;

import com.deepfine.service.inventoryHistory.domain.InventoryChangedEvent;
import com.deepfine.service.inventoryHistory.repository.InventoryHistoryEntity;
import com.deepfine.service.inventoryHistory.repository.InventoryHistoryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class InventoryHistoryEventListener {

    private final InventoryHistoryJpaRepository inventoryHistoryJpaRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(InventoryChangedEvent event) {
        inventoryHistoryJpaRepository.save(InventoryHistoryEntity.from(event));
    }

}

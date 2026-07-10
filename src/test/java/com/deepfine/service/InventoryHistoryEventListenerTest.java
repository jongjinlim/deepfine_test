package com.deepfine.service;

import com.deepfine.enums.InventoryChangeType;
import com.deepfine.service.inventory.domain.Inventory;
import com.deepfine.service.inventory.repository.InventoryJpaRepository;
import com.deepfine.service.inventory.service.InventoryService;
import com.deepfine.service.inventoryHistory.repository.InventoryHistoryEntity;
import com.deepfine.service.inventoryHistory.repository.InventoryHistoryJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 재고 변경(입고/출고) 트랜잭션이 커밋된 뒤 InventoryHistoryEventListener가
 * 이력을 정확히 남기는지 검증하는 통합 테스트.
 * docker-compose의 redis/postgres가 떠 있어야 한다.
 * 테스트가 만든 재고/이력만 inventoryId로 targeting해서 지우기 때문에,
 * 기존 DB에 있던 다른 데이터에는 영향을 주지 않는다.
 */
@SpringBootTest
class InventoryHistoryEventListenerTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryJpaRepository inventoryJpaRepository;

    @Autowired
    private InventoryHistoryJpaRepository inventoryHistoryJpaRepository;

    private Long inventoryId;

    @AfterEach
    void tearDown() {
        if (inventoryId == null) {
            return;
        }
        inventoryHistoryJpaRepository.deleteAll(inventoryHistoryJpaRepository.findByInventoryId(inventoryId));
        inventoryJpaRepository.deleteById(inventoryId);
    }

    @Test
    @DisplayName("재고 입고 시에 이력을 쌓는다.")
    void increaseHistoryTest() {
        String name = "SPRING_ONION-" + UUID.randomUUID();

        Inventory inventory = inventoryService.increase(name, 50);
        inventoryId = inventory.getInventoryId();

        List<InventoryHistoryEntity> histories = awaitHistories(inventoryId, 1);
        InventoryHistoryEntity history = histories.get(0);

        System.out.println("기록된 이력 = type: " + history.getType()
                + ", changeQuantity: " + history.getChangeQuantity()
                + ", resultQuantity: " + history.getResultQuantity());

        assertThat(history.getType()).isEqualTo(InventoryChangeType.INCREASE);
        assertThat(history.getChangeQuantity()).isEqualTo(50);
        assertThat(history.getResultQuantity()).isEqualTo(50);
    }

    @Test
    @DisplayName("재고 출고 시에 이력을 쌓는다.")
    void decreaseHistoryTest() {
        Inventory inventory = inventoryService.increase("TEST_PRODUCT_001", 100);
        inventoryId = inventory.getInventoryId();

        inventoryService.decrease(inventoryId, 30);

        List<InventoryHistoryEntity> histories = awaitHistories(inventoryId, 2);
        InventoryHistoryEntity decreaseHistory = histories.stream()
                .filter(h -> h.getType() == InventoryChangeType.DECREASE)
                .findFirst()
                .orElseThrow();

        System.out.println("기록된 이력 = type: " + decreaseHistory.getType()
                + ", changeQuantity: " + decreaseHistory.getChangeQuantity()
                + ", resultQuantity: " + decreaseHistory.getResultQuantity());

        assertThat(decreaseHistory.getChangeQuantity()).isEqualTo(30);
        assertThat(decreaseHistory.getResultQuantity()).isEqualTo(70);
    }

    /**
     * @Async로 도는 리스너라 커밋 즉시 이력이 보이지 않을 수 있어서, 생길 때까지 짧게 폴링한다.
     */
    private List<InventoryHistoryEntity> awaitHistories(Long inventoryId, int expectedCount) {
        long deadline = System.currentTimeMillis() + 3000;
        while (System.currentTimeMillis() < deadline) {
            List<InventoryHistoryEntity> histories = inventoryHistoryJpaRepository.findByInventoryId(inventoryId);
            if (histories.size() >= expectedCount) {
                return histories;
            }
            sleep(100);
        }
        throw new AssertionError("이력이 시간 내에 기록되지 않았습니다. inventoryId=" + inventoryId);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}

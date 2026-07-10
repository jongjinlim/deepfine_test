package com.deepfine.service.inventory.service;

import com.deepfine.service.inventory.domain.Inventory;
import com.deepfine.service.inventory.repository.InventoryEntity;
import com.deepfine.service.inventory.repository.InventoryJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * InventoryService의 @DistributedLock 이 실제로 동시성을 막아주는지 검증하는 통합 테스트.
 * docker-compose의 redis/postgres가 떠 있어야 한다.
 */
@SpringBootTest
class InventoryServiceConcurrencyTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InventoryJpaRepository inventoryJpaRepository;

    @AfterEach
    void tearDown() {
        inventoryJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("같은 재고를 동시에 여러 번 출고해도 분산락 덕분에 수량이 정확히 차감된다.")
    void decreaseTest() throws InterruptedException {
        int numberOfThreads = 100;
        Inventory inventory = inventoryService.increase("TEST_PRODUCT_001", numberOfThreads);

        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        for (int i = 0; i < numberOfThreads; i++) {
            int index = i;
            executorService.submit(() -> {
                try {
                    // 분산락 적용 메서드 호출 (락의 key는 재고의 id로 설정)
                    inventoryService.decrease(inventory.getInventoryId(), 1);
                    System.out.println("잔여 재고 수량 = " + (inventory.getQuantity() - index));
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        Inventory persistInventory = inventoryRepository.findById(inventory.getInventoryId())
                .orElseThrow(IllegalArgumentException::new);

        System.out.println("잔여 재고 수량 = " + persistInventory.getQuantity());
        assertThat(persistInventory.getQuantity()).isZero();
    }

    @Test
    @DisplayName("존재하지 않는 상품명으로 동시에 여러 번 입고해도 재고 행이 하나만 생성되고 수량이 정확히 합산된다.")
    void increaseTest() throws InterruptedException {
        String name = "TEST_PRODUCT_001" + UUID.randomUUID();

        int numberOfThreads = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                try {
                    // 분산락 적용 메서드 호출 (락의 key는 상품명으로 설정)
                    inventoryService.increase(name, 1);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        List<InventoryEntity> persistInventories = inventoryJpaRepository.findAll().stream()
                .filter(entity -> name.equals(entity.toModel().getName()))
                .toList();

        System.out.println("생성된 재고 행 개수 = " + persistInventories.size());
        System.out.println("최종 재고 수량 = " + persistInventories.get(0).toModel().getQuantity());

        assertThat(persistInventories).hasSize(1);
        assertThat(persistInventories.get(0).toModel().getQuantity()).isEqualTo(numberOfThreads);
    }
}

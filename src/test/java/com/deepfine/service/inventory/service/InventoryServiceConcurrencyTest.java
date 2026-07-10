package com.deepfine.service.inventory.service;

import com.deepfine.service.inventory.domain.Inventory;
import com.deepfine.service.inventory.repository.InventoryEntity;
import com.deepfine.service.inventory.repository.InventoryJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

    /**
     * Feature: 재고 출고 동시성 테스트
     * Background
     *     Given KURLY_001 이라는 이름의 재고 100개가 등록되어 있음
     */
    @Nested
    @DisplayName("재고 출고 동시성 테스트")
    class DecreaseConcurrency {

        private static final String NAME = "KURLY_001";
        private static final int NUMBER_OF_THREADS = 100;

        private Inventory inventory;

        @BeforeEach
        void setUp() {
            inventory = inventoryService.increase(NAME, NUMBER_OF_THREADS);
        }

        /**
         * Scenario: 100명의 사용자가 동시에 접근해 재고를 1개씩 출고 요청함
         *           Lock의 이름은 재고의 id로 설정함
         * <p>
         * Then 사용자들의 요청만큼 정확히 재고가 차감되어 0이 되어야 함
         */
        @Test
        void decreaseTest() throws InterruptedException {
            ExecutorService executorService = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
            CountDownLatch latch = new CountDownLatch(NUMBER_OF_THREADS);

            for (int i = 0; i < NUMBER_OF_THREADS; i++) {
                executorService.submit(() -> {
                    try {
                        // 분산락 적용 메서드 호출 (락의 key는 재고의 id로 설정)
                        inventoryService.decrease(inventory.getInventoryId(), 1);
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
    }

    /**
     * Feature: 신규 상품 입고 동시성 테스트
     * Background
     *     Given 상품명은 아직 등록되어 있지 않음 (동시 최초등록 레이스를 검증하는 것이 목적이라
     *           여기서 미리 등록해두면 안 됨)
     */
    @Nested
    @DisplayName("신규 상품 입고 동시성 테스트")
    class IncreaseConcurrency {

        private static final int NUMBER_OF_THREADS = 100;

        /**
         * Scenario: 100명의 사용자가 동시에 접근해 같은 상품명으로 재고를 1개씩 입고 요청함
         *           Lock의 이름은 상품명으로 설정함
         * <p>
         * Then 재고 행은 하나만 생성되고, 사용자들의 요청만큼 정확히 수량이 합산되어야 함
         */
        @Test
        void increaseTest() throws InterruptedException {
            String name = "SPRING_ONION-" + UUID.randomUUID();

            ExecutorService executorService = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
            CountDownLatch latch = new CountDownLatch(NUMBER_OF_THREADS);

            for (int i = 0; i < NUMBER_OF_THREADS; i++) {
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
            assertThat(persistInventories.get(0).toModel().getQuantity()).isEqualTo(NUMBER_OF_THREADS);
        }
    }
}

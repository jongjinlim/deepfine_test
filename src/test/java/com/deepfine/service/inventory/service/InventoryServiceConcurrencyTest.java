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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * InventoryService의 @DistributedLock 이 실제로 동시성을 막아주는지 검증하는 통합 테스트.
 * docker-compose의 redis/postgres가 떠 있어야 한다.
 */
@SpringBootTest
class InventoryServiceConcurrencyTest {

    private static final int THREAD_COUNT = 50;

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
    @DisplayName("같은 재고를 동시에 여러 번 출고해도 분산락 덕분에 수량이 정확히 차감된다")
    void decrease_concurrent_requests_do_not_lose_updates() throws InterruptedException {
        String name = "concurrency-decrease-" + UUID.randomUUID();
        int initialQuantity = 1000;
        Inventory created = inventoryService.increase(name, initialQuantity);
        Long inventoryId = created.getInventoryId();

        List<Throwable> failures = runConcurrently(THREAD_COUNT,
                () -> inventoryService.decrease(inventoryId, 1));

        assertThat(failures).isEmpty();

        Inventory result = inventoryRepository.findById(inventoryId).orElseThrow();
        assertThat(result.getQuantity()).isEqualTo(initialQuantity - THREAD_COUNT);
    }

    @Test
    @DisplayName("존재하지 않는 상품명으로 동시에 여러 번 입고해도 재고 행이 하나만 생성되고 수량이 정확히 합산된다")
    void increase_concurrent_requests_for_new_product_create_single_row() throws InterruptedException {
        String name = "concurrency-increase-" + UUID.randomUUID();

        List<Throwable> failures = runConcurrently(THREAD_COUNT,
                () -> inventoryService.increase(name, 1));

        assertThat(failures).isEmpty();

        List<InventoryEntity> matched = inventoryJpaRepository.findAll().stream()
                .filter(entity -> name.equals(entity.toModel().getName()))
                .toList();

        assertThat(matched).hasSize(1);
        assertThat(matched.get(0).toModel().getQuantity()).isEqualTo(THREAD_COUNT);
    }

    private List<Throwable> runConcurrently(int threadCount, Runnable task) throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        List<Throwable> failures = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    task.run();
                } catch (Throwable t) {
                    failures.add(t);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        doneLatch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        return failures;
    }
}

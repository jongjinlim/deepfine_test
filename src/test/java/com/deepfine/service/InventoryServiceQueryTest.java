package com.deepfine.service;

import com.deepfine.error.ErrorCode;
import com.deepfine.error.GlobalException;
import com.deepfine.service.inventory.domain.Inventory;
import com.deepfine.service.inventory.repository.InventoryJpaRepository;
import com.deepfine.service.inventory.service.InventoryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class InventoryServiceQueryTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryJpaRepository inventoryJpaRepository;

    private Long inventoryId;

    @AfterEach
    void tearDown() {
        if (inventoryId != null) {
            inventoryJpaRepository.deleteById(inventoryId);
        }
    }

    @Test
    @DisplayName("등록된 상품의 현재 재고 수량을 조회할 수 있다.")
    void getInventoryTest() {
        Inventory inventory = inventoryService.increase("QUERY_TEST_" + UUID.randomUUID(), 30);
        inventoryId = inventory.getInventoryId();

        Inventory found = inventoryService.getInventory(inventoryId);

        assertThat(found.getInventoryId()).isEqualTo(inventoryId);
        assertThat(found.getQuantity()).isEqualTo(30);
    }

    @Test
    @DisplayName("존재하지 않는 상품을 조회하면 예외가 발생한다.")
    void getInventoryNotFoundTest() {
        assertThatThrownBy(() -> inventoryService.getInventory(-1L))
                .isInstanceOf(GlobalException.class)
                .extracting(e -> ((GlobalException) e).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
    }
}

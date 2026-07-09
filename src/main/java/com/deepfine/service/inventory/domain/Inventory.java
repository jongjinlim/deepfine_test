package com.deepfine.service.inventory.domain;

import com.deepfine.error.ErrorCode;
import com.deepfine.error.GlobalException;
import lombok.Builder;
import lombok.Getter;

@Getter
public class Inventory {

    private Long inventoryId;
    private String name;
    private int quantity;

    @Builder
    private Inventory(
            Long inventoryId,
            String name,
            int quantity
    ) {
        this.inventoryId = inventoryId;
        this.name = name;
        this.quantity = quantity;
    }

    public static Inventory createInventory() {
        return Inventory.builder()
                .build();
    }

    /**
     * 입고: 재고 수량을 증가
     */
    public void increase(int amount) {
        validateAmount(amount);
        this.quantity += amount;
    }

    /**
     * 출고: 재고 수량을 감소
     * 감소 후 수량이 음수가 되는 요청은 처리하지 않고 예외
     */
    public void decrease(int amount) {
        validateAmount(amount);
        if (this.quantity - amount < 0) {
            throw new GlobalException(ErrorCode.INSUFFICIENT_STOCK,
                    "아이디=" + inventoryId + ", 현재수량=" + quantity + ", 요청수량=" + amount);
        }
        this.quantity -= amount;
    }

    private static void validateAmount(int amount) {
        if (amount <= 0) {
            throw new GlobalException(ErrorCode.INVALID_QUANTITY, "요청수량=" + amount);
        }
    }
}

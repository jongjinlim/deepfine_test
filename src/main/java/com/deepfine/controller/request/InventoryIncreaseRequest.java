package com.deepfine.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record InventoryIncreaseRequest(
        @NotBlank(message = "상품명은 필수입니다.")
        String name,

        @Positive(message = "입고 수량은 0보다 커야 합니다.")
        int quantity
) {
}

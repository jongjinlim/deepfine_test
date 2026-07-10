package com.deepfine.controller.request;

import jakarta.validation.constraints.Positive;

public record InventoryDecreaseRequest(
        @Positive(message = "출고 수량은 0보다 커야 합니다.")
        int quantity
) {
}

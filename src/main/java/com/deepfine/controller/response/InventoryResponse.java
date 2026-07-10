package com.deepfine.controller.response;

import com.deepfine.service.inventory.domain.Inventory;

public record InventoryResponse(
        Long id,
        String name,
        int quantity
) {

    public static InventoryResponse from(Inventory inventory) {
        return new InventoryResponse(inventory.getInventoryId(), inventory.getName(), inventory.getQuantity());
    }
}

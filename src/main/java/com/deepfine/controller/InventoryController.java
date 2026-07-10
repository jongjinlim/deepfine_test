package com.deepfine.controller;


import com.deepfine.controller.request.InventoryDecreaseRequest;
import com.deepfine.controller.request.InventoryIncreaseRequest;
import com.deepfine.controller.response.InventoryResponse;
import com.deepfine.error.response.GlobalResponse;
import com.deepfine.service.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventories")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/{id}")
    public GlobalResponse<InventoryResponse> getInventory(@PathVariable Long id) {
        InventoryResponse response = InventoryResponse.from(inventoryService.getInventory(id));
        return GlobalResponse.success(response);
    }

    @PostMapping("/increase")
    public GlobalResponse<InventoryResponse> increase(@Valid @RequestBody InventoryIncreaseRequest request) {
        InventoryResponse response = InventoryResponse.from(
                inventoryService.increase(request.name(), request.quantity()));
        return GlobalResponse.success(response);
    }

    @PostMapping("/{id}/decrease")
    public GlobalResponse<InventoryResponse> decrease(
            @PathVariable Long id, @Valid @RequestBody InventoryDecreaseRequest request) {
        InventoryResponse response = InventoryResponse.from(
                inventoryService.decrease(id, request.quantity()));
        return GlobalResponse.success(response);
    }

}

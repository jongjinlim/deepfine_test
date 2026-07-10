package com.deepfine.controller;

import com.deepfine.controller.request.ProductRegisterRequest;
import com.deepfine.controller.response.InventoryResponse;
import com.deepfine.error.response.GlobalResponse;
import com.deepfine.service.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final InventoryService inventoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GlobalResponse<InventoryResponse> register(@Valid @RequestBody ProductRegisterRequest request) {
        InventoryResponse response = InventoryResponse.from(inventoryService.register(request.name()));
        return GlobalResponse.success(response);
    }
}

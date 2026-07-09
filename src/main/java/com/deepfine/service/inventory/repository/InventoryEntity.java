package com.deepfine.service.inventory.repository;

import com.deepfine.service.inventory.domain.Inventory;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "INVENTORY")
@NoArgsConstructor(access = PROTECTED)
public class InventoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("재고 아이디")
    @Column(name = "ID")
    private Long inventoryId;

    @Comment("상품명")
    @Column(name = "NAME", nullable = false)
    private String name;

    @Comment("현재 재고 수량")
    @Column(name = "QUANTITY", nullable = false)
    private int quantity;

    @Builder
    private InventoryEntity(
            Long inventoryId,
            String name,
            int quantity
    ) {
        this.inventoryId = inventoryId;
        this.name = name;
        this.quantity = quantity;
    }

    public static InventoryEntity from(Inventory inventory) {
        return InventoryEntity.builder()
                .inventoryId(inventory.getInventoryId())
                .name(inventory.getName())
                .quantity(inventory.getQuantity())
                .build();
    }

    public Inventory toModel() {
        return Inventory.builder()
                .inventoryId(this.inventoryId)
                .name(this.name)
                .quantity(this.quantity)
                .build();
    }
}

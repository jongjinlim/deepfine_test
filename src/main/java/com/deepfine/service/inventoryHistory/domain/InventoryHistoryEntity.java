package com.deepfine.service.inventoryHistory.domain;

import com.deepfine.enums.InventoryChangeType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PROTECTED;

/**
 * 입고/출고 이벤트가 발생할 때마다 남기는 이력.
 * 재고 변경 트랜잭션이 커밋된 뒤, 컨슈머가 별도 트랜잭션으로 기록한다.
 */
@Getter
@Entity
@Table(name = "INVENTORY_HISTORY")
@NoArgsConstructor(access = PROTECTED)
public class InventoryHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("이력 아이디")
    @Column(name = "ID")
    private Long id;

    @Comment("대상 재고 아이디")
    @Column(name = "INVENTORY_ID", nullable = false)
    private Long inventoryId;

    @Comment("입고/출고 구분")
    @Enumerated(EnumType.STRING)
    @Column(name = "TYPE", nullable = false)
    private InventoryChangeType type;

    @Comment("변경된 수량")
    @Column(name = "CHANGE_QUANTITY", nullable = false)
    private int changeQuantity;

    @Comment("변경 후 남은 재고 수량")
    @Column(name = "RESULT_QUANTITY", nullable = false)
    private int resultQuantity;

    @Comment("이력 생성 시각")
    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;
}

package com.elvero.shop.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "inventory_transactions")
@Getter
@Setter
@NoArgsConstructor
public class InventoryTransaction extends Timestamped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id", nullable = false)
    private Inventory inventory;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "transaction_type", length = 50, nullable = false)
    private String transactionType;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "reference_id", length = 100)
    private String referenceId;
}

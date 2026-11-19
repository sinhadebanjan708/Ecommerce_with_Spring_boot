package com.elvero.shop.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "product_audit_logs")
@Getter
@Setter
@NoArgsConstructor
public class ProductAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "action", length = 50, nullable = false)
    private String action;

    @Column(name = "old_values", columnDefinition = "json")
    private String oldValues;

    @Column(name = "new_values", columnDefinition = "json")
    private String newValues;

    @Column(name = "performed_by")
    private Long performedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

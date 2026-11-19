package com.elvero.shop.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
public class AuditLog extends Timestamped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "action", length = 100, nullable = false)
    private String action;

    @Column(name = "module", length = 100, nullable = false)
    private String module;

    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @Column(name = "details", columnDefinition = "text")
    private String details;
}

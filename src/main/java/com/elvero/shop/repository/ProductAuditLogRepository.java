package com.elvero.shop.repository;

import com.elvero.shop.entity.ProductAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductAuditLogRepository extends JpaRepository<ProductAuditLog, Long> {
}

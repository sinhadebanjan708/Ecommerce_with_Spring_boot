package com.elvero.shop.repository;

import com.elvero.shop.entity.Inventory;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByVariantIdAndWarehouseId(Long variantId, Long warehouseId);

    List<Inventory> findByVariantId(Long variantId);

    @Query("""
        SELECT i FROM Inventory i
        LEFT JOIN FETCH i.variant v
        LEFT JOIN FETCH v.product
        LEFT JOIN FETCH i.warehouse
        WHERE i.quantityAvailable <= i.reorderLevel
        """)
    List<Inventory> findLowStock();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT i
        FROM Inventory i
        WHERE i.variant.id = :variantId
        """)
    List<Inventory> findByVariantIdForUpdate(
            @Param("variantId") Long variantId
    );
}

package com.elvero.shop.repository;

import com.elvero.shop.entity.Order;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"user", "address"})
    @Query("""
        SELECT o
        FROM Order o
        WHERE (:userId IS NULL OR o.user.id = :userId)
        AND (:status IS NULL OR o.orderStatus = :status)
        AND (:paymentStatus IS NULL OR o.paymentStatus = :paymentStatus)
        """)
    Page<Order> findOrders(
            @Param("userId") Long userId,
            @Param("status") String status,
            @Param("paymentStatus") String paymentStatus,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT o
        FROM Order o
        WHERE o.id = :orderId
        AND o.user.id = :userId
    """)
    Optional<Order> findByIdAndUserIdForUpdate(
            @Param("orderId") Long orderId,
            @Param("userId") Long userId
    );


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT o
    FROM Order o
    WHERE o.id = :orderId
""")
    Optional<Order> findByIdForUpdate(
            @Param("orderId") Long orderId
    );
}
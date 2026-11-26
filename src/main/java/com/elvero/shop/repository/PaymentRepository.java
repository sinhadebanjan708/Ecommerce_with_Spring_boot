
package com.elvero.shop.repository;

import com.elvero.shop.entity.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository
        extends JpaRepository<Payment, Long> {

    Optional<Payment> findByGatewayOrderIdAndGateway(
            String gatewayOrderId,
            String gateway
    );

    Optional<Payment> findByGatewayOrderIdAndGatewayAndOrder_User_Id(
            String gatewayOrderId,
            String gateway,
            Long userId
    );

    List<Payment> findByOrder_IdAndGatewayAndStatusOrderByIdDesc(
            Long orderId,
            String gateway,
            String status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT p
        FROM Payment p
        WHERE p.gatewayOrderId = :gatewayOrderId
        AND p.gateway = :gateway
    """)
    Optional<Payment> findByGatewayOrderIdForUpdate(
            @Param("gatewayOrderId") String gatewayOrderId,
            @Param("gateway") String gateway
    );
}

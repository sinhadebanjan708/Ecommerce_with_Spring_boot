
package com.elvero.shop.repository;

import com.elvero.shop.entity.Cart;
import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT c
        FROM Cart c
        WHERE c.user.id = :userId
          AND c.status = 'active'
        """)
    Optional<Cart> findActiveCartForUpdate(
            @Param("userId") Long userId
    );

    Optional<Cart> findFirstByUser_IdAndStatusOrderByIdDesc(
            Long userId,
            String status
    );

}

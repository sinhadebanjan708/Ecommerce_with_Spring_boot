
package com.elvero.shop.repository;

import com.elvero.shop.entity.CartItem;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.Collection;
import java.util.List;

public interface CartItemRepository
        extends JpaRepository<CartItem, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
            "variant",
            "variant.product"
    })
    @Query("""
        SELECT ci
        FROM CartItem ci
        WHERE ci.cart.id = :cartId
          AND ci.id IN :itemIds
        ORDER BY ci.id DESC
        """)
    List<CartItem> findSelectedItemsForUpdate(
            @Param("cartId") Long cartId,
            @Param("itemIds") Collection<Long> itemIds
    );

    @EntityGraph(attributePaths = {
            "variant",
            "variant.product",
            "variant.images"
    })
    @Query("""
        SELECT ci
        FROM CartItem ci
        WHERE ci.cart.id = :cartId
        ORDER BY ci.id DESC
        """)
    List<CartItem> findAllCheckoutItems(
            @Param("cartId") Long cartId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT ci
    FROM CartItem ci
    JOIN FETCH ci.variant v
    JOIN FETCH v.product p
    WHERE ci.cart.id = :cartId
      AND ci.id IN :itemIds
    ORDER BY ci.id DESC
    """)
    List<CartItem> findSelectedCheckoutItemsForUpdate(
            @Param("cartId") Long cartId,
            @Param("itemIds") List<Long> itemIds
    );

    @EntityGraph(attributePaths = {
            "variant",
            "variant.product",
            "variant.images"
    })
    @Query("""
        SELECT ci
        FROM CartItem ci
        WHERE ci.cart.id = :cartId
          AND ci.id IN :itemIds
        ORDER BY ci.id DESC
        """)
    List<CartItem> findSelectedCheckoutItems(
            @Param("cartId") Long cartId,
            @Param("itemIds") Collection<Long> itemIds
    );
}

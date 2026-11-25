package com.elvero.shop.repository;

import com.elvero.shop.entity.OrderItem;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository
        extends JpaRepository<OrderItem, Long> {

    @EntityGraph(attributePaths = {
            "variant",
            "variant.product",
            "variant.images"
    })
    List<OrderItem> findByOrder_IdIn(List<Long> orderIds);
}
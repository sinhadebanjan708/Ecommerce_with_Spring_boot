package com.elvero.shop.repository;

import com.elvero.shop.entity.ProductReturn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductReturnRepository extends JpaRepository<ProductReturn, Long> {
}

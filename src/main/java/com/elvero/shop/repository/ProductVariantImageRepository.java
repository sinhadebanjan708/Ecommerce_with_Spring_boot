package com.elvero.shop.repository;

import com.elvero.shop.entity.ProductVariantImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductVariantImageRepository extends JpaRepository<ProductVariantImage, Integer> {

    List<ProductVariantImage> findByVariantIdOrderByDisplayOrderAsc(Long variantId);

    void deleteByVariantId(Long variantId);
}

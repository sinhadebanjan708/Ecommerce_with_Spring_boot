package com.elvero.shop.repository;

import com.elvero.shop.entity.ProductVariant;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    Optional<ProductVariant> findBySku(String sku);

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    List<ProductVariant> findByProductIdAndStatus(Long productId, String status);

    List<ProductVariant> findByProductId(Long productId);

    @Query("""
        SELECT v FROM ProductVariant v
        LEFT JOIN FETCH v.images
        WHERE v.id = :id
        """)
    Optional<ProductVariant> findByIdWithImages(@Param("id") Long id);

    @Query("""
        SELECT DISTINCT v.color FROM ProductVariant v
        JOIN v.product p
        WHERE v.status = 'active' AND v.color IS NOT NULL
          AND p.status IN :statuses
          AND (:categoryId IS NULL OR p.category.id = :categoryId)
          AND (:subcategoryId IS NULL OR p.subcategory.id = :subcategoryId)
        """)
    List<String> findDistinctColors(
            @Param("statuses") List<String> statuses,
            @Param("categoryId") Long categoryId,
            @Param("subcategoryId") Long subcategoryId);

    @Query("""
        SELECT DISTINCT v.size FROM ProductVariant v
        JOIN v.product p
        WHERE v.status = 'active' AND v.size IS NOT NULL
          AND p.status IN :statuses
          AND (:categoryId IS NULL OR p.category.id = :categoryId)
          AND (:subcategoryId IS NULL OR p.subcategory.id = :subcategoryId)
        """)
    List<String> findDistinctSizes(
            @Param("statuses") List<String> statuses,
            @Param("categoryId") Long categoryId,
            @Param("subcategoryId") Long subcategoryId);


    @Query("""
    SELECT v FROM ProductVariant v
    WHERE v.product.id = :productId
      AND (
          :search IS NULL
          OR LOWER(COALESCE(v.sku, '')) LIKE LOWER(CONCAT('%', :search, '%'))
          OR LOWER(COALESCE(v.color, '')) LIKE LOWER(CONCAT('%', :search, '%'))
          OR LOWER(COALESCE(v.size, '')) LIKE LOWER(CONCAT('%', :search, '%'))
          OR LOWER(COALESCE(v.fit, '')) LIKE LOWER(CONCAT('%', :search, '%'))
      )
    """)
    Page<ProductVariant> searchByProductId(
            @Param("productId") Long productId,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("""
        SELECT DISTINCT v.fit FROM ProductVariant v
        JOIN v.product p
        WHERE v.status = 'active' AND v.fit IS NOT NULL
          AND p.status IN :statuses
          AND (:categoryId IS NULL OR p.category.id = :categoryId)
          AND (:subcategoryId IS NULL OR p.subcategory.id = :subcategoryId)
        """)
    List<String> findDistinctFits(
            @Param("statuses") List<String> statuses,
            @Param("categoryId") Long categoryId,
            @Param("subcategoryId") Long subcategoryId);

    @Query("""
        SELECT MIN(v.price), MAX(v.price) FROM ProductVariant v
        JOIN v.product p
        WHERE v.status = 'active'
          AND p.status IN :statuses
          AND (:categoryId IS NULL OR p.category.id = :categoryId)
          AND (:subcategoryId IS NULL OR p.subcategory.id = :subcategoryId)
        """)
    Object[] findPriceRange(
            @Param("statuses") List<String> statuses,
            @Param("categoryId") Long categoryId,
            @Param("subcategoryId") Long subcategoryId);
}

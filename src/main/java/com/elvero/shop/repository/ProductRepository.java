package com.elvero.shop.repository;

import com.elvero.shop.entity.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    long countBySubcategoryId(Long subcategoryId);

    long countByCategoryId(Long categoryId);

    @Query("""
        SELECT DISTINCT p FROM Product p
        LEFT JOIN FETCH p.category
        LEFT JOIN FETCH p.subcategory
        LEFT JOIN FETCH p.variants v
        WHERE p.id = :id
        """)
    Optional<Product> findByIdWithDetails(@Param("id") Long id);

    @Query("""
        SELECT p FROM Product p
        LEFT JOIN FETCH p.category
        LEFT JOIN FETCH p.subcategory
        WHERE p.status IN :statuses
          AND (:categoryId IS NULL OR p.category.id = :categoryId)
          AND (:subcategoryId IS NULL OR p.subcategory.id = :subcategoryId)
          AND (:search IS NULL OR LOWER(p.productName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(p.slug) LIKE LOWER(CONCAT('%', :search, '%')))
        """)
    List<Product> findPublished(
            @Param("statuses") List<String> statuses,
            @Param("categoryId") Long categoryId,
            @Param("subcategoryId") Long subcategoryId,
            @Param("search") String search,
            Pageable pageable);

    Page<Product> findByStatusIn(List<String> statuses, Pageable pageable);

    Page<Product> findByCategoryIdAndStatusIn(Long categoryId, List<String> statuses, Pageable pageable);

    Page<Product> findBySubcategoryIdAndStatusIn(Long subcategoryId, List<String> statuses, Pageable pageable);
}

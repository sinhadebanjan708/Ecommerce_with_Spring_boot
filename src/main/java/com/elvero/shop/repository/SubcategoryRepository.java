package com.elvero.shop.repository;

import com.elvero.shop.entity.Subcategory;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SubcategoryRepository
        extends JpaRepository<Subcategory, Long> {

    Optional<Subcategory> findBySlug(String slug);

    interface SubcategoryProductCountProjection {
        Long getSubcategoryId();
        Long getProductCount();
    }

    // Paginated subcategory search


    @Query(
            value = """
        SELECT s.*
        FROM subcategories s
        WHERE (
            :search IS NULL
            OR TRIM(:search) = ''
            OR LOWER(s.subcategory_name) LIKE
               CONCAT('%', LOWER(TRIM(:search)), '%')
            OR LOWER(s.slug) REGEXP
               CONCAT('(^|[-_])', LOWER(TRIM(:search)), '($|[-_])')
        )
        """,
            countQuery = """
        SELECT COUNT(*)
        FROM subcategories s
        WHERE (
            :search IS NULL
            OR TRIM(:search) = ''
            OR LOWER(s.subcategory_name) LIKE
               CONCAT('%', LOWER(TRIM(:search)), '%')
            OR LOWER(s.slug) REGEXP
               CONCAT('(^|[-_])', LOWER(TRIM(:search)), '($|[-_])')
        )
        """,
            nativeQuery = true
    )
    Page<Subcategory> findAllWithSearch(
            @Param("search") String search,
            Pageable pageable
    );


    // Product counts only for the subcategories on the current page
    @Query("""
        SELECT
            p.subcategory.id AS subcategoryId,
            COUNT(p.id) AS productCount
        FROM Product p
        WHERE p.subcategory.id IN :subcategoryIds
        GROUP BY p.subcategory.id
    """)
    List<SubcategoryProductCountProjection> findProductCountsBySubcategoryIds(
            @Param("subcategoryIds") List<Long> subcategoryIds
    );

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    List<Subcategory> findByCategoryIdAndStatusOrderBySubcategoryNameAsc(
            Long categoryId,
            String status
    );

    List<Subcategory> findByCategoryId(Long categoryId);

    @Query("SELECT s FROM Subcategory s LEFT JOIN FETCH s.category WHERE s.id = :id")
    Optional<Subcategory> findByIdWithCategory(@Param("id") Long id);

    long countByCategoryId(Long categoryId);
}
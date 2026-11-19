package com.elvero.shop.repository;

import com.elvero.shop.entity.Subcategory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SubcategoryRepository extends JpaRepository<Subcategory, Long> {

    Optional<Subcategory> findBySlug(String slug);

    public interface SubcategoryProductCountProjection {
        Long getSubcategoryId();
        Long getProductCount();
    }

    @Query("""
        SELECT
            p.subcategory.id AS subcategoryId,
            COUNT(p.id) AS productCount
        FROM Product p
        WHERE p.subcategory IS NOT NULL
        GROUP BY p.subcategory.id
    """)
    List<SubcategoryProductCountProjection> findProductCountsBySubcategory();

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    List<Subcategory> findByCategoryIdAndStatusOrderBySubcategoryNameAsc(Long categoryId, String status);

    List<Subcategory> findByCategoryId(Long categoryId);

    @Query("SELECT s FROM Subcategory s LEFT JOIN FETCH s.category WHERE s.id = :id")
    Optional<Subcategory> findByIdWithCategory(@Param("id") Long id);

    long countByCategoryId(Long categoryId);
}

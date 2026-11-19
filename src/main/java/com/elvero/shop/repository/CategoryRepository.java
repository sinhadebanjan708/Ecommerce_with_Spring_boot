package com.elvero.shop.repository;

import com.elvero.shop.entity.Category;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    @EntityGraph(attributePaths = {"department"})
    Page<Category> findAll(Pageable pageable);

    @Query("""
        SELECT c FROM Category c
        LEFT JOIN FETCH c.department
        WHERE (:status IS NULL OR c.status = :status)
          AND (:departmentId IS NULL OR c.department.id = :departmentId)
          AND (:search IS NULL OR LOWER(c.categoryName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(c.slug) LIKE LOWER(CONCAT('%', :search, '%')))
        """)
    List<Category> search(
            @Param("status") String status,
            @Param("departmentId") Long departmentId,
            @Param("search") String search);

    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.department WHERE c.id = :id")
    Optional<Category> findByIdWithDepartment(@Param("id") Long id);


    /**
     * Step 1 of list(): page only category IDs (filters + pagination, no collection join).
     */
    @Query("""
        SELECT c.id FROM Category c
        WHERE (:status IS NULL OR c.status = :status)
          AND (:departmentId IS NULL OR c.department.id = :departmentId)
          AND (:search IS NULL OR LOWER(c.categoryName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(c.slug) LIKE LOWER(CONCAT('%', :search, '%')))
        """)
    Page<Long> findIds(
            @Param("status") String status,
            @Param("departmentId") Long departmentId,
            @Param("search") String search,
            Pageable pageable);

    /**
     * Step 2 of list(): load those IDs with department + subcategories in one query.
     */
    @Query("""
        SELECT DISTINCT c FROM Category c
        LEFT JOIN FETCH c.department
        LEFT JOIN FETCH c.subcategories
        WHERE c.id IN :ids
        """)
    List<Category> findByIdInWithDepartmentAndSubcategories(
            List<Long> ids
    );

    @Query("""
        SELECT c FROM Category c
        LEFT JOIN FETCH c.department
        LEFT JOIN FETCH c.subcategories
        WHERE c.id = :id
        """)
    Optional<Category> findByIdWithDepartmentAndSubcategories(@Param("id") Long id);

    List<Category> findByStatusOrderByCategoryNameAsc(String status);

    List<Category> findByDepartmentIdAndStatus(Long departmentId, String status);
}

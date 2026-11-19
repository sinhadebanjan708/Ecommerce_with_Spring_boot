package com.elvero.shop.repository;

import com.elvero.shop.entity.Department;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    Optional<Department> findBySlug(String slug);

    List<Department> findByStatusOrderByDepartmentNameAsc(String status);

    List<Department> findAllByOrderByDepartmentNameAsc();
}

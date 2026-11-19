package com.elvero.shop.repository;

import com.elvero.shop.entity.RolePermission;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    @Query("select rp from RolePermission rp join fetch rp.permission where rp.role.id in :roleIds")
    List<RolePermission> findWithPermissionByRoleIds(@Param("roleIds") Collection<Long> roleIds);
}

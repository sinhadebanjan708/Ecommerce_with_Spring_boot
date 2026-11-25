package com.elvero.shop.repository;

import com.elvero.shop.entity.Address;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    /** Default address first, then newest first (the ordering used throughout the Node API). */
    @Query("select a from Address a where a.user.id = :userId order by a.isDefault desc, a.createdAt desc")
    List<Address> findByUserIdOrdered(@Param("userId") Long userId);
    Optional<Address> findByIdAndUser_Id(Long id, Long userId);
}

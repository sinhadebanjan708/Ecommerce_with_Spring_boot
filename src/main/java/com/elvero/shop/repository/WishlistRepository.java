package com.elvero.shop.repository;

import com.elvero.shop.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    Optional<Wishlist> findByUser_Id(Long userId);
    Optional<Wishlist> findByUser_IdAndStatus(
            Long userId,
            String status
    );
}

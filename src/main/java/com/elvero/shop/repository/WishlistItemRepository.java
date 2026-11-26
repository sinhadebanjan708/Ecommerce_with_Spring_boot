package com.elvero.shop.repository;

import com.elvero.shop.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {

    List<WishlistItem> findByWishlist_IdOrderByCreatedAtDesc(
            Long wishlistId
    );

    Optional<WishlistItem> findByIdAndWishlist_Id(
            Long itemId,
            Long wishlistId
    );

    Optional<WishlistItem> findByWishlist_IdAndVariant_Id(
            Long wishlistId,
            Long variantId
    );

    void deleteByWishlist_Id(Long wishlistId);
}


package com.elvero.shop.service;

import com.elvero.shop.dto.CartDtos;
import com.elvero.shop.dto.wishlist.WishlistDtos.*;
import com.elvero.shop.entity.Product;
import com.elvero.shop.entity.ProductVariant;
import com.elvero.shop.entity.ProductVariantImage;
import com.elvero.shop.entity.Wishlist;
import com.elvero.shop.entity.WishlistItem;
import com.elvero.shop.repository.ProductVariantRepository;
import com.elvero.shop.repository.WishlistItemRepository;
import com.elvero.shop.repository.WishlistRepository;

import jakarta.persistence.EntityNotFoundException;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.elvero.shop.entity.User;
import com.elvero.shop.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private static final String ACTIVE = "ACTIVE";
    private static final String PUBLISHED = "PUBLISHED";

    private final WishlistRepository wishlistRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;
    private final CartService cartService;

    // -----------------------------------
    // GET OR CREATE WISHLIST
    // -----------------------------------

    @Transactional
    public Wishlist getOrCreateWishlist(Long userId) {

        return wishlistRepository
                .findByUser_IdAndStatus(userId, "active")
                .orElseGet(() -> {

                    Wishlist wishlist = new Wishlist();

                    // Assumes Wishlist has a User relationship.
                    // Set the user using your existing UserRepository
                    // or entity reference implementation.
                    throw new UnsupportedOperationException(
                            "Implement wishlist user assignment using your User entity"
                    );
                });
    }

    // -----------------------------------
    // GET WISHLIST
    // -----------------------------------

    @Transactional(readOnly = true)
    public WishlistResponse getWishlist(Long userId) {

        Wishlist wishlist = wishlistRepository
                .findByUser_IdAndStatus(userId, "active")
                .orElse(null);

        if (wishlist == null) {
            return new WishlistResponse(null, 0, List.of());
        }

        List<WishlistItemResponse> items =
                wishlistItemRepository
                        .findByWishlist_IdOrderByCreatedAtDesc(wishlist.getId())
                        .stream()
                        .map(this::mapWishlistItem)
                        .toList();

        return new WishlistResponse(
                wishlist.getId(),
                items.size(),
                items
        );
    }

    // -----------------------------------
    // CREATE WISHLIST
    // -----------------------------------

    @Transactional
    public Wishlist getOrCreateWishlistForUser(Long userId) {

        return wishlistRepository
                .findByUser_IdAndStatus(userId, "active")
                .orElseThrow(() -> new EntityNotFoundException(
                        "Active wishlist not found. Implement wishlist creation using UserRepository."
                ));
    }

    // -----------------------------------
    // ADD TO WISHLIST
    // -----------------------------------

    @Transactional
    public WishlistMessageResponse addToWishlist(
            Long userId,
            Long variantId) {

        ProductVariant variant = productVariantRepository
                .findByIdAndStatus(variantId, ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product variant not available"
                ));

        Product product = variant.getProduct();

        if (product == null ||
                !PUBLISHED.equalsIgnoreCase(product.getStatus())) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Product is not available"
            );
        }

        Wishlist wishlist = findOrCreateWishlist(userId);

        boolean alreadyExists =
                wishlistItemRepository
                        .findByWishlist_IdAndVariant_Id(
                                wishlist.getId(),
                                variantId
                        )
                        .isPresent();

        if (alreadyExists) {
            return new WishlistMessageResponse(
                    "Product already exists in wishlist"
            );
        }

        WishlistItem item = new WishlistItem();
        item.setWishlist(wishlist);
        item.setVariant(variant);

        wishlistItemRepository.save(item);

        return new WishlistMessageResponse(
                "Product added to wishlist successfully"
        );
    }

    // -----------------------------------
    // REMOVE WISHLIST ITEM
    // -----------------------------------

    @Transactional
    public WishlistMessageResponse removeWishlistItem(
            Long userId,
            Long itemId) {

        Wishlist wishlist = findActiveWishlist(userId);

        WishlistItem item = wishlistItemRepository
                .findByIdAndWishlist_Id(itemId, wishlist.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Wishlist item not found"
                ));

        wishlistItemRepository.delete(item);

        return new WishlistMessageResponse(
                "Wishlist item removed successfully"
        );
    }

    // -----------------------------------
    // CLEAR WISHLIST
    // -----------------------------------

    @Transactional
    public WishlistMessageResponse clearWishlist(Long userId) {

        Wishlist wishlist = findActiveWishlist(userId);

        wishlistItemRepository.deleteByWishlist_Id(wishlist.getId());

        return new WishlistMessageResponse(
                "Wishlist cleared successfully"
        );
    }

    // -----------------------------------
    // MOVE WISHLIST ITEM TO CART
    // -----------------------------------


    @Transactional
    public WishlistMessageResponse moveToCart(
            Long userId,
            Long itemId) {

        Wishlist wishlist = findActiveWishlist(userId);

        // Find wishlist item using wishlist_items.id
        WishlistItem wishlistItem = wishlistItemRepository
                .findByIdAndWishlist_Id(
                        itemId,
                        wishlist.getId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Wishlist item not found"
                ));

        Long variantId = wishlistItem.getVariant().getId();

        // Prepare cart request
        CartDtos.AddCartItemRequest cartRequest =
                new CartDtos.AddCartItemRequest(
                        variantId,
                        1
                );

        // Reuse existing CartService
        cartService.addToCart(userId, cartRequest);

        // Delete wishlist item only after successful cart addition
        wishlistItemRepository.delete(wishlistItem);

        return new WishlistMessageResponse(
                "Wishlist item moved to cart successfully"
        );
    }


    @Transactional
    public WishlistResponse createWishlist(Long userId) {
        findOrCreateWishlist(userId);
        return getWishlist(userId);
    }

    private Wishlist findOrCreateWishlist(Long userId) {

        return wishlistRepository
                .findByUser_IdAndStatus(userId, "active")
                .orElseGet(() -> {

                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "User not found"
                            ));

                    Wishlist wishlist = new Wishlist();
                    wishlist.setUser(user);
                    wishlist.setStatus("active");

                    return wishlistRepository.save(wishlist);
                });
    }
    // -----------------------------------
    // PRIVATE HELPERS
    // -----------------------------------

    private Wishlist findActiveWishlist(Long userId) {

        return wishlistRepository
                .findByUser_IdAndStatus(userId, "active")
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Wishlist not found"
                ));
    }

    private Wishlist createWishlistForUser(Long userId) {

        // Implement this using your UserRepository.
        // Example:
        // User user = userRepository.findById(userId)
        //     .orElseThrow(...);
        //
        // Wishlist wishlist = new Wishlist();
        // wishlist.setUser(user);
        // wishlist.setStatus("active");
        // return wishlistRepository.save(wishlist);

        throw new UnsupportedOperationException(
                "Implement createWishlistForUser using UserRepository"
        );
    }

    private WishlistItemResponse mapWishlistItem(WishlistItem item) {

        ProductVariant variant = item.getVariant();
        Product product = variant.getProduct();

        List<VariantImageResponse> images =
                variant.getImages() == null
                        ? List.of()
                        : variant.getImages()
                          .stream()
                          .map(this::mapVariantImage)
                          .toList();

        ProductResponse productResponse = new ProductResponse(
                product.getId(),
                product.getProductName()
        );

        VariantResponse variantResponse = new VariantResponse(
                variant.getId(),
                variant.getSku(),
                variant.getColor(),
                variant.getSize(),
                variant.getPrice(),
                variant.getSalePrice(),
                variant.getStatus(),
                images
        );

        return new WishlistItemResponse(
                item.getId(),
                variant.getId(),
                productResponse,
                variantResponse
        );
    }

    private VariantImageResponse mapVariantImage(
            ProductVariantImage image) {

        return new VariantImageResponse(
                image.getId().longValue(),
                image.getImageUrl(),
                image.getDisplayOrder()
        );
    }
}

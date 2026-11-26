
package com.elvero.shop.controller;

import com.elvero.shop.dto.wishlist.WishlistDtos.*;
import com.elvero.shop.security.AuthenticatedUser;
import com.elvero.shop.service.WishlistService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ecommerce/customer/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    // GET WISHLIST
    @GetMapping
    public ResponseEntity<?> getWishlist(
            @AuthenticationPrincipal AuthenticatedUser user) {

        WishlistResponse response =
                wishlistService.getWishlist(user.userId());

        return ResponseEntity.ok(response);
    }

    // CREATE WISHLIST
    @PostMapping("/create")
    public ResponseEntity<?> createWishlist(
            @AuthenticationPrincipal AuthenticatedUser user) {

        WishlistResponse response =
                wishlistService.createWishlist(user.userId());

        return ResponseEntity.ok(response);
    }

    // ADD TO WISHLIST
    @PostMapping("/items")
    public ResponseEntity<?> addToWishlist(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody AddWishlistItemRequest request) {

        WishlistMessageResponse response =
                wishlistService.addToWishlist(
                        user.userId(),
                        request.variantId()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // REMOVE WISHLIST ITEM
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<?> removeWishlistItem(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long itemId) {

        WishlistMessageResponse response =
                wishlistService.removeWishlistItem(
                        user.userId(),
                        itemId
                );

        return ResponseEntity.ok(response);
    }

    // CLEAR WISHLIST
    @DeleteMapping
    public ResponseEntity<?> clearWishlist(
            @AuthenticationPrincipal AuthenticatedUser user) {

        WishlistMessageResponse response =
                wishlistService.clearWishlist(user.userId());

        return ResponseEntity.ok(response);
    }

    // MOVE WISHLIST ITEM TO CART
    @PostMapping("/items/{itemId}/move-to-cart")
    public ResponseEntity<?> moveToCart(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long itemId) {

        WishlistMessageResponse response =
                wishlistService.moveToCart(
                        user.userId(),
                        itemId
                );

        return ResponseEntity.ok(response);
    }
}


package com.elvero.shop.controller;

import com.elvero.shop.dto.CartDtos.*;
import com.elvero.shop.security.AuthenticatedUser;
import com.elvero.shop.service.CartService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@PreAuthorize("hasRole('CUSTOMER')")
@RequestMapping("/api/ecommerce/customer/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ResponseEntity<?> getCart(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        CartResponse response =
                cartService.getCart(user.userId());

        return ResponseEntity.ok(
                Map.of("success", true, "data", response)
        );
    }

    @PostMapping("/items")
    public ResponseEntity<?> addItem(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody AddCartItemRequest request
    ) {
        CartItemMutationResponse response =
                cartService.addToCart(user.userId(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                Map.of("success", true, "data", response)
        );
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<?> updateItem(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long itemId,
            @RequestBody UpdateCartItemRequest request
    ) {
        CartItemMutationResponse response =
                cartService.updateCartItem(
                        user.userId(),
                        itemId,
                        request
                );

        return ResponseEntity.ok(
                Map.of("success", true, "data", response)
        );
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<?> removeItem(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long itemId
    ) {
        CartMessageResponse response =
                cartService.removeCartItem(
                        user.userId(),
                        itemId
                );

        return ResponseEntity.ok(
                Map.of("success", true, "data", response)
        );
    }
}

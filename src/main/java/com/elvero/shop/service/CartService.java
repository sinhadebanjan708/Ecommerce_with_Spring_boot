
package com.elvero.shop.service;

import com.elvero.shop.dto.CartDtos.*;
import com.elvero.shop.entity.*;
import com.elvero.shop.repository.*;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository variantRepository;
    private final InventoryRepository inventoryRepository;
    private final UserRepository userRepository;

    public CartService(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            ProductVariantRepository variantRepository,
            InventoryRepository inventoryRepository,
            UserRepository userRepository
    ) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.variantRepository = variantRepository;
        this.inventoryRepository = inventoryRepository;
        this.userRepository = userRepository;
    }

    // --------------------------------------------------
    // GET OR CREATE ACTIVE CART
    // --------------------------------------------------

    private Cart getOrCreateCart(Long userId) {

        return cartRepository
                .findFirstByUser_IdAndStatusOrderByIdDesc(
                        userId, "active"
                )
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setUser(userRepository.getReferenceById(userId));
                    cart.setStatus("active");

                    return cartRepository.save(cart);
                });
    }

    // --------------------------------------------------
    // GET CART
    // --------------------------------------------------

    @Transactional
    public CartResponse getCart(Long userId) {

        Cart cart = getOrCreateCart(userId);

        List<CartItem> cartItems =
                cartItemRepository.findCartItemsWithDetails(cart.getId());

        BigDecimal subtotal = BigDecimal.ZERO;
        int totalQuantity = 0;

        List<CartItemResponse> items = new java.util.ArrayList<>();

        for (CartItem item : cartItems) {

            ProductVariant variant = item.getVariant();
            Product product = variant.getProduct();

            BigDecimal price = getSelectedPrice(variant);

            BigDecimal itemTotal = price.multiply(
                    BigDecimal.valueOf(item.getQuantity())
            );

            subtotal = subtotal.add(itemTotal);
            totalQuantity += item.getQuantity();

            items.add(toCartItemResponse(item, price, itemTotal));
        }

        return new CartResponse(
                cart.getId(),
                cart.getStatus(),
                items.size(),
                totalQuantity,
                subtotal,
                items
        );
    }

    // --------------------------------------------------
    // ADD TO CART
    // --------------------------------------------------

    @Transactional
        public CartItemMutationResponse addToCart(
            Long userId,
            AddCartItemRequest request
    ) {

        if (request.variantId() == null ||
                request.variantId() <= 0) {
            throw new IllegalArgumentException(
                    "Valid variant ID is required"
            );
        }

        int quantity = request.quantity() == null
                ? 1
                : request.quantity();

        if (quantity < 1) {
            throw new IllegalArgumentException(
                    "Quantity must be a positive integer"
            );
        }

        // Validate variant and product
        ProductVariant variant = variantRepository
                .findByIdAndStatusAndProduct_Status(
                        request.variantId(),
                        "ACTIVE",
                        "PUBLISHED"
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "Product variant not available"
                ));

        // Lock active cart while modifying it
        Cart cart = cartRepository
                .findActiveCartForUpdate(userId)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(
                            userRepository.getReferenceById(userId)
                    );
                    newCart.setStatus("active");

                    return cartRepository.save(newCart);
                });

        // Check whether this variant already exists
        CartItem cartItem = cartItemRepository
                .findByCart_IdAndVariant_Id(
                        cart.getId(),
                        variant.getId()
                )
                .orElse(null);

        int finalQuantity =
                (cartItem == null ? 0 : cartItem.getQuantity())
                        + quantity;

        // Validate stock for inventory products
        validateStock(variant, finalQuantity);

        if (cartItem != null) {

            cartItem.setQuantity(finalQuantity);

        } else {

            cartItem = new CartItem();
            cartItem.setCart(cart);
            cartItem.setVariant(variant);
            cartItem.setQuantity(quantity);

            // CartItem.size is non-null in your Java entity.
            cartItem.setSize(variant.getSize());
        }

        cartItemRepository.save(cartItem);

        return new CartItemMutationResponse(
                "Product added to cart successfully",
                cart.getId(),
                toCartItemResponse(
                        cartItem,
                        getSelectedPrice(variant),
                        getSelectedPrice(variant)
                                .multiply(BigDecimal.valueOf(cartItem.getQuantity()))
                )
        );
    }

    // --------------------------------------------------
    // UPDATE CART ITEM
    // --------------------------------------------------

    @Transactional
    public CartItemMutationResponse updateCartItem(
            Long userId,
            Long itemId,
            UpdateCartItemRequest request
    ) {

        if (request.quantity() == null ||
                request.quantity() < 1) {
            throw new IllegalArgumentException(
                    "Invalid quantity"
            );
        }

        Cart cart = cartRepository
                .findActiveCartForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cart not found"
                ));

        CartItem item = cartItemRepository
                .findCartItemForUpdate(itemId, cart.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cart item not found"
                ));

        ProductVariant variant = item.getVariant();

        if (variant == null ||
                !"ACTIVE".equalsIgnoreCase(variant.getStatus())) {
            throw new IllegalArgumentException(
                    "Product variant is unavailable"
            );
        }

        validateStock(variant, request.quantity());

        item.setQuantity(request.quantity());

        // Keep the size update supported by your existing API.
        // If size is determined entirely by the variant, use variant.getSize()
        // instead of accepting an independently supplied size.
        if (request.size() != null &&
                !request.size().isBlank()) {
            item.setSize(request.size());
        }

        cartItemRepository.save(item);

        BigDecimal price = getSelectedPrice(variant);
        BigDecimal itemTotal = price.multiply(
                BigDecimal.valueOf(item.getQuantity())
        );

        return new CartItemMutationResponse(
                "Cart quantity updated successfully",
                cart.getId(),
                toCartItemResponse(item, price, itemTotal)
        );
    }

    // --------------------------------------------------
    // REMOVE CART ITEM
    // --------------------------------------------------

    @Transactional
    public CartMessageResponse removeCartItem(
            Long userId,
            Long itemId
    ) {

        Cart cart = cartRepository
                .findActiveCartForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cart not found"
                ));

        CartItem item = cartItemRepository
                .findCartItemForUpdate(itemId, cart.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cart item not found"
                ));

        cartItemRepository.delete(item);

        return new CartMessageResponse(
                "Cart item removed successfully"
        );
    }

    // --------------------------------------------------
    // CLEAR CART
    // --------------------------------------------------

    @Transactional
    public CartMessageResponse clearCart(Long userId) {

        Cart cart = cartRepository
                .findActiveCartForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cart not found"
                ));

        cartItemRepository.deleteAllByCart_Id(cart.getId());

        return new CartMessageResponse(
                "Cart cleared successfully"
        );
    }

    // --------------------------------------------------
    // STOCK VALIDATION
    // --------------------------------------------------

    private void validateStock(
            ProductVariant variant,
            int requestedQuantity
    ) {

        if (!"Inventory".equalsIgnoreCase(
                variant.getSellerType()
        )) {
            return;
        }

        List<Inventory> inventories =
                inventoryRepository.findByVariantIdForUpdate(
                        variant.getId()
                );

        int availableStock = inventories.stream()
                .mapToInt(inventory ->
                        inventory.getQuantityAvailable()
                                - inventory.getQuantityReserved()
                )
                .sum();

        if (requestedQuantity > availableStock) {
            throw new IllegalArgumentException(
                    "Only " + Math.max(0, availableStock)
                            + " units available"
            );
        }
    }

    // --------------------------------------------------
    // SELECTED PRICE
    // --------------------------------------------------

    private BigDecimal getSelectedPrice(
            ProductVariant variant
    ) {

        BigDecimal salePrice = variant.getSalePrice();

        if (salePrice != null &&
                salePrice.compareTo(BigDecimal.ZERO) > 0) {
            return salePrice;
        }

        return variant.getPrice();
    }

    // --------------------------------------------------
    // CART ITEM RESPONSE MAPPER
    // --------------------------------------------------

    private CartItemResponse toCartItemResponse(
            CartItem item,
            BigDecimal selectedPrice,
            BigDecimal itemTotal
    ) {

        ProductVariant variant = item.getVariant();
        Product product = variant.getProduct();

        List<CartImageResponse> images =
                variant.getImages() == null
                        ? List.of()
                        : variant.getImages().stream()
                          .map(image -> new CartImageResponse(
                                  image.getId().longValue(),
                                  image.getImageUrl(),
                                  image.getDisplayOrder()
                          ))
                          .toList();

        CartProductResponse productResponse =
                new CartProductResponse(
                        product.getId(),
                        product.getProductName()
                );

        CartVariantResponse variantResponse =
                new CartVariantResponse(
                        variant.getId(),
                        variant.getSku(),
                        variant.getColor(),
                        variant.getSize(),
                        variant.getPrice(),
                        variant.getSalePrice(),
                        selectedPrice,
                        variant.getStatus()
                );

        return new CartItemResponse(
                item.getId(),
                variant.getId(),
                item.getQuantity(),
                item.getSize(),
                productResponse,
                variantResponse,
                images,
                itemTotal
        );
    }
}

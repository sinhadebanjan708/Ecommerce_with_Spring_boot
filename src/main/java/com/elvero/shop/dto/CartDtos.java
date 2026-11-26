
package com.elvero.shop.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

public class CartDtos {

    public record AddCartItemRequest(
            @JsonProperty("variant_id")
            Long variantId,

            Integer quantity
    ) {}

    public record UpdateCartItemRequest(
            Integer quantity,
            String size
    ) {}

    public record CartImageResponse(
            Long id,

            @JsonProperty("image_url")
            String imageUrl,

            @JsonProperty("display_order")
            Integer displayOrder
    ) {}

    public record CartProductResponse(
            Long id,
            String name
    ) {}

    public record CartVariantResponse(
            Long id,
            String sku,
            String color,
            String size,
            BigDecimal price,

            @JsonProperty("sale_price")
            BigDecimal salePrice,

            @JsonProperty("selected_price")
            BigDecimal selectedPrice,

            String status
    ) {}

    public record CartItemResponse(
            Long id,

            @JsonProperty("variant_id")
            Long variantId,

            Integer quantity,
            String size,
            CartProductResponse product,
            CartVariantResponse variant,
            List<CartImageResponse> images,

            @JsonProperty("item_total")
            BigDecimal itemTotal
    ) {}

    public record CartResponse(
            @JsonProperty("cart_id")
            Long cartId,

            String status,

            @JsonProperty("total_items")
            Integer totalItems,

            @JsonProperty("total_quantity")
            Integer totalQuantity,

            BigDecimal subtotal,
            List<CartItemResponse> items
    ) {}

    public record CartItemMutationResponse(
            String message,

            @JsonProperty("cart_id")
            Long cartId,

            @JsonProperty("cart_item")
            CartItemResponse cartItem
    ) {}

    public record CartMessageResponse(String message) {}
}

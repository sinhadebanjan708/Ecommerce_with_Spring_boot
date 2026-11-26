
package com.elvero.shop.dto.wishlist;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

public class WishlistDtos {

    public record AddWishlistItemRequest(
            @JsonProperty("variant_id")
            @JsonAlias("variantId")
            Long variantId
    ) {}

    public record WishlistResponse(
            @JsonProperty("wishlist_id")
            Long wishlistId,

            @JsonProperty("total_items")
            Integer totalItems,

            List<WishlistItemResponse> items
    ) {}

    public record WishlistItemResponse(
            Long id,

            @JsonProperty("variant_id")
            Long variantId,

            ProductResponse product,

            VariantResponse variant
    ) {}

    public record ProductResponse(
            Long id,
            String name
    ) {}

    public record VariantResponse(
            Long id,
            String sku,
            String color,
            String size,
            BigDecimal price,

            @JsonProperty("sale_price")
            BigDecimal salePrice,

            String status,
            List<VariantImageResponse> images
    ) {}

    public record VariantImageResponse(
            Long id,

            @JsonProperty("image_url")
            String imageUrl,

            @JsonProperty("display_order")
            Integer displayOrder
    ) {}

    public record WishlistMessageResponse(
            String message
    ) {}
}

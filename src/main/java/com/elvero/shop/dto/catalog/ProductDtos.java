package com.elvero.shop.dto.catalog;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class ProductDtos {

    private ProductDtos() {}

    public record CreateProductRequest(
            String productName,
            Long categoryId,
            Long subcategoryId,
            String description,
            String basePrice,
            String status
    ) {}

    public record UpdateProductRequest(
            String productName,
            Long categoryId,
            Long subcategoryId,
            String description,
            String basePrice,
            String status
    ) {}

    public record CreateVariantRequest(
            String sku,
            String color,
            String size,
            String fit,
            BigDecimal price,
            BigDecimal salePrice,
            String status,
            String sellerType,
            LocalDateTime deliveryDate,
            Long warehouseId,
            Integer quantityAvailable,
            Integer quantityReserved,
            Integer reorderLevel
    ) {}

    public record UpdateVariantRequest(
            String sku,
            String color,
            String size,
            String fit,
            BigDecimal price,
            BigDecimal salePrice,
            String status,
            String sellerType,
            LocalDateTime deliveryDate
    ) {}

    public record AdjustStockRequest(
            Long warehouseId,
            Integer quantity,
            String reason
    ) {}

    public record ReserveStockRequest(
            Long warehouseId,
            Integer quantity
    ) {}

    public record ThresholdRequest(
            Long warehouseId,
            Integer reorderLevel
    ) {}

    public record VariantImageDto(
            Integer id,
            String imageUrl,
            Integer displayOrder
    ) {}

    public record InventoryDto(
            Long id,
            Long warehouseId,
            Integer quantityAvailable,
            Integer quantityReserved,
            Integer reorderLevel
    ) {}

    public record VariantResponse(
            Long id,
            Long productId,
            String sku,
            String size,
            String color,
            String fit,
            BigDecimal price,
            BigDecimal salePrice,
            String status,
            String sellerType,
            LocalDateTime deliveryDate,
            List<VariantImageDto> images,
            List<InventoryDto> inventory
    ) {}

    public record ProductResponse(
            Long id,
            String productName,
            String slug,
            String description,
            String image,
            String basePrice,
            String status,
            Long categoryId,
            String categoryName,
            Long subcategoryId,
            String subcategoryName,
            Long createdBy,
            Long updatedBy,
            Long publishedBy,
            LocalDateTime publishedAt,
            LocalDateTime archivedAt,
            List<VariantResponse> variants
    ) {}

    public record ProductListItem(
            Long id,
            String productName,
            String slug,
            String image,
            String status,
            String description,
            String categoryName,
            String subcategoryName,
            String basePrice,
            int variantCount
    ) {}

    public record AvailabilityResponse(
            Long variantId,
            String sku,
            int availableQuantity,
            boolean inStock,
            String sellerType,
            BigDecimal price,
            BigDecimal salePrice
    ) {}

    public record ProductFilters(
            List<String> colors,
            List<String> sizes,
            List<String> fits,
            BigDecimal minPrice,
            BigDecimal maxPrice
    ) {}

    public record PageMeta(
            long total,
            int page,
            int limit,
            int totalPages
    ) {}
}

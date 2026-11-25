package com.elvero.shop.dto.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CustomerOrderResponse(

        Long orderId,
        String orderNumber,
        String orderStatus,
        String paymentStatus,
        String paymentMethod,

        BigDecimal totalAmount,

        Integer totalItems,
        Integer totalOrderLines,

        LocalDateTime expectedDeliveryDate,
        LocalDateTime createdAt,

        UserDetails user,
        AddressDetails address,

        List<OrderItemDetails> orderItems

) {

    public record UserDetails(
            Long userId,
            String name,
            String email,
            String phone
    ) {}

    public record AddressDetails(
            Long addressId,
            String fullName,
            String addressLine1,
            String city,
            String state,
            String pincode,
            BigDecimal latitude,
            BigDecimal longitude,
            BigDecimal locationAccuracy
    ) {}

    public record OrderItemDetails(
            Long orderItemId,
            Long variantId,
            String productName,
            String sku,
            String color,
            String size,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal,
            List<VariantImageDetails> images
    ) {}

    public record VariantImageDetails(
            Integer id,
            String imageUrl,
            Integer displayOrder
    ) {}
}
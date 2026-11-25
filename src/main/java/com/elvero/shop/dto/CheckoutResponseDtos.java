
package com.elvero.shop.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class CheckoutResponseDtos {

    public record CreateOrderResponse(
            boolean success,
            String message,
            OrderCheckoutData order
    ) {}

    public record OrderCheckoutData(
            Long id,
            String order_number,
            String status,
            String payment_status,
            String payment_method,
            BigDecimal total_amount,
            LocalDateTime expected_delivery_date,
            Long address_id,
            LocalDateTime reservation_expires_at,
            List<CreatedOrderItemData> items
    ) {}

    public record CreatedOrderItemData(
            Long cart_item_id,
            Long variant_id,
            String product_name,
            String sku,
            Integer quantity,
            BigDecimal unit_price,
            BigDecimal line_total,
            LocalDateTime expected_delivery_date
    ) {}

    public record CheckoutSummaryResponse(
            boolean success,
            String message,
            CheckoutSummaryData data
    ) {}

    public record CheckoutSummaryData(
            Long cart_id,
            AddressData selected_address,
            List<CheckoutSummaryItemData> items,
            PricingData pricing
    ) {}

    public record CheckoutSummaryItemData(
            Long cart_item_id,
            Long product_id,
            String product_name,
            Long variant_id,
            String sku,
            String size,
            String color,
            String fit,
            Integer quantity,
            BigDecimal unit_price,
            BigDecimal item_subtotal,
            List<VariantImageData> images
    ) {}

    public record VariantImageData(
            Integer id,
            String image_url,
            Integer display_order
    ) {}

    public record PricingData(
            BigDecimal subtotal,
            BigDecimal delivery_charge,
            BigDecimal discount,
            BigDecimal total_amount
    ) {}

    public record AddressData(
            Long id,
            String full_name,
            String address_line1,
            String city,
            String state,
            String pincode,
            BigDecimal latitude,
            BigDecimal longitude,
            BigDecimal location_accuracy
    ) {}

    public record SelectDeliveryAddressResponse(
            boolean success,
            String message,
            AddressData selected_address
    ) {}
}

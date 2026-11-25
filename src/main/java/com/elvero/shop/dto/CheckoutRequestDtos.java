
package com.elvero.shop.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public class CheckoutRequestDtos {

    public record CreateOrderRequest(
            @NotEmpty
            List<@Valid CheckoutItemRequest> items,

            @NotBlank
            String payment_method
    ) {}

    public record CheckoutItemRequest(
            @NotNull
            @Positive
            Long cart_item_id,

            @NotNull
            @Positive
            Integer quantity
    ) {}

    public record SelectDeliveryAddressRequest(
            @NotNull
            @Positive
            Long address_id
    ) {}
}

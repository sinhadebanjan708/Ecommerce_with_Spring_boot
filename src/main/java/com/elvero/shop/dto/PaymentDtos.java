
package com.elvero.shop.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PaymentDtos {

    public record CreateRazorpayOrderRequest(
            @JsonProperty("order_id")
            Long orderId
    ) {}

    public record CreateRazorpayOrderResponse(
            @JsonProperty("payment_id")
            Long paymentId,

            @JsonProperty("order_id")
            Long orderId,

            @JsonProperty("razorpay_key_id")
            String razorpayKeyId,

            @JsonProperty("razorpay_order_id")
            String razorpayOrderId,

            Long amount,

            String currency
    ) {}

    public record VerifyRazorpayPaymentRequest(
            @JsonProperty("razorpay_payment_id")
            String razorpayPaymentId,

            @JsonProperty("razorpay_order_id")
            String razorpayOrderId,

            @JsonProperty("razorpay_signature")
            String razorpaySignature
    ) {}

    public record PaymentResponse(
            @JsonProperty("payment_id")
            Long paymentId,

            @JsonProperty("order_id")
            Long orderId,

            @JsonProperty("gateway_payment_id")
            String gatewayPaymentId,

            String status,

            String message
    ) {}
}

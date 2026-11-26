
package com.elvero.shop.controller;

import com.elvero.shop.dto.PaymentDtos;
import com.elvero.shop.security.AuthenticatedUser;
import com.elvero.shop.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer/payments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class PaymentController {

    private final PaymentService paymentService;

    // CREATE RAZORPAY ORDER
    @PostMapping("/razorpay/create-order")
    public ResponseEntity<?> createPaymentOrder(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody PaymentDtos.CreateRazorpayOrderRequest request
    ) {

        PaymentDtos.CreateRazorpayOrderResponse response =
                paymentService.createPaymentOrder(
                        user.userId(),
                        request.orderId()
                );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // VERIFY RAZORPAY PAYMENT
    @PostMapping("/razorpay/verify")
    public ResponseEntity<?> verifyPayment(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody PaymentDtos.VerifyRazorpayPaymentRequest request
    ) {

        PaymentDtos.PaymentResponse response =
                paymentService.verifyPayment(
                        user.userId(),
                        request
                );

        return ResponseEntity.ok(response);
    }
}

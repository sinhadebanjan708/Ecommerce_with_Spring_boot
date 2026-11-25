
package com.elvero.shop.controller;

import com.elvero.shop.dto.CheckoutRequestDtos;
import com.elvero.shop.dto.CheckoutResponseDtos;
import com.elvero.shop.dto.CheckoutRequestDtos.*;
import com.elvero.shop.dto.CheckoutResponseDtos.*;
import com.elvero.shop.security.AuthenticatedUser;
import com.elvero.shop.service.CheckoutService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ecommerce/customer/checkout")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class CheckoutController {

    private final CheckoutService checkoutService;

    @PostMapping("/create-order")
    public ResponseEntity<CheckoutResponseDtos.CreateOrderResponse> createOrder(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CheckoutRequestDtos.CreateOrderRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(checkoutService.createOrder(user.userId(), request));
    }

    @GetMapping("/summary")
    public ResponseEntity<CheckoutResponseDtos.CheckoutSummaryResponse> getCheckoutSummary(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(name = "item_ids", required = false) String itemIds
    ) {
        return ResponseEntity.ok(
                checkoutService.getCheckoutSummary(user.userId(), itemIds)
        );
    }

    @PatchMapping("/address")
    public ResponseEntity<CheckoutResponseDtos.SelectDeliveryAddressResponse> selectDeliveryAddress(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CheckoutRequestDtos.SelectDeliveryAddressRequest request
    ) {
        return ResponseEntity.ok(
                checkoutService.selectDeliveryAddress(
                        user.userId(),
                        request.address_id()
                )
        );
    }
}

package com.elvero.shop.controller;

import com.elvero.shop.common.ApiResponse;
import com.elvero.shop.dto.order.CustomerOrdersPageResponse;
import com.elvero.shop.security.AuthenticatedUser;
import com.elvero.shop.service.CustomerOrderService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ecommerce/customer")
public class CustomerOrderController {

    private final CustomerOrderService orderService;

    public CustomerOrderController(
            CustomerOrderService orderService
    ) {
        this.orderService = orderService;
    }

    @GetMapping("/my-orders")
    public ApiResponse<CustomerOrdersPageResponse> getMyOrders(

            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,

            @RequestParam(defaultValue = "1") int page,

            @RequestParam(defaultValue = "10") int limit,

            @RequestParam(required = false) String status,

            @RequestParam(name = "payment_status", required = false)
            String paymentStatus

    ) {

        Long userId = authenticatedUser.userId();

        CustomerOrdersPageResponse result =
                orderService.getMyOrders(
                        userId,
                        page,
                        limit,
                        status,
                        paymentStatus
                );

        return ApiResponse.ok(
                "Orders fetched successfully",
                result
        );
    }
}
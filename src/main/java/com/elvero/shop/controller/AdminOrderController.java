package com.elvero.shop.controller;

import com.elvero.shop.common.ApiResponse;
import com.elvero.shop.dto.order.CustomerOrdersPageResponse;
import com.elvero.shop.service.CustomerOrderService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ecommerce/admin/orders")
public class AdminOrderController {

    private final CustomerOrderService orderService;

    public AdminOrderController(
            CustomerOrderService orderService
    ) {
        this.orderService = orderService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ApiResponse<CustomerOrdersPageResponse> getAllOrders(

            @RequestParam(defaultValue = "1") int page,

            @RequestParam(defaultValue = "10") int limit,

            @RequestParam(required = false) Long userId,

            @RequestParam(required = false) String status,

            @RequestParam(name = "payment_status", required = false)
            String paymentStatus

    ) {

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
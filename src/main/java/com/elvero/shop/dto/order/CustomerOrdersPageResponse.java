package com.elvero.shop.dto.order;

import java.util.List;

public record CustomerOrdersPageResponse(
        List<CustomerOrderResponse> orders,
        Pagination pagination
) {

    public record Pagination(
            long totalOrders,
            int currentPage,
            int limit,
            int totalPages,
            boolean hasNextPage,
            boolean hasPreviousPage
    ) {}
}
package com.elvero.shop.service;

import com.elvero.shop.dto.order.CustomerOrderResponse;
import com.elvero.shop.dto.order.CustomerOrdersPageResponse;
import com.elvero.shop.entity.Order;
import com.elvero.shop.entity.OrderItem;
import com.elvero.shop.entity.ProductVariantImage;
import com.elvero.shop.exception.ApiException;
import com.elvero.shop.repository.OrderItemRepository;
import com.elvero.shop.repository.OrderRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.elvero.shop.entity.User;
import com.elvero.shop.entity.Address;
import com.elvero.shop.entity.Product;
import com.elvero.shop.entity.ProductVariant;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.math.BigDecimal;

@Service
public class CustomerOrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public CustomerOrderService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

//    @Transactional(readOnly = true)
//    public CustomerOrdersPageResponse getMyOrders(
//            Long userId,
//            int page,
//            int limit,
//            String status,
//            String paymentStatus
//    ) {
//
//        // Validate pagination
//        if (page < 1 || limit < 1 || limit > 100) {
//            throw new IllegalArgumentException(
//                    "Invalid pagination parameters"
//            );
//        }
//
//        Pageable pageable = PageRequest.of(
//                page - 1,
//                limit,
//                Sort.by(Sort.Direction.DESC, "createdAt")
//        );
//
//        // Fetch customer orders
//        Page<Order> orderPage =
//                orderRepository.findCustomerOrders(
//                        userId,
//                        normalizeFilter(status),
//                        normalizeFilter(paymentStatus),
//                        pageable
//                );
//
//        List<Order> orders = orderPage.getContent();
//
//        if (orders.isEmpty()) {
//            return new CustomerOrdersPageResponse(
//                    List.of(),
//                    new CustomerOrdersPageResponse.Pagination(
//                            orderPage.getTotalElements(),
//                            page,
//                            limit,
//                            orderPage.getTotalPages(),
//                            orderPage.hasNext(),
//                            orderPage.hasPrevious()
//                    )
//            );
//        }
//
//        // Fetch order IDs from current page
//        List<Long> orderIds = orders.stream()
//                .map(Order::getId)
//                .toList();
//
//        // Fetch all order items for these orders
//        List<OrderItem> orderItems =
//                orderItemRepository.findByOrder_IdIn(orderIds);
//
//        // Group order items by order ID
//        Map<Long, List<OrderItem>> itemsByOrderId =
//                orderItems.stream()
//                        .collect(Collectors.groupingBy(
//                                item -> item.getOrder().getId()
//                        ));
//
//        // Map orders into response DTOs
//        List<CustomerOrderResponse> responseOrders =
//                orders.stream()
//                        .map(order -> mapOrder(
//                                order,
//                                itemsByOrderId.getOrDefault(
//                                        order.getId(),
//                                        List.of()
//                                )
//                        ))
//                        .toList();
//
//        return new CustomerOrdersPageResponse(
//                responseOrders,
//                new CustomerOrdersPageResponse.Pagination(
//                        orderPage.getTotalElements(),
//                        page,
//                        limit,
//                        orderPage.getTotalPages(),
//                        orderPage.hasNext(),
//                        orderPage.hasPrevious()
//                )
//        );
//    }
private BigDecimal parseAmount(String amount) {

    if (amount == null || amount.isBlank()) {
        return BigDecimal.ZERO;
    }

    try {
        return new BigDecimal(amount.trim());
    } catch (NumberFormatException ex) {
        throw ApiException.badRequest(
                "Invalid order total amount: " + amount
        );
    }
}
    @Transactional(readOnly = true)
    public CustomerOrdersPageResponse getMyOrders(
            Long userId,
            int page,
            int limit,
            String status,
            String paymentStatus
    ) {

        if (page < 1 || limit < 1 || limit > 100) {
            throw ApiException.badRequest(
                    "Invalid pagination parameters"
            );
        }

        Pageable pageable = PageRequest.of(
                page - 1,
                limit,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Order> orderPage = orderRepository.findOrders(
                userId,
                normalizeFilter(status),
                normalizeFilter(paymentStatus),
                pageable
        );

        List<Order> orders = orderPage.getContent();

        List<CustomerOrderResponse> responseOrders = List.of();

        if (!orders.isEmpty()) {

            List<Long> orderIds = orders.stream()
                    .map(Order::getId)
                    .toList();

            List<OrderItem> orderItems =
                    orderItemRepository.findByOrder_IdIn(orderIds);

            Map<Long, List<OrderItem>> itemsByOrderId =
                    orderItems.stream()
                            .collect(Collectors.groupingBy(
                                    item -> item.getOrder().getId()
                            ));

            responseOrders = orders.stream()
                    .map(order -> mapOrder(
                            order,
                            itemsByOrderId.getOrDefault(
                                    order.getId(),
                                    List.of()
                            )
                    ))
                    .toList();
        }

        return new CustomerOrdersPageResponse(
                responseOrders,
                new CustomerOrdersPageResponse.Pagination(
                        orderPage.getTotalElements(),
                        page,
                        limit,
                        orderPage.getTotalPages(),
                        orderPage.hasNext(),
                        orderPage.hasPrevious()
                )
        );
    }

    private String normalizeFilter(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim();
    }

    private CustomerOrderResponse mapOrder(
            Order order,
            List<OrderItem> orderItems
    ) {

        User user = order.getUser();
        Address address = order.getAddress();

        int totalItems = orderItems.stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();

        int totalOrderLines = orderItems.size();

        var userDetails =
                new CustomerOrderResponse.UserDetails(
                        user.getId(),
                        user.getFirstName() + " " + user.getLastName(),
                        user.getEmail(),
                        user.getPhone()
                );

        var addressDetails =
                new CustomerOrderResponse.AddressDetails(
                        address.getId(),
                        address.getFullName(),
                        address.getAddressLine1(),
                        address.getCity(),
                        address.getState(),
                        address.getPincode(),
                        address.getLatitude(),
                        address.getLongitude(),
                        address.getLocationAccuracy()
                );

        List<CustomerOrderResponse.OrderItemDetails> itemDetails =
                orderItems.stream()
                        .map(this::mapOrderItem)
                        .toList();

        BigDecimal totalAmount = parseAmount(order.getTotalAmount());

        return new CustomerOrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getOrderStatus(),
                order.getPaymentStatus(),
                order.getPaymentMethod(),
                totalAmount,
                totalItems,
                totalOrderLines,
                order.getExpectedDeliveryDate(),
                order.getCreatedAt(),
                userDetails,
                addressDetails,
                itemDetails
        );
    }

    private CustomerOrderResponse.OrderItemDetails mapOrderItem(
            OrderItem item
    ) {

        ProductVariant variant = item.getVariant();

        Product product = variant.getProduct();

        BigDecimal lineTotal = item.getUnitPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));

        List<CustomerOrderResponse.VariantImageDetails> images =
                variant.getImages() == null
                        ? List.of()
                        : variant.getImages()
                          .stream()
                          .sorted(Comparator.comparing(
                                  img -> img.getDisplayOrder() == null
                                         ? Integer.MAX_VALUE
                                         : img.getDisplayOrder()
                          ))
                          .map(img ->
                               new CustomerOrderResponse.VariantImageDetails(
                                       img.getId(),
                                       img.getImageUrl(),
                                       img.getDisplayOrder()
                               )
                          )
                          .toList();

        return new CustomerOrderResponse.OrderItemDetails(
                item.getId(),
                variant.getId(),
                product.getProductName(),
                variant.getSku(),
                variant.getColor(),
                variant.getSize(),
                item.getQuantity(),
                item.getUnitPrice(),
                lineTotal,
                images
        );
    }

    private CustomerOrderResponse.VariantImageDetails mapImage(
            ProductVariantImage image
    ) {
        return new CustomerOrderResponse.VariantImageDetails(
                image.getId(),
                image.getImageUrl(),
                image.getDisplayOrder()
        );
    }
}
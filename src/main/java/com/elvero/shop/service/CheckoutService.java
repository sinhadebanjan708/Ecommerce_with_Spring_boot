
package com.elvero.shop.service;

import com.elvero.shop.dto.CheckoutRequestDtos.*;
import com.elvero.shop.dto.CheckoutResponseDtos.*;

import com.elvero.shop.entity.*;
import com.elvero.shop.exception.ApiException;

import com.elvero.shop.repository.*;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final AddressRepository addressRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;

    private static final String ACTIVE_CART = "active";
    private static final String PUBLISHED = "PUBLISHED";
    private static final String ACTIVE_VARIANT = "ACTIVE";

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    // ---------------------------------------------------------
    // CREATE ORDER
    // ---------------------------------------------------------

    @Transactional
    public CreateOrderResponse createOrder(
            Long userId,
            CreateOrderRequest request
    ) {

        if (request.items() == null || request.items().isEmpty()) {
            throw ApiException.badRequest(
                    "Please select at least one cart item"
            );
        }

        // Validate selected cart item IDs and quantities.
        Set<Long> selectedIds = new HashSet<>();

        for (CheckoutItemRequest item : request.items()) {

            if (item.cart_item_id() == null
                    || item.cart_item_id() <= 0
                    || item.quantity() == null
                    || item.quantity() <= 0) {

                throw ApiException.badRequest(
                        "Invalid cart item ID or quantity"
                );
            }

            if (!selectedIds.add(item.cart_item_id())) {
                throw ApiException.badRequest(
                        "Duplicate cart items are not allowed"
                );
            }
        }

        Map<Long, Integer> requestedQuantities =
                request.items().stream().collect(
                        Collectors.toMap(
                                CheckoutItemRequest::cart_item_id,
                                CheckoutItemRequest::quantity
                        )
                );

        // 1. Lock active cart.
        Cart cart = cartRepository
                .findActiveCartForUpdate(userId)
                .orElseThrow(() ->
                        ApiException.badRequest("Active cart not found")
                );

        // 2. Validate selected delivery address.
        Long selectedAddressId = cart.getSelectedAddressId();

        if (selectedAddressId == null) {
            throw ApiException.badRequest(
                    "Please select a delivery address"
            );
        }

        Address address = addressRepository
                .findByIdAndUser_Id(selectedAddressId, userId)
                .orElseThrow(() ->
                        ApiException.badRequest(
                                "Selected delivery address not found"
                        )
                );
        List<Long> selectedItemIds = new ArrayList<>(selectedIds);
        // 3. Fetch only selected items belonging to this cart.
        List<CartItem> cartItems =
                cartItemRepository.findSelectedCheckoutItemsForUpdate(
                        cart.getId(),
                         selectedItemIds
                );
        System.out.println("Requested cart item IDs: " + selectedIds);

        System.out.println("Fetched cart item count: " + cartItems.size());

        cartItems.forEach(item ->
                System.out.println("Fetched cart item ID: " + item.getId())
        );
        if (cartItems.size() != selectedIds.size()) {
            throw ApiException.badRequest(
                    "One or more selected cart items are invalid"
            );
        }

        // 4. Validate products and calculate prices from DB.
        List<OrderLineData> orderLines = new ArrayList<>();

        BigDecimal totalAmount = ZERO;

        for (CartItem cartItem : cartItems) {

            ProductVariant variant = cartItem.getVariant();

            if (variant == null || variant.getProduct() == null) {
                throw ApiException.badRequest(
                        "Product or variant not found"
                );
            }

            Product product = variant.getProduct();

            if (!PUBLISHED.equalsIgnoreCase(product.getStatus())) {
                throw ApiException.badRequest(
                        "Product " + product.getProductName()
                                + " is not available"
                );
            }

            if (!ACTIVE_VARIANT.equalsIgnoreCase(variant.getStatus())) {
                throw ApiException.badRequest(
                        "Selected product variant is not active"
                );
            }

            Integer quantity =
                    requestedQuantities.get(cartItem.getId());

            if (quantity == null || quantity <= 0) {
                throw ApiException.badRequest(
                        "Invalid cart quantity"
                );
            }

            BigDecimal unitPrice =
                    variant.getSalePrice() != null
                            ? variant.getSalePrice()
                            : variant.getPrice();

            if (unitPrice == null || unitPrice.signum() < 0) {
                throw ApiException.badRequest(
                        "Invalid product price"
                );
            }

            BigDecimal lineTotal = unitPrice.multiply(
                    BigDecimal.valueOf(quantity)
            );

            LocalDateTime expectedDeliveryDate =
                    variant.getDeliveryDate();

            OrderLineData line = new OrderLineData(
                    cartItem,
                    variant,
                    product,
                    quantity,
                    unitPrice,
                    lineTotal,
                    expectedDeliveryDate
            );

            orderLines.add(line);
            totalAmount = totalAmount.add(lineTotal);
        }

        // 5. Overall delivery date = latest item delivery date.
        LocalDateTime orderExpectedDeliveryDate =
                orderLines.stream()
                        .map(OrderLineData::expectedDeliveryDate)
                        .filter(Objects::nonNull)
                        .max(LocalDateTime::compareTo)
                        .orElse(null);

        // 6. Generate order number and reservation expiry.
        String orderNumber = "ORD-" + UUID.randomUUID();

        LocalDateTime reservationExpiry =
                LocalDateTime.now().plusMinutes(15);

        // 7. Create order.
        Order order = new Order();

        order.setUser(userRepository.getReferenceById(userId));
        order.setAddress(address);
        order.setOrderNumber(orderNumber);
        order.setOrderStatus("PENDING_PAYMENT");
        order.setPaymentStatus("PAYMENT_PENDING");
        order.setPaymentMethod(request.payment_method());

        // Your current Order entity stores totalAmount as String.
        order.setTotalAmount(totalAmount.toPlainString());

        order.setExpectedDeliveryDate(orderExpectedDeliveryDate);

        Order savedOrder = orderRepository.save(order);

        // 8. Create order items and update selected cart quantities.
        List<CreatedOrderItemData> responseItems = new ArrayList<>();

        for (OrderLineData line : orderLines) {

            OrderItem orderItem = new OrderItem();

            orderItem.setOrder(savedOrder);
            orderItem.setVariant(line.variant());
            orderItem.setQuantity(line.quantity());
            orderItem.setUnitPrice(line.unitPrice());
            orderItem.setExpectedDeliveryDate(
                    line.expectedDeliveryDate()
            );

            orderItemRepository.save(orderItem);

            // Preserve your Node.js behavior:
            // update the selected cart item's quantity to the
            // quantity supplied during checkout.
            line.cartItem().setQuantity(line.quantity());

            responseItems.add(
                    new CreatedOrderItemData(
                            line.cartItem().getId(),
                            line.variant().getId(),
                            line.product().getProductName(),
                            line.variant().getSku(),
                            line.quantity(),
                            line.unitPrice(),
                            line.lineTotal(),
                            line.expectedDeliveryDate()
                    )
            );
        }

        cartItemRepository.saveAll(
                orderLines.stream()
                        .map(OrderLineData::cartItem)
                        .toList()
        );

        OrderCheckoutData orderData = new OrderCheckoutData(
                savedOrder.getId(),
                savedOrder.getOrderNumber(),
                savedOrder.getOrderStatus(),
                savedOrder.getPaymentStatus(),
                savedOrder.getPaymentMethod(),
                totalAmount,
                orderExpectedDeliveryDate,
                address.getId(),
                reservationExpiry,
                responseItems
        );

        return new CreateOrderResponse(
                true,
                "Order created successfully",
                orderData
        );
    }

    // ---------------------------------------------------------
    // GET CHECKOUT SUMMARY
    // ---------------------------------------------------------

    @Transactional(readOnly = true)
    public CheckoutSummaryResponse getCheckoutSummary(
            Long userId,
            String itemIds
    ) {

        Cart cart = cartRepository
                .findByUser_IdAndStatus(userId, ACTIVE_CART)
                .orElseThrow(() ->
                        ApiException.badRequest("Cart not found")
                );

        // Parse selected cart item IDs.
        List<Long> selectedIds = parseItemIds(itemIds);

        List<CartItem> cartItems;

        if (selectedIds.isEmpty()) {
            cartItems = cartItemRepository.findAllCheckoutItems(
                    cart.getId()
            );
        } else {
            cartItems = cartItemRepository.findSelectedCheckoutItems(
                    cart.getId(),
                    selectedIds
            );

            if (cartItems.size() != new HashSet<>(selectedIds).size()) {
                throw ApiException.badRequest(
                        "One or more selected cart items are invalid"
                );
            }
        }

        if (cartItems.isEmpty()) {
            throw ApiException.badRequest(
                    "No selected cart items found"
            );
        }

        BigDecimal subtotal = ZERO;

        List<CheckoutSummaryItemData> responseItems =
                new ArrayList<>();

        for (CartItem item : cartItems) {

            ProductVariant variant = item.getVariant();
            Product product = variant.getProduct();

            if (!PUBLISHED.equalsIgnoreCase(product.getStatus())) {
                throw ApiException.badRequest(
                        "One or more products are unavailable"
                );
            }

            if (!ACTIVE_VARIANT.equalsIgnoreCase(variant.getStatus())) {
                throw ApiException.badRequest(
                        "One or more product variants are unavailable"
                );
            }

            BigDecimal unitPrice =
                    variant.getSalePrice() != null
                            ? variant.getSalePrice()
                            : variant.getPrice();

            if (unitPrice == null || unitPrice.signum() < 0) {
                throw ApiException.badRequest(
                        "Invalid product price"
                );
            }

            Integer quantity = item.getQuantity();

            if (quantity == null || quantity <= 0) {
                throw ApiException.badRequest(
                        "Invalid cart quantity"
                );
            }

            BigDecimal itemSubtotal = unitPrice.multiply(
                    BigDecimal.valueOf(quantity)
            );

            subtotal = subtotal.add(itemSubtotal);

            List<VariantImageData> images =
                    variant.getImages() == null
                            ? List.of()
                            : variant.getImages().stream()
                              .sorted(
                                      Comparator.comparing(
                                              ProductVariantImage::getDisplayOrder,
                                              Comparator.nullsLast(
                                                      Integer::compareTo
                                              )
                                      )
                              )
                              .map(image ->
                                   new VariantImageData(
                                           image.getId(),
                                           image.getImageUrl(),
                                           image.getDisplayOrder()
                                   )
                              )
                              .toList();

            responseItems.add(
                    new CheckoutSummaryItemData(
                            item.getId(),
                            product.getId(),
                            product.getProductName(),
                            variant.getId(),
                            variant.getSku(),
                            variant.getSize(),
                            variant.getColor(),
                            variant.getFit(),
                            quantity,
                            unitPrice,
                            itemSubtotal,
                            images
                    )
            );
        }

        // Fetch selected address only if it belongs to this user.
        AddressData selectedAddress = null;

        if (cart.getSelectedAddressId() != null) {

            selectedAddress = addressRepository
                    .findByIdAndUser_Id(
                            cart.getSelectedAddressId(),
                            userId
                    )
                    .map(this::mapAddress)
                    .orElse(null);
        }

        PricingData pricing = new PricingData(
                subtotal,
                ZERO,
                ZERO,
                subtotal
        );

        CheckoutSummaryData data = new CheckoutSummaryData(
                cart.getId(),
                selectedAddress,
                responseItems,
                pricing
        );

        return new CheckoutSummaryResponse(
                true,
                "Checkout summary fetched successfully",
                data
        );
    }

    // ---------------------------------------------------------
    // SELECT DELIVERY ADDRESS
    // ---------------------------------------------------------

    @Transactional
    public SelectDeliveryAddressResponse selectDeliveryAddress(
            Long userId,
            Long addressId
    ) {

        if (addressId == null || addressId <= 0) {
            throw ApiException.badRequest("Invalid address ID");
        }

        Cart cart = cartRepository
                .findByUser_IdAndStatus(userId, ACTIVE_CART)
                .orElseThrow(() ->
                        ApiException.badRequest("Cart not found")
                );

        Address address = addressRepository
                .findByIdAndUser_Id(addressId, userId)
                .orElseThrow(() ->
                        ApiException.badRequest(
                                "Address not found or does not belong to this customer"
                        )
                );

        cart.setSelectedAddressId(address.getId());

        cartRepository.save(cart);

        return new SelectDeliveryAddressResponse(
                true,
                "Delivery address selected successfully",
                mapAddress(address)
        );
    }

    // ---------------------------------------------------------
    // HELPERS
    // ---------------------------------------------------------

    private List<Long> parseItemIds(String itemIds) {

        if (itemIds == null || itemIds.isBlank()) {
            return List.of();
        }

        List<Long> ids = new ArrayList<>();

        try {
            for (String value : itemIds.split(",")) {

                long id = Long.parseLong(value.trim());

                if (id <= 0) {
                    throw new NumberFormatException();
                }

                ids.add(id);
            }
        } catch (NumberFormatException ex) {
            throw ApiException.badRequest(
                    "Invalid cart item IDs"
            );
        }

        if (new HashSet<>(ids).size() != ids.size()) {
            throw ApiException.badRequest(
                    "Duplicate cart item IDs are not allowed"
            );
        }

        return ids;
    }

    private AddressData mapAddress(Address address) {

        return new AddressData(
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
    }

    private record OrderLineData(
            CartItem cartItem,
            ProductVariant variant,
            Product product,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal,
            LocalDateTime expectedDeliveryDate
    ) {}
}

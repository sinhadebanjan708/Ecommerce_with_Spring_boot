
package com.elvero.shop.service;

import com.elvero.shop.dto.PaymentDtos;
import com.elvero.shop.entity.Order;
import com.elvero.shop.entity.Payment;
import com.elvero.shop.repository.OrderRepository;
import com.elvero.shop.repository.PaymentRepository;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;


    // ==========================================
    // CREATE RAZORPAY ORDER
    // ==========================================

    @Transactional
    public PaymentDtos.CreateRazorpayOrderResponse createPaymentOrder(
            Long userId,
            Long orderId
    ) {

        if (orderId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Order ID is required"
            );
        }

        // 1. Lock order and verify ownership
        Order order = orderRepository
                .findByIdAndUserIdForUpdate(orderId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Order not found"
                ));

        // 2. Check existing order payment status
        if ("PAID".equalsIgnoreCase(order.getPaymentStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Order is already paid"
            );
        }

        // 3. Read amount from database, never frontend
        BigDecimal amount;

        try {
            amount = new BigDecimal(order.getTotalAmount());
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid order amount"
            );
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0
                || amount.scale() > 2) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid order amount"
            );
        }

        long amountInPaise;

        try {
            amountInPaise = amount
                    .movePointRight(2)
                    .longValueExact();
        } catch (ArithmeticException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid payment amount"
            );
        }

        // 4. Reuse a pending payment attempt if possible
        List<Payment> pendingPayments =
                paymentRepository
                        .findByOrder_IdAndGatewayAndStatusOrderByIdDesc(
                                order.getId(),
                                "RAZORPAY",
                                "PENDING"
                        );

        RazorpayClient razorpayClient = getRazorpayClient();

        for (Payment existingPayment : pendingPayments) {

            try {

                com.razorpay.Order existingRazorpayOrder =
                        razorpayClient.orders.fetch(
                                existingPayment.getGatewayOrderId()
                        );

                String gatewayStatus =
                        existingRazorpayOrder.get("status");

                Number gatewayAmount =
                        existingRazorpayOrder.get("amount");

                String gatewayCurrency =
                        existingRazorpayOrder.get("currency");

                if (
                        "created".equalsIgnoreCase(gatewayStatus)
                                && gatewayAmount != null
                                && gatewayAmount.longValue() == amountInPaise
                                && "INR".equalsIgnoreCase(gatewayCurrency)
                ) {

                    return new PaymentDtos.CreateRazorpayOrderResponse(
                            existingPayment.getId(),
                            order.getId(),
                            razorpayKeyId,
                            existingPayment.getGatewayOrderId(),
                            amountInPaise,
                            "INR"
                    );
                }

            } catch (Exception ex) {

                // A gateway/network error should not be treated
                // as proof that the existing order is invalid.
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Unable to check existing Razorpay order",
                        ex
                );
            }
        }

        // 5. Create a new Razorpay order
        try {

            JSONObject request = new JSONObject();

            request.put("amount", amountInPaise);
            request.put("currency", "INR");
            request.put(
                    "receipt",
                    "ORDER_" + order.getId()
            );

            JSONObject notes = new JSONObject();

            notes.put(
                    "ecommerce_order_id",
                    String.valueOf(order.getId())
            );

            notes.put(
                    "customer_id",
                    String.valueOf(userId)
            );

            request.put("notes", notes);

            com.razorpay.Order razorpayOrder =
                    razorpayClient.orders.create(request);

            String gatewayOrderId =
                    razorpayOrder.get("id");

            // 6. Save internal payment attempt
            Payment payment = new Payment();

            payment.setOrder(order);
            payment.setGateway("RAZORPAY");
            payment.setGatewayOrderId(gatewayOrderId);
            payment.setAmount(amount);
            payment.setCurrencyCode("INR");
            payment.setStatus("PENDING");

            paymentRepository.save(payment);

            // 7. Return checkout information
            return new PaymentDtos.CreateRazorpayOrderResponse(
                    payment.getId(),
                    order.getId(),
                    razorpayKeyId,
                    gatewayOrderId,
                    amountInPaise,
                    "INR"
            );

        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to create Razorpay order",
                    ex
            );
        }
    }


    // ==========================================
    // VERIFY RAZORPAY PAYMENT
    // ==========================================

    @Transactional
    public PaymentDtos.PaymentResponse verifyPayment(
            Long userId,
            PaymentDtos.VerifyRazorpayPaymentRequest request
    ) {

        if (
                request == null
                        || isBlank(request.razorpayPaymentId())
                        || isBlank(request.razorpayOrderId())
                        || isBlank(request.razorpaySignature())
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment verification details are required"
            );
        }

        // 1. Find payment and verify ownership
        Payment payment = paymentRepository
                .findByGatewayOrderIdAndGatewayAndOrder_User_Id(
                        request.razorpayOrderId(),
                        "RAZORPAY",
                        userId
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment record not found"
                ));

        Order order = payment.getOrder();

        // 2. Idempotent success
        if (
                "PAID".equalsIgnoreCase(payment.getStatus())
                        && "PAID".equalsIgnoreCase(order.getPaymentStatus())
                        && request.razorpayPaymentId().equals(
                        payment.getGatewayPaymentId()
                )
        ) {
            return new PaymentDtos.PaymentResponse(
                    payment.getId(),
                    order.getId(),
                    payment.getGatewayPaymentId(),
                    "PAID",
                    "Payment already verified"
            );
        }

        // 3. Verify signature
        try {

            String signatureData =
                    payment.getGatewayOrderId()
                            + "|"
                            + request.razorpayPaymentId();

            boolean validSignature = Utils.verifySignature(
                    signatureData,
                    request.razorpaySignature(),
                    razorpayKeySecret
            );

            if (!validSignature) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid payment signature"
                );
            }

        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment signature verification failed",
                    ex
            );
        }

        // 4. Fetch actual payment details from Razorpay
        try {

            RazorpayClient razorpayClient = getRazorpayClient();

            com.razorpay.Payment razorpayPayment =
                    razorpayClient.payments.fetch(
                            request.razorpayPaymentId()
                    );

            String gatewayOrderId =
                    razorpayPayment.get("order_id");

            String gatewayStatus =
                    razorpayPayment.get("status");

            Number gatewayAmount =
                    razorpayPayment.get("amount");

            String gatewayCurrency =
                    razorpayPayment.get("currency");

            long expectedAmount = payment.getAmount()
                    .movePointRight(2)
                    .longValueExact();

            // 5. Validate gateway details
            if (
                    !payment.getGatewayOrderId().equals(gatewayOrderId)
                            || gatewayAmount == null
                            || gatewayAmount.longValue() != expectedAmount
                            || !payment.getCurrencyCode().equalsIgnoreCase(
                            gatewayCurrency
                    )
            ) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Payment details mismatch"
                );
            }

            if (!"captured".equalsIgnoreCase(gatewayStatus)) {

                return new PaymentDtos.PaymentResponse(
                        payment.getId(),
                        order.getId(),
                        request.razorpayPaymentId(),
                        gatewayStatus,
                        "Payment has not been captured yet"
                );
            }

// Lock records before updating
            Payment lockedPayment = paymentRepository
                    .findByGatewayOrderIdForUpdate(
                            request.razorpayOrderId(),
                            "RAZORPAY"
                    )
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Payment record not found"
                    ));

            Order lockedOrder = orderRepository
                    .findByIdAndUserIdForUpdate(
                            order.getId(),
                            userId
                    )
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Order not found"
                    ));

// Prevent overwriting an already confirmed payment
            if ("PAID".equalsIgnoreCase(lockedPayment.getStatus())
                    && request.razorpayPaymentId().equals(
                    lockedPayment.getGatewayPaymentId()
            )) {

                return new PaymentDtos.PaymentResponse(
                        lockedPayment.getId(),
                        lockedOrder.getId(),
                        lockedPayment.getGatewayPaymentId(),
                        "PAID",
                        "Payment already verified"
                );
            }

// Store the actual Razorpay payment ID
            lockedPayment.setGatewayPaymentId(
                    request.razorpayPaymentId()
            );

            if ("captured".equalsIgnoreCase(gatewayStatus)) {

                lockedPayment.setStatus("PAID");

                lockedPayment.setPaidAt(
                        java.time.LocalDateTime.now()
                );

                if (!"PAID".equalsIgnoreCase(
                        lockedOrder.getPaymentStatus()
                )) {

                    lockedOrder.setPaymentStatus("PAID");
                    lockedOrder.setOrderStatus("CONFIRMED");

                    orderRepository.save(lockedOrder);
                }

            } else if ("authorized".equalsIgnoreCase(gatewayStatus)) {

                lockedPayment.setStatus("AUTHORIZED");

            } else if ("failed".equalsIgnoreCase(gatewayStatus)) {

                lockedPayment.setStatus("FAILED");

            } else {

                lockedPayment.setStatus("PENDING");
            }

            paymentRepository.save(lockedPayment);

            return new PaymentDtos.PaymentResponse(
                    lockedPayment.getId(),
                    lockedOrder.getId(),
                    lockedPayment.getGatewayPaymentId(),
                    lockedPayment.getStatus(),
                    "Payment status updated successfully"
            );

        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to verify Razorpay payment",
                    ex
            );
        }
    }


    // ==========================================
    // RAZORPAY CLIENT
    // ==========================================

    private RazorpayClient getRazorpayClient() {

        try {

            return new RazorpayClient(
                    razorpayKeyId,
                    razorpayKeySecret
            );

        } catch (Exception ex) {

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to initialize Razorpay client",
                    ex
            );
        }
    }


    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

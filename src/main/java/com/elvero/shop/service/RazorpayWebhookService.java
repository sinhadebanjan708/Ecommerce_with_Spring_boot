
package com.elvero.shop.service;

import com.elvero.shop.entity.Order;
import com.elvero.shop.entity.Payment;
import com.elvero.shop.entity.WebhookEvent;
import com.elvero.shop.repository.OrderRepository;
import com.elvero.shop.repository.PaymentRepository;
import com.elvero.shop.repository.WebhookEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class RazorpayWebhookService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final WebhookEventRepository webhookEventRepository;
    private final ObjectMapper objectMapper;

    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;


    // ==========================================
    // PROCESS WEBHOOK
    // ==========================================

    @Transactional
    public void processWebhook(
            byte[] rawBody,
            String signature,
            String eventId
    ) {

        // 1. Validate request
        if (
                rawBody == null
                        || rawBody.length == 0
                        || signature == null
                        || signature.isBlank()
                        || eventId == null
                        || eventId.isBlank()
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid Razorpay webhook request"
            );
        }

        // 2. Verify signature BEFORE parsing JSON
        verifySignature(rawBody, signature);

        // 3. Parse verified payload
        JsonNode payload;

        try {
            payload = objectMapper.readTree(rawBody);
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid webhook JSON",
                    ex
            );
        }

        String eventType = payload.path("event").asText(null);

        if (eventType == null || eventType.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Webhook event type missing"
            );
        }

        // 4. Idempotency check
        if (webhookEventRepository.existsByGatewayAndEventId(
                "RAZORPAY",
                eventId
        )) {
            return;
        }

        // 5. Register event in the same transaction
        WebhookEvent webhookEvent = new WebhookEvent();

        webhookEvent.setGateway("RAZORPAY");
        webhookEvent.setEventId(eventId);
        webhookEvent.setEventType(eventType);
        webhookEvent.setStatus("PROCESSING");

        webhookEventRepository.saveAndFlush(webhookEvent);

        // 6. Process supported events
        switch (eventType) {

            case "payment.captured" ->
                    handlePaymentCaptured(payload);

            case "payment.failed" ->
                    handlePaymentFailed(payload);

            case "order.paid" -> {
                // payment.captured is the event used
                // for our payment-state update.
                // Keep order.paid for future reconciliation.
            }

            default -> {
                // Unsupported events are acknowledged
                // and recorded without changing payment data.
            }
        }

        // 7. Mark event as processed
        webhookEvent.setStatus("PROCESSED");
        webhookEvent.setProcessedAt(LocalDateTime.now());

        webhookEventRepository.save(webhookEvent);
    }


    // ==========================================
    // PAYMENT CAPTURED
    // ==========================================

    private void handlePaymentCaptured(JsonNode payload) {

        JsonNode paymentEntity =
                payload.path("payload")
                        .path("payment")
                        .path("entity");

        if (paymentEntity.isMissingNode()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment information missing"
            );
        }

        String razorpayPaymentId =
                paymentEntity.path("id").asText(null);

        String razorpayOrderId =
                paymentEntity.path("order_id").asText(null);

        String currency =
                paymentEntity.path("currency").asText(null);

        long gatewayAmount =
                paymentEntity.path("amount").asLong(-1);

        long createdAt =
                paymentEntity.path("created_at").asLong(0);

        if (
                razorpayPaymentId == null
                        || razorpayOrderId == null
                        || gatewayAmount < 0
                        || currency == null
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid captured payment details"
            );
        }

        // 1. Lock payment record
        Payment payment = paymentRepository
                .findByGatewayOrderIdForUpdate(
                        razorpayOrderId,
                        "RAZORPAY"
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment record not found"
                ));

        // 2. Validate amount and currency
        long expectedAmount = payment.getAmount()
                .movePointRight(2)
                .longValueExact();

        if (
                gatewayAmount != expectedAmount
                        || !payment.getCurrencyCode().equalsIgnoreCase(currency)
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Webhook payment details mismatch"
            );
        }

        // 3. Lock associated order
        Order order = orderRepository
                .findByIdForUpdate(payment.getOrder().getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Order not found"
                ));

        // 4. Duplicate delivery for same payment
        if (
                "PAID".equalsIgnoreCase(payment.getStatus())
                        && razorpayPaymentId.equals(
                        payment.getGatewayPaymentId()
                )
        ) {
            return;
        }

        // 5. Record actual captured gateway payment
        payment.setGatewayPaymentId(razorpayPaymentId);
        payment.setStatus("PAID");

        if (createdAt > 0) {
            payment.setPaidAt(
                    LocalDateTime.ofEpochSecond(
                            createdAt,
                            0,
                            java.time.ZoneOffset.UTC
                    )
            );
        } else {
            payment.setPaidAt(LocalDateTime.now());
        }

        paymentRepository.save(payment);

        // 6. Update order only if not already paid
        if (!"PAID".equalsIgnoreCase(order.getPaymentStatus())) {

            order.setPaymentStatus("PAID");
            order.setOrderStatus("CONFIRMED");

            orderRepository.save(order);
        }

        // If the order was already PAID through another attempt,
        // do not overwrite its order state. This second captured
        // payment must be flagged for duplicate-payment/refund
        // reconciliation by your business logic.
    }


    // ==========================================
    // PAYMENT FAILED
    // ==========================================

    private void handlePaymentFailed(JsonNode payload) {

        JsonNode paymentEntity =
                payload.path("payload")
                        .path("payment")
                        .path("entity");

        if (paymentEntity.isMissingNode()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment information missing"
            );
        }

        String razorpayPaymentId =
                paymentEntity.path("id").asText(null);

        String razorpayOrderId =
                paymentEntity.path("order_id").asText(null);

        if (
                razorpayPaymentId == null
                        || razorpayOrderId == null
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid failed payment details"
            );
        }

        // Lock payment record
        Payment payment = paymentRepository
                .findByGatewayOrderIdForUpdate(
                        razorpayOrderId,
                        "RAZORPAY"
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment record not found"
                ));

        // Never overwrite a captured payment with FAILED
        if (!"PENDING".equalsIgnoreCase(payment.getStatus())) {
            return;
        }

        payment.setGatewayPaymentId(razorpayPaymentId);
        payment.setStatus("FAILED");

        paymentRepository.save(payment);
    }


    // ==========================================
    // WEBHOOK SIGNATURE VERIFICATION
    // ==========================================

    private void verifySignature(
            byte[] rawBody,
            String receivedSignature
    ) {

        try {

            if (
                    webhookSecret == null
                            || webhookSecret.isBlank()
                            || !receivedSignature.matches("(?i)^[a-f0-9]{64}$")
            ) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid webhook signature"
                );
            }

            Mac mac = Mac.getInstance("HmacSHA256");

            SecretKeySpec secretKey = new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );

            mac.init(secretKey);

            byte[] expectedSignature = mac.doFinal(rawBody);

            byte[] actualSignature =
                    HexFormat.of().parseHex(receivedSignature);

            if (!MessageDigest.isEqual(
                    expectedSignature,
                    actualSignature
            )) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid webhook signature"
                );
            }

        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Webhook signature verification failed",
                    ex
            );
        }
    }
}

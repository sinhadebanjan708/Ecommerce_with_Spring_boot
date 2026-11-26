
package com.elvero.shop.controller;

import com.elvero.shop.service.RazorpayWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks/razorpay")
@RequiredArgsConstructor
public class RazorpayWebhookController {

    private final RazorpayWebhookService webhookService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> handleWebhook(
            @RequestBody byte[] rawBody,

            @RequestHeader("X-Razorpay-Signature")
            String signature,

            @RequestHeader(
                    value = "X-Razorpay-Event-Id",
                    required = false
            )
            String eventId
    ) {

        webhookService.processWebhook(
                rawBody,
                signature,
                eventId
        );

        return ResponseEntity.ok().build();
    }
}

package com.webshop.payments;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** The API Checkout calls. Amounts arrive in cents; card details never arrive at all. */
@RestController
public class PaymentsController {

    private final PaymentGateway gateway;

    public PaymentsController(PaymentGateway gateway) {
        this.gateway = gateway;
    }

    public record StartRequest(UUID orderId, long amountCents, String currency) {}
    public record StartResponse(String clientSecret) {}
    public record CaptureResponse(PaymentGateway.PaymentResult result) {}

    @PostMapping("/payments")
    public StartResponse start(@RequestBody StartRequest r) throws StripeException {
        return new StartResponse(gateway.start(r.orderId(), r.amountCents(), r.currency()));
    }

    @PostMapping("/payments/{orderId}/capture")
    public CaptureResponse capture(@PathVariable UUID orderId) throws StripeException {
        return new CaptureResponse(gateway.capture(orderId));
    }

    @PostMapping(path = "/stripe/webhook", consumes = "application/json")
    public ResponseEntity<Void> webhook(@RequestBody String payload,
                                        @RequestHeader("Stripe-Signature") String signature) {
        try {
            gateway.onWebhook(payload, signature);
            return ResponseEntity.ok().build();
        } catch (SignatureVerificationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}

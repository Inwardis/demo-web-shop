package com.webshop.payments;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Creates and captures Stripe PaymentIntents, verifies webhook signatures, and maps
 * Stripe's states onto ours. The only class that holds the Stripe secret — keep it
 * that way (ADR-003).
 *
 * @inwardis 3cd9a367-ebd5-4cca-8a78-eb573a917533 PaymentGateway
 */
@Service
public class PaymentGateway {

    private final FraudCheck fraudCheck;
    private final Ledger ledger;
    private final PaymentStore payments;
    private final String webhookSecret;

    public PaymentGateway(FraudCheck fraudCheck, Ledger ledger, PaymentStore payments,
                          @Value("${stripe.secret-key}") String secretKey,
                          @Value("${stripe.webhook-secret}") String webhookSecret) {
        this.fraudCheck = fraudCheck;
        this.ledger = ledger;
        this.payments = payments;
        this.webhookSecret = webhookSecret;
        com.stripe.Stripe.apiKey = secretKey;
    }

    /** Step 1 of a checkout: authorise the amount. The client secret goes back to Checkout. */
    @Transactional
    public String start(UUID orderId, long amountCents, String currency) throws StripeException {
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountCents)
                .setCurrency(currency)
                .setCaptureMethod(PaymentIntentCreateParams.CaptureMethod.MANUAL)
                .putMetadata("orderId", orderId.toString())
                .build();
        PaymentIntent intent = PaymentIntent.create(params, idempotent(orderId, "start"));
        payments.insert(orderId, intent.getId(), amountCents, currency);
        ledger.record(orderId, Ledger.Event.AUTHORISED, amountCents, currency);
        return intent.getClientSecret();
    }

    /** Step 2: the order is confirmed — ask the fraud check, then capture. */
    @Transactional
    public PaymentResult capture(UUID orderId) throws StripeException {
        PaymentStore.Payment p = payments.byOrder(orderId)
                .orElseThrow(() -> new IllegalStateException("no payment started for order " + orderId));
        FraudCheck.Verdict verdict = fraudCheck.before(orderId, p.amountCents());
        if (verdict == FraudCheck.Verdict.DECLINE) {
            PaymentIntent.retrieve(p.paymentIntentId()).cancel();
            payments.setState(orderId, PaymentStore.State.DECLINED);
            return PaymentResult.DECLINED;
        }
        PaymentIntent.retrieve(p.paymentIntentId()).capture(idempotent(orderId, "capture"));
        ledger.record(orderId, Ledger.Event.CAPTURED, p.amountCents(), p.currency());
        PaymentResult result = verdict == FraudCheck.Verdict.REVIEW ? PaymentResult.CAPTURED_FOR_REVIEW : PaymentResult.CAPTURED;
        payments.setState(orderId, result == PaymentResult.CAPTURED ? PaymentStore.State.CAPTURED : PaymentStore.State.CAPTURED_FOR_REVIEW);
        return result;
    }

    /** Stripe's webhook. Unsigned or wrongly signed events are rejected before anything is read. */
    @Transactional
    public void onWebhook(String payload, String signatureHeader) throws SignatureVerificationException {
        Event event = Webhook.constructEvent(payload, signatureHeader, webhookSecret);
        if ("charge.dispute.created".equals(event.getType())) {
            event.getDataObjectDeserializer().getObject()
                    .map(o -> ((com.stripe.model.Dispute) o).getPaymentIntent())
                    .flatMap(payments::byIntent)
                    .ifPresent(p -> ledger.record(p.orderId(), Ledger.Event.CHARGEBACK, p.amountCents(), p.currency()));
        }
        if ("payment_intent.payment_failed".equals(event.getType())) {
            event.getDataObjectDeserializer().getObject()
                    .map(o -> ((PaymentIntent) o).getId())
                    .flatMap(payments::byIntent)
                    .ifPresent(p -> payments.setState(p.orderId(), PaymentStore.State.FAILED));
        }
    }

    // Refunds: not here yet. Support refunds in the Stripe dashboard; the ledger never hears of them.

    private static RequestOptions idempotent(UUID orderId, String step) {
        return RequestOptions.builder().setIdempotencyKey(orderId + ":" + step).build();
    }

    public enum PaymentResult { CAPTURED, CAPTURED_FOR_REVIEW, DECLINED }
}

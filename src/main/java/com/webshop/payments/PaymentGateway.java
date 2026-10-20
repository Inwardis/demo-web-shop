package com.webshop.payments;

import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Creates and captures Stripe PaymentIntents and maps Stripe's states onto ours.
 * The only class that holds the Stripe secret — keep it that way (ADR-003).
 *
 * @inwardis 3cd9a367-ebd5-4cca-8a78-eb573a917533 PaymentGateway
 */
@Service
public class PaymentGateway {

    private final FraudCheck fraudCheck;
    private final Ledger ledger;

    public PaymentGateway(FraudCheck fraudCheck, Ledger ledger) {
        this.fraudCheck = fraudCheck;
        this.ledger = ledger;
    }

    /** Step 1 of a checkout: authorise the amount. The client secret goes back to Checkout. */
    public String start(UUID orderId, long amountCents, String currency) throws Exception {
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountCents)
                .setCurrency(currency)
                .setCaptureMethod(PaymentIntentCreateParams.CaptureMethod.MANUAL)
                .putMetadata("orderId", orderId.toString())
                .build();
        PaymentIntent intent = PaymentIntent.create(params, idempotent(orderId));
        ledger.record(orderId, Ledger.Event.AUTHORISED, amountCents, currency);
        return intent.getClientSecret();
    }

    /** Step 2: the order is confirmed — ask the fraud check, then capture. */
    public PaymentResult capture(UUID orderId, String paymentIntentId, long amountCents, String currency) throws Exception {
        FraudCheck.Verdict verdict = fraudCheck.before(orderId, amountCents);
        if (verdict == FraudCheck.Verdict.DECLINE) {
            PaymentIntent.retrieve(paymentIntentId).cancel();
            return PaymentResult.DECLINED;
        }
        PaymentIntent.retrieve(paymentIntentId).capture();
        ledger.record(orderId, Ledger.Event.CAPTURED, amountCents, currency);
        return verdict == FraudCheck.Verdict.REVIEW ? PaymentResult.CAPTURED_FOR_REVIEW : PaymentResult.CAPTURED;
    }

    // Refunds: not here yet. Support refunds in the Stripe dashboard; the webhook tells the ledger a day later.

    private static com.stripe.net.RequestOptions idempotent(UUID orderId) {
        return com.stripe.net.RequestOptions.builder().setIdempotencyKey(orderId.toString()).build();
    }

    public enum PaymentResult { CAPTURED, CAPTURED_FOR_REVIEW, DECLINED }
}

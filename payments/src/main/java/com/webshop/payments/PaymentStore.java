package com.webshop.payments;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** Our side of a payment: the PaymentIntent id and the state we last mapped it to. */
@Repository
public class PaymentStore {

    public enum State { AUTHORISED, CAPTURED, CAPTURED_FOR_REVIEW, DECLINED, FAILED }

    public record Payment(UUID orderId, String paymentIntentId, long amountCents, String currency, State state) {}

    private final JdbcClient jdbc;

    public PaymentStore(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(UUID orderId, String paymentIntentId, long amountCents, String currency) {
        jdbc.sql("""
                INSERT INTO payments.payment (order_id, payment_intent_id, amount_cents, currency, state)
                VALUES (:orderId, :pi, :amount, :currency, 'AUTHORISED')""")
                .param("orderId", orderId).param("pi", paymentIntentId)
                .param("amount", amountCents).param("currency", currency)
                .update();
    }

    public Optional<Payment> byOrder(UUID orderId) {
        return jdbc.sql("""
                SELECT order_id, payment_intent_id, amount_cents, currency, state
                FROM payments.payment WHERE order_id = :orderId""")
                .param("orderId", orderId)
                .query(Payment.class).optional();
    }

    public Optional<Payment> byIntent(String paymentIntentId) {
        return jdbc.sql("""
                SELECT order_id, payment_intent_id, amount_cents, currency, state
                FROM payments.payment WHERE payment_intent_id = :pi""")
                .param("pi", paymentIntentId)
                .query(Payment.class).optional();
    }

    /** Moves the payment and the order's payment_status together — Checkout reads the latter. */
    public void setState(UUID orderId, State state) {
        jdbc.sql("UPDATE payments.payment SET state = :s, updated_at = now() WHERE order_id = :orderId")
                .param("s", state.name()).param("orderId", orderId).update();
        jdbc.sql("UPDATE orders.customer_order SET payment_status = :s WHERE id = :orderId")
                .param("s", state.name()).param("orderId", orderId).update();
    }
}

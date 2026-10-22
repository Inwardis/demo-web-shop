package com.webshop.payments;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Append-only record of every money movement, one row per event, written in the
 * same transaction as the payment state change it records.
 *
 * @inwardis cc1f9589-954f-4356-946b-e3dac51d4d45 Ledger
 */
@Component
public class Ledger {

    public enum Event { AUTHORISED, CAPTURED, REFUNDED, CHARGEBACK }

    private final JdbcClient jdbc;

    public Ledger(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** MANDATORY: a ledger row without the state change beside it is the bug this class exists to prevent. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(UUID orderId, Event event, long amountCents, String currency) {
        jdbc.sql("""
                INSERT INTO payments.ledger (order_id, event, amount_cents, currency)
                VALUES (:orderId, :event, :amount, :currency)""")
                .param("orderId", orderId)
                .param("event", event.name())
                .param("amount", amountCents)
                .param("currency", currency)
                .update();
    }
}

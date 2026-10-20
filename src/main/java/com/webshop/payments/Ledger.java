package com.webshop.payments;

import org.springframework.stereotype.Component;

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

    public void record(UUID orderId, Event event, long amountCents, String currency) {
        // INSERT INTO payments.ledger (order_id, event, amount_cents, currency, at) VALUES (...)
    }
}

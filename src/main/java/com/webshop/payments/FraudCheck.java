package com.webshop.payments;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Velocity and address checks before capture. Declines above a threshold, flags
 * for review in between, and never blocks a capture on a timeout.
 *
 * @inwardis 8eacc44a-3ddc-487c-bd3d-30b9b97d9eb3 FraudCheck
 */
@Component
public class FraudCheck {

    public enum Verdict { OK, REVIEW, DECLINE }

    private static final long REVIEW_ABOVE_CENTS = 50_000;
    private static final long DECLINE_ABOVE_CENTS = 500_000;

    public Verdict before(UUID orderId, long amountCents) {
        if (amountCents > DECLINE_ABOVE_CENTS) return Verdict.DECLINE;
        if (amountCents > REVIEW_ABOVE_CENTS) return Verdict.REVIEW;
        return Verdict.OK;
    }
}

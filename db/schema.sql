-- Orders DB — one PostgreSQL 16 database, three schemas, one per container that writes it.
-- Splitting it into one database per container is ADR-005: decided, not yet done.
--
-- @inwardis 52562884-ac58-49e2-935e-ae82d1a80467 Orders DB

CREATE SCHEMA IF NOT EXISTS catalog;
CREATE SCHEMA IF NOT EXISTS orders;
CREATE SCHEMA IF NOT EXISTS payments;

-- catalog: owned by Catalog. Checkout reads it only through Catalog's API.
CREATE TABLE catalog.product (
    sku          TEXT PRIMARY KEY,
    name         TEXT        NOT NULL,
    description  TEXT        NOT NULL DEFAULT '',
    price_cents  BIGINT      NOT NULL CHECK (price_cents >= 0),
    currency     CHAR(3)     NOT NULL DEFAULT 'EUR',
    stock        INTEGER     NOT NULL DEFAULT 0 CHECK (stock >= 0),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- orders: owned by Checkout.
CREATE TABLE orders.customer_order (
    id              UUID PRIMARY KEY,
    email           TEXT        NOT NULL,
    ship_to         JSONB       NOT NULL,
    total_cents     BIGINT      NOT NULL,
    currency        CHAR(3)     NOT NULL,
    payment_status  TEXT        NOT NULL DEFAULT 'PENDING',  -- written by Payments
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE orders.order_line (
    order_id     UUID    NOT NULL REFERENCES orders.customer_order (id),
    sku          TEXT    NOT NULL,
    quantity     INTEGER NOT NULL CHECK (quantity > 0),
    price_cents  BIGINT  NOT NULL,
    PRIMARY KEY (order_id, sku)
);

-- payments: owned by Payments, the only container in PCI scope (ADR-003). No card data here —
-- Stripe holds it; we keep the PaymentIntent id and our own record of every money movement.
CREATE TABLE payments.payment (
    order_id           UUID PRIMARY KEY,
    payment_intent_id  TEXT        NOT NULL UNIQUE,
    amount_cents       BIGINT      NOT NULL,
    currency           CHAR(3)     NOT NULL,
    state              TEXT        NOT NULL,
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Append-only. Never UPDATE or DELETE a row; a correction is a new event.
CREATE TABLE payments.ledger (
    id            BIGSERIAL PRIMARY KEY,
    order_id      UUID        NOT NULL,
    event         TEXT        NOT NULL CHECK (event IN ('AUTHORISED', 'CAPTURED', 'REFUNDED', 'CHARGEBACK')),
    amount_cents  BIGINT      NOT NULL,
    currency      CHAR(3)     NOT NULL,
    at            TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ledger_order_idx ON payments.ledger (order_id);

# Web shop

*Trailhead Supply*, a small outdoor-gear shop: a demo codebase for [Inwardis](https://inwardis.com).
The architecture — why each part exists, what it is governed by, how the parts talk — lives in the
Inwardis model **Web shop**. The `@inwardis <uuid>` marks in the code name the element each file is,
so the model and the code point at each other.

| Path | Model element | What it is |
|---|---|---|
| `checkout/` | Checkout (container) | Next.js 15 — basket, address, order confirmation. Starts a payment, never sees card data. |
| `catalog/` | Catalog (container) | Spring Boot 4 — products, prices, stock. Product reads are cached for 60 s. |
| `payments/` | Payments (container) | Spring Boot 4 — the only service that talks to Stripe and the only one in PCI scope (ADR-003). |
| `payments/…/PaymentGateway.java` | PaymentGateway | PaymentIntents, capture, webhook signatures. Holds the Stripe secret. |
| `payments/…/FraudCheck.java` | FraudCheck | Checks before capture. |
| `payments/…/Ledger.java` | Ledger | Append-only record of every money movement. |
| `db/schema.sql` | Orders DB | One PostgreSQL 16 database, three schemas (ADR-005 will split it). |

Refunds are not built yet — they happen in the Stripe dashboard today.

## Run it

Java 21 and Node 22 or later.

```
./gradlew build -x test
(cd checkout && npx pnpm install && npx pnpm build)
docker compose up
```

The shop is on http://localhost:3000. With the placeholder Stripe keys you can browse and fill a basket;
to pay, put Stripe **test-mode** keys in `.env` (`STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET`,
`STRIPE_PUBLISHABLE_KEY`) and use card `4242 4242 4242 4242`.

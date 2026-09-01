# 6: Explicit Multi-Currency Price Lists (Discrete Pricing with Value Objects)

Date: 2026-08-25

## Status

Accepted

## Context

In retail and e-commerce platforms, products are sold across multiple geographical markets and financial jurisdictions requiring support for diverse currencies (e.g., EUR, USD, GBP). 

Handling multi-currency pricing in distributed backend engines presents critical trade-offs between dynamic real-time foreign exchange (FX) conversion and explicit discrete pricing. Storing a single base currency and dynamically multiplying by FX rates introduces significant architectural and business drawbacks:
1. **Charm & Psychological Pricing Loss**: Retail pricing relies on market-calibrated charm pricing (e.g., 99.99 EUR vs. 119.99 USD). Algorithmic FX conversions produce arbitrary values (e.g., 108.43 USD).
2. **Rounding Drift & Idempotency Degradation**: Repeated floating-point or arbitrary-precision division/multiplication on write-read cycles creates non-reversible rounding drift and non-deterministic assertions.
3. **Query Latency**: Joining real-time FX rate tables or invoking external FX services on hot read paths introduces latency and availability dependencies.

---

## Decision Drivers

- **Zero Rounding Drift**: Guarantee absolute precision without mathematical degradation across storage and retrieval cycles.
- **Market-Specific Charm Pricing**: Preserve explicit retail price points per target market and currency.
- **Ultra-Low Latency Read Paths**: Optimize active price resolution to single-digit index scans without joins or runtime math.
- **Strict Concurrency & Overlap Isolation**: Guarantee non-overlapping temporal validity intervals per product AND per currency.
- **Domain Purity & Immutability**: Model monetary amounts as self-validating Value Objects in pure Java 25.

---

## Considered Options

### Option A: Base Currency + Dynamic FX Multipliers
Store all prices in a single base currency (e.g., EUR) and calculate localized prices at query time using dynamic exchange rates.
- *Pros*: Single price entry per product interval.
- *Cons*: Destroys market charm pricing; introduces runtime FX join overhead; vulnerable to rounding drift; fails to support market-specific pricing strategies.

### Option B: Explicit Multi-Currency Price Lists (Discrete Pricing with Value Objects) — [SELECTED]
Store explicit, discrete price rows per `(product_id, price_currency)` with exact scale (`NUMERIC(19, 2)`) and ISO-4217 currency codes (`VARCHAR(3)`). Enforce temporal non-overlap per currency at the database layer using composite PostgreSQL GiST exclusion constraints.
- *Pros*: Exact charm pricing; zero runtime FX calculation overhead; deterministic scale normalization via Value Objects; independent lifecycle per market currency.
- *Cons*: Requires discrete price definitions for each target currency.

### Option C: Hybrid Dynamic Fallback
Store explicit prices when available, falling back to dynamic FX conversion from base currency when missing.
- *Pros*: Fallback availability for unconfigured markets.
- *Cons*: Complex bifurcated query logic; inconsistent pricing contracts; non-deterministic response times.

---

## Decision Outcome

We choose **Option B: Explicit Multi-Currency Price Lists (Discrete Pricing with Value Objects)**.

All prices are stored and resolved as discrete entities scoped by `(product_id, price_currency)`.

---

## Consequences

### Positive
- **Deterministic Financial Integrity**: Money is encapsulated in an immutable `Money` Value Object with scale normalized to the currency's fractional digits.
- **Zero Query-Time Computation**: Queries fetch pre-materialized, market-approved prices directly from the database index.
- **Composite Temporal Concurrency**: PostgreSQL `btree_gist` exclusion constraints guarantee that no product can have overlapping price intervals within the same currency, while allowing concurrent valid intervals across different currencies.
- **REST Contract Clarity**: API endpoints default to `EUR` when omitted, while accepting explicit ISO-4217 currency query parameters and payload fields.

### Negative / Trade-offs
- **Storage Multiplier**: Products sold in multiple currencies require separate price rows.
- **Administrative Responsibility**: Price changes must be published per currency.

---

## Database & API Mapping

### 1. PostgreSQL Schema with Composite GiST Constraint

```sql
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE product_prices (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    price_amount NUMERIC(19, 2) NOT NULL,
    price_currency VARCHAR(3) NOT NULL,
    init_date DATE NOT NULL,
    end_date DATE,
    CONSTRAINT chk_price_amount_positive CHECK (price_amount > 0),
    CONSTRAINT chk_price_dates CHECK (end_date IS NULL OR init_date < end_date),
    CONSTRAINT ex_product_currency_validity EXCLUDE USING gist (
        product_id WITH =,
        price_currency WITH =,
        daterange(init_date, coalesce(end_date, 'infinity'), '[]') WITH &&
    )
);

CREATE INDEX idx_product_prices_lookup 
ON product_prices (product_id, price_currency, init_date ASC);
```

### 2. Core Domain Model (`Money` Record)

```java
package com.mango.products.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

public record Money(BigDecimal amount, Currency currency) {

  public Money {
    validate(amount, currency);
    amount = amount.setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_UP);
  }

  private static void validate(BigDecimal amount, Currency currency) {
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Amount must be greater than zero");
    }
    if (currency == null) {
      throw new IllegalArgumentException("Currency must not be null");
    }
  }
}
```

### 3. REST API Contract & Fallback Behavior

- `POST /products/{id}/prices`: Accepts optional `currency` in the JSON request body (defaults to `EUR` if omitted).
- `GET /products/{id}/prices?date=YYYY-MM-DD&currency=EUR`: Queries active price for the specific product and currency, defaulting `currency` to `EUR` when omitted.
- `GET /products/{id}/prices?currency=EUR`: Returns historical price series for the specified product and currency (or all currencies if unconstrained).

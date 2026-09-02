# 4. Price Lifecycle Invariants and Historical Pricing Immutability

Date: 2026-08-28

## Status

Accepted

## Context

The Products & Prices Management API manages temporal price intervals for products across multiple currencies. Because pricing data is used for financial records, order billing reconciliation, and audit compliance, price histories must be treated with strict immutability and continuous validity guarantees.

Allowing arbitrary creation or deletion of past price intervals risks compromising the integrity of historical audits. Furthermore, managing open-ended prices without temporal sequence validation could result in overlapping intervals or unresolvable active prices.

## Decision

We establish the following core architectural and domain assumptions regarding the price lifecycle:

1. **Historical Immutability & Audit Trail**:
   - Historical prices are immutable to guarantee billing and audit reproducibility.
   - Modifying past price records or executing hard deletions is strictly prohibited in the public API.
   - Future support for administrative adjustments of past price data will be governed by a dedicated role-based authorization system (out of scope for this API version).

2. **Sequential Price Creation (`AddPriceToProductUseCase`)**:
   - New prices can only be registered sequentially after the existing price timeline for a product and currency.
   - When adding a price:
     - The latest price for the product and currency is queried.
     - If the latest price exists with an open-ended validity period (`endDate == null`), creation is rejected until that price's `endDate` is explicitly set.
     - The new price's `initDate` must be strictly after the latest price's `endDate` (`initDate > latestPrice.endDate`).
     - If no prior price exists for the product and currency, creation proceeds unconditionally.

3. **Active Price Updating (`UpdatePriceUseCase`)**:
   - Only the currently active price (`initDate <= today <= endDate`) can be updated.
   - Updating past or future prices via the standard update endpoint is rejected.

4. **Removal of Hard Deletion (`DeletePrice`)**:
   - The price deletion endpoint (`DELETE /products/{id}/prices/{priceId}`) is permanently discarded.
   - Price records cannot be deleted through this API, preserving data lineage and historical integrity.

## Consequences

### Positive
- **Guaranteed Audit Integrity**: Eliminates the risk of deleting or corrupting past price points used in completed transactions.
- **Deterministic Temporal Sequencing**: Prevents overlapping validity windows and ensures unambiguous active price resolution.

### Negative / Trade-offs
- **Correction Workflow**: Correcting an active price's end date requires explicit updates before new intervals can be scheduled.
- **Administrative Flexibility**: Operators cannot retroactively backdate prices without a separate authorized administrative tool.

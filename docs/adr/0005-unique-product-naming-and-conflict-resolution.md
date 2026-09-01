# 5. Case-Insensitive Product Name Uniqueness and Domain Port Conflict Resolution

Date: 2026-09-01

## Status

Accepted

## Context

In catalog management systems, creating products with duplicate or nearly duplicate names (e.g., varying only by letter casing) creates data pollution, inventory confusion, and poor customer experience.

Enforcing uniqueness strictly at the application layer via pre-insert queries (`SELECT COUNT(*) ...`) introduces Time-of-Check to Time-of-Use (TOCTOU) race conditions under concurrent requests and doubles database network latency. Conversely, enforcing uniqueness exclusively via database primary keys or standard column unique constraints either prevents preserving original display capitalization or requires complex database-side triggers.

Furthermore, when a client attempts to create a duplicate product, the API must provide actionable feedback by returning the UUID of the existing conflicting product resource.

## Decision

We adopt a **Database-Enforced Functional Unique Index paired with Domain Port Conflict Resolution**:

1. **Database Layer (Integrity & Performance)**:
   - Create a case-insensitive functional unique index on the product table:
     ```sql
     CREATE UNIQUE INDEX uk_product_name_lower ON product (LOWER(name));
     ```
   - This preserves user-entered display casing in `product.name` while guaranteeing atomic, sub-microsecond uniqueness enforcement during writes without table locks.

2. **Domain Layer (Ports & Invariants)**:
   - Declare `Optional<Id> findConflictingProductId(Name name);` on the domain `ProductRepository` port interface.
   - Model `DuplicateProductNameException` as a pure domain exception carrying the non-deterministic conflicting product `Id` and `Name`.

3. **Application Layer (Use Case Orchestration)**:
   - In `CreateProductUseCase`, execute `productRepository.save(product)`.
   - On duplicate violation caught from the repository, query `productRepository.findConflictingProductId(name)` and throw the enriched `DuplicateProductNameException(conflictingId, name)`.

4. **Infrastructure Layer (HTTP ProblemDetail)**:
   - In `ProductsApiExceptionHandler`, map `DuplicateProductNameException` to HTTP `409 Conflict` returning an RFC 9457 `ProblemDetail` with `title: "Duplicate Product Name"` and `conflictingProductId` property.

## Consequences

### Positive
- **100% Race Condition Proof**: Atomic PostgreSQL index locks prevent duplicate insertions regardless of concurrency.
- **Single Round-Trip on Happy Path**: New product creation requires exactly 1 database write round-trip.
- **Display Capitalization Preserved**: Original character casing (e.g. `"Nike Air Max 2025"`) is stored in the database.
- **Actionable Client Diagnostics**: Clients receive HTTP 409 Conflict with the exact conflicting resource ID for immediate deduplication or navigation.

### Negative / Trade-offs
- Conflict failure paths execute a secondary $O(\log N)$ index read to resolve the conflicting ID.

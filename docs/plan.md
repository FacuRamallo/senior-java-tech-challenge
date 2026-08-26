# Products & Prices System: Incremental TDD Implementation Plan

High-performance Hexagonal Architecture system for product management and dynamic multi-currency price resolution in Java 25 and Spring Boot 4.1.0.

## Multi-Currency Core System Architecture
- **Pricing Strategy**: Explicit Discrete Multi-Currency Price Lists ([ADR-0043](adr/ADR-0043-multi-currency-discrete-pricing.md)). Pre-materialized discrete price rows per `(product_id, price_currency)` to preserve retail charm pricing (.99) and eliminate runtime FX conversion latency.
- **Financial Domain Modeling**: Immutable `Money(BigDecimal amount, Currency currency)` record with scale normalized to currency fraction digits.
- **Temporal Modeling**: Civil dates (`LocalDate`) mapped to PostgreSQL `DATE` with injected `Clock` ([ADR-0003](adr/0003-temporal-modeling-and-timezone-architecture.md)).
- **Database Concurrency & Integrity**: PostgreSQL `btree_gist` composite exclusion constraint `EXCLUDE USING gist (product_id WITH =, price_currency WITH =, daterange(init_date, coalesce(end_date, 'infinity'), '[]') WITH &&)` ensuring non-overlapping validity ranges per product AND per currency.
- **REST Fallback Semantics**:
  - `POST /products/{id}/prices`: Accepts optional/explicit `currency` (defaults to `EUR` if omitted).
  - `GET /products/{id}/prices?date=YYYY-MM-DD&currency=EUR`: Resolves active price for product and currency (defaults `currency` to `EUR`).
  - `GET /products/{id}/prices?currency=EUR`: Chronological price history filtered by currency (or all currencies).

---

## Feature Slices & Roadmap

### Feature 1: Create a Product (`POST /products`) — [COMPLETED & PUSHED]
- **Domain:** `Product`, `Id` (UUIDv7 validated), `Name` (non-blank), `Description` (non-blank), `ProductRepository`.
- **Application:** `CreateProductCommand`, `CreateProductUseCase` (tested in `CreateProductUseCaseShould`).
- **Infrastructure:** `V0001__create_products_table.sql`, `PostgreSqlProductRepository` (`NamedParameterJdbcTemplate`), `CreateProductController`, `PricesExceptionHandler`.
- **Acceptance Test:** `CreateProductFeature` (single happy-path test).
- **OpenAPI:** Documented in `docs/openapi.yaml`.

### Feature 2: Add a Price to a Product (`POST /products/{id}/prices`)
- **Step 1 (RED):** Define `Price` / `Money` (normalized scale, ISO-4217 currency) / `ValidityPeriod` (`LocalDate initDate`, nullable `LocalDate endDate`) domain model and failing test.
- **Step 2 (GATEWAY):** Human checkpoint to validate aggregate boundaries.
- **Step 3 (GREEN & REFACTOR):** Implement `AddPriceToProductUseCase` and unit tests in `AddPriceToProductUseCaseShould` asserting domain invariants (`amount > 0`, non-null `currency`, `initDate < endDate` when `endDate != null`).
- **Step 4 (Infrastructure & DB):** Flyway migration `V0002__create_product_prices_table.sql` with `btree_gist` extension, composite GiST exclusion constraint on `(product_id, price_currency, daterange)`, and `PostgreSqlPriceRepository`.
- **Step 5 (Acceptance & Docs):** `AddPriceFeature` acceptance test (with explicit and default `EUR` currency) and update `docs/openapi.yaml`.

### Feature 3: Get Active Price on Date (`GET /products/{id}/prices?date=YYYY-MM-DD&currency=EUR`)
- **Step 1 (RED & GATEWAY):** Define `GetActivePriceUseCase` query contract with currency scope.
- **Step 2 (GREEN):** Implement active price resolution query for currency-scoped date match.
- **Step 3 (Infrastructure & DB):** Query `PostgreSqlPriceRepository` with Postgres `daterange @> :date::date` and `price_currency = :currency` filtering.
- **Step 4 (Acceptance & Docs):** `GetActivePriceFeature` acceptance test and update `docs/openapi.yaml`.

### Feature 4: Get Price History (`GET /products/{id}/prices?currency=EUR`)
- **Step 1 (RED & GATEWAY):** Define `GetPriceHistoryUseCase` query contract.
- **Step 2 (GREEN):** Implement price history projection mapping.
- **Step 3 (Infrastructure & DB):** Query `PostgreSqlPriceRepository` for chronological price history ordered by `init_date ASC`.
- **Step 4 (Acceptance & Docs):** `GetPriceHistoryFeature` acceptance test and update `docs/openapi.yaml`.

### Verification & Performance Gates
- `./gradlew clean build` (runs unit tests, integration tests, JaCoCo, and Spotless checks).
- GraalVM Native Image compilation & load benchmark via `docker-compose.yml`.

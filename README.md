# 🛍️ High-Performance Multi-Currency Product & Historical Pricing Engine

A production-grade, ultra-high-throughput Hexagonal API for Product Management and Dynamic Multi-Currency Historical Price Resolution, engineered in **Java 25 (Virtual Threads)**, **Spring Boot 4.1**, **GraalVM Native Image**, and **PostgreSQL 17** with `btree_gist` temporal exclusion constraints.

---

## ⚡ Tech Stack & Architecture

- **Language & Runtime**: Java 25 (Virtual Threads / Project Loom), GraalVM Native Image (SubstrateVM).
- **Framework**: Spring Boot 4.1.0 (Native AOT compilation, Spring JDBC `NamedParameterJdbcTemplate`).
- **Database & Concurrency**: PostgreSQL 17 with `btree_gist` extension for kernel-level composite exclusion constraints.
- **Database Migrations**: Eager Flyway migrations (`V0001`, `V0002`) with 4-digit versioning.
- **Architecture**: Pure Hexagonal Architecture (Ports & Adapters) organized by feature slice.
- **Testing**: Inside-Out TDD, MockMvc acceptance tests (`src/integrationTest`) backed by Testcontainers PostgreSQL, whole-object unit tests (`src/test`).
- **Code Standards**: Spotless (Google Java Format 1.30.0), JaCoCo code coverage, Zero Comments Policy, Zero Dead Code Policy.

---

## 🏛️ Architectural Foundations & Key Decisions

### 1. Hexagonal Architecture (Ports & Adapters)
The codebase strictly enforces the Ports & Adapters pattern within `com.mango.products.prices`:
- **`domain`**: Pure business models, aggregates (`Product`, `Price`), self-validating Value Objects (`Id`, `Money`, `Currency`, `ValidityPeriod`, `Name`, `Description`), and outbound port interfaces (`ProductRepository`, `PriceRepository`). Zero framework or database dependencies.
- **`application`**: CQRS-ready Use Cases and commands/queries (`CreateProductUseCase`, `AddPriceToProductUseCase`, `GetActivePriceUseCase`, `GetPriceHistoryUseCase`). Orchestrates domain models and outbound ports.
- **`infrastructure`**: REST controllers, web DTOs, PostgreSQL JDBC adapters (`PostgreSqlProductRepository`, `PostgreSqlPriceRepository`), Spring configurations (`PricesConfiguration`), and global `@RestControllerAdvice` error handlers.

### 2. Multi-Currency Discrete Pricing (ADR-0043)
Rather than performing volatile dynamic FX conversions on read, the engine implements **Option B: Discrete Multi-Currency Price Lists**:
- Discrete `Money(BigDecimal amount, Currency currency)` with scale normalized to ISO-4217 fraction digits (e.g. 2 decimal places for `EUR`/`USD`).
- Each currency interval is independently stored and queried per product.
- Supported default currency fallback to `EUR` when omitted by clients.

### 3. Concurrency & Temporal Exclusion Invariants (ADR-0003)
Non-overlapping validity ranges are guaranteed at the database engine level via PostgreSQL `btree_gist` composite exclusion index:
```sql
CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE product_prices
ADD CONSTRAINT product_prices_no_overlap
EXCLUDE USING gist (
    product_id WITH =,
    price_currency WITH =,
    daterange(init_date, coalesce(end_date, 'infinity'), '[]') WITH &&
);
```
- **Zero Race Conditions**: Eliminates the need for pessimistic table locks or distributed locks during concurrent price insertions.
- **O(1) / O(log N) Active Price Resolution**: Active prices on any date are resolved via index-accelerated PostgreSQL range containment:
  ```sql
  SELECT id, product_id, price_amount, price_currency, init_date, end_date
  FROM product_prices
  WHERE product_id = :productId
    AND price_currency = :priceCurrency
    AND daterange(init_date, coalesce(end_date, 'infinity'), '[]') @> :date::date
  LIMIT 1;
  ```

---

## 📘 API Reference

Detailed OpenAPI 3.1 specification is available in [`docs/openapi.yaml`](docs/openapi.yaml).

### 1. Create a Product
Registers a new product using a client-generated UUIDv7 identifier for idempotency.

- **Endpoint**: `POST /products`
- **Headers**: `Content-Type: application/json`
- **Request Body**:
  ```json
  {
    "id": "01952e42-7a57-7000-8000-000000000001",
    "name": "Zapatillas de correr",
    "description": "Modelo profesional amortiguado"
  }
  ```
- **Response**: `201 Created`
  - **Header**: `Location: /products/01952e42-7a57-7000-8000-000000000001`

---

### 2. Add a Price to a Product
Adds a discrete price interval to a product. Enforces date ordering (`initDate < endDate`) and non-overlapping intervals per currency.

- **Endpoint**: `POST /products/{id}/prices`
- **Headers**: `Content-Type: application/json`
- **Request Body**:
  ```json
  {
    "value": 99.99,
    "currency": "EUR",
    "initDate": "2024-01-01",
    "endDate": "2024-06-30"
  }
  ```
- **Response**: `201 Created`
  - **Header**: `Location: /products/{id}/prices/{priceId}`

---

### 3. Get Active Price on Date
Resolves the active price for a product on a specific date and currency (currency defaults to `EUR` if omitted).

- **Endpoint**: `GET /products/{id}/prices?date=2024-03-15&currency=EUR`
- **Response**: `200 OK`
  ```json
  {
    "value": 99.99,
    "currency": "EUR"
  }
  ```
- **Response**: `404 Not Found` (when no price is active for the given date/currency).

---

### 4. Get Complete Price History
Retrieves all historical price intervals for a product ordered chronologically (`init_date ASC`).

- **Endpoint**: `GET /products/{id}/prices?currency=EUR`
- **Response**: `200 OK`
  ```json
  [
    {
      "id": "01952e42-7a57-7000-8000-000000000002",
      "value": 99.99,
      "currency": "EUR",
      "initDate": "2024-01-01",
      "endDate": "2024-06-30"
    },
    {
      "id": "01952e42-7a57-7000-8000-000000000003",
      "value": 149.99,
      "currency": "EUR",
      "initDate": "2024-07-01",
      "endDate": "2024-12-31"
    }
  ]
  ```

---

## 🛠️ Build & Execution Commands

### Local Development (Gradle)

```bash
# Run all unit tests (65 tests)
./gradlew test

# Run all integration & acceptance tests with Testcontainers
./gradlew integrationTest

# Full clean build (compilation + unit tests + integration tests + JaCoCo coverage)
./gradlew clean build

# Format source code using Google Java Format
./gradlew spotlessApply

# Verify code formatting compliance
./gradlew spotlessCheck
```

### Docker Compose (Standard Mode)

```bash
# Start PostgreSQL and Spring Boot application
docker compose up --build
```

### Automated Benchmark & Load Testing (k6)

```bash
# Run application and automated high-throughput k6 performance benchmark
docker compose -f docker-compose.yml -f docker-compose.override.yml up --build --abort-on-container-exit
```

---

## 📂 Architecture Decision Records (ADRs)

| ADR | Title | Status |
| :--- | :--- | :--- |
| [ADR-0001](docs/adr/0001-hexagonal-architecture-and-package-structure.md) | Hexagonal Architecture & Package Structure | Accepted |
| [ADR-0002](docs/adr/0002-testing-strategy-and-tdd-state-machine.md) | Testing Strategy & Inside-Out TDD State Machine | Accepted |
| [ADR-0003](docs/adr/0003-temporal-modeling-and-timezone-architecture.md) | Temporal Modeling & PostgreSQL Range Containment | Accepted |
| [ADR-0043](docs/adr/ADR-0043-multi-currency-discrete-pricing.md) | Multi-Currency Discrete Pricing & Money Value Object | Accepted |

---

## 📄 Original Challenge Requirements
For reference, the original challenge specification and problem statement are preserved in [`INSTRUCTIONS.md`](INSTRUCTIONS.md).

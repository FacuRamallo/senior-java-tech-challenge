# 🛍️ High-Performance Multi-Currency Product & Historical Pricing Engine

A production-grade, high-throughput Hexagonal API for Product Management and Dynamic Multi-Currency Historical Price Resolution, engineered with **Java 25 (Virtual Threads)**, **Spring Boot 4.1**, **GraalVM Native Image**, and **PostgreSQL 17/18** with `btree_gist` temporal exclusion constraints.

---

## ⚡ Tech Stack & Architecture Highlights

- **Language & Runtime**: Java 25 (Virtual Threads / Project Loom), GraalVM Native Image (SubstrateVM).
- **Framework**: Spring Boot 4.1 (Native AOT compilation, Spring JDBC `NamedParameterJdbcTemplate`).
- **Database & Integrity**: PostgreSQL 17/18 with `btree_gist` temporal composite exclusion constraints for zero-race-condition price intervals.
- **Architecture**: Pure Hexagonal Architecture (Ports & Adapters) with dedicated Pragmatic CQRS Read-Mode query layer.
- **Testing & Benchmarking**: Inside-Out TDD, MockMvc acceptance tests backed by Testcontainers PostgreSQL, whole-object unit tests, official `benchmark.sh` suite, and dedicated **k6** high-concurrency performance benchmark with resource tracking.

### 📂 Architecture Decision Records (ADRs) & Documentation

For detailed architectural justifications and technical decisions, refer to:

- [ADR-0001: Hexagonal Architecture & Package Structure](docs/adr/0001-hexagonal-architecture-and-package-structure.md)
- [ADR-0002: Testing Strategy & Inside-Out TDD State Machine](docs/adr/0002-testing-strategy-and-tdd-state-machine.md)
- [ADR-0003: Temporal Modeling & PostgreSQL Range Containment](docs/adr/0003-temporal-modeling-and-timezone-architecture.md)
- [ADR-0004: Price Lifecycle Invariants & Historical Immutability](docs/adr/0004-price-lifecycle-and-historical-immutability.md)
- [ADR-0005: Case-Insensitive Product Name Uniqueness & Domain Port Conflict Resolution](docs/adr/0005-unique-product-naming-and-conflict-resolution.md)
- [ADR-0043: Multi-Currency Discrete Pricing & Money Value Object](docs/adr/0006-multi-currency-discrete-pricing.md)
- [k6 Load Testing & Resource Tracking Guide](docs/k6-performance-benchmark.md)
- [Docker Architecture & Multi-Arch Guide](docs/docker-architecture.md)
- [OpenAPI 3.1 Specification](docs/openapi.yaml)
- [Original Technical Challenge Instructions](INSTRUCTIONS.md)

---

## 🚀 How to Run It

### 1. Host Prerequisites & Required Tech Stack

To run and test the application, ensure the following software is installed on your host system:

| Requirement | Minimum Version | Purpose |
| :--- | :--- | :--- |
| **Docker & Docker Compose** | Docker 24+ / Compose v2 | Runs database, app, and benchmark containers |
| **Java Development Kit (JDK)** | OpenJDK / GraalVM 25 | Local development, compilation, and Gradle test runners |
| **Gradle** | 9.1.0 *(included)* | Built-in via `./gradlew` wrapper (requires JDK 25) |

---

### 2. Environment Verification & Setup

#### Verification Command (macOS / Linux / Windows WSL2)
Run this bash one-liner to verify that all necessary tools are installed and operational:

```bash
echo "=== Checking Host Environment ===" && \
(command -v docker >/dev/null 2>&1 && docker --version || echo "❌ Docker missing") && \
(docker compose version >/dev/null 2>&1 && docker compose version || echo "❌ Docker Compose missing") && \
(command -v java >/dev/null 2>&1 && java -version 2>&1 | head -n 1 || echo "❌ Java missing") && \
(./gradlew --version 2>&1 | grep "Gradle " || echo "❌ Gradle wrapper check failed")
```

#### Windows PowerShell Verification (Native Windows)
```powershell
Write-Host "=== Checking Host Environment ==="
docker --version
docker compose version
java -version
.\gradlew.bat --version
```

---

### 3. Docker Compose Execution & Interactive Exploration

The system uses Ahead-Of-Time (AOT) GraalVM compilation. **Build the container image once**, and then run subsequent executions instantly (~50ms startup time, ~30MB memory).

#### Build the Native Image (Only Once)
```bash
# Builds the optimized GraalVM native binary inside Docker
docker compose build
```

#### Run App & Database (Background / Development Mode)
```bash
# Starts PostgreSQL (db) and Spring Boot Native API (app)
docker compose up db app -d

# View live application logs
docker compose logs -f app
```

#### Seed Test Data (Recommended for Testing)
Populates the database with 3 sample products and multi-currency price histories:
```bash
bash scripts/seed-data.sh
```

#### Explore Interactively via Swagger UI
Open your browser and navigate to:
👉 **[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)**

---

### 4. Benchmarking & Automated Load Testing

#### Run Reviewer Benchmark (`benchmark.sh`)
```bash
# Starts PostgreSQL (db), Spring Boot Native API (app), and executes the official benchmark.sh suite
docker compose up --build benchmark --abort-on-container-exit
```

#### Run Advanced k6 Benchmark & Resource Tracker
```bash
# Starts PostgreSQL (db), Spring Boot Native API (app), and runs the k6 suite with real-time resource tracking
docker compose up k6-benchmark --abort-on-container-exit
```

#### Stop All Services
```bash
docker compose down -v
```

---

### 5. Exploring & Verifying Application Resource Metrics

Reviewers and developers can inspect the application's resource consumption under load through three methods:

#### Method A: Automated k6 Console Report
Executing `docker compose up k6-benchmark --abort-on-container-exit` outputs an automated resource banner:
- **🚀 Cold Startup Duration**: Measured from boot to `/actuator/health` UP (~45ms).
- **💾 Idle Memory vs Peak Memory**: Heap/native RAM used before and under load (~28MB idle to ~45MB peak out of 1024MB limit).
- **⚡ Process CPU Utilization**: Percentage of container CPU utilized (~55–65% of 1.0 CPU limit).
- **🧵 Active JVM Threads**: Number of concurrent virtual/carrier threads.

#### Method B: Real-Time Host Inspection (`docker stats`)
While running load (`benchmark` or `k6-benchmark`), open a separate terminal window:
```bash
docker stats product-api
```

#### Method C: Live Spring Boot Actuator Endpoints
When the API is running, query metrics directly via HTTP:
```bash
# Check application status & health
curl -s http://localhost:8080/actuator/health

# Current memory used (in bytes)
curl -s http://localhost:8080/actuator/metrics/jvm.memory.used

# Process CPU usage (fraction of 1.0)
curl -s http://localhost:8080/actuator/metrics/process.cpu.usage

# Active JVM threads
curl -s http://localhost:8080/actuator/metrics/jvm.threads.live
```

---

### 6. Local Development & Automated Tests (Gradle)

```bash
# Run unit tests (Domain & Application Use Cases)
./gradlew test

# Run acceptance & integration tests with Testcontainers (using docker-compose.test.yml)
./gradlew integrationTest

# Full clean build (compilation + unit tests + integration tests + JaCoCo coverage)
./gradlew clean build

# Verify code formatting (Google Java Format)
./gradlew spotlessCheck

# Automatically format code
./gradlew spotlessApply
```

---

## 🧪 Predefined Edge-Case Verification Suite

After starting the application (`docker compose up db app -d`) and running `bash scripts/seed-data.sh`, the following pre-seeded products are available:
- **Product 1** (`01952e42-7a57-7000-8000-000000000001` - *Zapatillas Running Pro*): 48 EUR prices, 36 USD prices (enables multi-page pagination testing).
- **Product 2** (`01952e42-7a57-7000-8000-000000000002` - *Camiseta DryFit*): Has an active open-ended price (`endDate: null`).
- **Product 3** (`01952e42-7a57-7000-8000-000000000003` - *Mochila Senderismo*): Fresh product with zero prices.

You can execute these predefined requests in **Swagger UI** or copy-paste them directly into your terminal:

---

### 1. Endpoint: `POST /products` (Product Creation)

#### ✅ Happy Path: Create product with auto-generated UUIDv7
```bash
curl -i -X POST http://localhost:8080/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Gorra Deportiva","description":"Protección UV edición 2026"}'
# Expected: 201 Created with Location header and generated UUIDv7
```

#### ❌ Edge Case: Reject blank name
```bash
curl -i -X POST http://localhost:8080/products \
  -H "Content-Type: application/json" \
  -d '{"name":"   ","description":"Descripción válida"}'
# Expected: 400 Bad Request (Detail: "Name cannot be blank")
```

#### ❌ Edge Case: Reject duplicate product name (case-insensitive)
```bash
curl -i -X POST http://localhost:8080/products \
  -H "Content-Type: application/json" \
  -d '{"name":"gorra deportiva","description":"Variación en minúsculas"}'
# Expected: 409 Conflict (Detail: "A product with the name 'gorra deportiva' already exists", conflictingProductId: "<existing-uuid>")
```

#### ❌ Edge Case: Reject duplicate client-specified UUIDv7
```bash
curl -i -X POST http://localhost:8080/products \
  -H "Content-Type: application/json" \
  -d '{"id":"01952e42-7a57-7000-8000-000000000001","name":"Duplicado","description":"Test"}'
# Expected: 409 Conflict (Product already exists)
```

---

### 2. Endpoint: `POST /products/{id}/prices` (Sequential Price Registration)

#### ✅ Happy Path: Add valid sequential price on Product 1 (after last endDate 2026-12-31)
```bash
curl -i -X POST http://localhost:8080/products/01952e42-7a57-7000-8000-000000000001/prices \
  -H "Content-Type: application/json" \
  -d '{"value":229.99,"currency":"EUR","initDate":"2027-01-01","endDate":"2027-12-31"}'
# Expected: 201 Created with Location header
```

#### ❌ Edge Case: Reject creation when previous price is open-ended (Product 2)
```bash
curl -i -X POST http://localhost:8080/products/01952e42-7a57-7000-8000-000000000002/prices \
  -H "Content-Type: application/json" \
  -d '{"value":59.99,"currency":"EUR","initDate":"2027-01-01","endDate":"2027-12-31"}'
# Expected: 400 Bad Request (Detail: "Cannot add a new price while the latest price has an open-ended validity period")
```

#### ❌ Edge Case: Reject non-sequential / past overlapping interval on Product 1
```bash
curl -i -X POST http://localhost:8080/products/01952e42-7a57-7000-8000-000000000001/prices \
  -H "Content-Type: application/json" \
  -d '{"value":99.99,"currency":"EUR","initDate":"2024-03-01","endDate":"2024-05-01"}'
# Expected: 400 Bad Request (Detail: "New price init date must be strictly after the latest price end date")
```

---

### 3. Endpoint: `GET /products/{id}/prices?date=...` (Active Price Resolution)

#### ✅ Happy Path: Query active price on historical date in EUR (Product 1)
```bash
curl -i "http://localhost:8080/products/01952e42-7a57-7000-8000-000000000001/prices?date=2024-04-15&currency=EUR"
# Expected: 200 OK -> {"value": 99.99, "currency": "EUR"}
```

#### ✅ Happy Path: Query active price on multi-currency USD (Product 1)
```bash
curl -i "http://localhost:8080/products/01952e42-7a57-7000-8000-000000000001/prices?date=2024-04-15&currency=USD"
# Expected: 200 OK -> {"value": 109.99, "currency": "USD"}
```

#### ✅ Happy Path: Query open-ended active price (Product 2)
```bash
curl -i "http://localhost:8080/products/01952e42-7a57-7000-8000-000000000002/prices?date=2026-08-29&currency=EUR"
# Expected: 200 OK -> {"value": 49.99, "currency": "EUR"}
```

#### ❌ Edge Case: Date with no active price configured
```bash
curl -i "http://localhost:8080/products/01952e42-7a57-7000-8000-000000000001/prices?date=2020-01-01&currency=EUR"
# Expected: 404 Not Found
```

#### ❌ Edge Case: Malformed date format
```bash
curl -i "http://localhost:8080/products/01952e42-7a57-7000-8000-000000000001/prices?date=2024/13/45"
# Expected: 400 Bad Request
```

---

### 4. Endpoint: `PUT /products/{id}/prices/{priceId}` (Active Price Updating)

#### ❌ Edge Case: Reject updating past / non-active historical price
```bash
# Attempt to update Product 1's 2024 price (assuming today is in 2025/2026)
curl -i -X PUT http://localhost:8080/products/01952e42-7a57-7000-8000-000000000001/prices/01952e42-7a57-7000-8000-000000000011 \
  -H "Content-Type: application/json" \
  -d '{"value":119.99,"currency":"EUR","initDate":"2024-01-01","endDate":"2024-06-30"}'
# Expected: 400 Bad Request or 404 (Detail: "Only currently active prices can be updated")
```

---

### 5. Endpoint: `GET /products/{id}/prices` (Price History & Keyset Pagination)

#### ✅ Happy Path: Fetch chronological history ordered newest first (DESC)
```bash
curl -i "http://localhost:8080/products/01952e42-7a57-7000-8000-000000000001/prices?currency=EUR&pageSize=20&sortOrder=DESC"
# Expected: 200 OK with envelope containing 20 items (sorted DESC by initDate), "next" URL with cursor to page 2, and "previous": null
```

#### ✅ Happy Path: Fetch chronological history ordered oldest first (ASC)
```bash
curl -i "http://localhost:8080/products/01952e42-7a57-7000-8000-000000000001/prices?currency=EUR&pageSize=20&sortOrder=ASC"
# Expected: 200 OK with envelope containing 20 items (sorted ASC by initDate), "next" URL with cursor to page 2, and "previous": null
```

#### ❌ Edge Case: Reject invalid Base64 pagination cursor
```bash
curl -i "http://localhost:8080/products/01952e42-7a57-7000-8000-000000000001/prices?cursor=not-a-valid-base64!"
# Expected: 400 Bad Request
```

#### ❌ Edge Case: Reject oversized page size (> 100)
```bash
curl -i "http://localhost:8080/products/01952e42-7a57-7000-8000-000000000001/prices?pageSize=101"
# Expected: 400 Bad Request (Detail: "PageSize must not exceed 100")
```

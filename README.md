# 🛍️ High-Performance Multi-Currency Product & Historical Pricing Engine

A production-grade, high-throughput Hexagonal API for Product Management and Dynamic Multi-Currency Historical Price Resolution, engineered with **Java 25 (Virtual Threads)**, **Spring Boot 4.1**, **GraalVM Native Image**, and **PostgreSQL 17/18** with `btree_gist` temporal exclusion constraints.

---

## ⚡ Tech Stack & Architecture Highlights

- **Language & Runtime**: Java 25 (Virtual Threads / Project Loom), GraalVM Native Image (SubstrateVM).
- **Framework**: Spring Boot 4.1 (Native AOT compilation, Spring JDBC `NamedParameterJdbcTemplate`).
- **Database & Integrity**: PostgreSQL 17/18 with `btree_gist` temporal composite exclusion constraints for zero-race-condition price intervals.
- **Architecture**: Pure Hexagonal Architecture (Ports & Adapters) organized by feature slice.
- **Testing**: Inside-Out TDD, MockMvc acceptance tests backed by Testcontainers PostgreSQL, whole-object unit tests.

### 📂 Architecture Decision Records (ADRs) & Documentation

For detailed architectural justifications and technical decisions, refer to:

- [ADR-0001: Hexagonal Architecture & Package Structure](docs/adr/0001-hexagonal-architecture-and-package-structure.md)
- [ADR-0002: Testing Strategy & Inside-Out TDD State Machine](docs/adr/0002-testing-strategy-and-tdd-state-machine.md)
- [ADR-0003: Temporal Modeling & PostgreSQL Range Containment](docs/adr/0003-temporal-modeling-and-timezone-architecture.md)
- [ADR-0043: Multi-Currency Discrete Pricing & Money Value Object](docs/adr/ADR-0043-multi-currency-discrete-pricing.md)
- [Docker Architecture & Multi-Arch Guide](docs/docker-architecture.md)
- [OpenAPI 3.1 Specification](docs/openapi.yaml)
- [Original Technical Challenge Instructions](INSTRUCTIONS.md)

---

## 🚀 How to Run It

### 1. Host Prerequisites & Required Tech Stack

To run and test the application, ensure the following software is installed on your host system:

| Requirement | Minimum Version | Purpose |
| :--- | :--- | :--- |
| **Docker & Docker Compose** | Docker 24+ / Compose v2 | Runs database, app, and high-concurrency benchmark containers |
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

#### Installing & Updating Missing Dependencies

<details>
<summary><b>macOS (Homebrew)</b></summary>

```bash
# Install Docker Desktop and OpenJDK 25
brew install --cask docker
brew install openjdk@25

# Link OpenJDK 25 and configure JAVA_HOME in ~/.zshrc
export JAVA_HOME="/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
```
</details>

<details>
<summary><b>Linux (Ubuntu/Debian) & Windows WSL2</b></summary>

```bash
# Install Docker and Compose plugin
sudo apt update && sudo apt install -y curl git docker.io docker-compose-v2
sudo usermod -aG docker $USER

# Install Java 25 via SDKMAN
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 25-open
```
</details>

<details>
<summary><b>Windows (Native via Winget / Chocolatey)</b></summary>

```powershell
# Install Docker Desktop and Oracle/Eclipse Temurin JDK 25
winget install Docker.DockerDesktop
winget install Oracle.JDK.25
```
</details>

---

### 3. Docker Compose Execution (GraalVM Native Image)

The system uses Ahead-Of-Time (AOT) GraalVM compilation. **Build the container image once**, and then run subsequent executions instantly (~50ms startup time, ~30MB memory).

#### Build the Native Image (Only Once)
```bash
# Builds the optimized GraalVM native binary inside Docker
docker compose build
```

#### Run Full Stack + Automated Concurrency Benchmark
```bash
# Starts PostgreSQL (db), Spring Boot Native API (app), and load test (benchmark)
docker compose up --abort-on-container-exit
```

#### Run App & Database Only (Background / Development Mode)
```bash
# Starts db and app in the background
docker compose up db app -d

# View live application logs
docker compose logs -f app
```

#### Stop All Services
```bash
docker compose down -v
```

---

### 4. Local Development & Automated Tests (Gradle)

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

## 📘 REST API Reference Summary

Full OpenAPI 3.1 specification available in [`docs/openapi.yaml`](docs/openapi.yaml).

### 1. Create a Product
- **Endpoint**: `POST /products`
- **Request Body**:
  ```json
  {
    "name": "Zapatillas deportivas",
    "description": "Modelo 2025 edición limitada"
  }
  ```
  *(Optional: `"id": "01952e42-7a57-7000-8000-000000000001"` for client-side UUIDv7 idempotency)*
- **Response**: `201 Created`
  - **Header**: `Location: /products/01952e42-7a57-7000-8000-000000000001`
  - **Body**:
    ```json
    {
      "id": "01952e42-7a57-7000-8000-000000000001",
      "name": "Zapatillas deportivas",
      "description": "Modelo 2025 edición limitada"
    }
    ```

---

### 2. Add a Price to a Product
- **Endpoint**: `POST /products/{id}/prices`
- **Request Body**:
  ```json
  {
    "value": 99.99,
    "currency": "EUR",
    "initDate": "2024-01-01",
    "endDate": "2024-06-30"
  }
  ```
- **Response**: `201 Created` (`Location: /products/{id}/prices/{priceId}`)

---

### 3. Get Active Price on Date
- **Endpoint**: `GET /products/{id}/prices?date=2024-03-15&currency=EUR`
- **Response**: `200 OK`
  ```json
  {
    "value": 99.99,
    "currency": "EUR"
  }
  ```

---

### 4. Get Complete Price History
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

# Docker Architecture & Execution Workflow

This document explains the Docker configuration supporting **GraalVM Native Image** compilation (Java 25 baseline), seamless **Multi-Architecture** execution (ARM64 & AMD64), and our dual-compose testing vs runtime separation.

---

## 1. `Dockerfile` (Spring Boot Application)

The primary Dockerfile uses a **Multi-Stage GraalVM Native Image Build**:

* **Stage 1: The Builder (`ghcr.io/graalvm/native-image-community:25`)**
  * Utilizes the official GraalVM Community 25 image with Gradle 9.1.0.
  * Ahead-Of-Time (AOT) compiler compiles bytecode into a standalone machine-code binary (`/app/build/native/nativeCompile/app`).
* **Stage 2: The Runtime (`ubuntu:22.04`)**
  * Standalone native binary runs without a JVM, reducing runtime memory footprint to ~30-50MB and achieving sub-50ms cold startup times.
* **Multi-Arch Support**: Docker automatically builds and runs on the native architecture of the host (Apple Silicon ARM64, Linux ARM64/x86_64, Windows WSL2) without emulation overhead.

---

## 2. GraalVM Build Lifecycle: Build Once, Run Blazingly Fast

* **Build Phase (Once)**: Native Image compilation is CPU/memory-intensive and takes ~2-3 minutes during the initial image build (`docker compose build` or `docker compose up --build`).
* **Execution Phase (Subsequent Runs)**: Once the `product-api:latest` Docker image is built, all subsequent runs (`docker compose up`) launch instantly without rebuilding.

---

## 3. Docker Compose Strategy

### `docker-compose.yml` (Standard Runtime & Benchmark)
Serves as the root compose configuration for evaluating and running the full system:
* `db`: PostgreSQL 18 with built-in `pg_isready` health check.
* `app` (`product-api`): GraalVM Native Image Spring Boot service, starts once `db` is healthy.
* `benchmark` (`product-benchmark`): Automated high-concurrency load testing container.

### `docker-compose.test.yml` (Integration Test Harness)
Dedicated minimal compose file containing only the isolated `db` container used by Testcontainers (`DockerComposeHelper`) during `./gradlew integrationTest`.

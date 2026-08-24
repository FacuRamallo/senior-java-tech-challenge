# Docker Architecture & Multi-Architecture Support

This document explains the structural changes made to the Docker configuration to support a **GraalVM Native Image** build (Java 25 baseline) and ensure flawless **Multi-Architecture** (Apple Silicon ARM64 + Intel AMD64) execution.

---

## 1. `Dockerfile` (Spring Boot Application)

The primary Dockerfile is a **Multi-Stage GraalVM Native Image Build**:

* **Stage 1: The Builder (`ghcr.io/graalvm/native-image-community:25`)**
  * Utilizes the official GraalVM Community 25 image with Gradle 9.1.0.
  * The build command is `./gradlew nativeCompile --no-daemon -x test -x integrationTest`. This triggers Spring Boot 4's Ahead-Of-Time (AOT) engine (`processAot`) to analyze the code and compile it into a standalone machine-code executable (`/app/build/native/nativeCompile/app`).
* **Stage 2: The Runtime (`ubuntu:22.04`)**
  * Because a GraalVM native image does not require a JVM to run, the runtime base is a clean Ubuntu image. This drastically shrinks the container memory footprint (~30-50MB vs ~400MB+ for standard JVM) and achieves sub-200ms cold startup times.
  * The `ENTRYPOINT` executes the native binary directly (`["/app/app"]`).
* **Multi-Arch Support:** Both the GraalVM builder and the Ubuntu runtime natively publish manifests for `linux/amd64` and `linux/arm64`. By omitting hardcoded `--platform` flags, Docker automatically builds and runs on the native architecture of the host without Rosetta/QEMU emulation.

---

## 2. `Dockerfile.benchmark` (Performance & Health Test Script)

The benchmark container is optimized for maximum efficiency and low overhead:

* **Alpine Linux (`alpine:3.19`):** Lightweight base image (~5MB) with instant startup.
* **Package Manager:** Installs dependencies via `apk add --no-cache curl jq bc`.
* **Multi-Arch Support:** Alpine executes natively on both Apple Silicon (M-series ARM64) and Intel/AMD architectures without emulation overhead.

---

## 3. Ephemeral Dual-Target Compose Setup

* **`docker-compose.yml`**: Contains only the stateless `db` service (`postgres:18-alpine`), allowing Testcontainers (`ComposeContainer`) to run integration tests against clean ephemeral containers.
* **`docker-compose.override.yml`**: Adds `app` (`product-api`) and `benchmark` (`product-benchmark`) services for full evaluator runs via `docker compose up --build`.

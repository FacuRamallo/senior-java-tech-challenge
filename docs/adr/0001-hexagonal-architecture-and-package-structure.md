# 1. Hexagonal Architecture and Feature-Oriented Package Structure

Date: 2026-08-24

## Status

Accepted

## Context

The system manages products and historical price lifecycles requiring high throughput, low latency, robust business rule validation (e.g. non-overlapping date intervals), and long-term maintainability. 

Traditional layered architectures (controller -> service -> dao/entity) often lead to an anemic domain model coupled directly to database frameworks (JPA/Hibernate) or web frameworks. To ensure domain logic remains pure, isolated, testable, and independent of framework lifecycles, we require a clean Hexagonal Architecture (Ports & Adapters) with a clear, predictable package structure.

## Decision

We adopt a **Hexagonal Architecture** (`com.mango.products`). The module is structured into three clean layers:

1. **`domain`**:
   - Contains pure domain entities, aggregates, value objects, domain exceptions, and domain events.
   - Contains all **Interfaces / Ports** (`ProductRepository`, `PriceRepository`, `IdGenerator`).
   - **Constraint:** 100% pure Java. Zero external third-party or framework dependencies.
2. **`application`**:
   - Contains Use Cases coordinating business flows, invoking domain models, and driving port interfaces.
   - Contains application-level input/output records.
3. **`infrastructure`**:
   - Organized by adapter responsibility:
     - `controller`: REST Controllers, Web DTOs, request validation, RFC 9457 exception mappers.
     - `repository`: PostgreSQL JDBC repositories implementing domain repository ports.
     - `service`: Infrastructure services implementing domain ports (e.g. `UuidV7IdGenerator`).
     - `configuration`: Spring configuration classes (`@Configuration`, bean definitions).
     - `ProductsApplication`: Application entry point.

```
com.mango.products/
├── domain/                      # Aggregates, Value Objects, Domain Exceptions, Port Interfaces
├── application/                 # Use Cases & Application Orchestration
└── infrastructure/              # Adapters, Spring Config, Bootstrapping
    ├── controller/              # REST Controllers, DTOs, Exception Handlers
    ├── repository/              # PostgreSQL JDBC Repositories
    ├── service/                 # Outbound Port Adapters (IdGenerator, etc.)
    └── configuration/           # Spring Configurations
```

## Consequences

### Positive
- **Domain Purity:** Domain logic is 100% framework-agnostic and immune to Spring/database upgrade churn.
- **High Cohesion:** Feature packaging keeps related business logic, use cases, and adapters co-located.
- **Clear Boundaries:** Port interfaces living in `domain` enforce the Dependency Inversion Principle (DIP) naturally.
- **Fast Testability:** Domain models and application use cases can be tested without booting Spring contexts.

### Negative / Trade-offs
- Requires explicit mapping between web DTOs, application models, domain models, and database rows.
- Strict subpackage enforcement requires discipline when adding new components.

# 1. Hexagonal Architecture and Feature-Oriented Package Structure

Date: 2026-08-24

## Status

Accepted

## Context

The system manages products and historical price lifecycles requiring high throughput, low latency, robust business rule validation (e.g. non-overlapping date intervals), and long-term maintainability. 

Traditional layered architectures (controller -> service -> dao/entity) often lead to an anemic domain model coupled directly to database frameworks (JPA/Hibernate) or web frameworks. To ensure domain logic remains pure, isolated, testable, and independent of framework lifecycles, we require a clean Hexagonal Architecture (Ports & Adapters) with a clear, predictable package structure.

## Decision

We adopt a **Feature-Oriented Hexagonal Architecture** (`com.mango.products.<feature>`). Every feature module is strictly restricted to three sub-packages:

1. **`domain`**:
   - Contains pure domain entities, aggregates, value objects, domain exceptions, and domain events.
   - Contains all **Interfaces / Ports** (contracts for persistence, external systems, and domain gateways).
   - **Constraint:** 100% pure Java. Zero framework annotations (no Spring, no JPA, no Jackson annotations).
2. **`application`**:
   - Contains Use Cases coordinating business flows, invoking domain models, and driving port interfaces.
   - Contains application-level input/output records.
3. **`infrastructure`**:
   - Contains all adapter implementations: REST Controllers, Web DTOs, request validation, RFC 9457 exception mappers.
   - Contains database repository implementations (e.g., Spring Data JDBC / JdbcTemplate) implementing domain port interfaces.
   - Contains all Spring configuration classes (`@Configuration`, beans).

```
com.mango.products.<feature>/
├── domain/                      # Aggregates, Value Objects, Domain Exceptions, Port Interfaces
├── application/                 # Use Cases & Application Orchestration
└── infrastructure/              # Controllers, Web DTOs, Spring Configurations, JDBC Repositories
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

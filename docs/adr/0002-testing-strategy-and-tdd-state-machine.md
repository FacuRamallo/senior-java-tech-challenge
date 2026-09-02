# 2. Use Case-Centric Testing Strategy and Inside-Out TDD Flow

Date: 2026-08-24

## Status

Accepted

## Context

High-coverage test suites often suffer from two major anti-patterns:
1. **Over-mocking and Implementation Coupling:** Tests coupled to internal class structure or fine-grained private/package helper methods create high resistance to refactoring.
2. **Context-Heavy Slow Tests:** Excessive reliance on `@SpringBootTest` slows down test execution loops and discourages continuous TDD.

We need a test suite strategy that provides maximum behavioral confidence with minimal maintenance overhead, coupled with a predictable, efficient TDD development flow.

## Decision

We establish a **Use-Case Centric Testing Strategy** and an **Inside-Out TDD Flow**:

### 1. The Test Hierarchy
- **Use Case Acceptance Tests (Happy Path):** Exactly one acceptance test per feature/use case (`[Feature]Feature`). Verifies the primary happy path end-to-end against outer adapters and ephemeral Testcontainers infrastructure.
- **Use Case Unit Tests (Failure Modes & Domain Invariants):** Tests edge cases, validation rules, and domain invariants by invoking the Use Case boundary (`[Target]Should`) with parameters designed to trigger those business rules.
- **Standalone Domain / Infrastructure Unit Tests:** Prohibited without explicit, justifiable architectural value. Domain invariants and validation rules must be exercised through the Use Case boundary by default. Standalone unit tests are reserved strictly for components or domain aggregates with high internal algorithmic complexity that cannot be effectively exercised through the Use Case boundary alone.
- **Whole-Object Assertions:** Enforces complete object comparison (`assertThat(actual).usingRecursiveComparison().isEqualTo(expected)`) rather than brittle single-field assertions.
- **Read-Mode Controller Unit Tests:** Consolidated into a single `[Controller]Should` class rather than fragmented into helper test suites.

### 2. The Inside-Out TDD Flow
- **Inside-Out Approach:** Business logic develops from core domain aggregates and invariants outwards towards application use cases and external infrastructure adapters.
- **Phase 1 (RED):** Write the unit/acceptance test defining the behavioral contract along with minimal compiling dummy signatures, executing the test to prove genuine failure.
- **Phase 2 (GREEN):** Implement the minimal domain and application logic to satisfy the test contract.
- **Phase 3 (REFACTOR):** Eliminate duplication, ensure zero framework dependencies in `domain`, run the full test suite, and enforce formatting standards.

## Consequences

### Positive
- **Refactoring Resilience:** Tests assert public behavioral outcomes and side-effects at the Use Case boundary, allowing domain models to be refactored freely without breaking tests.
- **Rapid Feedback:** Pure Java unit tests and lightweight mocks run in milliseconds without Spring Boot startup overhead.
- **Reduced Test Duplication & Maintenance:** Eliminates redundant testing across multiple internal layers.

### Negative / Trade-offs
- Requires developer discipline to focus testing on the Use Case boundary rather than creating redundant unit tests for trivial internal helper classes.

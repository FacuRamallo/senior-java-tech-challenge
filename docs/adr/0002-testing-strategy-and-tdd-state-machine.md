# 2. Use Case Testing Strategy and Inside-Out TDD State Machine

Date: 2026-08-24

## Status

Accepted

## Context

High-coverage test suites often suffer from two major anti-patterns:
1. **Over-mocking and Implementation Coupling:** Tests coupled to internal class structure or fine-grained private/package methods create high resistance to refactoring.
2. **Context-Heavy Slow Tests:** Excessive reliance on `@SpringBootTest` slows down test execution loops and discourages continuous TDD.

We need a test suite strategy that provides maximum behavioral confidence with minimal maintenance overhead, coupled with an explicit, predictable TDD development state machine.

## Decision

We establish a **Use-Case Centric Testing Strategy** and a strict **Inside-Out TDD State Machine**:

### 1. The Test Hierarchy
- **Use Case Acceptance Tests (Happy Path):** Exactly one test per Use Case class. Executes the primary entry point, mocking only external infrastructure ports (deterministic and fast, no full Spring contexts).
- **Use Case Unit Tests (Failure Modes & Domain Invariants):** Tests edge cases, validation, and domain rule violations by invoking the Use Case with parameters designed to trigger those invariants.
- **Standalone Domain / Infrastructure Unit Tests:** Strictly locked behind an explicit developer authorization gate (*"Can I write a dedicated domain unit test for [AggregateName]?"*).
- **Whole-Object Assertions:** Enforces complete object comparison (`assertThat(actual).usingRecursiveComparison().isEqualTo(expected)`) rather than brittle single-field assertions.

### 2. The Strict Inside-Out TDD State Machine
- **Inside-Out Approach:** Business logic develops from core domain aggregates outwards towards application use cases and outer infrastructure adapters.
- **Step 1 (RED):** Write JUnit 5 test, minimal dummy implementation, execute test to prove genuine failure.
- **Human Gateway Gate:** The agent must pause and ask: *"Does this Aggregate boundary and behavioral contract look correct?"* before writing actual domain logic.
- **Step 2 (GREEN):** Minimal domain logic to pass.
- **Step 3 (REFACTOR):** Eliminate duplication, verify zero framework dependencies in `domain`, run full test suite, format via Spotless.

## Consequences

### Positive
- **Refactoring Resilience:** Tests assert public behavioral outcomes and side-effects at the Use Case boundary, allowing domain models to be refactored freely without breaking tests.
- **Rapid Feedback:** Pure Java and lightweight mock tests run in milliseconds without Spring Boot startup overhead.
- **High Intentionality:** The Human Gateway guarantees alignment on aggregate boundaries and domain contracts before implementation begins.
- **Reduced Test Duplication:** Eliminates redundant testing across multiple layers.

### Negative / Trade-offs
- Developers and agents must strictly resist the urge to write standalone unit tests for every internal domain helper class unless explicitly authorized.

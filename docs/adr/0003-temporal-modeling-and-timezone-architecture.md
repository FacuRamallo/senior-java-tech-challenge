# 3. Temporal Modeling & Timezone Architecture

Date: 2026-08-25

## Status

Accepted

## Context

Temporal bugs are among the most persistent and subtle failures in distributed backend systems. Unchecked implicit conversions, reliance on system default timezones, mutable legacy types (`java.util.Date`, `java.util.Calendar`), and conflating instant-in-time points with human-scheduled wall-clock times or civil calendar dates cause data corruption, test flakiness, and silent Daylight Saving Time (DST) shift bugs.

This standard defines the architectural rules and patterns for modeling, persisting, serializing, and testing timestamps and calendar dates across all system components.

---

## Decision Matrix: Temporal Type Selection

All domain models must strictly use immutable `java.time` types (JSR-310). Legacy types (`Date`, `Calendar`, `java.sql.Timestamp`) are strictly prohibited.

| Java Type | Semantics | Target Use Case | Database Mapping (PostgreSQL/SQL Standard) |
| :--- | :--- | :--- | :--- |
| **`Instant`** | Universal timeline point (UTC epoch offset). | Audit timestamps (`created_at`, `updated_at`), event logs, cache TTLs, telemetry. | `TIMESTAMPTZ` / `TIMESTAMP WITH TIME ZONE` |
| **`OffsetDateTime`** | Point on timeline with fixed numerical offset (e.g., `+02:00`). | REST/HTTP API contracts (ISO-8601), wire payloads where original offset must be retained. | `TIMESTAMPTZ` |
| **`ZonedDateTime`** | Point on timeline with dynamic IANA zone rules (handles DST transitions). | Future user-facing scheduling, calendar alerts. | Composite: `TIMESTAMP` (local) + `VARCHAR` (Zone ID) |
| **`LocalDateTime`** | Wall-clock time without offset or zone context. | Zone-agnostic templates, recurring alarm definitions. | `TIMESTAMP WITHOUT TIME ZONE` |
| **`LocalDate` / `LocalTime`** | Pure civil date (day) or pure civil time. | Price validity dates (`initDate`, `endDate`), billing cycles, operating dates. | `DATE` |

---

## Core Architectural Directives

### Directive 1: Timezone Normalization at System Boundaries
* **The Boundary Principle:** Normalize to UTC as early as possible on ingress. Persist exclusively in UTC for timestamps. Localize only at the final presentation layer or client contract boundary.
* **Database Layer:** Store timestamps in UTC (`TIMESTAMPTZ`). Store civil dates as `DATE`. Avoid implicit host timezone dependencies in JDBC drivers.
* **Application Layer:** Domain entities and business logic operate on `Instant` for timeline points or `LocalDate` for civil calendar dates.

---

### Directive 2: Injected `Clock` for Total Determinism
Direct calls to static time-providers like `Instant.now()`, `System.currentTimeMillis()`, or `LocalDate.now()` are prohibited in domain and use case logic. A `java.time.Clock` bean must be injected into services to enable deterministic unit and integration testing.

```java
@Configuration
public class TimeConfiguration {

  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }
}
```

---

### Directive 3: Civil Date Ranges & Database Exclusion
For domain models where validity is defined across calendar dates (such as historical product pricing intervals `initDate` to `endDate`), civil dates (`LocalDate`) map to PostgreSQL `DATE`. Overlap prevention is enforced via PostgreSQL GiST exclusion constraints (`EXCLUDE USING gist (product_id WITH =, daterange(init_date, coalesce(end_date, 'infinity'), '[]') WITH &&)`).

---

## Serialization & Framework Standards

API responses must serialize temporal values according to ISO-8601 (RFC 3339). In Spring Boot 4.1.0 (with Jackson 3), Java 25 `java.time` types are serialized natively to ISO-8601 strings.

### Standard Output Formats
* **`Instant`**: `"2026-08-25T08:30:00Z"`
* **`OffsetDateTime`**: `"2026-08-25T10:30:00+02:00"`
* **`LocalDate`**: `"2026-08-25"`

---

## Anti-Patterns vs. Required Standards

| Banned Anti-Pattern | Mandatory Standard |
| :--- | :--- |
| Calling `System.currentTimeMillis()` for duration measurement. | Use `System.nanoTime()` or monotonic timers for elapsed durations. |
| Using `LocalDateTime` for audit timestamps. | Use `Instant` (unambiguous UTC timestamp). |
| Storing 3-letter abbreviation timezones (e.g. `CST`). | Validate and store canonical IANA identifiers (e.g. `Europe/Madrid`). |
| Relying on JVM default timezone (`ZoneId.systemDefault()`). | Explicitly configure UTC or pass contextual `ZoneId`. |
| Manual arithmetic with raw millisecond primitives (`long`). | Use `Duration` and `Period` for domain-safe temporal arithmetic. |

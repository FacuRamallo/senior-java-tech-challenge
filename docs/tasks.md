# Project Setup & Baseline
- `[x]` Gradle Kotlin DSL build setup (`build.gradle.kts`) with Java 25 & Spring Boot 4.1.0
- `[x]` Docker Compose setup (`docker-compose.yml` for runtime & benchmark, `docker-compose.test.yml` for tests)
- `[x]` Multi-stage GraalVM Native Image `Dockerfile`
- `[x]` Two test source sets (`test` and `integrationTest`) with `IntegrationTestSuite`
- `[x]` ADRs and `.agents/rules/` architecture governance documentation (local only, ignored in git)
- `[x]` Spotless Google Java Format 1.30.0 & JaCoCo configuration with automatic report generation

# Feature 1: Create a Product (POST /products)
- `[x]` Domain: `Product`, `Id` (UUIDv7), `Name`, `Description`, `ProductRepository`
- `[x]` Application: `CreateProductCommand`, `CreateProductUseCase` (tested in `CreateProductUseCaseShould`)
- `[x]` Infrastructure: `V0001__create_products_table.sql`, `PostgreSqlProductRepository` (`NamedParameterJdbcTemplate`), `CreateProductController`, `ProductsExceptionHandler`
- `[x]` Acceptance Test: `CreateProductFeature` (single happy-path test)
- `[x]` Documentation: `docs/openapi.yaml` documenting `POST /products`

# Feature 2: Add a Price to a Product (POST /products/{id}/prices)
- `[x]` **RED**: Define `Price` / `Money` (normalized scale, ISO-4217 currency) / `ValidityPeriod` domain model and failing test
- `[x]` **GATEWAY**: Obtain human approval for `Price` domain boundary
- `[x]` **GREEN**: Implement `Money` value object with validation and currency scale normalization
- `[x]` **REFACTOR**: Domain purity and Spotless formatting
- `[x]` Implement `AddPriceToProductUseCase` and unit tests (`AddPriceToProductUseCaseShould`)
- `[x]` Database migration: `V0002__create_product_prices_table.sql` with `btree_gist` and composite `EXCLUDE` constraint on `(product_id, price_currency, validity_range)`
- `[x]` Implement `PostgreSqlPriceRepository` filtering and persisting discrete multi-currency price rows
- `[x]` Web layer: Update DTOs, Jackson config, `AddPriceController` (defaulting `currency` to `EUR`), and `@RestControllerAdvice` error mapping for PostgreSQL exclusion violations (`23P01`)
- `[x]` Acceptance & Integration Tests: `AddPriceFeature` asserting discrete multi-currency insertion and concurrent exclusion violations per currency
- `[x]` Documentation: Update `docs/openapi.yaml` with multi-currency `POST /products/{id}/prices`

# Feature 3: Get Active Price on Date (GET /products/{id}/prices?date=YYYY-MM-DD&currency=EUR)
- `[x]` Implement `GetActivePriceUseCase` and unit tests (`GetActivePriceUseCaseShould`) scoped by currency
- `[x]` Repository query in `PostgreSqlPriceRepository` filtering by `product_id`, `price_currency`, and `daterange @> :date::date`
- `[x]` Web layer: Implement `GetActivePriceController` with currency query param fallback to `EUR`
- `[x]` Acceptance & Integration Tests: `GetActivePriceFeature` for multi-currency resolution
- `[x]` Documentation: Update `docs/openapi.yaml` with `GET /products/{id}/prices?date=...&currency=...`

# Feature 4: Get Price History (GET /products/{id}/prices?currency=EUR)
- `[x]` Implement `GetPriceHistoryUseCase` and unit tests (`GetPriceHistoryUseCaseShould`)
- `[x]` Repository query in `PostgreSqlPriceRepository` for chronological price list (`ORDER BY init_date ASC`)
- `[x]` Web layer: Implement `GetPriceHistoryController` and `GetPriceHistoryResponse` DTO
- `[x]` Acceptance & Integration Tests: `GetPriceHistoryFeature` extending `IntegrationTestBase`
- `[x]` Documentation: Update `docs/openapi.yaml` with `GET /products/{id}/prices`

# Final Verification & Benchmark
- `[x]` Run full test suite: `./gradlew clean build` (unit + integration tests + JaCoCo)
- `[x]` Verify GraalVM Native Image build and benchmark via `docker-compose.yml`
- `[x]` Complete ADRs and update `README.md`

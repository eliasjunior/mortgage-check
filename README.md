# Mortgage API

A REST API for mortgage feasibility checks and interest rate lookups, built with Java 21 and Spring Boot 3.

---

## Introduction

This project was built with one guiding principle: **solve the problem clearly, nothing more**.

The spec asked for two endpoints — a rate lookup and a mortgage feasibility check. Rather than reaching for patterns or abstractions that weren't yet justified, every decision was made in service of the requirements at hand. The result is a codebase that a new engineer can read top to bottom in under an hour and confidently change.

**Readability and maintainability were treated as features.** The layering is deliberate — controllers handle HTTP, services own business logic, and the provider interface is the single seam where the data source can be swapped without touching anything else. `BigDecimal` was chosen over primitives because financial arithmetic demands it. Records signal immutability. Exceptions make failure paths explicit. Each choice has a clear reason behind it.

**We also documented what we did not do.** Knowing the boundaries of an MVP is as important as knowing what it does. The [Known Evolution Points](#known-evolution-points) section captures the assumptions made, the shortcuts taken consciously, and the exact touch points that would need to change as the product grows — from the monthly cost formula to authentication to resilience patterns. A future engineer should never have to guess why something was left a certain way.

---

## Quick Start

**Prerequisites:** Java 21, Maven 3.8+

```bash
# Run
mvn spring-boot:run

# Test
mvn test
```

The application starts on `http://localhost:8080`.
Health check: `http://localhost:8080/actuator/health`

---

## Test Coverage

JaCoCo is configured with an **80% minimum** on instructions and branches. The build fails if coverage drops below that threshold.

```bash
# Run tests and generate coverage report
mvn verify

# Open the HTML report in your browser
open target/site/jacoco/index.html
```

The report is generated at `target/site/jacoco/index.html` after every `mvn verify` run.

---

## Architecture

```
┌─────────────────────────────────────────────────┐
│                   Controllers                   │  ← HTTP layer, input validation
│  InterestRateController  MortgageController     │
└────────────────────┬────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────┐
│                    Services                     │  ← Domain logic, business rules
│  InterestRateService     MortgageService        │
└────────────────────┬────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────┐
│              MortgageRateProvider               │  ← Interface (first citizen)
│                  « interface »                  │
└────────────────────┬────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────┐
│         InMemoryMortgageRateProvider            │  ← Current implementation
└─────────────────────────────────────────────────┘
```

### Package Structure

```
com.mortgage
├── controller          # REST endpoints
├── service             # Business logic
├── provider            # Data source abstraction
│   ├── MortgageRateProvider.java          (interface)
│   └── InMemoryMortgageRateProvider.java  (implementation)
├── model               # Domain records
└── exception           # Exception types and global handler
```

### Key Design Principles

- **Provider as first-class citizen** — `MortgageRateProvider` is the central seam. Nothing above it knows where rates come from. Swap the implementation without touching anything else.
- **Interface-driven naming** — method names reflect the intent of their layer (`getRates()` at the provider, `findAllRates()` at the service).
- **`BigDecimal` for all monetary values** — `double`/`float` cannot represent decimal fractions exactly. `BigDecimal` gives exact arithmetic with controlled rounding, which is required for financial calculations.
- **Records for domain models** — immutable by default, no boilerplate, signal pure data.
- **Throw exception** — `findByMaturityPeriod()` throws `MaturityPeriodNotFoundException` rather than returning null. Failure paths are explicit and typed.

---

## API Reference

### `GET /api/interest-rates`

Returns mortgage interest rates loaded at application startup.

**Response `200 OK`**
```json
[
  { "maturityPeriod": 10, "interestRate": 3.50, "lastUpdate": "2024-03-13T10:00:00Z" },
  { "maturityPeriod": 15, "interestRate": 3.75, "lastUpdate": "2024-03-13T10:00:00Z" },
  { "maturityPeriod": 20, "interestRate": 4.00, "lastUpdate": "2024-03-13T10:00:00Z" },
  { "maturityPeriod": 25, "interestRate": 4.25, "lastUpdate": "2024-03-13T10:00:00Z" },
  { "maturityPeriod": 30, "interestRate": 4.50, "lastUpdate": "2024-03-13T10:00:00Z" }
]
```

---

### `POST /api/mortgage-check`

Checks whether a mortgage is feasible and returns the monthly costs.

**Request**
```json
{
  "income": 100000,
  "maturityPeriod": 30,
  "loanValue": 300000,
  "homeValue": 400000
}
```

**Response `200 OK`**
```json
{ "feasible": true,  "monthlyCosts": 1125.00 }
{ "feasible": false, "monthlyCosts": 0 }
```

**Error responses**

| Status | When |
|--------|------|
| `400 Bad Request` | Missing or invalid fields |
| `422 Unprocessable Entity` | No rate found for the given `maturityPeriod` |
| `404 Not Found` | Unknown endpoint |

All errors follow [RFC 7807 Problem Details](https://www.rfc-editor.org/rfc/rfc7807):
```json
{
  "title": "Maturity Period Not Found",
  "status": 422,
  "detail": "No interest rate found for maturity period: 99",
  "instance": "/api/mortgage-check",
  "timestamp": "2026-03-13T21:43:13Z"
}
```

---

## Business Rules

- A mortgage should not exceed **4 times the income**
- A mortgage should not exceed the **home value**

When a mortgage is not feasible, the API returns `200` with `feasible: false` and `monthlyCosts: 0`. Infeasibility is a valid business outcome, not an error.

---

## Deep Dive

### Extending the Provider

The `MortgageRateProvider` interface is the only thing that needs to change when rates come from an external source:

```java
@Component
public class ExternalMortgageRateProvider implements MortgageRateProvider {

    @Override
    public List<MortgageRate> getRates() {
        // call external service
    }
}
```

Remove `@Component` from `InMemoryMortgageRateProvider`. Nothing else changes.

When the external provider is expensive or unreliable, two patterns apply directly to the new implementation — no existing code is touched:

```java
@Cacheable("mortgage-rates")                          // avoid a call on every request
public List<MortgageRate> getRates() { ... }

@Scheduled(fixedRateString = "${mortgage.rates.cache.ttl:3600000}")
@CacheEvict(value = "mortgage-rates", allEntries = true)
public void evictCache() {}

@CircuitBreaker(name = "mortgageRates", fallbackMethod = "fallbackRates")  // handle downtime
public List<MortgageRate> getRates() { ... }
```

### Adding a Business Rule

All feasibility logic lives in `MortgageService.isFeasible()`:

```java
private boolean isFeasible(MortgageCheckRequest request) {
    boolean withinIncomeLimit = request.loanValue()
        .compareTo(request.income().multiply(MAX_INCOME_MULTIPLIER)) <= 0;
    boolean withinHomeValue = request.loanValue()
        .compareTo(request.homeValue()) <= 0;
    // add new rule here
    return withinIncomeLimit && withinHomeValue;
}
```

---

## Known Evolution Points

### Monthly Cost Calculation

The spec requires `monthlyCosts` but does not define how to calculate it. The current implementation uses simple monthly interest (`loanValue × interestRate / 100 / 12`). This is an assumption that should be clarified with the business. Only `MortgageService.calculateMonthlyCosts()` needs to change — nothing else is affected.

### DTO Layer

Domain models (`MortgageRate`, `MortgageCheckRequest`) are currently used directly as HTTP request/response objects. This is acceptable while the API surface and domain are identical.

Introduce a `controller/dto` package when:
- A domain field should not be exposed in the API (e.g. `providerRef`, `source`)
- The API needs to rename a field without changing the domain
- Multiple API versions need to coexist

### Resilience

When the in-memory provider is replaced with an external service, add `@Cacheable` and a circuit breaker (Resilience4j) directly to the new provider implementation.

### Security

The code itself has no injection, deserialization, or path traversal risks. The gaps are at the infrastructure/config layer, expected for an MVP without auth wired yet.

**High**
- **No authentication/authorization** — all endpoints are open. When auth lands, protect routes via Spring Security (`oauth2ResourceServer` + JWT). `POST /api/mortgage-check` requires write scope; `GET /api/interest-rates` read scope.
- **No rate limiting** — `POST /api/mortgage-check` is CPU work. Add throttling at the gateway or filter layer per client/IP to prevent DoS.
- **`RootController` leaks app version** — `GET /` returns the exact version to any unauthenticated caller. Remove or put behind auth.

**Medium**
- **No `@DecimalMax` on financial inputs** — `loanValue`, `homeValue`, `income` are only validated as `@Positive`. Add a sane business ceiling via `@DecimalMax`.
- **`maturityPeriod` has no `@Max`** — any positive integer passes validation and triggers a full list scan. Constrain to the real domain range (e.g. `@Min(1) @Max(30)`).
- **No CORS policy** — Spring Boot defaults to allowing all origins. Define an explicit `CorsConfigurer` unless this is a pure server-to-server API enforced at the network layer.
- **`metrics` actuator exposed in non-prod** — request rates and internal naming are visible. Restrict to internal network or require auth.

**Low**
- **No `consumes` on `POST`** — add `consumes = APPLICATION_JSON_VALUE` to make the contract explicit.
- **`info` actuator enabled in prod** — exposes app name and version. Remove or restrict to internal network.

**Auth skeleton (when ready)**
```java
@Bean
SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http
        .csrf(csrf -> csrf.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/actuator/**").hasRole("ADMIN")
            .requestMatchers(POST, "/api/mortgage-check").hasRole("USER")
            .requestMatchers(GET,  "/api/interest-rates").hasRole("USER")
            .anyRequest().authenticated()
        )
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
        .build();
}
```
